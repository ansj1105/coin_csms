package com.csms.admin.repository;

import com.csms.admin.dto.AirdropPhaseDto;
import com.csms.admin.dto.AirdropPhaseListDto;
import com.csms.admin.dto.AirdropTransferDto;
import com.csms.admin.dto.AirdropTransferListDto;
import com.csms.common.database.RowMapper;
import com.csms.common.repository.BaseRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AdminAirdropRepository extends BaseRepository {
    
    private final RowMapper<AirdropPhaseDto> phaseMapper = row -> AirdropPhaseDto.builder()
        .id(getLong(row, "id"))
        .userId(getLong(row, "user_id"))
        .loginId(getString(row, "login_id"))
        .nickname(getString(row, "nickname"))
        .phase(getInteger(row, "phase"))
        .status(getString(row, "status"))
        .amount(getBigDecimal(row, "amount"))
        .claimed(getBoolean(row, "claimed"))
        .unlockDate(getLocalDateTime(row, "unlock_date"))
        .daysRemaining(getInteger(row, "days_remaining"))
        .createdAt(getLocalDateTime(row, "created_at"))
        .updatedAt(getLocalDateTime(row, "updated_at"))
        .build();
    
    private final RowMapper<AirdropTransferDto> transferMapper = row -> AirdropTransferDto.builder()
        .id(getLong(row, "id"))
        .transferId(getString(row, "transfer_id"))
        .userId(getLong(row, "user_id"))
        .loginId(getString(row, "login_id"))
        .nickname(getString(row, "nickname"))
        .walletId(getLong(row, "wallet_id"))
        .currencyId(getInteger(row, "currency_id"))
        .currencyCode(getString(row, "currency_code"))
        .amount(getBigDecimal(row, "amount"))
        .status(getString(row, "status"))
        .orderNumber(getString(row, "order_number"))
        .createdAt(getLocalDateTime(row, "created_at"))
        .updatedAt(getLocalDateTime(row, "updated_at"))
        .build();
    
    /**
     * 에어드랍 Phase 목록 조회 (검색, 필터링, 페이지네이션)
     */
    public Future<AirdropPhaseListDto> getPhases(
        SqlClient client,
        Integer limit,
        Integer offset,
        String searchCategory,
        String searchKeyword,
        Long userId,
        Integer phase,
        String status
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                ap.id,
                ap.user_id,
                u.login_id,
                u.nickname,
                ap.phase,
                ap.status,
                ap.amount,
                ap.claimed,
                ap.unlock_date,
                ap.days_remaining,
                ap.created_at,
                ap.updated_at
            FROM airdrop_phases ap
            INNER JOIN users u ON u.id = ap.user_id
            WHERE ap.deleted_at IS NULL
            """);
        
        Map<String, Object> params = new HashMap<>();
        
        // 검색 조건
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ID" -> {
                    sql.append(" AND (u.id::text = :search_keyword OR u.login_id ILIKE :search_keyword_pattern)");
                    params.put("search_keyword", searchKeyword);
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "NICKNAME" -> {
                    sql.append(" AND u.nickname ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
            }
        }
        
        // 필터 조건
        if (userId != null) {
            sql.append(" AND ap.user_id = :user_id");
            params.put("user_id", userId);
        }
        
        if (phase != null) {
            sql.append(" AND ap.phase = :phase");
            params.put("phase", phase);
        }
        
        if (status != null && !status.isEmpty()) {
            sql.append(" AND ap.status = :status");
            params.put("status", status);
        }
        
        // 정렬
        sql.append(" ORDER BY ap.created_at DESC");
        
        // 전체 개수 조회
        String countSql = "SELECT COUNT(*) as total FROM (" + sql.toString() + ") as subquery";
        
        // 페이지네이션
        sql.append(" LIMIT :limit OFFSET :offset");
        params.put("limit", limit);
        params.put("offset", offset);
        
        return query(client, countSql, params)
            .compose(countRows -> {
                Integer total = countRows.iterator().hasNext() 
                    ? getInteger(countRows.iterator().next(), "total") 
                    : 0;
                
                return query(client, sql.toString(), params)
                    .map(rows -> {
                        List<AirdropPhaseDto> phases = new ArrayList<>();
                        for (var row : rows) {
                            phases.add(phaseMapper.map(row));
                        }
                        return AirdropPhaseListDto.builder()
                            .phases(phases)
                            .total(total)
                            .limit(limit)
                            .offset(offset)
                            .build();
                    });
            })
            .onFailure(throwable -> log.error("에어드랍 Phase 목록 조회 실패", throwable));
    }
    
    /**
     * 에어드랍 Phase 단건 조회
     */
    public Future<AirdropPhaseDto> getPhaseById(SqlClient client, Long phaseId) {
        String sql = """
            SELECT 
                ap.id,
                ap.user_id,
                u.login_id,
                u.nickname,
                ap.phase,
                ap.status,
                ap.amount,
                ap.claimed,
                ap.unlock_date,
                ap.days_remaining,
                ap.created_at,
                ap.updated_at
            FROM airdrop_phases ap
            INNER JOIN users u ON u.id = ap.user_id
            WHERE ap.id = :phase_id
            AND ap.deleted_at IS NULL
            """;
        
        return query(client, sql, Collections.singletonMap("phase_id", phaseId))
            .map(rows -> fetchOne(phaseMapper, rows))
            .onFailure(throwable -> log.error("에어드랍 Phase 조회 실패 - phaseId: {}", phaseId, throwable));
    }
    
    /**
     * 에어드랍 Phase 생성
     */
    public Future<AirdropPhaseDto> createPhase(
        SqlClient client,
        Long userId,
        Integer phase,
        BigDecimal amount,
        LocalDateTime unlockDate,
        Integer daysRemaining
    ) {
        // unlockDate가 없으면 daysRemaining으로 계산
        if (unlockDate == null && daysRemaining != null) {
            unlockDate = LocalDateTime.now().plusDays(daysRemaining);
        }
        
        String sql = """
            INSERT INTO airdrop_phases (user_id, phase, amount, unlock_date, days_remaining, status, created_at, updated_at)
            VALUES (:user_id, :phase, :amount, :unlock_date, :days_remaining, 'PROCESSING', NOW(), NOW())
            RETURNING id, user_id, phase, status, amount, claimed, unlock_date, days_remaining, created_at, updated_at
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("user_id", userId);
        params.put("phase", phase);
        params.put("amount", amount);
        params.put("unlock_date", unlockDate);
        params.put("days_remaining", daysRemaining);
        
        return query(client, sql, params)
            .compose(rows -> {
                var row = rows.iterator().next();
                Long phaseId = getLong(row, "id");
                return getPhaseById(client, phaseId);
            })
            .onFailure(throwable -> log.error("에어드랍 Phase 생성 실패", throwable));
    }
    
    /**
     * 에어드랍 Phase 수정
     */
    public Future<AirdropPhaseDto> updatePhase(
        SqlClient client,
        Long phaseId,
        BigDecimal amount,
        LocalDateTime unlockDate,
        Integer daysRemaining,
        String status
    ) {
        StringBuilder sql = new StringBuilder("UPDATE airdrop_phases SET updated_at = NOW()");
        Map<String, Object> params = new HashMap<>();
        params.put("phase_id", phaseId);
        
        if (amount != null) {
            sql.append(", amount = :amount");
            params.put("amount", amount);
        }
        
        if (unlockDate != null) {
            sql.append(", unlock_date = :unlock_date");
            params.put("unlock_date", unlockDate);
        } else if (daysRemaining != null) {
            // daysRemaining이 변경되면 unlockDate 재계산
            sql.append(", unlock_date = NOW() + (INTERVAL '1 day' * :days_remaining), days_remaining = :days_remaining");
            params.put("days_remaining", daysRemaining);
        }
        
        if (status != null && !status.isEmpty()) {
            sql.append(", status = :status");
            params.put("status", status);
        }
        
        sql.append(" WHERE id = :phase_id AND deleted_at IS NULL RETURNING id");
        
        return query(client, sql.toString(), params)
            .compose(rows -> {
                if (rows.size() == 0) {
                    return Future.failedFuture("Phase not found");
                }
                return getPhaseById(client, phaseId);
            })
            .onFailure(throwable -> log.error("에어드랍 Phase 수정 실패 - phaseId: {}", phaseId, throwable));
    }
    
    /**
     * 에어드랍 Phase 삭제 (Soft Delete)
     */
    public Future<Void> deletePhase(SqlClient client, Long phaseId) {
        String sql = """
            UPDATE airdrop_phases
            SET deleted_at = NOW(), updated_at = NOW()
            WHERE id = :phase_id
            AND deleted_at IS NULL
            """;
        
        return query(client, sql, Collections.singletonMap("phase_id", phaseId))
            .map(updateResult -> {
                if (updateResult.rowCount() == 0) {
                    throw new RuntimeException("Phase not found");
                }
                return (Void) null;
            })
            .onFailure(throwable -> log.error("에어드랍 Phase 삭제 실패 - phaseId: {}", phaseId, throwable));
    }
    
    /**
     * 에어드랍 전송 내역 조회
     */
    public Future<AirdropTransferListDto> getTransfers(
        SqlClient client,
        Integer limit,
        Integer offset,
        String searchCategory,
        String searchKeyword,
        Long userId,
        String status
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                at.id,
                at.transfer_id,
                at.user_id,
                u.login_id,
                u.nickname,
                at.wallet_id,
                at.currency_id,
                c.code as currency_code,
                at.amount,
                at.status,
                at.order_number,
                at.created_at,
                at.updated_at
            FROM airdrop_transfers at
            INNER JOIN users u ON u.id = at.user_id
            INNER JOIN currency c ON c.id = at.currency_id
            WHERE at.deleted_at IS NULL
            """);
        
        Map<String, Object> params = new HashMap<>();
        
        // 검색 조건
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ID" -> {
                    sql.append(" AND (u.id::text = :search_keyword OR u.login_id ILIKE :search_keyword_pattern)");
                    params.put("search_keyword", searchKeyword);
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "NICKNAME" -> {
                    sql.append(" AND u.nickname ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "TRANSFER_ID" -> {
                    sql.append(" AND at.transfer_id ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
            }
        }
        
        // 필터 조건
        if (userId != null) {
            sql.append(" AND at.user_id = :user_id");
            params.put("user_id", userId);
        }
        
        if (status != null && !status.isEmpty()) {
            sql.append(" AND at.status = :status");
            params.put("status", status);
        }
        
        // 정렬
        sql.append(" ORDER BY at.created_at DESC");
        
        // 전체 개수 조회
        String countSql = "SELECT COUNT(*) as total FROM (" + sql.toString() + ") as subquery";
        
        // 페이지네이션
        sql.append(" LIMIT :limit OFFSET :offset");
        params.put("limit", limit);
        params.put("offset", offset);
        
        return query(client, countSql, params)
            .compose(countRows -> {
                Integer total = countRows.iterator().hasNext() 
                    ? getInteger(countRows.iterator().next(), "total") 
                    : 0;
                
                return query(client, sql.toString(), params)
                    .map(rows -> {
                        List<AirdropTransferDto> transfers = new ArrayList<>();
                        for (var row : rows) {
                            transfers.add(transferMapper.map(row));
                        }
                        return AirdropTransferListDto.builder()
                            .transfers(transfers)
                            .total(total)
                            .limit(limit)
                            .offset(offset)
                            .build();
                    });
            })
            .onFailure(throwable -> log.error("에어드랍 전송 내역 조회 실패", throwable));
    }
}
