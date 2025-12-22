package com.csms.admin.repository;

import com.csms.admin.dto.MiningHistoryDetailDto;
import com.csms.common.database.RowMapper;
import com.csms.common.repository.BaseRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AdminMiningHistoryRepository extends BaseRepository {
    
    private final PgPool pool;
    
    public AdminMiningHistoryRepository(PgPool pool) {
        this.pool = pool;
    }
    
    private final RowMapper<MiningHistoryDetailDto.MiningHistoryRecord> recordMapper = row -> {
        LocalDateTime createdAt = getLocalDateTime(row, "created_at");
        String date = createdAt != null ? createdAt.toLocalDate().toString() : null;
        String time = createdAt != null ? createdAt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null;
        Integer year = createdAt != null ? createdAt.getYear() : null;
        
        // 기기정보 (IP 주소)
        String deviceInfo = getString(row, "device_info");
        String ipAddress = getString(row, "ip_address");
        String deviceInfoStr = "";
        if (deviceInfo != null && ipAddress != null) {
            deviceInfoStr = deviceInfo + " / " + ipAddress;
        } else if (deviceInfo != null) {
            deviceInfoStr = deviceInfo;
        } else if (ipAddress != null) {
            deviceInfoStr = ipAddress;
        }
        
        return MiningHistoryDetailDto.MiningHistoryRecord.builder()
            .id(getLong(row, "id"))
            .year(year)
            .date(date)
            .time(time)
            .level(getInteger(row, "level"))
            .miningType(getString(row, "mining_type"))
            .miningEfficiency(getInteger(row, "mining_efficiency"))
            .invitationCode(getString(row, "invitation_code"))
            .teamMemberCount(getInteger(row, "team_member_count"))
            .miningAmount(getDouble(row, "mining_amount"))
            .referralRevenue(getDouble(row, "referral_revenue"))
            .totalMinedHoldings(getDouble(row, "total_mined_holdings"))
            .deviceInfo(deviceInfoStr)
            .build();
    };
    
    public Future<MiningHistoryDetailDto> getMiningHistory(
        Long userId,
        Integer limit,
        Integer offset,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        // 사용자 기본 정보 조회
        String userSql = """
            SELECT 
                u.id,
                u.nickname,
                u.level
            FROM users u
            WHERE u.id = :user_id
            """;
        
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("user_id", userId);
        
        return query(pool, userSql, userParams)
            .compose(userRows -> {
                if (userRows.size() == 0) {
                    return Future.failedFuture(new com.csms.common.exceptions.NotFoundException("User not found"));
                }
                
                var userRow = userRows.iterator().next();
                String userNickname = getString(userRow, "nickname");
                Integer userLevel = getInteger(userRow, "level");
                
                // 채굴 내역 조회
                StringBuilder sql = new StringBuilder();
                Map<String, Object> params = new HashMap<>();
                
                sql.append("""
                    SELECT 
                        mh.id,
                        mh.created_at,
                        u.level,
                        mh.type as mining_type,
                        COALESCE(mh.efficiency, 0) as mining_efficiency,
                        u.referral_code as invitation_code,
                        COALESCE(team_stats.team_member_count, 0) as team_member_count,
                        CASE 
                            WHEN mh.type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH') THEN mh.amount
                            ELSE 0
                        END as mining_amount,
                        CASE 
                            WHEN mh.type = 'REFERRAL_REWARD' THEN mh.amount
                            ELSE 0
                        END as referral_revenue,
                        COALESCE(
                            (SELECT SUM(amount) 
                             FROM mining_history mh2 
                             WHERE mh2.user_id = mh.user_id 
                             AND mh2.type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH', 'REFERRAL_REWARD')
                             AND mh2.created_at <= mh.created_at), 0
                        ) as total_mined_holdings,
                        mh.device_info,
                        mh.ip_address
                    FROM mining_history mh
                    INNER JOIN users u ON u.id = mh.user_id
                    LEFT JOIN (
                        SELECT referrer_id, COUNT(DISTINCT referred_id) as team_member_count
                        FROM referral_relations
                        WHERE status = 'ACTIVE' AND deleted_at IS NULL
                        GROUP BY referrer_id
                    ) team_stats ON team_stats.referrer_id = u.id
                    WHERE mh.user_id = :user_id
                    """);
                
                params.put("user_id", userId);
                
                // 날짜 범위 필터
                if (startDate != null && endDate != null) {
                    sql.append(" AND mh.created_at >= :start_date AND mh.created_at <= :end_date");
                    params.put("start_date", startDate);
                    params.put("end_date", endDate);
                }
                
                sql.append(" ORDER BY mh.created_at DESC");
                
                // 총 개수 조회
                StringBuilder countSql = new StringBuilder();
                countSql.append("SELECT COUNT(*) as total FROM mining_history mh WHERE mh.user_id = :user_id");
                if (startDate != null && endDate != null) {
                    countSql.append(" AND mh.created_at >= :start_date AND mh.created_at <= :end_date");
                }
                
                return query(pool, countSql.toString(), params)
                    .compose(countRows -> {
                        Integer total = 0;
                        if (countRows.size() > 0) {
                            total = getInteger(countRows.iterator().next(), "total");
                        }
                        final Integer finalTotal = total;
                        
                        // 데이터 조회
                        sql.append(" LIMIT :limit OFFSET :offset");
                        params.put("limit", limit);
                        params.put("offset", offset);
                        
                        return query(pool, sql.toString(), params)
                            .compose(rows -> {
                                List<MiningHistoryDetailDto.MiningHistoryRecord> records = new ArrayList<>();
                                for (var row : rows) {
                                    records.add(recordMapper.map(row));
                                }
                                
                                // 요약 통계 조회
                                return getSummary(userId, startDate, endDate)
                                    .map(summary -> MiningHistoryDetailDto.builder()
                                        .userId(userId)
                                        .userNickname(userNickname)
                                        .userLevel(userLevel)
                                        .records(records)
                                        .total(finalTotal)
                                        .limit(limit)
                                        .offset(offset)
                                        .summary(summary)
                                        .build());
                            });
                    });
            })
            .onFailure(throwable -> log.error("채굴 내역 조회 실패 - userId: {}", userId, throwable));
    }
    
    private Future<MiningHistoryDetailDto.Summary> getSummary(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        sql.append("""
            SELECT 
                COALESCE(SUM(CASE WHEN type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH') THEN amount ELSE 0 END), 0) as total_mining_amount,
                COALESCE(SUM(CASE WHEN type = 'REFERRAL_REWARD' THEN amount ELSE 0 END), 0) as total_referral_revenue,
                COALESCE(SUM(CASE WHEN type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH', 'REFERRAL_REWARD') THEN amount ELSE 0 END), 0) as total_mined_holdings
            FROM mining_history
            WHERE user_id = :user_id
            """);
        
        params.put("user_id", userId);
        
        if (startDate != null && endDate != null) {
            sql.append(" AND created_at >= :start_date AND created_at <= :end_date");
            params.put("start_date", startDate);
            params.put("end_date", endDate);
        }
        
        return query(pool, sql.toString(), params)
            .map(rows -> {
                if (rows.size() == 0) {
                    return MiningHistoryDetailDto.Summary.builder()
                        .totalMiningAmount(0.0)
                        .totalReferralRevenue(0.0)
                        .totalMinedHoldings(0.0)
                        .build();
                }
                
                var row = rows.iterator().next();
                return MiningHistoryDetailDto.Summary.builder()
                    .totalMiningAmount(getDouble(row, "total_mining_amount"))
                    .totalReferralRevenue(getDouble(row, "total_referral_revenue"))
                    .totalMinedHoldings(getDouble(row, "total_mined_holdings"))
                    .build();
            });
    }
}

