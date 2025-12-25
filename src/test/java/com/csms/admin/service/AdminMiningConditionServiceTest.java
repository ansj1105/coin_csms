package com.csms.admin.service;

import com.csms.admin.dto.*;
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

import java.util.ArrayList;
import java.util.List;

import static com.csms.common.TestArgumentMatchers.anySqlClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class AdminMiningConditionServiceTest {
    
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
    void testGetMiningConditions(VertxTestContext context) {
        // Given
        MiningConditionDto expectedResult = MiningConditionDto.builder()
            .basicConditions(MiningConditionDto.BasicConditions.builder()
                .isEnabled(true)
                .baseTimeEnabled(false)
                .baseTimeMinutes(30)
                .missions(new ArrayList<>())
                .build())
            .progressSettings(MiningConditionDto.ProgressSettings.builder()
                .broadcastProgress(MiningConditionDto.BroadcastSetting.builder()
                    .isEnabled(true)
                    .timePerHour(15)
                    .coinsPerHour(0.002)
                    .build())
                .broadcastListening(MiningConditionDto.BroadcastSetting.builder()
                    .isEnabled(true)
                    .timePerHour(15)
                    .coinsPerHour(0.002)
                    .build())
                .build())
            .levelLimits(new ArrayList<>())
            .levelLimitsEnabled(true)
            .build();
        
        when(repository.getMiningConditions(anySqlClient())).thenReturn(Future.succeededFuture(expectedResult));
        
        // When
        service.getMiningConditions()
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    assertNotNull(result);
                    assertNotNull(result.getBasicConditions());
                    assertNotNull(result.getProgressSettings());
                    verify(repository, times(1)).getMiningConditions(anySqlClient());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testUpdateBasicConditions(VertxTestContext context) {
        // Given
        UpdateBasicConditionRequestDto request = UpdateBasicConditionRequestDto.builder()
            .isEnabled(true)
            .baseTimeEnabled(false)
            .baseTimeMinutes(30)
            .missions(new ArrayList<>())
            .build();
        
        when(repository.updateBasicConditions(anySqlClient(), anyBoolean(), anyBoolean(), anyInt()))
            .thenReturn(Future.succeededFuture());
        
        // When
        service.updateBasicConditions(request)
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    verify(repository, times(1)).updateBasicConditions(anySqlClient(), anyBoolean(), anyBoolean(), anyInt());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testUpdateLevelLimit_InvalidLevel(VertxTestContext context) {
        // Given
        UpdateLevelLimitRequestDto request = UpdateLevelLimitRequestDto.builder()
            .level(10) // 범위 초과
            .dailyLimit(0.002)
            .build();
        
        // When
        service.updateLevelLimit(request)
            .onComplete(context.failing(throwable -> {
                // Then
                context.verify(() -> {
                    assertTrue(throwable instanceof IllegalArgumentException);
                    verify(repository, never()).updateLevelLimit(anySqlClient(), anyInt(), anyDouble());
                });
                context.completeNow();
            }));
    }
}

