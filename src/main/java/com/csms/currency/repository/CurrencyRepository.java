package com.csms.currency.repository;

import com.csms.common.database.RowMapper;
import com.csms.common.repository.BaseRepository;
import com.csms.currency.entities.Currency;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;

@Slf4j
public class CurrencyRepository extends BaseRepository {
    
    private final RowMapper<Currency> currencyMapper = row -> Currency.builder()
        .id(getInteger(row, "id"))
        .code(getString(row, "code"))
        .name(getString(row, "name"))
        .chain(getString(row, "chain"))
        .isActive(getBoolean(row, "is_active"))
        .createdAt(getLocalDateTime(row, "created_at"))
        .updatedAt(getLocalDateTime(row, "updated_at"))
        .build();
    
    public Future<List<Currency>> getActiveCurrencies(SqlClient client) {
        String sql = """
            SELECT * FROM currency 
            WHERE is_active = true 
            ORDER BY code, chain
            """;
        
        return query(client, sql, new HashMap<>())
            .map(rows -> fetchAll(currencyMapper, rows))
            .onFailure(throwable -> log.error("통화 목록 조회 실패", throwable));
    }
}

