package com.csms.admin.service;

import com.csms.admin.dto.ReferralBonusDto;
import com.csms.admin.dto.UpdateReferralBonusRequestDto;
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
class AdminReferralBonusServiceTest {
    
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
    void testGetReferralBonus(VertxTestContext context) {
        // Given
        ReferralBonusDto expectedResult = ReferralBonusDto.builder()
            .isEnabled(true)
            .distributionRate(5)
            .build();
        
        when(repository.getReferralBonus(anySqlClient())).thenReturn(Future.succeededFuture(expectedResult));
        
        // When
        service.getReferralBonus()
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    assertNotNull(result);
                    assertEquals(true, result.getIsEnabled());
                    assertEquals(5, result.getDistributionRate());
                    verify(repository, times(1)).getReferralBonus(anySqlClient());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testUpdateReferralBonus_Success(VertxTestContext context) {
        // Given
        UpdateReferralBonusRequestDto request = UpdateReferralBonusRequestDto.builder()
            .isEnabled(true)
            .distributionRate(5)
            .build();
        
        when(repository.updateReferralBonus(anySqlClient(), eq(true), eq(5)))
            .thenReturn(Future.succeededFuture());
        
        // When
        service.updateReferralBonus(request)
            .onComplete(context.succeeding(result -> {
                // Then
                context.verify(() -> {
                    verify(repository, times(1)).updateReferralBonus(anySqlClient(), anyBoolean(), anyInt());
                });
                context.completeNow();
            }));
    }
    
    @Test
    void testUpdateReferralBonus_InvalidRate(VertxTestContext context) {
        // Given
        UpdateReferralBonusRequestDto request = UpdateReferralBonusRequestDto.builder()
            .isEnabled(true)
            .distributionRate(101) // Invalid: > 100
            .build();
        
        // When
        service.updateReferralBonus(request)
            .onComplete(context.failing(throwable -> {
                // Then
                context.verify(() -> {
                    assertTrue(throwable instanceof IllegalArgumentException);
                    verify(repository, never()).updateReferralBonus(anySqlClient(), anyBoolean(), anyInt());
                });
                context.completeNow();
            }));
    }
}

