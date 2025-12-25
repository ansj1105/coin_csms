package com.csms.admin.service;

import com.csms.admin.dto.*;
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
class AdminMemberServiceExtendedTest extends HandlerTestBase {
    
    private static PgPool pool;
    private AdminMemberService service;
    
    public AdminMemberServiceExtendedTest() {
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
        service = new AdminMemberService(pool);
    }
    
    @Test
    void testUpdateMember_WithPhone(VertxTestContext context) {
        // Given
        UpdateMemberRequestDto request = UpdateMemberRequestDto.builder()
            .phone("010-1234-5678")
            .build();
        
        // When
        service.updateMember(1L, request)
            .onSuccess(result -> {
                // Then
                assertThat(result).isNull();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testUpdateMember_WithEmail(VertxTestContext context) {
        // Given
        UpdateMemberRequestDto request = UpdateMemberRequestDto.builder()
            .email("newemail@example.com")
            .build();
        
        // When
        service.updateMember(1L, request)
            .onSuccess(result -> {
                // Then
                assertThat(result).isNull();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testUpdateMember_WithLevel(VertxTestContext context) {
        // Given
        UpdateMemberRequestDto request = UpdateMemberRequestDto.builder()
            .level(5)
            .build();
        
        // When
        service.updateMember(1L, request)
            .onSuccess(result -> {
                // Then
                assertThat(result).isNull();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testResetTransactionPassword_Success(VertxTestContext context) {
        // When
        service.resetTransactionPassword(1L)
            .onSuccess(result -> {
                // Then
                assertThat(result).isNull();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testAdjustCoin_Add(VertxTestContext context) {
        // Given
        CoinAdjustRequestDto request = CoinAdjustRequestDto.builder()
            .userId(1L)
            .amount(1000.0)
            .type("ADD")
            .build();
        
        // When
        service.adjustCoin(request)
            .onSuccess(result -> {
                // Then
                assertThat(result).isNull();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testAdjustCoin_Withdraw(VertxTestContext context) {
        // Given
        CoinAdjustRequestDto request = CoinAdjustRequestDto.builder()
            .userId(1L)
            .amount(500.0)
            .type("WITHDRAW")
            .build();
        
        // When
        service.adjustCoin(request)
            .onSuccess(result -> {
                // Then
                assertThat(result).isNull();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testAdjustCoin_InvalidType(VertxTestContext context) {
        // Given
        CoinAdjustRequestDto request = CoinAdjustRequestDto.builder()
            .userId(1L)
            .amount(1000.0)
            .type("INVALID")
            .build();
        
        // When
        service.adjustCoin(request)
            .onSuccess(result -> context.failNow("Should fail with invalid type"))
            .onFailure(error -> {
                // Then
                assertThat(error).isNotNull();
                context.completeNow();
            });
    }
    
    @Test
    void testAdjustKoriPoint_Add(VertxTestContext context) {
        // Given
        KoriPointAdjustRequestDto request = KoriPointAdjustRequestDto.builder()
            .userId(1L)
            .amount(1000.0)
            .type("ADD")
            .build();
        
        // When
        service.adjustKoriPoint(request)
            .onSuccess(result -> {
                // Then
                assertThat(result).isNull();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testGetMemberWallets_WithFilters(VertxTestContext context) {
        // When
        service.getMemberWallets(1L, "KRC-20", "KRWT", 20, 0)
            .onSuccess(result -> {
                // Then
                assertThat(result).isNotNull();
                assertThat(result.getWallets()).isNotNull();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
}

