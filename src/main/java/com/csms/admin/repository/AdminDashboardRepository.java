package com.csms.admin.repository;

import com.csms.admin.dto.DashboardStatsDto;
import com.csms.common.repository.BaseRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AdminDashboardRepository extends BaseRepository {
    
    /**
     * 대시보드 통계 조회
     * SqlTemplate의 서브쿼리 파라미터 바인딩 문제를 피하기 위해 여러 개의 간단한 쿼리로 분리
     */
    public Future<DashboardStatsDto.StatsDto> getTotalStats(
        SqlClient client,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime todayStart,
        LocalDateTime todayEnd
    ) {
        log.debug("Executing getTotalStats - startDate={}, endDate={}, todayStart={}, todayEnd={}", 
            startDate, endDate, todayStart, todayEnd);
        
        // 여러 개의 간단한 쿼리로 분리하여 병렬 실행 (Future.all은 List를 받아야 함)
        List<Future<Object>> futures = List.of(
            // 범위 기간 통계
            getStatValue(client, 
                "SELECT COALESCE(SUM(mh.amount), 0) as value FROM mining_history mh WHERE mh.created_at >= :start_date AND mh.created_at <= :end_date",
                Map.of("start_date", startDate, "end_date", endDate),
                "total_mined"),
            // 오늘 통계들
            getStatValue(client,
                "SELECT COUNT(DISTINCT td.id) as value FROM token_deposits td WHERE td.created_at >= :today_start AND td.created_at <= :today_end",
                Map.of("today_start", todayStart, "today_end", todayEnd),
                "today_deposit_count"),
            getStatValue(client,
                "SELECT COUNT(DISTINCT pd.id) as value FROM payment_deposits pd WHERE pd.created_at >= :today_start AND pd.created_at <= :today_end",
                Map.of("today_start", todayStart, "today_end", todayEnd),
                "today_payment_count"),
            getStatValue(client,
                "SELECT COUNT(DISTINCT e.id) as value FROM exchanges e WHERE e.created_at >= :today_start AND e.created_at <= :today_end",
                Map.of("today_start", todayStart, "today_end", todayEnd),
                "today_exchange_count"),
            getStatValue(client,
                "SELECT COUNT(DISTINCT et.id) as value FROM external_transfers et WHERE et.created_at >= :today_start AND et.created_at <= :today_end",
                Map.of("today_start", todayStart, "today_end", todayEnd),
                "today_withdrawal_count"),
            // 전체 통계들 (파라미터 없음)
            getStatValue(client, "SELECT COUNT(DISTINCT td.id) as value FROM token_deposits td", Map.of(), "coin_deposit_count"),
            getStatValue(client, "SELECT COUNT(DISTINCT pd.id) as value FROM payment_deposits pd", Map.of(), "payment_deposit_count"),
            getStatValue(client, "SELECT COUNT(DISTINCT s.id) as value FROM swaps s", Map.of(), "swap_count"),
            getStatValue(client, "SELECT COUNT(DISTINCT e.id) as value FROM exchanges e", Map.of(), "exchange_count"),
            getStatValue(client, "SELECT COUNT(DISTINCT et.id) as value FROM external_transfers et", Map.of(), "withdrawal_count"),
            // 범위 기간 합계들
            getStatValue(client,
                "SELECT COALESCE(SUM(mh.amount), 0) as value FROM mining_history mh WHERE mh.created_at >= :start_date",
                Map.of("start_date", startDate),
                "total_mining_amount"),
            getStatValue(client,
                "SELECT COALESCE(SUM(pd.amount), 0) as value FROM payment_deposits pd WHERE pd.created_at >= :start_date",
                Map.of("start_date", startDate),
                "total_payment_amount"),
            getStatValue(client,
                "SELECT COALESCE(SUM(e.from_amount), 0) as value FROM exchanges e WHERE e.created_at >= :start_date",
                Map.of("start_date", startDate),
                "total_exchange_amount"),
            getStatValue(client,
                "SELECT COALESCE(SUM(et.amount), 0) as value FROM external_transfers et WHERE et.created_at >= :start_date",
                Map.of("start_date", startDate),
                "total_withdrawal_amount"),
            getStatValue(client,
                "SELECT COALESCE(SUM(et.fee), 0) as value FROM external_transfers et WHERE et.created_at >= :start_date",
                Map.of("start_date", startDate),
                "total_withdrawal_fee_revenue"),
            getStatValue(client,
                "SELECT COUNT(DISTINCT rr.id) as value FROM referral_relations rr WHERE rr.created_at >= :start_date AND rr.created_at <= :end_date",
                Map.of("start_date", startDate, "end_date", endDate),
                "referral_registration_count"),
            // 기타 통계들
            getStatValue(client, "SELECT COUNT(DISTINCT u.id) as value FROM users u", Map.of(), "total_users"),
            getStatValue(client, "SELECT COUNT(DISTINCT dm.user_id) as value FROM daily_mining dm WHERE dm.mining_date = CURRENT_DATE", Map.of(), "realtime_mining_users"),
            getStatValue(client,
                "SELECT COUNT(DISTINCT mh.user_id) as value FROM mining_history mh WHERE mh.type = 'BROADCAST_PROGRESS' AND mh.created_at >= :start_date",
                Map.of("start_date", startDate),
                "realtime_broadcasts"),
            getStatValue(client,
                "SELECT COUNT(DISTINCT mh.user_id) as value FROM mining_history mh WHERE mh.type = 'BROADCAST_WATCH' AND mh.created_at >= :start_date",
                Map.of("start_date", startDate),
                "realtime_listeners")
        );
        
        return Future.all(futures)
        .compose(results -> {
            // 결과를 Map으로 변환
            Map<String, Object> stats = new HashMap<>();
            String[] keys = {
                "total_mined", "today_deposit_count", "today_payment_count", "today_exchange_count", "today_withdrawal_count",
                "coin_deposit_count", "payment_deposit_count", "swap_count", "exchange_count", "withdrawal_count",
                "total_mining_amount", "total_payment_amount", "total_exchange_amount", "total_withdrawal_amount", "total_withdrawal_fee_revenue",
                "referral_registration_count", "total_users", "realtime_mining_users", "realtime_broadcasts", "realtime_listeners"
            };
            
            for (int i = 0; i < keys.length && i < results.size(); i++) {
                stats.put(keys[i], results.resultAt(i));
            }
            
            // DashboardStatsDto.StatsDto 생성
            DashboardStatsDto.StatsDto statsDto = DashboardStatsDto.StatsDto.builder()
                .totalIssuance(5000000000.0)
                .totalMined(getDoubleFromMap(stats, "total_mined"))
                .todayDepositCount(getIntegerFromMap(stats, "today_deposit_count"))
                .todayPaymentCount(getIntegerFromMap(stats, "today_payment_count"))
                .todayExchangeCount(getIntegerFromMap(stats, "today_exchange_count"))
                .todayWithdrawalCount(getIntegerFromMap(stats, "today_withdrawal_count"))
                .totalDeposit(getIntegerFromMap(stats, "coin_deposit_count"))
                .totalPayment(getIntegerFromMap(stats, "payment_deposit_count"))
                .totalExchange(getIntegerFromMap(stats, "exchange_count"))
                .totalWithdrawal(getIntegerFromMap(stats, "withdrawal_count"))
                .totalMiningAmount(getDoubleFromMap(stats, "total_mining_amount"))
                .totalPaymentAmount(getDoubleFromMap(stats, "total_payment_amount"))
                .totalExchangeAmount(getDoubleFromMap(stats, "total_exchange_amount"))
                .totalWithdrawalAmount(getDoubleFromMap(stats, "total_withdrawal_amount"))
                .totalWithdrawalFeeRevenue(getDoubleFromMap(stats, "total_withdrawal_fee_revenue"))
                .coinDepositCount(getIntegerFromMap(stats, "coin_deposit_count"))
                .paymentDepositCount(getIntegerFromMap(stats, "payment_deposit_count"))
                .swapCount(getIntegerFromMap(stats, "swap_count"))
                .exchangeCount(getIntegerFromMap(stats, "exchange_count"))
                .withdrawalCount(getIntegerFromMap(stats, "withdrawal_count"))
                .realtimeMiningUsers(getIntegerFromMap(stats, "realtime_mining_users"))
                .realtimeBroadcasts(getIntegerFromMap(stats, "realtime_broadcasts"))
                .realtimeListeners(getIntegerFromMap(stats, "realtime_listeners"))
                .referralRegistrationCount(getIntegerFromMap(stats, "referral_registration_count"))
                .totalUsers(getIntegerFromMap(stats, "total_users"))
                .build();
            
            log.debug("getTotalStats succeeded - totalMined: {}, totalUsers: {}", 
                statsDto.getTotalMined(), statsDto.getTotalUsers());
            
            return Future.succeededFuture(statsDto);
        });
    }
    
    /**
     * 차트 데이터 조회
     */
    public Future<List<DashboardStatsDto.ChartDataDto>> getChartData(
        SqlClient client,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        // ::date 대신 CAST 사용 (파라미터 파싱 문제 방지)
        String sql = """
            SELECT
                CAST(mh.created_at AS DATE) as date,
                COALESCE(SUM(mh.amount), 0) as total_mined,
                COUNT(DISTINCT mh.user_id) as user_count
            FROM mining_history mh
            WHERE mh.created_at >= :start_date AND mh.created_at <= :end_date
            GROUP BY CAST(mh.created_at AS DATE)
            ORDER BY date ASC
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("start_date", startDate);
        params.put("end_date", endDate);
        
        log.debug("Executing getChartData query with params: startDate={}, endDate={}", startDate, endDate);
        
        return query(client, sql, params)
            .map(rows -> {
                List<DashboardStatsDto.ChartDataDto> chartData = new ArrayList<>();
                for (var row : rows) {
                    chartData.add(DashboardStatsDto.ChartDataDto.builder()
                        .date(getLocalDate(row, "date") != null 
                            ? getLocalDate(row, "date").toString()
                            : null)
                        .totalMined(getDouble(row, "total_mined"))
                        .userCount(getInteger(row, "user_count"))
                        .build());
                }
                log.debug("getChartData succeeded - count: {}", chartData.size());
                return chartData;
            })
            .onFailure(err -> {
                log.error("getChartData failed", err);
            });
    }
    
    /**
     * 상위 회원 조회
     */
    public Future<List<DashboardStatsDto.TopMemberDto>> getTopMembers(SqlClient client) {
        String sql = """
            SELECT 
                u.id,
                u.login_id as nickname,
                COALESCE(SUM(mh.amount), 0) as mined_amount,
                ROW_NUMBER() OVER (ORDER BY SUM(mh.amount) DESC) as rank
            FROM users u
            LEFT JOIN mining_history mh ON mh.user_id = u.id
            GROUP BY u.id, u.login_id
            ORDER BY mined_amount DESC
            LIMIT 10
            """;
        
        log.debug("Executing getTopMembers query");
        
        return query(client, sql, new HashMap<>())
            .map(rows -> {
                List<DashboardStatsDto.TopMemberDto> topMembers = new ArrayList<>();
                int rank = 1;
                for (var row : rows) {
                    topMembers.add(DashboardStatsDto.TopMemberDto.builder()
                        .rank(rank++)
                        .nickname(getString(row, "nickname"))
                        .minedAmount(getDouble(row, "mined_amount"))
                        .build());
                }
                log.debug("getTopMembers succeeded - count: {}", topMembers.size());
                return topMembers;
            })
            .onFailure(err -> {
                log.error("getTopMembers failed", err);
            });
    }
    
    /**
     * 알림 조회
     */
    public Future<List<DashboardStatsDto.NotificationDto>> getNotifications(SqlClient client) {
        String sql = """
            SELECT 
                n.id,
                n.type,
                n.title as category,
                n.message as content,
                n.created_at,
                n.is_read
            FROM notifications n
            WHERE n.type IN ('INQUIRY', 'WITHDRAWAL', 'ANOMALY')
            ORDER BY n.created_at DESC
            LIMIT 50
            """;
        
        log.debug("Executing getNotifications query");
        
        return query(client, sql, new HashMap<>())
            .map(rows -> {
                List<DashboardStatsDto.NotificationDto> notifications = new ArrayList<>();
                for (var row : rows) {
                    LocalDateTime createdAt = getLocalDateTime(row, "created_at");
                    notifications.add(DashboardStatsDto.NotificationDto.builder()
                        .id(getLong(row, "id"))
                        .type(getString(row, "type"))
                        .category(getString(row, "category"))
                        .content(getString(row, "content"))
                        .createdAt(createdAt != null ? createdAt.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME) : null)
                        .isRead(getBoolean(row, "is_read"))
                        .build());
                }
                log.debug("getNotifications succeeded - count: {}", notifications.size());
                return notifications;
            })
            .onFailure(err -> {
                log.error("getNotifications failed", err);
            });
    }
    
    /**
     * 상위 추천인 조회
     */
    public Future<List<DashboardStatsDto.TopReferrerDto>> getTopReferrers(SqlClient client) {
        String sql = """
            SELECT 
                u.id,
                u.login_id as nickname,
                COALESCE(COUNT(DISTINCT rr.referred_id), 0) as team_member_count,
                ROW_NUMBER() OVER (ORDER BY COUNT(DISTINCT rr.referred_id) DESC) as rank
            FROM users u
            LEFT JOIN referral_relations rr ON rr.referrer_id = u.id AND rr.status = 'ACTIVE'
            GROUP BY u.id, u.login_id
            ORDER BY team_member_count DESC
            LIMIT 10
            """;
        
        log.debug("Executing getTopReferrers query");
        
        return query(client, sql, new HashMap<>())
            .map(rows -> {
                List<DashboardStatsDto.TopReferrerDto> topReferrers = new ArrayList<>();
                int rank = 1;
                for (var row : rows) {
                    topReferrers.add(DashboardStatsDto.TopReferrerDto.builder()
                        .rank(rank++)
                        .nickname(getString(row, "nickname"))
                        .teamMemberCount(getInteger(row, "team_member_count"))
                        .build());
                }
                log.debug("getTopReferrers succeeded - count: {}", topReferrers.size());
                return topReferrers;
            })
            .onFailure(err -> {
                log.error("getTopReferrers failed", err);
            });
    }
    
    /**
     * 단일 통계 값을 조회하는 헬퍼 메서드
     */
    private Future<Object> getStatValue(SqlClient client, String sql, Map<String, Object> params, String statName) {
        return query(client, sql, params)
            .map(rows -> {
                if (rows.size() == 0) {
                    log.warn("getStatValue returned no rows for: {}", statName);
                    return 0;
                }
                var row = rows.iterator().next();
                // value 컬럼의 타입에 따라 적절히 반환
                if (hasColumn(row, "value")) {
                    Object value = row.getValue("value");
                    if (value instanceof Number) {
                        return value;
                    }
                }
                return 0;
            })
            .onFailure(err -> {
                log.error("getStatValue failed for: {}", statName, err);
            })
            .recover(err -> {
                log.warn("getStatValue recovered for: {}, returning 0", statName);
                return Future.succeededFuture(0);
            });
    }
    
    private Double getDoubleFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
    
    private Integer getIntegerFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
    
    private DashboardStatsDto.StatsDto createEmptyStats() {
        return DashboardStatsDto.StatsDto.builder()
            .totalIssuance(5000000000.0)
            .totalMined(0.0)
            .todayDepositCount(0)
            .todayPaymentCount(0)
            .todayExchangeCount(0)
            .todayWithdrawalCount(0)
            .totalDeposit(0)
            .totalPayment(0)
            .totalExchange(0)
            .totalWithdrawal(0)
            .totalMiningAmount(0.0)
            .totalPaymentAmount(0.0)
            .totalExchangeAmount(0.0)
            .totalWithdrawalAmount(0.0)
            .totalWithdrawalFeeRevenue(0.0)
            .coinDepositCount(0)
            .paymentDepositCount(0)
            .swapCount(0)
            .exchangeCount(0)
            .withdrawalCount(0)
            .realtimeMiningUsers(0)
            .realtimeBroadcasts(0)
            .realtimeListeners(0)
            .referralRegistrationCount(0)
            .totalUsers(0)
            .build();
    }
}

