package com.csms.admin.repository;

import com.csms.admin.dto.TransactionHistoryDto;
import com.csms.admin.dto.TransactionHistoryListDto;
import com.csms.admin.dto.WithdrawalRequestDto;
import com.csms.admin.dto.WithdrawalRequestListDto;
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
public class AdminFundsRepository extends BaseRepository {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    private final RowMapper<WithdrawalRequestDto> withdrawalRequestMapper = row -> {
        LocalDateTime createdAt = getLocalDateTime(row, "created_at");
        return WithdrawalRequestDto.builder()
            .id(getLong(row, "id"))
            .orderNumber(getString(row, "order_number"))
            .userId(getLong(row, "user_id"))
            .loginId(getString(row, "login_id"))
            .nickname(getString(row, "nickname"))
            .year(createdAt != null ? createdAt.getYear() : null)
            .date(createdAt != null ? createdAt.format(DATE_FORMATTER) : null)
            .time(createdAt != null ? createdAt.format(TIME_FORMATTER) : null)
            .type("WITHDRAW")
            .asset(getString(row, "asset"))
            .network(getString(row, "network"))
            .currencyCode(getString(row, "currency_code"))
            .requestAmount(getBigDecimal(row, "request_amount"))
            .spread(getBigDecimal(row, "spread"))
            .feeRate(getBigDecimal(row, "fee_rate"))
            .realtimePrice(getBigDecimal(row, "realtime_price"))
            .settlementAmount(getBigDecimal(row, "settlement_amount"))
            .feeRevenue(getBigDecimal(row, "fee_revenue"))
            .walletAddress(getString(row, "wallet_address"))
            .status(getString(row, "status"))
            .createdAt(createdAt)
            .build();
    };
    
    private final RowMapper<TransactionHistoryDto> transactionHistoryMapper = row -> {
        LocalDateTime createdAt = getLocalDateTime(row, "created_at");
        return TransactionHistoryDto.builder()
            .id(getLong(row, "id"))
            .orderNumber(getString(row, "order_number"))
            .userId(getLong(row, "user_id"))
            .loginId(getString(row, "login_id"))
            .nickname(getString(row, "nickname"))
            .year(createdAt != null ? createdAt.getYear() : null)
            .date(createdAt != null ? createdAt.format(DATE_FORMATTER) : null)
            .time(createdAt != null ? createdAt.format(TIME_FORMATTER) : null)
            .type(getString(row, "type"))
            .asset(getString(row, "asset"))
            .network(getString(row, "network"))
            .currencyCode(getString(row, "currency_code"))
            .requestAmount(getBigDecimal(row, "request_amount"))
            .spread(getBigDecimal(row, "spread"))
            .feeRate(getBigDecimal(row, "fee_rate"))
            .realtimePrice(getBigDecimal(row, "realtime_price"))
            .settlementAmount(getBigDecimal(row, "settlement_amount"))
            .feeRevenue(getBigDecimal(row, "fee_revenue"))
            .walletAddress(getString(row, "wallet_address"))
            .status(getString(row, "status"))
            .createdAt(createdAt)
            .build();
    };
    
    /**
     * 출금신청 목록 조회
     */
    public Future<WithdrawalRequestListDto> getWithdrawalRequests(
        SqlClient client,
        Integer limit,
        Integer offset,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String network,
        String currencyCode,
        String searchCategory,
        String searchKeyword,
        String status
    ) {
        log.debug("Executing getWithdrawalRequests - limit: {}, offset: {}, startDate: {}, endDate: {}", 
            limit, offset, startDate, endDate);
        
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        // SELECT 절
        sql.append("""
            SELECT 
                et.id,
                COALESCE(et.order_number, '') as order_number,
                et.user_id,
                u.login_id,
                u.nickname,
                et.amount as request_amount,
                0 as spread,
                0 as fee_rate,
                NULL as realtime_price,
                (et.amount - COALESCE(et.fee, 0)) as settlement_amount,
                COALESCE(et.fee, 0) as fee_revenue,
                et.to_address as wallet_address,
                et.status,
                c.code as currency_code,
                c.network,
                CONCAT(c.code, '/', c.network) as asset,
                et.created_at
            FROM external_transfers et
            INNER JOIN users u ON u.id = et.user_id
            INNER JOIN currency c ON c.id = et.currency_id
            WHERE (et.transaction_type = 'WITHDRAW' OR et.transaction_type IS NULL)
            """);
        
        // 날짜 필터
        if (startDate != null) {
            sql.append(" AND et.created_at >= :start_date");
            params.put("start_date", startDate);
        }
        if (endDate != null) {
            sql.append(" AND et.created_at <= :end_date");
            params.put("end_date", endDate);
        }
        
        // 네트워크 필터
        if (network != null && !network.isEmpty()) {
            sql.append(" AND c.network = :network");
            params.put("network", network);
        }
        
        // 자산 필터
        if (currencyCode != null && !currencyCode.isEmpty()) {
            sql.append(" AND c.code = :currency_code");
            params.put("currency_code", currencyCode);
        }
        
        // 상태 필터
        if (status != null && !status.isEmpty() && !"ALL".equals(status)) {
            sql.append(" AND et.status = :status");
            params.put("status", status);
        }
        
        // 검색 조건
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ORDER_NUMBER" -> {
                    sql.append(" AND et.order_number ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
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
        
        // 정렬 및 페이지네이션
        sql.append(" ORDER BY et.created_at DESC");
        sql.append(" LIMIT :limit OFFSET :offset");
        params.put("limit", limit);
        params.put("offset", offset);
        
        // 카운트 쿼리
        StringBuilder countSql = new StringBuilder();
        countSql.append("SELECT COUNT(*) as count FROM external_transfers et");
        countSql.append(" INNER JOIN users u ON u.id = et.user_id");
        countSql.append(" INNER JOIN currency c ON c.id = et.currency_id");
        countSql.append(" WHERE et.transaction_type = 'WITHDRAW'");
        
        Map<String, Object> countParams = new HashMap<>(params);
        countParams.remove("limit");
        countParams.remove("offset");
        
        // 동일한 WHERE 조건 추가
        if (startDate != null) {
            countSql.append(" AND et.created_at >= :start_date");
        }
        if (endDate != null) {
            countSql.append(" AND et.created_at <= :end_date");
        }
        if (network != null && !network.isEmpty()) {
            countSql.append(" AND c.network = :network");
        }
        if (currencyCode != null && !currencyCode.isEmpty()) {
            countSql.append(" AND c.code = :currency_code");
        }
        if (status != null && !status.isEmpty() && !"ALL".equals(status)) {
            countSql.append(" AND et.status = :status");
        }
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ORDER_NUMBER" -> countSql.append(" AND et.order_number ILIKE :search_keyword_pattern");
                case "ID" -> countSql.append(" AND (u.id::text = :search_keyword OR u.login_id ILIKE :search_keyword_pattern)");
                case "NICKNAME" -> countSql.append(" AND u.nickname ILIKE :search_keyword_pattern");
            }
        }
        
        // 합계 쿼리
        StringBuilder sumSql = new StringBuilder();
        sumSql.append("""
            SELECT 
                COALESCE(SUM(et.amount), 0) as total_withdrawal_amount,
                COALESCE(SUM(et.fee), 0) as total_fee_revenue
            FROM external_transfers et
            INNER JOIN users u ON u.id = et.user_id
            INNER JOIN currency c ON c.id = et.currency_id
            WHERE et.transaction_type = 'WITHDRAW'
            """);
        
        Map<String, Object> sumParams = new HashMap<>(countParams);
        
        // 동일한 WHERE 조건 추가
        if (startDate != null) {
            sumSql.append(" AND et.created_at >= :start_date");
        }
        if (endDate != null) {
            sumSql.append(" AND et.created_at <= :end_date");
        }
        if (network != null && !network.isEmpty()) {
            sumSql.append(" AND c.network = :network");
        }
        if (currencyCode != null && !currencyCode.isEmpty()) {
            sumSql.append(" AND c.code = :currency_code");
        }
        if (status != null && !status.isEmpty() && !"ALL".equals(status)) {
            sumSql.append(" AND et.status = :status");
        }
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ORDER_NUMBER" -> sumSql.append(" AND et.order_number ILIKE :search_keyword_pattern");
                case "ID" -> sumSql.append(" AND (u.id::text = :search_keyword OR u.login_id ILIKE :search_keyword_pattern)");
                case "NICKNAME" -> sumSql.append(" AND u.nickname ILIKE :search_keyword_pattern");
            }
        }
        
        Future<List<WithdrawalRequestDto>> requestsFuture = query(client, sql.toString(), params)
            .map(rows -> fetchAll(withdrawalRequestMapper, rows));
        
        Future<Integer> countFuture = query(client, countSql.toString(), countParams)
            .map(rows -> {
                if (rows.size() > 0) {
                    return rows.iterator().next().getInteger("count");
                }
                return 0;
            });
        
        Future<Map<String, BigDecimal>> sumFuture = query(client, sumSql.toString(), sumParams)
            .map(rows -> {
                Map<String, BigDecimal> sums = new HashMap<>();
                if (rows.size() > 0) {
                    Row row = rows.iterator().next();
                    sums.put("total_withdrawal_amount", getBigDecimal(row, "total_withdrawal_amount"));
                    sums.put("total_fee_revenue", getBigDecimal(row, "total_fee_revenue"));
                } else {
                    sums.put("total_withdrawal_amount", BigDecimal.ZERO);
                    sums.put("total_fee_revenue", BigDecimal.ZERO);
                }
                return sums;
            });
        
        return Future.all(List.of(requestsFuture, countFuture, sumFuture))
            .map(results -> {
                List<WithdrawalRequestDto> requests = requestsFuture.result();
                Integer total = countFuture.result();
                Map<String, BigDecimal> sums = sumFuture.result();
                
                return WithdrawalRequestListDto.builder()
                    .requests(requests)
                    .total(total)
                    .limit(limit)
                    .offset(offset)
                    .totalWithdrawalAmount(sums.get("total_withdrawal_amount"))
                    .totalFeeRevenue(sums.get("total_fee_revenue"))
                    .build();
            });
    }
    
    /**
     * 출금신청 상태 업데이트
     */
    public Future<Void> updateWithdrawalStatus(SqlClient client, Long id, String status) {
        log.debug("Executing updateWithdrawalStatus - id: {}, status: {}", id, status);
        
        String sql = """
            UPDATE external_transfers
            SET status = :status
            WHERE id = :id AND (transaction_type = 'WITHDRAW' OR transaction_type IS NULL)
            """;
        
        Map<String, Object> params = Map.of(
            "id", id,
            "status", status
        );
        
        return query(client, sql, params)
            .map(rows -> {
                if (rows.rowCount() == 0) {
                    throw new RuntimeException("Withdrawal request not found");
                }
                return null;
            });
    }
    
    /**
     * 거래내역 목록 조회 (통합)
     */
    public Future<TransactionHistoryListDto> getTransactionHistory(
        SqlClient client,
        Integer limit,
        Integer offset,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String transactionType,
        String currencyCode,
        String searchCategory,
        String searchKeyword,
        String status
    ) {
        log.debug("Executing getTransactionHistory - limit: {}, offset: {}, startDate: {}, endDate: {}", 
            limit, offset, startDate, endDate);
        
        // UNION을 사용하여 모든 거래 유형 통합
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        sql.append("""
            WITH all_transactions AS (
                -- 출금 (WITHDRAW)
                SELECT 
                    et.id,
                    COALESCE(et.order_number, '') as order_number,
                    et.user_id,
                    u.login_id,
                    u.nickname,
                    et.amount as request_amount,
                    0 as spread,
                    0 as fee_rate,
                    NULL as realtime_price,
                    (et.amount - COALESCE(et.fee, 0)) as settlement_amount,
                    COALESCE(et.fee, 0) as fee_revenue,
                    et.to_address as wallet_address,
                    et.status,
                    c.code as currency_code,
                    c.network,
                    CONCAT(c.code, '/', c.network) as asset,
                    'WITHDRAW' as type,
                    et.created_at
                FROM external_transfers et
                INNER JOIN users u ON u.id = et.user_id
                INNER JOIN currency c ON c.id = et.currency_id
                WHERE (et.transaction_type = 'WITHDRAW' OR et.transaction_type IS NULL)
            """);
        
        // 날짜 필터
        if (startDate != null) {
            sql.append(" AND et.created_at >= :start_date");
            params.put("start_date", startDate);
        }
        if (endDate != null) {
            sql.append(" AND et.created_at <= :end_date");
            params.put("end_date", endDate);
        }
        
        // 거래 유형 필터
        if (transactionType != null && !transactionType.isEmpty() && !"ALL".equals(transactionType)) {
            // WITHDRAW만 처리 (나중에 다른 타입 추가)
        }
        
        // 자산 필터
        if (currencyCode != null && !currencyCode.isEmpty()) {
            sql.append(" AND c.code = :currency_code");
            params.put("currency_code", currencyCode);
        }
        
        // 상태 필터
        if (status != null && !status.isEmpty() && !"ALL".equals(status)) {
            sql.append(" AND et.status = :status");
            params.put("status", status);
        }
        
        // 검색 조건
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ORDER_NUMBER" -> {
                    sql.append(" AND et.order_number ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
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
        
        // TODO: 다른 거래 유형 추가 (TOKEN_DEPOSIT, PAYMENT_DEPOSIT, SWAP, EXCHANGE)
        // 현재는 WITHDRAW만 구현
        
        sql.append("""
            )
            SELECT * FROM all_transactions
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
            """);
        
        params.put("limit", limit);
        params.put("offset", offset);
        
        // 카운트 쿼리
        StringBuilder countSql = new StringBuilder();
        countSql.append("""
            SELECT COUNT(*) as count
            FROM external_transfers et
            INNER JOIN users u ON u.id = et.user_id
            INNER JOIN currency c ON c.id = et.currency_id
            WHERE (et.transaction_type = 'WITHDRAW' OR et.transaction_type IS NULL)
            """);
        
        Map<String, Object> countParams = new HashMap<>(params);
        countParams.remove("limit");
        countParams.remove("offset");
        
        // 동일한 WHERE 조건 추가
        if (startDate != null) {
            countSql.append(" AND et.created_at >= :start_date");
        }
        if (endDate != null) {
            countSql.append(" AND et.created_at <= :end_date");
        }
        if (currencyCode != null && !currencyCode.isEmpty()) {
            countSql.append(" AND c.code = :currency_code");
        }
        if (status != null && !status.isEmpty() && !"ALL".equals(status)) {
            countSql.append(" AND et.status = :status");
        }
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ORDER_NUMBER" -> countSql.append(" AND et.order_number ILIKE :search_keyword_pattern");
                case "ID" -> countSql.append(" AND (u.id::text = :search_keyword OR u.login_id ILIKE :search_keyword_pattern)");
                case "NICKNAME" -> countSql.append(" AND u.nickname ILIKE :search_keyword_pattern");
            }
        }
        
        // 합계 쿼리
        StringBuilder sumSql = new StringBuilder();
        sumSql.append("""
            SELECT 
                COALESCE(SUM(CASE WHEN (et.transaction_type = 'WITHDRAW' OR et.transaction_type IS NULL) THEN et.amount ELSE 0 END), 0) as total_withdrawal_amount,
                COALESCE(SUM(CASE WHEN (et.transaction_type = 'WITHDRAW' OR et.transaction_type IS NULL) THEN et.fee ELSE 0 END), 0) as total_fee_revenue
            FROM external_transfers et
            INNER JOIN users u ON u.id = et.user_id
            INNER JOIN currency c ON c.id = et.currency_id
            WHERE (et.transaction_type = 'WITHDRAW' OR et.transaction_type IS NULL)
            """);
        
        Map<String, Object> sumParams = new HashMap<>(countParams);
        
        // 동일한 WHERE 조건 추가
        if (startDate != null) {
            sumSql.append(" AND et.created_at >= :start_date");
        }
        if (endDate != null) {
            sumSql.append(" AND et.created_at <= :end_date");
        }
        if (currencyCode != null && !currencyCode.isEmpty()) {
            sumSql.append(" AND c.code = :currency_code");
        }
        if (status != null && !status.isEmpty() && !"ALL".equals(status)) {
            sumSql.append(" AND et.status = :status");
        }
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ORDER_NUMBER" -> sumSql.append(" AND et.order_number ILIKE :search_keyword_pattern");
                case "ID" -> sumSql.append(" AND (u.id::text = :search_keyword OR u.login_id ILIKE :search_keyword_pattern)");
                case "NICKNAME" -> sumSql.append(" AND u.nickname ILIKE :search_keyword_pattern");
            }
        }
        
        // TODO: 다른 거래 유형의 합계도 추가 필요
        
        Future<List<TransactionHistoryDto>> transactionsFuture = query(client, sql.toString(), params)
            .map(rows -> fetchAll(transactionHistoryMapper, rows));
        
        Future<Integer> countFuture = query(client, countSql.toString(), countParams)
            .map(rows -> {
                if (rows.size() > 0) {
                    return rows.iterator().next().getInteger("count");
                }
                return 0;
            });
        
        Future<Map<String, BigDecimal>> sumFuture = query(client, sumSql.toString(), sumParams)
            .map(rows -> {
                Map<String, BigDecimal> sums = new HashMap<>();
                if (rows.size() > 0) {
                    Row row = rows.iterator().next();
                    sums.put("total_withdrawal_amount", getBigDecimal(row, "total_withdrawal_amount"));
                    sums.put("total_fee_revenue", getBigDecimal(row, "total_fee_revenue"));
                } else {
                    sums.put("total_withdrawal_amount", BigDecimal.ZERO);
                    sums.put("total_fee_revenue", BigDecimal.ZERO);
                }
                // TODO: 다른 거래 유형 합계 추가
                sums.put("total_payment_deposit_amount", BigDecimal.ZERO);
                sums.put("total_token_deposit_amount", BigDecimal.ZERO);
                sums.put("total_exchange_amount", BigDecimal.ZERO);
                sums.put("total_swap_amount", BigDecimal.ZERO);
                return sums;
            });
        
        return Future.all(List.of(transactionsFuture, countFuture, sumFuture))
            .map(results -> {
                List<TransactionHistoryDto> transactions = transactionsFuture.result();
                Integer total = countFuture.result();
                Map<String, BigDecimal> sums = sumFuture.result();
                
                return TransactionHistoryListDto.builder()
                    .transactions(transactions)
                    .total(total)
                    .limit(limit)
                    .offset(offset)
                    .totalPaymentDepositAmount(sums.get("total_payment_deposit_amount"))
                    .totalTokenDepositAmount(sums.get("total_token_deposit_amount"))
                    .totalWithdrawalAmount(sums.get("total_withdrawal_amount"))
                    .totalExchangeAmount(sums.get("total_exchange_amount"))
                    .totalSwapAmount(sums.get("total_swap_amount"))
                    .totalFeeRevenue(sums.get("total_fee_revenue"))
                    .build();
            });
    }
}

