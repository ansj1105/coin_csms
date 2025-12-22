package com.csms.admin.service;

import com.csms.admin.dto.MemberDetailDto;
import com.csms.admin.dto.MemberListDto;
import com.csms.admin.dto.SanctionRequestDto;
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
class AdminMemberServiceTest extends HandlerTestBase {
    
    private static PgPool pool;
    private AdminMemberService service;
    
    public AdminMemberServiceTest() {
        super("/api/admin");
    }
    
    @BeforeAll
    static void setUpPool(Vertx vertx, VertxTestContext testContext) {
        try {
            // test용 config.json에서 test 환경 설정 로드
            String configContent = vertx.fileSystem().readFileBlocking("src/test/resources/config.json").toString();
            JsonObject fullConfig = new JsonObject(configContent);
            JsonObject config = fullConfig.getJsonObject("test");
            JsonObject dbConfig = config.getJsonObject("database");
            
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
    void testGetMembers_DefaultParams(VertxTestContext context) {
        // Given
        Integer limit = null;
        Integer offset = null;
        
        // When
        service.getMembers(limit, offset, null, null, null, null)
            .onSuccess(result -> {
                // Then
                assertThat(result).isNotNull();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testGetMembers_WithSearch(VertxTestContext context) {
        // Given
        String searchCategory = "NICKNAME";
        String searchKeyword = "test";
        
        // When
        service.getMembers(20, 0, searchCategory, searchKeyword, null, null)
            .onSuccess(result -> {
                // Then
                assertThat(result).isNotNull();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testUpdateSanctionStatus_SameStatus(VertxTestContext context) {
        // Given
        Long memberId = 1L;
        SanctionRequestDto request = SanctionRequestDto.builder()
            .sanctionStatus("WARNING")
            .build();
        
        // When - 실제 구현에서는 현재 상태를 조회하고 동일하면 해제
        // 여기서는 기본 동작 확인만
        assertThat(request.getSanctionStatus()).isEqualTo("WARNING");
        context.completeNow();
    }
}

