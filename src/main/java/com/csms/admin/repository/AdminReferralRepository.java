package com.csms.admin.repository;

import com.csms.admin.dto.ReferralTransactionHistoryDto;
import com.csms.admin.dto.ReferralTransactionHistoryListDto;
import com.csms.admin.dto.ReferralTreeListDto;
import com.csms.admin.dto.ReferralTreeMemberDto;
import com.csms.common.database.RowMapper;
import com.csms.common.repository.BaseRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AdminReferralRepository extends BaseRepository {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    private final RowMapper<ReferralTransactionHistoryDto> referralTransactionMapper = row -> {
        LocalDateTime createdAt = getLocalDateTime(row, "created_at");
        return ReferralTransactionHistoryDto.builder()
            .id(getLong(row, "id"))
            .referrerNickname(getString(row, "referrer_nickname"))
            .invitationCode(getString(row, "invitation_code"))
            .nickname(getString(row, "nickname"))
            .level(getInteger(row, "level"))
            .teamMemberCount(getInteger(row, "team_member_count"))
            .referralRevenueBefore(getBigDecimal(row, "referral_revenue_before"))
            .referralRevenue(getBigDecimal(row, "referral_revenue"))
            .referralRevenueAfter(getBigDecimal(row, "referral_revenue_after"))
            .date(createdAt != null ? createdAt.format(DATE_FORMATTER) : null)
            .time(createdAt != null ? createdAt.format(TIME_FORMATTER) : null)
            .createdAt(createdAt)
            .build();
    };
    
    /**
     * 래퍼럴 거래내역 목록 조회
     */
    public Future<ReferralTransactionHistoryListDto> getReferralTransactionHistory(
        SqlClient client,
        Integer limit,
        Integer offset,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String searchCategory,
        String searchKeyword,
        String sortType
    ) {
        log.debug("Executing getReferralTransactionHistory - limit: {}, offset: {}, startDate: {}, endDate: {}", 
            limit, offset, startDate, endDate);
        
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        // SELECT 절 - mining_history에서 REFERRAL_REWARD 타입 조회
        sql.append("""
            SELECT 
                mh.id,
                referrer.nickname as referrer_nickname,
                referrer.referral_code as invitation_code,
                u.nickname,
                COALESCE(rr.level, 1) as level,
                COALESCE(team_stats.team_member_count, 0) as team_member_count,
                COALESCE(before_stats.referral_revenue, 0) as referral_revenue_before,
                mh.amount as referral_revenue,
                COALESCE(before_stats.referral_revenue, 0) + mh.amount as referral_revenue_after,
                mh.created_at
            FROM mining_history mh
            INNER JOIN users u ON u.id = mh.user_id
            LEFT JOIN referral_relations rr ON rr.referred_id = mh.user_id AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL AND rr.level = 1
            LEFT JOIN users referrer ON referrer.id = rr.referrer_id
            LEFT JOIN (
                SELECT referrer_id, COUNT(DISTINCT referred_id) as team_member_count
                FROM referral_relations
                WHERE status = 'ACTIVE' AND deleted_at IS NULL
                GROUP BY referrer_id
            ) team_stats ON team_stats.referrer_id = mh.user_id
            LEFT JOIN (
                SELECT 
                    user_id,
                    SUM(amount) OVER (PARTITION BY user_id ORDER BY created_at ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING) as referral_revenue
                FROM mining_history
                WHERE type = 'REFERRAL_REWARD'
            ) before_stats ON before_stats.user_id = mh.user_id
            WHERE mh.type = 'REFERRAL_REWARD'
            """);
        
        // 날짜 필터
        if (startDate != null) {
            sql.append(" AND mh.created_at >= :start_date");
            params.put("start_date", startDate);
        }
        if (endDate != null) {
            sql.append(" AND mh.created_at <= :end_date");
            params.put("end_date", endDate);
        }
        
        // 검색 조건
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ID" -> {
                    sql.append(" AND (u.id::text = :search_keyword OR u.login_id ILIKE :search_keyword_pattern)");
                    params.put("search_keyword", searchKeyword);
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "REFERRER" -> {
                    sql.append(" AND referrer.nickname ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "INVITATION_CODE" -> {
                    sql.append(" AND referrer.referral_code ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "NICKNAME" -> {
                    sql.append(" AND u.nickname ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "LEVEL" -> {
                    try {
                        Integer level = Integer.parseInt(searchKeyword);
                        sql.append(" AND COALESCE(MAX(rr.level), 1) = :level");
                        params.put("level", level);
                    } catch (NumberFormatException e) {
                        // 무시
                    }
                }
            }
        }
        
        sql.append(" GROUP BY mh.id, referrer.nickname, referrer.referral_code, u.nickname, mh.amount, mh.created_at, team_stats.team_member_count, before_stats.referral_revenue, rr.level");
        
        // 정렬
        if ("TEAM_MEMBER_COUNT".equals(sortType)) {
            sql.append(" ORDER BY team_member_count DESC, mh.created_at DESC");
        } else {
            sql.append(" ORDER BY level DESC, mh.created_at DESC");
        }
        
        sql.append(" LIMIT :limit OFFSET :offset");
        params.put("limit", limit);
        params.put("offset", offset);
        
        // 카운트 쿼리
        StringBuilder countSql = new StringBuilder();
        countSql.append("""
            SELECT COUNT(DISTINCT mh.id) as count
            FROM mining_history mh
            INNER JOIN users u ON u.id = mh.user_id
            LEFT JOIN referral_relations rr ON rr.referred_id = mh.user_id AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL
            LEFT JOIN users referrer ON referrer.id = rr.referrer_id
            WHERE mh.type = 'REFERRAL_REWARD'
            """);
        
        Map<String, Object> countParams = new HashMap<>(params);
        countParams.remove("limit");
        countParams.remove("offset");
        
        // 동일한 WHERE 조건 추가
        if (startDate != null) {
            countSql.append(" AND mh.created_at >= :start_date");
        }
        if (endDate != null) {
            countSql.append(" AND mh.created_at <= :end_date");
        }
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ID" -> {
                    countSql.append(" AND (u.id::text = :search_keyword OR u.login_id ILIKE :search_keyword_pattern)");
                }
                case "REFERRER" -> countSql.append(" AND referrer.nickname ILIKE :search_keyword_pattern");
                case "INVITATION_CODE" -> countSql.append(" AND referrer.referral_code ILIKE :search_keyword_pattern");
                case "NICKNAME" -> countSql.append(" AND u.nickname ILIKE :search_keyword_pattern");
                case "LEVEL" -> {
                    try {
                        Integer level = Integer.parseInt(searchKeyword);
                        countSql.append(" AND COALESCE(MAX(rr.level), 1) = :level");
                    } catch (NumberFormatException e) {
                        // 무시
                    }
                }
            }
        }
        
        Future<List<ReferralTransactionHistoryDto>> transactionsFuture = query(client, sql.toString(), params)
            .map(rows -> fetchAll(referralTransactionMapper, rows));
        
        Future<Integer> countFuture = query(client, countSql.toString(), countParams)
            .map(rows -> {
                if (rows.size() > 0) {
                    return rows.iterator().next().getInteger("count");
                }
                return 0;
            });
        
        return Future.all(List.of(transactionsFuture, countFuture))
            .map(results -> {
                List<ReferralTransactionHistoryDto> transactions = transactionsFuture.result();
                Integer total = countFuture.result();
                
                return ReferralTransactionHistoryListDto.builder()
                    .transactions(transactions)
                    .total(total)
                    .limit(limit)
                    .offset(offset)
                    .build();
            });
    }
    
    /**
     * 래퍼럴 트리구조 목록 조회
     */
    public Future<ReferralTreeListDto> getReferralTree(
        SqlClient client,
        Integer limit,
        Integer offset,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String searchCategory,
        String searchKeyword,
        String activityStatus,
        String sortType
    ) {
        log.debug("Executing getReferralTree - limit: {}, offset: {}, startDate: {}, endDate: {}", 
            limit, offset, startDate, endDate);
        
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        // SELECT 절 - 상위 회원 조회
        sql.append("""
            SELECT 
                u.id,
                rr.referrer_id,
                referrer.nickname as referrer_nickname,
                u.referral_code as invitation_code,
                u.nickname,
                COALESCE(MAX(rr.level), 1) as level,
                COALESCE(team_stats.team_member_count, 0) as team_member_count,
                COALESCE(revenue_stats.total_referral_revenue, 0) as total_referral_revenue,
                u.status as activity_status
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
                SELECT user_id, SUM(amount) as total_referral_revenue
                FROM mining_history
                WHERE type = 'REFERRAL_REWARD'
                GROUP BY user_id
            ) revenue_stats ON revenue_stats.user_id = u.id
            WHERE 1=1
            """);
        
        // 날짜 필터 (래퍼럴 등록일 기준)
        if (startDate != null || endDate != null) {
            sql.append(" AND EXISTS (");
            sql.append(" SELECT 1 FROM referral_relations rr2");
            sql.append(" WHERE rr2.referred_id = u.id AND rr2.status = 'ACTIVE' AND rr2.deleted_at IS NULL");
            if (startDate != null) {
                sql.append(" AND rr2.created_at >= :start_date");
                params.put("start_date", startDate);
            }
            if (endDate != null) {
                sql.append(" AND rr2.created_at <= :end_date");
                params.put("end_date", endDate);
            }
            sql.append(")");
        }
        
        // 활동상태 필터
        if (activityStatus != null && !activityStatus.isEmpty() && !"ALL".equals(activityStatus)) {
            sql.append(" AND u.status = :activity_status");
            params.put("activity_status", activityStatus);
        }
        
        // 검색 조건
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ID" -> {
                    sql.append(" AND (u.id::text = :search_keyword OR u.login_id ILIKE :search_keyword_pattern)");
                    params.put("search_keyword", searchKeyword);
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "REFERRER" -> {
                    sql.append(" AND referrer.nickname ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "INVITATION_CODE" -> {
                    sql.append(" AND u.referral_code ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "NICKNAME" -> {
                    sql.append(" AND u.nickname ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "LEVEL" -> {
                    try {
                        Integer level = Integer.parseInt(searchKeyword);
                        sql.append(" AND COALESCE(MAX(rr.level), 1) = :level");
                        params.put("level", level);
                    } catch (NumberFormatException e) {
                        // 무시
                    }
                }
            }
        }
        
        sql.append(" GROUP BY u.id, rr.referrer_id, referrer.nickname, u.referral_code, u.nickname, team_stats.team_member_count, revenue_stats.total_referral_revenue, u.status");
        
        // 정렬
        if ("TEAM_MEMBER_COUNT".equals(sortType)) {
            sql.append(" ORDER BY team_member_count DESC");
        } else {
            sql.append(" ORDER BY level DESC");
        }
        
        sql.append(" LIMIT :limit OFFSET :offset");
        params.put("limit", limit);
        params.put("offset", offset);
        
        // 카운트 쿼리
        StringBuilder countSql = new StringBuilder();
        countSql.append("""
            SELECT COUNT(DISTINCT u.id) as count
            FROM users u
            LEFT JOIN referral_relations rr ON rr.referred_id = u.id AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL
            LEFT JOIN users referrer ON referrer.id = rr.referrer_id
            WHERE 1=1
            """);
        
        Map<String, Object> countParams = new HashMap<>(params);
        countParams.remove("limit");
        countParams.remove("offset");
        
        // 동일한 WHERE 조건 추가
        if (startDate != null || endDate != null) {
            countSql.append(" AND EXISTS (");
            countSql.append(" SELECT 1 FROM referral_relations rr2");
            countSql.append(" WHERE rr2.referred_id = u.id AND rr2.status = 'ACTIVE' AND rr2.deleted_at IS NULL");
            if (startDate != null) {
                countSql.append(" AND rr2.created_at >= :start_date");
            }
            if (endDate != null) {
                countSql.append(" AND rr2.created_at <= :end_date");
            }
            countSql.append(")");
        }
        if (activityStatus != null && !activityStatus.isEmpty() && !"ALL".equals(activityStatus)) {
            countSql.append(" AND u.status = :activity_status");
        }
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ID" -> {
                    countSql.append(" AND (u.id::text = :search_keyword OR u.login_id ILIKE :search_keyword_pattern)");
                }
                case "REFERRER" -> countSql.append(" AND referrer.nickname ILIKE :search_keyword_pattern");
                case "INVITATION_CODE" -> countSql.append(" AND u.referral_code ILIKE :search_keyword_pattern");
                case "NICKNAME" -> countSql.append(" AND u.nickname ILIKE :search_keyword_pattern");
                case "LEVEL" -> {
                    try {
                        Integer level = Integer.parseInt(searchKeyword);
                        countSql.append(" AND COALESCE(MAX(rr.level), 1) = :level");
                    } catch (NumberFormatException e) {
                        // 무시
                    }
                }
            }
        }
        
        Future<List<ReferralTreeMemberDto>> membersFuture = query(client, sql.toString(), params)
            .compose(rows -> {
                List<ReferralTreeMemberDto> members = fetchAll(referralTreeMemberMapper, rows);
                // 각 회원의 하부 회원 조회
                List<Future<ReferralTreeMemberDto>> memberFutures = new ArrayList<>();
                for (ReferralTreeMemberDto member : members) {
                    memberFutures.add(getChildrenForMember(client, member.getId(), startDate, endDate));
                }
                return Future.all(memberFutures)
                    .map(results -> {
                        for (int i = 0; i < members.size(); i++) {
                            ReferralTreeMemberDto member = members.get(i);
                            ReferralTreeMemberDto memberWithChildren = (ReferralTreeMemberDto) results.resultAt(i);
                            member.setChildren(memberWithChildren.getChildren());
                        }
                        return members;
                    });
            });
        
        Future<Integer> countFuture = query(client, countSql.toString(), countParams)
            .map(rows -> {
                if (rows.size() > 0) {
                    return rows.iterator().next().getInteger("count");
                }
                return 0;
            });
        
        return Future.all(List.of(membersFuture, countFuture))
            .map(results -> {
                List<ReferralTreeMemberDto> members = membersFuture.result();
                Integer total = countFuture.result();
                
                return ReferralTreeListDto.builder()
                    .members(members)
                    .total(total)
                    .limit(limit)
                    .offset(offset)
                    .build();
            });
    }
    
    private Future<ReferralTreeMemberDto> getChildrenForMember(
        SqlClient client,
        Long referrerId,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        sql.append("""
            SELECT 
                u.id,
                rr.referrer_id,
                referrer.nickname as referrer_nickname,
                u.referral_code as invitation_code,
                u.nickname,
                COALESCE(MAX(rr.level), 1) as level,
                COALESCE(team_stats.team_member_count, 0) as team_member_count,
                0 as total_referral_revenue,
                COALESCE(revenue_stats.referral_revenue, 0) as referral_revenue,
                DATE(rr.created_at)::text as referral_registration_date,
                TO_CHAR(rr.created_at, 'HH24:MI') as referral_registration_time,
                u.status as activity_status
            FROM users u
            INNER JOIN referral_relations rr ON rr.referred_id = u.id AND rr.referrer_id = :referrer_id AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL
            LEFT JOIN users referrer ON referrer.id = rr.referrer_id
            LEFT JOIN (
                SELECT referrer_id, COUNT(DISTINCT referred_id) as team_member_count
                FROM referral_relations
                WHERE status = 'ACTIVE' AND deleted_at IS NULL
                GROUP BY referrer_id
            ) team_stats ON team_stats.referrer_id = u.id
            LEFT JOIN (
                SELECT user_id, SUM(amount) as referral_revenue
                FROM mining_history
                WHERE type = 'REFERRAL_REWARD'
                GROUP BY user_id
            ) revenue_stats ON revenue_stats.user_id = u.id
            WHERE rr.referrer_id = :referrer_id
            """);
        
        params.put("referrer_id", referrerId);
        
        // 날짜 필터
        if (startDate != null) {
            sql.append(" AND rr.created_at >= :start_date");
            params.put("start_date", startDate);
        }
        if (endDate != null) {
            sql.append(" AND rr.created_at <= :end_date");
            params.put("end_date", endDate);
        }
        
        sql.append(" GROUP BY u.id, rr.referrer_id, referrer.nickname, u.referral_code, u.nickname, team_stats.team_member_count, revenue_stats.referral_revenue, rr.created_at, u.status");
        sql.append(" ORDER BY rr.created_at DESC");
        
        return query(client, sql.toString(), params)
            .map(rows -> {
                ReferralTreeMemberDto parent = ReferralTreeMemberDto.builder()
                    .id(referrerId)
                    .children(fetchAll(referralTreeChildMapper, rows))
                    .build();
                return parent;
            });
    }
    
    private final RowMapper<ReferralTreeMemberDto> referralTreeMemberMapper = row -> {
        return ReferralTreeMemberDto.builder()
            .id(getLong(row, "id"))
            .referrerId(getLong(row, "referrer_id"))
            .referrerNickname(getString(row, "referrer_nickname"))
            .invitationCode(getString(row, "invitation_code"))
            .nickname(getString(row, "nickname"))
            .level(getInteger(row, "level"))
            .teamMemberCount(getInteger(row, "team_member_count"))
            .totalReferralRevenue(getBigDecimal(row, "total_referral_revenue"))
            .activityStatus(getString(row, "activity_status"))
            .build();
    };
    
    private final RowMapper<ReferralTreeMemberDto> referralTreeChildMapper = row -> {
        return ReferralTreeMemberDto.builder()
            .id(getLong(row, "id"))
            .referrerId(getLong(row, "referrer_id"))
            .referrerNickname(getString(row, "referrer_nickname"))
            .invitationCode(getString(row, "invitation_code"))
            .nickname(getString(row, "nickname"))
            .level(getInteger(row, "level"))
            .teamMemberCount(getInteger(row, "team_member_count"))
            .referralRevenue(getBigDecimal(row, "referral_revenue"))
            .referralRegistrationDate(getString(row, "referral_registration_date"))
            .referralRegistrationTime(getString(row, "referral_registration_time"))
            .activityStatus(getString(row, "activity_status"))
            .build();
    };
}

