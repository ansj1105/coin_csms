package com.csms.admin.service;

import com.csms.admin.repository.AdminDashboardRepository;
import com.csms.common.HandlerTestBase;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.junit5.VertxExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
class AdminDashboardServiceTest extends HandlerTestBase {
    
    private AdminDashboardService dashboardService;
    
    public AdminDashboardServiceTest() {
        super("/api/admin");
    }
    
    @BeforeEach
    void setUp() {
        AdminDashboardRepository repository = new AdminDashboardRepository();
        dashboardService = new AdminDashboardService(pool, repository);
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

