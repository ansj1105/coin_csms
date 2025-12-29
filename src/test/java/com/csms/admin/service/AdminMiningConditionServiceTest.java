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
    void testUpdateLevelLimit_ValidCase(VertxTestContext context) {
        // Given
        UpdateLevelLimitRequestDto request = UpdateLevelLimitRequestDto.builder()
            .level(5)
            .dailyLimit(0.002)
            .build();
        
        when(repository.updateLevelLimit(anySqlClient(), eq(5), eq(0.002)))
            .thenReturn(Future.succeededFuture());
        
        // When
        service.updateLevelLimit(request)
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    verify(repository, times(1)).updateLevelLimit(anySqlClient(), eq(5), eq(0.002));
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
    
    @Test
    void testUpdateLevelLimit_InvalidDailyLimit(VertxTestContext context) {
        // Given
        UpdateLevelLimitRequestDto request = UpdateLevelLimitRequestDto.builder()
            .level(5)
            .dailyLimit(-0.001) // 음수는 허용되지 않음
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
    
    @Test
    void testUpdateLevelLimitsEnabled(VertxTestContext context) {
        // Given
        UpdateLevelLimitsEnabledRequestDto request = UpdateLevelLimitsEnabledRequestDto.builder()
            .enabled(true)
            .build();
        
        when(repository.updateLevelLimitsEnabled(anySqlClient(), eq(true)))
            .thenReturn(Future.succeededFuture());
        
        // When
        service.updateLevelLimitsEnabled(request)
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    verify(repository, times(1)).updateLevelLimitsEnabled(anySqlClient(), eq(true));
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testUpdateProgressSetting_BroadcastProgressOnly(VertxTestContext context) {
        // Given
        UpdateProgressSettingRequestDto request = UpdateProgressSettingRequestDto.builder()
            .broadcastProgress(UpdateProgressSettingRequestDto.BroadcastSettingUpdate.builder()
                .isEnabled(true)
                .timePerHour(15)
                .coinsPerHour(0.002)
                .build())
            .broadcastListening(null)
            .build();
        
        when(repository.updateProgressSetting(
            anySqlClient(),
            eq("BROADCAST_PROGRESS"),
            eq(true),
            eq(15),
            eq(0.002)
        )).thenReturn(Future.succeededFuture());
        
        // When
        service.updateProgressSetting(request)
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    verify(repository, times(1)).updateProgressSetting(
                        anySqlClient(),
                        eq("BROADCAST_PROGRESS"),
                        eq(true),
                        eq(15),
                        eq(0.002)
                    );
                    verify(repository, never()).updateProgressSetting(
                        anySqlClient(),
                        eq("BROADCAST_LISTENING"),
                        any(),
                        any(),
                        any()
                    );
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testUpdateProgressSetting_BothSettings(VertxTestContext context) {
        // Given
        UpdateProgressSettingRequestDto request = UpdateProgressSettingRequestDto.builder()
            .broadcastProgress(UpdateProgressSettingRequestDto.BroadcastSettingUpdate.builder()
                .isEnabled(true)
                .timePerHour(15)
                .coinsPerHour(0.002)
                .build())
            .broadcastListening(UpdateProgressSettingRequestDto.BroadcastSettingUpdate.builder()
                .isEnabled(false)
                .timePerHour(20)
                .coinsPerHour(0.003)
                .build())
            .build();
        
        when(repository.updateProgressSetting(
            anySqlClient(),
            anyString(),
            anyBoolean(),
            anyInt(),
            anyDouble()
        )).thenReturn(Future.succeededFuture());
        
        // When
        service.updateProgressSetting(request)
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    verify(repository, times(1)).updateProgressSetting(
                        anySqlClient(),
                        eq("BROADCAST_PROGRESS"),
                        eq(true),
                        eq(15),
                        eq(0.002)
                    );
                    verify(repository, times(1)).updateProgressSetting(
                        anySqlClient(),
                        eq("BROADCAST_LISTENING"),
                        eq(false),
                        eq(20),
                        eq(0.003)
                    );
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testUpdateProgressSetting_EmptySettings(VertxTestContext context) {
        // Given
        UpdateProgressSettingRequestDto request = UpdateProgressSettingRequestDto.builder()
            .broadcastProgress(null)
            .broadcastListening(null)
            .build();
        
        // When
        service.updateProgressSetting(request)
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    verify(repository, never()).updateProgressSetting(
                        anySqlClient(),
                        anyString(),
                        any(),
                        any(),
                        any()
                    );
                });
                context.completeNow();
            }));
    }
}

