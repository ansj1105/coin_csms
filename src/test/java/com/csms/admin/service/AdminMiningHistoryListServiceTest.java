package com.csms.admin.service;

import com.csms.admin.dto.MiningHistoryListDto;
import com.csms.admin.repository.AdminMiningHistoryListRepository;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class AdminMiningHistoryListServiceTest {
    
    @Mock
    private PgPool pool;
    
    @Mock
    private AdminMiningHistoryListRepository repository;
    
    private AdminMiningHistoryListService service;
    
    @BeforeEach
    void setUp() {
        service = new AdminMiningHistoryListService(pool, repository);
    }
    
    @Test
    void testGetMiningHistoryList_DefaultValues(VertxTestContext context) {
        // Given
        MiningHistoryListDto expectedResult = MiningHistoryListDto.builder()
            .total(0)
            .limit(20)
            .offset(0)
            .build();
        
        when(repository.getMiningHistoryList(
            eq(20),
            eq(0),
            eq("ALL"),
            isNull(),
            isNull(),
            eq("ALL"),
            eq("ALL")
        )).thenReturn(Future.succeededFuture(expectedResult));
        
        // When
        service.getMiningHistoryList(null, null, null, null, null, null, null)
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    assertNotNull(result);
                    assertEquals(20, result.getLimit());
                    assertEquals(0, result.getOffset());
                    verify(repository, times(1)).getMiningHistoryList(anyInt(), anyInt(), anyString(), any(), any(), anyString(), anyString());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testGetMiningHistoryList_WithSearchAndSort(VertxTestContext context) {
        // Given
        MiningHistoryListDto expectedResult = MiningHistoryListDto.builder()
            .total(10)
            .limit(30)
            .offset(0)
            .build();
        
        when(repository.getMiningHistoryList(
            eq(30),
            eq(0),
            eq("NICKNAME"),
            eq("test"),
            eq("LEVEL"),
            eq("ACTIVE"),
            eq("ALL")
        )).thenReturn(Future.succeededFuture(expectedResult));
        
        // When
        service.getMiningHistoryList(30, 0, "NICKNAME", "test", "LEVEL", "ACTIVE", "ALL")
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    assertNotNull(result);
                    assertEquals(30, result.getLimit());
                    verify(repository, times(1)).getMiningHistoryList(anyInt(), anyInt(), anyString(), anyString(), anyString(), anyString(), anyString());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testGetMiningHistoryList_SearchKeywordTruncation(VertxTestContext context) {
        // Given
        String longKeyword = "a".repeat(30); // 30자 검색어
        MiningHistoryListDto expectedResult = MiningHistoryListDto.builder()
            .total(0)
            .limit(20)
            .offset(0)
            .build();
        
        when(repository.getMiningHistoryList(
            eq(20),
            eq(0),
            eq("ALL"),
            eq("a".repeat(20)), // 20자로 잘림
            isNull(),
            eq("ALL"),
            eq("ALL")
        )).thenReturn(Future.succeededFuture(expectedResult));
        
        // When
        service.getMiningHistoryList(20, 0, "ALL", longKeyword, null, "ALL", "ALL")
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    assertNotNull(result);
                    verify(repository, times(1)).getMiningHistoryList(anyInt(), anyInt(), anyString(), eq("a".repeat(20)), any(), anyString(), anyString());
                });
                context.completeNow();
            }));
    }
}

