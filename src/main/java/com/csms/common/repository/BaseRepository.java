package com.csms.common.repository;

import com.csms.common.database.RowMapper;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.templates.SqlTemplate;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class BaseRepository {
    
    public static final RowMapper<Integer> COUNT_MAPPER = row -> row.getInteger("count");
    
    protected Future<RowSet<Row>> query(SqlClient client, String sql, Map<String, Object> parameters) {
        log.debug("Executing SQL: {}\nParameters: {}", sql, parameters);
        return SqlTemplate.forQuery(client, sql).execute(parameters);
    }
    
    protected Future<RowSet<Row>> query(SqlClient client, String sql) {
        log.debug("Executing SQL: {}", sql);
        return client.query(sql).execute();
    }
    
    protected boolean success(RowSet<Row> rows) {
        return rows.rowCount() == 1;
    }
    
    protected boolean successAll(RowSet<Row> rows) {
        return rows.rowCount() >= 1;
    }
    
    protected boolean hasColumn(Row row, String column) {
        return row.getColumnIndex(column) != -1;
    }
    
    protected String getString(Row row, String column) {
        return hasColumn(row, column) ? row.getString(column) : null;
    }
    
    protected Integer getInteger(Row row, String column) {
        return hasColumn(row, column) ? row.getInteger(column) : null;
    }
    
    protected Long getLong(Row row, String column) {
        return hasColumn(row, column) ? row.getLong(column) : null;
    }
    
    protected BigDecimal getBigDecimal(Row row, String column) {
        return hasColumn(row, column) ? row.getBigDecimal(column) : null;
    }
    
    protected LocalDateTime getLocalDateTime(Row row, String column) {
        return hasColumn(row, column) ? row.getLocalDateTime(column) : null;
    }
    
    protected JsonObject getJsonObject(Row row, String column) {
        return hasColumn(row, column) ? row.getJsonObject(column) : null;
    }
    
    protected Double getDouble(Row row, String column) {
        return hasColumn(row, column) ? row.getDouble(column) : null;
    }
    
    protected Boolean getBoolean(Row row, String column) {
        return hasColumn(row, column) ? row.getBoolean(column) : null;
    }
    
    protected <T> T getValue(Row row, Class<T> type, String column) {
        return hasColumn(row, column) && row.getString(column) != null 
            ? row.get(type, column) 
            : null;
    }
    
    protected <T> T fetchOne(RowMapper<T> mapper, RowSet<Row> rows) {
        if (rows.size() == 0) {
            return null;
        }
        return mapper.map(rows.iterator().next());
    }
    
    protected <T> List<T> fetchAll(RowMapper<T> mapper, RowSet<Row> rows) {
        if (rows.size() == 0) {
            return List.of();
        }
        
        List<T> data = new ArrayList<>();
        for (Row row : rows) {
            data.add(mapper.map(row));
        }
        return data;
    }
}

