package com.csms.admin.service;

import com.csms.admin.dto.MiningBoosterDto;
import com.csms.admin.dto.UpdateBoosterRequestDto;
import com.csms.admin.repository.AdminMiningRepository;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.csms.common.TestArgumentMatchers.anySqlClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class AdminMiningBoosterServiceTest {
    
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
    void testGetMiningBoosters(VertxTestContext context) {
        // Given
        MiningBoosterDto expectedResult = MiningBoosterDto.builder()
            .boosters(new java.util.ArrayList<>())
            .summary(MiningBoosterDto.Summary.builder()
                .totalEfficiency(230)
                .miningAmountPerMinute(0.100)
                .build())
            .build();
        
        when(repository.getMiningBoosters(anySqlClient())).thenReturn(Future.succeededFuture(expectedResult));
        
        // When
        service.getMiningBoosters()
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    assertNotNull(result);
                    assertNotNull(result.getBoosters());
                    assertNotNull(result.getSummary());
                    verify(repository, times(1)).getMiningBoosters(anySqlClient());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testUpdateBooster_SimpleBooster(VertxTestContext context) {
        // Given
        UpdateBoosterRequestDto request = UpdateBoosterRequestDto.builder()
            .type("SOCIAL_LINK")
            .isEnabled(true)
            .efficiency(5)
            .build();
        
        when(repository.updateBooster(
            anySqlClient(),
            eq("SOCIAL_LINK"),
            eq(true),
            eq(5),
            isNull(),
            isNull()
        )).thenReturn(Future.succeededFuture());
        
        // When
        service.updateBooster(request)
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    verify(repository, times(1)).updateBooster(anySqlClient(), anyString(), anyBoolean(), anyInt(), any(), any());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testUpdateBooster_ComplexBooster(VertxTestContext context) {
        // Given
        UpdateBoosterRequestDto request = UpdateBoosterRequestDto.builder()
            .type("AD_VIEW")
            .isEnabled(true)
            .maxCount(5)
            .perUnitEfficiency(5)
            .build();
        
        // 복합 부스터의 경우 efficiency = maxCount * perUnitEfficiency = 25
        when(repository.updateBooster(
            anySqlClient(),
            eq("AD_VIEW"),
            eq(true),
            eq(25), // 자동 계산된 값
            eq(5),
            eq(5)
        )).thenReturn(Future.succeededFuture());
        
        // When
        service.updateBooster(request)
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    verify(repository, times(1)).updateBooster(anySqlClient(), anyString(), anyBoolean(), anyInt(), anyInt(), anyInt());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testUpdateBooster_MissingType(VertxTestContext context) {
        // Given
        UpdateBoosterRequestDto request = UpdateBoosterRequestDto.builder()
            .type(null)
            .isEnabled(true)
            .efficiency(5)
            .build();
        
        // When
        service.updateBooster(request)
            .onComplete(context.failing(throwable -> {
                // Then
                context.verify(() -> {
                    assertTrue(throwable instanceof IllegalArgumentException);
                    verify(repository, never()).updateBooster(anySqlClient(), anyString(), any(), any(), any(), any());
                });
                context.completeNow();
            }));
    }
}

