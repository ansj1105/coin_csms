package com.csms.admin.service;

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
    
    private static PgPool pool;
    private AdminDashboardService dashboardService;
    
    public AdminDashboardServiceTest() {
        super("/api/admin");
    }
    
    @BeforeAll
    static void setUpPool(Vertx vertx, VertxTestContext testContext) {
        try {
            // test용 config.json에서 test 환경 설정 로드
            String configContent = vertx.fileSystem().readFileBlocking("src/test/resources/config.json").toString();
            JsonObject fullConfig = new JsonObject(configContent);
            JsonObject config = fullConfig.getJsonObject("test");
            JsonObject dbConfig = HandlerTestBase.overrideDatabaseConfig(config.getJsonObject("database"));
            
            PgConnectOptions connectOptions = new PgConnectOptions()
                .setHost(dbConfig.getString("host"))
                .setPort(dbConfig.getInteger("port"))
                .setDatabase(dbConfig.getString("database"))
                .setUser(dbConfig.getString("user"))
                .setPassword(dbConfig.getString("password"));
            
            PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(dbConfig.getInteger("pool_size", 5));
            
            pool = PgPool.pool(vertx, connectOptions, poolOptions);
            testContext.completeNow();
        } catch (Exception e) {
            testContext.failNow(e);
        }
    }
    
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

