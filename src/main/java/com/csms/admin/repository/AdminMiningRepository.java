package com.csms.admin.repository;

import com.csms.admin.dto.MiningRecordDto;
import com.csms.admin.dto.MiningRecordListDto;
import com.csms.common.database.RowMapper;
import com.csms.common.repository.BaseRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AdminMiningRepository extends BaseRepository {
    
    private final PgPool pool;
    
    public AdminMiningRepository(PgPool pool) {
        this.pool = pool;
    }
    
    private final RowMapper<MiningRecordDto> miningRecordMapper = row -> MiningRecordDto.builder()
        .id(getLong(row, "id"))
        .userId(getLong(row, "user_id"))
        .referrerNickname(getString(row, "referrer_nickname"))
        .nickname(getString(row, "nickname"))
        .email(getString(row, "email"))
        .level(getInteger(row, "level"))
        .miningStartTime(getLocalDateTime(row, "mining_start_time"))
        .miningEndTime(getLocalDateTime(row, "mining_end_time"))
        .miningAmount(getDouble(row, "mining_amount"))
        .cumulativeMiningAmount(getDouble(row, "cumulative_mining_amount"))
        .miningEfficiency(getInteger(row, "mining_efficiency"))
        .activityStatus(getString(row, "activity_status"))
        .build();
    
    public Future<MiningRecordListDto> getMiningRecords(
        Integer limit,
        Integer offset,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String searchCategory,
        String searchKeyword,
        String activityStatus
    ) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        // SELECT 절
        sql.append("""
            SELECT 
                mh.id,
                mh.user_id,
                referrer.nickname as referrer_nickname,
                u.nickname,
                u.email,
                u.level,
                mh.created_at as mining_start_time,
                mh.updated_at as mining_end_time,
                mh.amount as mining_amount,
                COALESCE(cumulative_stats.cumulative_amount, 0) as cumulative_mining_amount,
                COALESCE(mh.efficiency, 0) as mining_efficiency,
                u.status as activity_status
            FROM mining_history mh
            INNER JOIN users u ON u.id = mh.user_id
            LEFT JOIN referral_relations rr ON rr.referred_id = u.id AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL
            LEFT JOIN users referrer ON referrer.id = rr.referrer_id
            LEFT JOIN (
                SELECT 
                    user_id,
                    SUM(amount) OVER (PARTITION BY user_id ORDER BY created_at) as cumulative_amount
                FROM mining_history
                WHERE type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH')
            ) cumulative_stats ON cumulative_stats.user_id = mh.user_id
            WHERE mh.type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH')
            AND mh.created_at >= :start_date
            AND mh.created_at <= :end_date
            """);
        
        params.put("start_date", startDate);
        params.put("end_date", endDate);
        
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
                case "NICKNAME" -> {
                    sql.append(" AND u.nickname ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "EMAIL" -> {
                    sql.append(" AND u.email ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "LEVEL" -> {
                    try {
                        Integer level = Integer.parseInt(searchKeyword);
                        sql.append(" AND u.level = :search_level");
                        params.put("search_level", level);
                    } catch (NumberFormatException e) {
                        // 레벨 파싱 실패 시 무시
                    }
                }
            }
        }
        
        // 활동상태 필터
        if (activityStatus != null && !activityStatus.equals("ALL")) {
            sql.append(" AND u.status = :activity_status");
            params.put("activity_status", activityStatus);
        }
        
        // 정렬 및 페이지네이션
        sql.append(" ORDER BY mh.created_at DESC");
        
        // 총 개수 조회를 위한 별도 쿼리
        StringBuilder countSql = new StringBuilder();
        countSql.append("""
            SELECT COUNT(*) as total
            FROM mining_history mh
            INNER JOIN users u ON u.id = mh.user_id
            LEFT JOIN referral_relations rr ON rr.referred_id = u.id AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL
            LEFT JOIN users referrer ON referrer.id = rr.referrer_id
            WHERE mh.type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH')
            AND mh.created_at >= :start_date
            AND mh.created_at <= :end_date
            """);
        
        // 검색 조건 추가 (count 쿼리에도 동일하게 적용)
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ID" -> countSql.append(" AND (u.id::text = :search_keyword OR u.login_id ILIKE :search_keyword_pattern)");
                case "REFERRER" -> countSql.append(" AND referrer.nickname ILIKE :search_keyword_pattern");
                case "NICKNAME" -> countSql.append(" AND u.nickname ILIKE :search_keyword_pattern");
                case "EMAIL" -> countSql.append(" AND u.email ILIKE :search_keyword_pattern");
                case "LEVEL" -> {
                    try {
                        Integer level = Integer.parseInt(searchKeyword);
                        countSql.append(" AND u.level = :search_level");
                    } catch (NumberFormatException e) {
                        // 레벨 파싱 실패 시 무시
                    }
                }
            }
        }
        
        if (activityStatus != null && !activityStatus.equals("ALL")) {
            countSql.append(" AND u.status = :activity_status");
        }
        
        Map<String, Object> countParams = new HashMap<>(params);
        
        return query(pool, countSql.toString(), countParams)
            .compose(countRows -> {
                Integer totalValue = fetchOne(row -> getInteger(row, "total"), countRows);
                final Integer total = totalValue != null ? totalValue : 0;
                
                // 데이터 조회
                sql.append(" LIMIT :limit OFFSET :offset");
                params.put("limit", limit);
                params.put("offset", offset);
                
                return query(pool, sql.toString(), params)
                    .map(rows -> {
                        List<MiningRecordDto> records = new ArrayList<>();
                        for (var row : rows) {
                            records.add(miningRecordMapper.map(row));
                        }
                        return MiningRecordListDto.builder()
                            .records(records)
                            .total(total)
                            .limit(limit)
                            .offset(offset)
                            .build();
                    });
            })
            .onFailure(throwable -> log.error("채굴 기록 조회 실패", throwable));
    }
}

