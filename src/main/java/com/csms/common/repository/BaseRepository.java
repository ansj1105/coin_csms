package com.csms.common.repository;

import com.csms.common.database.RowMapper;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.templates.SqlTemplate;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class BaseRepository {
    
    public static final RowMapper<Integer> COUNT_MAPPER = row -> row.getInteger("count");
    
    // :parameter_name 형식을 $1, $2 등으로 변환하는 패턴
    // :: (두 개의 콜론, PostgreSQL 캐스팅)은 제외하고, 단일 콜론으로 시작하는 파라미터만 매칭
    private static final Pattern PARAM_PATTERN = Pattern.compile("(?<!:):([a-zA-Z_][a-zA-Z0-9_]*)");
    
    /**
     * 날짜/시간 타입을 명시적으로 처리하여 Tuple에 추가
     * LocalDateTime과 LocalDate는 PostgreSQL과의 호환성을 위해 명시적으로 처리
     */
    private void addParameterToTuple(Tuple tuple, Object value) {
        if (value == null) {
            tuple.addValue(null);
        } else if (value instanceof LocalDateTime) {
            tuple.addLocalDateTime((LocalDateTime) value);
        } else if (value instanceof LocalDate) {
            tuple.addLocalDate((LocalDate) value);
        } else {
            tuple.addValue(value);
        }
    }
    
    protected Future<RowSet<Row>> query(SqlClient client, String sql, Map<String, Object> parameters) {
        log.debug("Executing SQL: {}\nParameters: {}", sql, parameters);
        
        if (parameters == null || parameters.isEmpty()) {
            return query(client, sql);
        }
        
        // SqlTemplate의 파라미터 바인딩 문제를 우회하기 위해
        // :parameter_name 형식을 $1, $2 등으로 변환하여 PreparedQuery 사용
        try {
            // 파라미터를 순서대로 정렬하여 Tuple 생성
            List<String> paramNames = new ArrayList<>();
            Matcher matcher = PARAM_PATTERN.matcher(sql);
            while (matcher.find()) {
                if (matcher.groupCount() >= 1) {
                    String paramName = matcher.group(1);
                    if (paramName != null && !paramNames.contains(paramName)) {
                        paramNames.add(paramName);
                    }
                }
            }
            
            if (paramNames.isEmpty()) {
                // 파라미터가 없으면 일반 쿼리로 실행
                return query(client, sql);
            }
            
            // SQL에서 :parameter_name을 $1, $2 등으로 치환
            // ::date 같은 PostgreSQL 캐스팅은 유지
            // 각 파라미터 이름에 대한 매핑 생성
            Map<String, String> paramToPlaceholder = new LinkedHashMap<>();
            for (int i = 0; i < paramNames.size(); i++) {
                paramToPlaceholder.put(paramNames.get(i), "$" + (i + 1));
            }
            
            // 정규표현식을 사용하여 각 파라미터를 찾아 치환 (역순으로 처리하여 긴 이름 우선)
            String preparedSql = sql;
            List<String> sortedParamNames = new ArrayList<>(paramNames);
            sortedParamNames.sort((a, b) -> Integer.compare(b.length(), a.length()));
            
            for (String paramName : sortedParamNames) {
                String placeholder = paramToPlaceholder.get(paramName);
                // 정규표현식으로 :paramName 패턴만 매칭 (앞에 :가 하나만 있고, 뒤에 단어 문자가 아닌 것)
                // 파라미터 이름은 [a-zA-Z_][a-zA-Z0-9_]* 패턴이므로 특수문자 이스케이프 불필요
                String regex = "(?<!:):" + paramName + "(?!\\w)";
                // Matcher를 사용하여 직접 치환 ($ 문자가 특수문자로 해석되지 않도록)
                Pattern pattern = Pattern.compile(regex);
                Matcher replacementMatcher = pattern.matcher(preparedSql);
                StringBuffer sb = new StringBuffer();
                while (replacementMatcher.find()) {
                    replacementMatcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
                }
                replacementMatcher.appendTail(sb);
                preparedSql = sb.toString();
            }
            
            // Tuple 생성 - 날짜/시간 타입을 명시적으로 처리
            Tuple tuple = Tuple.tuple();
            for (String paramName : paramNames) {
                Object value = parameters.get(paramName);
                addParameterToTuple(tuple, value);
                log.debug("  Parameter: {} = {} (type: {})", paramName, value, value != null ? value.getClass().getSimpleName() : "null");
            }
            
            final String finalPreparedSql = preparedSql;
            final Tuple finalTuple = tuple;
            
            log.debug("SQL template creation - SQL length: {}, Parameter count: {}", finalPreparedSql.length(), finalTuple.size());
            
            return client.preparedQuery(finalPreparedSql)
                .execute(finalTuple)
                .onFailure(err -> {
                    log.error("SQL execution failed - SQL: {}\nPrepared SQL: {}\nParameters: {}\nTuple: {}\nError: {}", 
                        sql, finalPreparedSql, parameters, finalTuple, err.getMessage(), err);
                });
        } catch (Exception e) {
            log.warn("Failed to convert named parameters to positional, falling back to SqlTemplate: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            // 실패 시 원래 SqlTemplate 방식으로 시도
            return SqlTemplate.forQuery(client, sql)
                .execute(parameters)
                .onFailure(err -> {
                    log.error("SQL execution failed - SQL: {}\nParameters: {}\nError: {}", 
                        sql, parameters, err.getMessage(), err);
                });
        }
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
    
    /**
     * 성공한 Future<Void>를 반환
     * @return 성공한 Future<Void>
     */
    protected Future<Void> succeededVoid() {
        return Future.succeededFuture();
    }
    
    protected LocalDate getLocalDate(Row row, String column) {
        if (!hasColumn(row, column)) {
            return null;
        }
        try {
            // PostgreSQL DATE 타입은 LocalDate로 직접 반환됨
            // getValue를 사용하여 타입을 자동으로 감지
            Object value = row.getValue(column);
            if (value == null) {
                return null;
            }
            if (value instanceof LocalDate) {
                return (LocalDate) value;
            } else if (value instanceof LocalDateTime) {
                return ((LocalDateTime) value).toLocalDate();
            } else {
                // 문자열인 경우 파싱 시도
                log.warn("Unexpected type for LocalDate column {}: {}", column, value.getClass().getName());
                return null;
            }
        } catch (Exception e) {
            log.warn("Failed to get LocalDate from column {}: {}", column, e.getMessage());
            return null;
        }
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

