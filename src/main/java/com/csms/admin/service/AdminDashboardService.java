package com.csms.admin.service;

import com.csms.admin.dto.DashboardStatsDto;
import com.csms.common.repository.BaseRepository;
import com.csms.common.service.BaseService;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AdminDashboardService extends BaseRepository {
    
    private final PgPool pool;
    
    public AdminDashboardService(PgPool pool) {
        this.pool = pool;
    }
    
    public Future<DashboardStatsDto> getDashboardStats(String dateRange, String startDate, String endDate) {
        // 날짜 범위 계산
        DateRange range = calculateDateRange(dateRange, startDate, endDate);
        
        return Future.all(
            getTotalStats(range),
            getChartData(range),
            getTopMembers(),
            getNotifications(),
            getTopReferrers()
        ).map(result -> {
            DashboardStatsDto.StatsDto stats = result.resultAt(0);
            List<DashboardStatsDto.ChartDataDto> chartData = result.resultAt(1);
            List<DashboardStatsDto.TopMemberDto> topMembers = result.resultAt(2);
            List<DashboardStatsDto.NotificationDto> notifications = result.resultAt(3);
            List<DashboardStatsDto.TopReferrerDto> topReferrers = result.resultAt(4);
            
            return DashboardStatsDto.builder()
                .stats(stats)
                .chartData(chartData)
                .topMembers(topMembers)
                .notifications(notifications)
                .topReferrers(topReferrers)
                .build();
        });
    }
    
    private Future<DashboardStatsDto.StatsDto> getTotalStats(DateRange range) {
        // 오늘 날짜 계산
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(23, 59, 59);
        
        String sql = """
            SELECT 
                COALESCE(SUM(CASE WHEN mh.id IS NOT NULL THEN mh.amount ELSE 0 END), 0) as total_mined,
                COALESCE(COUNT(DISTINCT CASE WHEN td.id IS NOT NULL AND td.created_at >= :today_start AND td.created_at <= :today_end THEN td.id END), 0) as today_deposit_count,
                COALESCE(COUNT(DISTINCT CASE WHEN pd.id IS NOT NULL AND pd.created_at >= :today_start AND pd.created_at <= :today_end THEN pd.id END), 0) as today_payment_count,
                COALESCE(COUNT(DISTINCT CASE WHEN e.id IS NOT NULL AND e.created_at >= :today_start AND e.created_at <= :today_end THEN e.id END), 0) as today_exchange_count,
                COALESCE(COUNT(DISTINCT CASE WHEN et.id IS NOT NULL AND et.created_at >= :today_start AND et.created_at <= :today_end THEN et.id END), 0) as today_withdrawal_count,
                COALESCE(COUNT(DISTINCT CASE WHEN td.id IS NOT NULL THEN td.id END), 0) as coin_deposit_count,
                COALESCE(COUNT(DISTINCT CASE WHEN pd.id IS NOT NULL THEN pd.id END), 0) as payment_deposit_count,
                COALESCE(COUNT(DISTINCT CASE WHEN s.id IS NOT NULL THEN s.id END), 0) as swap_count,
                COALESCE(COUNT(DISTINCT CASE WHEN e.id IS NOT NULL THEN e.id END), 0) as exchange_count,
                COALESCE(COUNT(DISTINCT CASE WHEN et.id IS NOT NULL THEN et.id END), 0) as withdrawal_count,
                COALESCE(SUM(CASE WHEN mh.id IS NOT NULL AND mh.created_at >= :start_date THEN mh.amount ELSE 0 END), 0) as total_mining_amount,
                COALESCE(SUM(CASE WHEN pd.id IS NOT NULL AND pd.created_at >= :start_date THEN pd.amount ELSE 0 END), 0) as total_payment_amount,
                COALESCE(SUM(CASE WHEN e.id IS NOT NULL AND e.created_at >= :start_date THEN e.from_amount ELSE 0 END), 0) as total_exchange_amount,
                COALESCE(SUM(CASE WHEN et.id IS NOT NULL AND et.created_at >= :start_date THEN et.amount ELSE 0 END), 0) as total_withdrawal_amount,
                COALESCE(SUM(CASE WHEN et.id IS NOT NULL AND et.created_at >= :start_date THEN et.fee ELSE 0 END), 0) as total_withdrawal_fee_revenue,
                COALESCE(COUNT(DISTINCT CASE WHEN rr.id IS NOT NULL AND rr.created_at >= :start_date THEN rr.id END), 0) as referral_registration_count,
                COALESCE(COUNT(DISTINCT u.id), 0) as total_users,
                COALESCE(COUNT(DISTINCT CASE WHEN dm.user_id IS NOT NULL THEN dm.user_id END), 0) as realtime_mining_users,
                COALESCE(COUNT(DISTINCT CASE WHEN mh.type = 'BROADCAST_PROGRESS' AND mh.created_at >= :start_date THEN mh.user_id END), 0) as realtime_broadcasts,
                COALESCE(COUNT(DISTINCT CASE WHEN mh.type = 'BROADCAST_WATCH' AND mh.created_at >= :start_date THEN mh.user_id END), 0) as realtime_listeners
            FROM users u
            LEFT JOIN mining_history mh ON mh.user_id = u.id AND mh.created_at >= :start_date AND mh.created_at <= :end_date
            LEFT JOIN token_deposits td ON td.user_id = u.id AND (td.created_at >= :start_date AND td.created_at <= :end_date OR td.created_at >= :today_start AND td.created_at <= :today_end)
            LEFT JOIN payment_deposits pd ON pd.user_id = u.id AND (pd.created_at >= :start_date AND pd.created_at <= :end_date OR pd.created_at >= :today_start AND pd.created_at <= :today_end)
            LEFT JOIN swaps s ON s.user_id = u.id AND s.created_at >= :start_date AND s.created_at <= :end_date
            LEFT JOIN exchanges e ON e.user_id = u.id AND (e.created_at >= :start_date AND e.created_at <= :end_date OR e.created_at >= :today_start AND e.created_at <= :today_end)
            LEFT JOIN external_transfers et ON et.user_id = u.id AND (et.created_at >= :start_date AND et.created_at <= :end_date OR et.created_at >= :today_start AND et.created_at <= :today_end)
            LEFT JOIN referral_relations rr ON rr.referred_id = u.id AND rr.created_at >= :start_date AND rr.created_at <= :end_date
            LEFT JOIN daily_mining dm ON dm.user_id = u.id AND dm.mining_date = CURRENT_DATE
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("start_date", range.startDate);
        params.put("end_date", range.endDate);
        params.put("today_start", todayStart);
        params.put("today_end", todayEnd);
        
        return query(pool, sql, params)
            .map(rows -> {
                if (rows.size() == 0) {
                    return createEmptyStats();
                }
                
                var row = rows.iterator().next();
                return DashboardStatsDto.StatsDto.builder()
                    .totalIssuance(5000000000.0) // 고정값 또는 설정에서 가져오기
                    .totalMined(getDouble(row, "total_mined"))
                    .todayDepositCount(getInteger(row, "today_deposit_count"))
                    .todayPaymentCount(getInteger(row, "today_payment_count"))
                    .todayExchangeCount(getInteger(row, "today_exchange_count"))
                    .todayWithdrawalCount(getInteger(row, "today_withdrawal_count"))
                    .totalDeposit(getInteger(row, "coin_deposit_count"))
                    .totalPayment(getInteger(row, "payment_deposit_count"))
                    .totalExchange(getInteger(row, "exchange_count"))
                    .totalWithdrawal(getInteger(row, "withdrawal_count"))
                    .totalMiningAmount(getDouble(row, "total_mining_amount"))
                    .totalPaymentAmount(getDouble(row, "total_payment_amount"))
                    .totalExchangeAmount(getDouble(row, "total_exchange_amount"))
                    .totalWithdrawalAmount(getDouble(row, "total_withdrawal_amount"))
                    .totalWithdrawalFeeRevenue(getDouble(row, "total_withdrawal_fee_revenue"))
                    .coinDepositCount(getInteger(row, "coin_deposit_count"))
                    .paymentDepositCount(getInteger(row, "payment_deposit_count"))
                    .swapCount(getInteger(row, "swap_count"))
                    .exchangeCount(getInteger(row, "exchange_count"))
                    .withdrawalCount(getInteger(row, "withdrawal_count"))
                    .realtimeMiningUsers(getInteger(row, "realtime_mining_users"))
                    .realtimeBroadcasts(getInteger(row, "realtime_broadcasts"))
                    .realtimeListeners(getInteger(row, "realtime_listeners"))
                    .referralRegistrationCount(getInteger(row, "referral_registration_count"))
                    .totalUsers(getInteger(row, "total_users"))
                    .build();
            });
    }
    
    private Future<List<DashboardStatsDto.ChartDataDto>> getChartData(DateRange range) {
        String sql = """
            SELECT 
                DATE(mh.created_at) as date,
                COALESCE(SUM(mh.amount), 0) as total_mined,
                COUNT(DISTINCT mh.user_id) as user_count
            FROM mining_history mh
            WHERE mh.created_at >= :start_date AND mh.created_at <= :end_date
            GROUP BY DATE(mh.created_at)
            ORDER BY date ASC
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("start_date", range.startDate);
        params.put("end_date", range.endDate);
        
        return query(pool, sql, params)
            .map(rows -> {
                List<DashboardStatsDto.ChartDataDto> chartData = new ArrayList<>();
                for (var row : rows) {
                    chartData.add(DashboardStatsDto.ChartDataDto.builder()
                        .date(getLocalDateTime(row, "date") != null 
                            ? getLocalDateTime(row, "date").toLocalDate().toString()
                            : null)
                        .totalMined(getDouble(row, "total_mined"))
                        .userCount(getInteger(row, "user_count"))
                        .build());
                }
                return chartData;
            });
    }
    
    private Future<List<DashboardStatsDto.TopMemberDto>> getTopMembers() {
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
        
        return query(pool, sql, new HashMap<>())
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
                return topMembers;
            });
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
    
    private DateRange calculateDateRange(String dateRange, String startDate, String endDate) {
        LocalDate end = LocalDate.now();
        LocalDate start;
        
        if ("custom".equals(dateRange) && startDate != null && endDate != null) {
            start = LocalDate.parse(startDate);
            end = LocalDate.parse(endDate);
        } else {
            int days = switch (dateRange != null ? dateRange : "7") {
                case "14" -> 14;
                case "30" -> 30;
                case "180" -> 180;
                case "365" -> 365;
                default -> 7;
            };
            start = end.minusDays(days);
        }
        
        // 최대 365일 제한
        if (start.isBefore(end.minusDays(365))) {
            start = end.minusDays(365);
        }
        
        return new DateRange(
            start.atStartOfDay(),
            end.atTime(23, 59, 59)
        );
    }
    
    private Future<List<DashboardStatsDto.NotificationDto>> getNotifications() {
        // 관리자용 알림 조회: INQUIRY, WITHDRAWAL, ANOMALY 타입
        // notifications 테이블에서 관리자 승인/답변이 필요한 알림만 조회
        // 실제 구현에서는 별도 관리자 알림 테이블이 있을 수 있지만, 여기서는 notifications 활용
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
        
        return query(pool, sql, new HashMap<>())
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
                return notifications;
            });
    }
    
    private Future<List<DashboardStatsDto.TopReferrerDto>> getTopReferrers() {
        // 팀원을 가장 많이 보유한 회원 순위 (referral_relations에서 팀원 수 계산)
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
        
        return query(pool, sql, new HashMap<>())
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
                return topReferrers;
            });
    }
    
    private record DateRange(LocalDateTime startDate, LocalDateTime endDate) {}
}

