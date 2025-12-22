package com.csms.admin.service;

import com.csms.admin.dto.MiningHistoryDetailDto;
import com.csms.admin.repository.AdminMiningHistoryRepository;
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
class AdminMiningHistoryServiceTest {
    
    @Mock
    private PgPool pool;
    
    @Mock
    private AdminMiningHistoryRepository repository;
    
    private AdminMiningHistoryService service;
    
    @BeforeEach
    void setUp() {
        service = new AdminMiningHistoryService(pool, repository);
    }
    
    @Test
    void testGetMiningHistory_DefaultValues(VertxTestContext context) {
        // Given
        Long userId = 1L;
        MiningHistoryDetailDto expectedResult = MiningHistoryDetailDto.builder()
            .userId(userId)
            .userNickname("TestUser")
            .userLevel(1)
            .total(0)
            .limit(20)
            .offset(0)
            .build();
        
        when(repository.getMiningHistory(
            eq(userId),
            eq(20),
            eq(0),
            isNull(),
            isNull()
        )).thenReturn(Future.succeededFuture(expectedResult));
        
        // When
        service.getMiningHistory(userId, null, null, "ALL")
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    assertNotNull(result);
                    assertEquals(20, result.getLimit());
                    assertEquals(0, result.getOffset());
                    verify(repository, times(1)).getMiningHistory(anyLong(), anyInt(), anyInt(), any(), any());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testGetMiningHistory_WithDateRange(VertxTestContext context) {
        // Given
        Long userId = 1L;
        MiningHistoryDetailDto expectedResult = MiningHistoryDetailDto.builder()
            .userId(userId)
            .userNickname("TestUser")
            .userLevel(1)
            .total(10)
            .limit(30)
            .offset(0)
            .build();
        
        when(repository.getMiningHistory(
            eq(userId),
            eq(30),
            eq(0),
            any(),
            any()
        )).thenReturn(Future.succeededFuture(expectedResult));
        
        // When
        service.getMiningHistory(userId, 30, 0, "7")
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    assertNotNull(result);
                    assertEquals(30, result.getLimit());
                    verify(repository, times(1)).getMiningHistory(anyLong(), anyInt(), anyInt(), any(), any());
                });
                context.completeNow();
            }));
    }
}

