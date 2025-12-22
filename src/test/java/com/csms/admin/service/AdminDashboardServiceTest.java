package com.csms.admin.service;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class AdminDashboardServiceTest {
    
    @Mock
    private PgPool pool;
    
    private AdminDashboardService dashboardService;
    
    @BeforeEach
    void setUp() {
        dashboardService = new AdminDashboardService(pool);
    }
    
    @Test
    void testGetDashboardStats_DefaultDateRange(VertxTestContext context) {
        // Given
        String dateRange = "7";
        String startDate = null;
        String endDate = null;
        
        // When
        dashboardService.getDashboardStats(dateRange, startDate, endDate)
            .onSuccess(result -> {
                // Then
                assertThat(result).isNotNull();
                assertThat(result.getStats()).isNotNull();
                assertThat(result.getChartData()).isNotNull();
                assertThat(result.getTopMembers()).isNotNull();
                assertThat(result.getNotifications()).isNotNull();
                assertThat(result.getTopReferrers()).isNotNull();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testGetDashboardStats_CustomDateRange(VertxTestContext context) {
        // Given
        String dateRange = "custom";
        String startDate = "2025-01-01";
        String endDate = "2025-01-31";
        
        // When
        dashboardService.getDashboardStats(dateRange, startDate, endDate)
            .onSuccess(result -> {
                // Then
                assertThat(result).isNotNull();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
}

