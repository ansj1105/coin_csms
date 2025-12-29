package com.csms.admin.service;

import com.csms.admin.dto.MiningHistoryListDto;
import com.csms.admin.dto.MiningRecordListDto;
import com.csms.admin.repository.AdminMiningRepository;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static com.csms.common.TestArgumentMatchers.anySqlClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class AdminMiningServiceTest {
    
    @Mock
    private PgPool pool;
    
    @Mock
    private AdminMiningRepository repository;
    
    private AdminMiningService service;
    
    @BeforeEach
    void setUp() {
        service = new AdminMiningService(pool, repository);
    }
    
    @Test
    void testGetMiningRecords_DefaultValues(VertxTestContext context) {
        // Given
        MiningRecordListDto expectedResult = MiningRecordListDto.builder()
            .total(0)
            .limit(20)
            .offset(0)
            .build();
        
        when(repository.getMiningRecords(
            anySqlClient(),
            eq(20),
            eq(0),
            any(),
            any(),
            eq("ALL"),
            isNull(),
            eq("ALL")
        )).thenReturn(Future.succeededFuture(expectedResult));
        
        // When
        service.getMiningRecords(null, null, null, null, null, null, null, null)
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    assertNotNull(result);
                    assertEquals(20, result.getLimit());
                    assertEquals(0, result.getOffset());
                    verify(repository, times(1)).getMiningRecords(anySqlClient(), anyInt(), anyInt(), any(), any(), anyString(), any(), anyString());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testGetMiningRecords_CustomDateRange(VertxTestContext context) {
        // Given
        MiningRecordListDto expectedResult = MiningRecordListDto.builder()
            .total(10)
            .limit(30)
            .offset(0)
            .build();
        
        when(repository.getMiningRecords(
            anySqlClient(),
            eq(30),
            eq(0),
            any(),
            any(),
            eq("NICKNAME"),
            eq("test"),
            eq("ACTIVE")
        )).thenReturn(Future.succeededFuture(expectedResult));
        
        // When
        service.getMiningRecords(30, 0, "30", null, null, "NICKNAME", "test", "ACTIVE")
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    assertNotNull(result);
                    assertEquals(30, result.getLimit());
                    verify(repository, times(1)).getMiningRecords(anySqlClient(), anyInt(), anyInt(), any(), any(), anyString(), anyString(), anyString());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testGetMiningRecords_SearchKeywordTruncation(VertxTestContext context) {
        // Given
        String longKeyword = "a".repeat(30); // 30자 검색어
        MiningRecordListDto expectedResult = MiningRecordListDto.builder()
            .total(0)
            .limit(20)
            .offset(0)
            .build();
        
        when(repository.getMiningRecords(
            anySqlClient(),
            eq(20),
            eq(0),
            any(),
            any(),
            eq("ALL"),
            eq("a".repeat(20)), // 20자로 잘림
            eq("ALL")
        )).thenReturn(Future.succeededFuture(expectedResult));
        
        // When
        service.getMiningRecords(20, 0, "7", null, null, "ALL", longKeyword, "ALL")
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    assertNotNull(result);
                    verify(repository, times(1)).getMiningRecords(anySqlClient(), anyInt(), anyInt(), any(), any(), anyString(), eq("a".repeat(20)), anyString());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testExportMiningRecords(VertxTestContext context) {
        // Given
        MiningRecordListDto miningRecordList = MiningRecordListDto.builder()
            .total(2)
            .limit(Integer.MAX_VALUE)
            .offset(0)
            .records(new ArrayList<>())
            .build();
        
        when(repository.getMiningRecords(
            anySqlClient(),
            eq(Integer.MAX_VALUE),
            eq(0),
            any(),
            any(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(Future.succeededFuture(miningRecordList));
        
        // When
        service.exportMiningRecords("7", null, null, "ALL", null, "ALL")
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    assertNotNull(result);
                    assertTrue(result instanceof Buffer);
                    verify(repository, times(1)).getMiningRecords(
                        anySqlClient(),
                        eq(Integer.MAX_VALUE),
                        eq(0),
                        any(),
                        any(),
                        anyString(),
                        anyString(),
                        anyString()
                    );
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testExportMiningHistoryList(VertxTestContext context) {
        // Given
        MiningHistoryListDto historyListDto = MiningHistoryListDto.builder()
            .total(2)
            .limit(Integer.MAX_VALUE)
            .offset(0)
            .items(new ArrayList<>())
            .build();
        
        when(repository.getMiningHistoryList(
            anySqlClient(),
            eq(Integer.MAX_VALUE),
            eq(0),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(Future.succeededFuture(historyListDto));
        
        // When
        service.exportMiningHistoryList("ALL", null, null, "ALL", "ALL")
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    assertNotNull(result);
                    assertTrue(result instanceof Buffer);
                    verify(repository, times(1)).getMiningHistoryList(
                        anySqlClient(),
                        eq(Integer.MAX_VALUE),
                        eq(0),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString()
                    );
                });
                context.completeNow();
            }));
    }
}

