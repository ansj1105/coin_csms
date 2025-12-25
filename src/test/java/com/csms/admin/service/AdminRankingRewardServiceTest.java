package com.csms.admin.service;

import com.csms.admin.dto.RankingRewardDto;
import com.csms.admin.dto.UpdateRankingRewardRequestDto;
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
class AdminRankingRewardServiceTest {
    
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
    void testGetRankingReward(VertxTestContext context) {
        // Given
        RankingRewardDto expectedResult = RankingRewardDto.builder()
            .regional(RankingRewardDto.RegionalReward.builder()
                .rank1(10.00)
                .rank2(5.00)
                .rank3(2.50)
                .rank4to10(0.80)
                .build())
            .national(RankingRewardDto.NationalReward.builder()
                .rank1(10.00)
                .rank2(5.00)
                .rank3(2.50)
                .rank4to10(0.80)
                .build())
            .build();
        
        when(repository.getRankingReward(anySqlClient())).thenReturn(Future.succeededFuture(expectedResult));
        
        // When
        service.getRankingReward()
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    assertNotNull(result);
                    assertNotNull(result.getRegional());
                    assertNotNull(result.getNational());
                    verify(repository, times(1)).getRankingReward(anySqlClient());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testUpdateRankingReward_Regional(VertxTestContext context) {
        // Given
        UpdateRankingRewardRequestDto request = UpdateRankingRewardRequestDto.builder()
            .type("REGIONAL")
            .rank1(10.00)
            .rank2(5.00)
            .rank3(2.50)
            .rank4to10(0.80)
            .build();
        
        when(repository.updateRankingReward(
            anySqlClient(),
            eq("REGIONAL"),
            eq(10.00),
            eq(5.00),
            eq(2.50),
            eq(0.80)
        )).thenReturn(Future.succeededFuture());
        
        // When
        service.updateRankingReward(request)
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    verify(repository, times(1)).updateRankingReward(anySqlClient(), anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testUpdateRankingReward_MissingType(VertxTestContext context) {
        // Given
        UpdateRankingRewardRequestDto request = UpdateRankingRewardRequestDto.builder()
            .type(null)
            .rank1(10.00)
            .build();
        
        // When
        service.updateRankingReward(request)
            .onComplete(context.failing(throwable -> {
                // Then
                context.verify(() -> {
                    assertTrue(throwable instanceof IllegalArgumentException);
                    verify(repository, never()).updateRankingReward(anySqlClient(), anyString(), any(), any(), any(), any());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testUpdateRankingReward_InvalidType(VertxTestContext context) {
        // Given
        UpdateRankingRewardRequestDto request = UpdateRankingRewardRequestDto.builder()
            .type("INVALID")
            .rank1(10.00)
            .build();
        
        // When
        service.updateRankingReward(request)
            .onComplete(context.failing(throwable -> {
                // Then
                context.verify(() -> {
                    assertTrue(throwable instanceof IllegalArgumentException);
                    verify(repository, never()).updateRankingReward(anySqlClient(), anyString(), any(), any(), any(), any());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testUpdateRankingReward_NegativeValue(VertxTestContext context) {
        // Given
        UpdateRankingRewardRequestDto request = UpdateRankingRewardRequestDto.builder()
            .type("REGIONAL")
            .rank1(-10.00) // Invalid: negative
            .build();
        
        // When
        service.updateRankingReward(request)
            .onComplete(context.failing(throwable -> {
                // Then
                context.verify(() -> {
                    assertTrue(throwable instanceof IllegalArgumentException);
                    verify(repository, never()).updateRankingReward(anySqlClient(), anyString(), any(), any(), any(), any());
                });
                context.completeNow();
            }));
    }
}

