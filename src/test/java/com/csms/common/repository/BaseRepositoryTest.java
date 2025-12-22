package com.csms.common.repository;

import com.csms.common.database.RowMapper;
import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class BaseRepositoryTest {
    
    @Mock
    private Row row;
    
    @Mock
    private RowSet<Row> rowSet;
    
    @Mock
    private Iterator<Row> iterator;
    
    private TestRepository repository;
    
    // 테스트용 Repository 구현
    static class TestRepository extends BaseRepository {
        public String testGetString(Row row, String column) {
            return getString(row, column);
        }
        
        public Integer testGetInteger(Row row, String column) {
            return getInteger(row, column);
        }
        
        public <T> T testFetchOne(RowMapper<T> mapper, RowSet<Row> rows) {
            return fetchOne(mapper, rows);
        }
    }
    
    @Test
    void testGetString_WithColumn(VertxTestContext context) {
        // Given
        repository = new TestRepository();
        when(row.getColumnIndex("name")).thenReturn(0);
        when(row.getString("name")).thenReturn("test");
        
        // When
        String result = repository.testGetString(row, "name");
        
        // Then
        assertThat(result).isEqualTo("test");
        context.completeNow();
    }
    
    @Test
    void testGetString_WithoutColumn(VertxTestContext context) {
        // Given
        repository = new TestRepository();
        when(row.getColumnIndex("name")).thenReturn(-1);
        
        // When
        String result = repository.testGetString(row, "name");
        
        // Then
        assertThat(result).isNull();
        context.completeNow();
    }
}

