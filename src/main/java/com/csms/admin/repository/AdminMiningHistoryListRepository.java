package com.csms.admin.repository;

import com.csms.admin.dto.MiningHistoryListDto;
import com.csms.common.database.RowMapper;
import com.csms.common.repository.BaseRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AdminMiningHistoryListRepository extends BaseRepository {
    
    private final PgPool pool;
    
    public AdminMiningHistoryListRepository(PgPool pool) {
        this.pool = pool;
    }
    
    private final RowMapper<MiningHistoryListDto.MiningHistoryItem> itemMapper = row -> {
        String sanctionStatus = getString(row, "sanction_status");
        if (sanctionStatus == null) {
            sanctionStatus = "NONE";
        }
        
        return MiningHistoryListDto.MiningHistoryItem.builder()
            .id(getLong(row, "id"))
            .referrerNickname(getString(row, "referrer_nickname"))
            .nickname(getString(row, "nickname"))
            .miningEfficiency(getInteger(row, "mining_efficiency"))
            .level(getInteger(row, "level"))
            .invitationCode(getString(row, "invitation_code"))
            .teamMemberCount(getInteger(row, "team_member_count"))
            .totalMiningAmount(getDouble(row, "total_mining_amount"))
            .referralRevenue(getDouble(row, "referral_revenue"))
            .totalMinedHoldings(getDouble(row, "total_mined_holdings"))
            .activityStatus(getString(row, "activity_status"))
            .sanctionStatus(sanctionStatus)
            .build();
    };
    
    public Future<MiningHistoryListDto> getMiningHistoryList(
        Integer limit,
        Integer offset,
        String searchCategory,
        String searchKeyword,
        String sortType,
        String activityStatus,
        String sanctionStatus
    ) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        // SELECT 절
        sql.append("""
            SELECT 
                u.id,
                referrer.nickname as referrer_nickname,
                u.nickname,
                COALESCE(avg_efficiency.mining_efficiency, 0) as mining_efficiency,
                u.level,
                u.referral_code as invitation_code,
                COALESCE(team_stats.team_member_count, 0) as team_member_count,
                COALESCE(mining_stats.total_mining_amount, 0) as total_mining_amount,
                COALESCE(revenue_stats.referral_revenue, 0) as referral_revenue,
                COALESCE(mining_stats.total_mining_amount, 0) + COALESCE(revenue_stats.referral_revenue, 0) as total_mined_holdings,
                u.status as activity_status,
                u.sanction_status
            FROM users u
            LEFT JOIN referral_relations rr ON rr.referred_id = u.id AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL
            LEFT JOIN users referrer ON referrer.id = rr.referrer_id
            LEFT JOIN (
                SELECT referrer_id, COUNT(DISTINCT referred_id) as team_member_count
                FROM referral_relations
                WHERE status = 'ACTIVE' AND deleted_at IS NULL
                GROUP BY referrer_id
            ) team_stats ON team_stats.referrer_id = u.id
            LEFT JOIN (
                SELECT user_id, SUM(amount) as total_mining_amount
                FROM mining_history
                WHERE type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH')
                GROUP BY user_id
            ) mining_stats ON mining_stats.user_id = u.id
            LEFT JOIN (
                SELECT user_id, SUM(amount) as referral_revenue
                FROM mining_history
                WHERE type = 'REFERRAL_REWARD'
                GROUP BY user_id
            ) revenue_stats ON revenue_stats.user_id = u.id
            LEFT JOIN (
                SELECT user_id, AVG(efficiency) as mining_efficiency
                FROM mining_history
                WHERE type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH')
                AND efficiency IS NOT NULL
                GROUP BY user_id
            ) avg_efficiency ON avg_efficiency.user_id = u.id
            WHERE (mining_stats.user_id IS NOT NULL OR revenue_stats.user_id IS NOT NULL)
            """);
        
        // 검색 조건
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() &&
            searchCategory != null && !"ALL".equals(searchCategory)) {
            switch (searchCategory) {
                case "ID" -> {
                    sql.append(" AND (u.id::text LIKE :search_keyword OR u.login_id LIKE :search_keyword)");
                    params.put("search_keyword", "%" + searchKeyword.trim() + "%");
                }
                case "REFERRER" -> {
                    sql.append(" AND referrer.nickname LIKE :search_keyword");
                    params.put("search_keyword", "%" + searchKeyword.trim() + "%");
                }
                case "NICKNAME" -> {
                    sql.append(" AND u.nickname LIKE :search_keyword");
                    params.put("search_keyword", "%" + searchKeyword.trim() + "%");
                }
                case "LEVEL" -> {
                    try {
                        Integer level = Integer.parseInt(searchKeyword.trim());
                        sql.append(" AND u.level = :search_keyword");
                        params.put("search_keyword", level);
                    } catch (NumberFormatException e) {
                        // 레벨이 숫자가 아니면 검색 결과 없음
                        sql.append(" AND 1=0");
                    }
                }
                case "INVITATION_CODE" -> {
                    sql.append(" AND u.referral_code LIKE :search_keyword");
                    params.put("search_keyword", "%" + searchKeyword.trim() + "%");
                }
            }
        }
        
        // 활동상태 필터
        if (activityStatus != null && !"ALL".equals(activityStatus)) {
            sql.append(" AND u.status = :activity_status");
            params.put("activity_status", activityStatus);
        }
        
        // 제재상태 필터
        if (sanctionStatus != null && !"ALL".equals(sanctionStatus)) {
            sql.append(" AND u.sanction_status = :sanction_status");
            params.put("sanction_status", sanctionStatus);
        }
        
        // 정렬
        if (sortType != null) {
            switch (sortType) {
                case "LEVEL" -> sql.append(" ORDER BY u.level DESC");
                case "TEAM_MEMBER_COUNT" -> sql.append(" ORDER BY team_member_count DESC");
                default -> sql.append(" ORDER BY u.id DESC");
            }
        } else {
            sql.append(" ORDER BY u.id DESC");
        }
        
        // 총 개수 조회
        StringBuilder countSql = new StringBuilder();
        countSql.append("""
            SELECT COUNT(DISTINCT u.id) as total
            FROM users u
            LEFT JOIN (
                SELECT user_id, SUM(amount) as total_mining_amount
                FROM mining_history
                WHERE type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH')
                GROUP BY user_id
            ) mining_stats ON mining_stats.user_id = u.id
            LEFT JOIN (
                SELECT user_id, SUM(amount) as referral_revenue
                FROM mining_history
                WHERE type = 'REFERRAL_REWARD'
                GROUP BY user_id
            ) revenue_stats ON revenue_stats.user_id = u.id
            LEFT JOIN referral_relations rr ON rr.referred_id = u.id AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL
            LEFT JOIN users referrer ON referrer.id = rr.referrer_id
            WHERE (mining_stats.user_id IS NOT NULL OR revenue_stats.user_id IS NOT NULL)
            """);
        
        // 검색 조건 추가 (count 쿼리에도 동일하게)
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() &&
            searchCategory != null && !"ALL".equals(searchCategory)) {
            switch (searchCategory) {
                case "ID" -> {
                    countSql.append(" AND (u.id::text LIKE :search_keyword OR u.login_id LIKE :search_keyword)");
                }
                case "REFERRER" -> {
                    countSql.append(" AND referrer.nickname LIKE :search_keyword");
                }
                case "NICKNAME" -> {
                    countSql.append(" AND u.nickname LIKE :search_keyword");
                }
                case "LEVEL" -> {
                    try {
                        Integer level = Integer.parseInt(searchKeyword.trim());
                        countSql.append(" AND u.level = :search_keyword");
                    } catch (NumberFormatException e) {
                        countSql.append(" AND 1=0");
                    }
                }
                case "INVITATION_CODE" -> {
                    countSql.append(" AND u.referral_code LIKE :search_keyword");
                }
            }
        }
        
        if (activityStatus != null && !"ALL".equals(activityStatus)) {
            countSql.append(" AND u.status = :activity_status");
        }
        
        if (sanctionStatus != null && !"ALL".equals(sanctionStatus)) {
            countSql.append(" AND u.sanction_status = :sanction_status");
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
                    .map(rows -> {
                        List<MiningHistoryListDto.MiningHistoryItem> items = new ArrayList<>();
                        for (var row : rows) {
                            items.add(itemMapper.map(row));
                        }
                        
                        return MiningHistoryListDto.builder()
                            .items(items)
                            .total(finalTotal)
                            .limit(limit)
                            .offset(offset)
                            .build();
                    });
            })
            .onFailure(throwable -> log.error("채굴 내역 목록 조회 실패", throwable));
    }
}

