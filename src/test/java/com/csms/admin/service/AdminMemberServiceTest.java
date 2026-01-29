package com.csms.admin.service;

import com.csms.admin.dto.MemberDetailDto;
import com.csms.admin.dto.MemberListDto;
import com.csms.admin.dto.SanctionRequestDto;
import com.csms.common.HandlerTestBase;
import com.csms.common.service.TronService;
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
    
    private AdminMemberService service;
    
    public AdminMemberServiceTest() {
        super("/api/admin");
    }
    
    @BeforeEach
    void setUp() {
        // TronService 생성 (테스트용 - 더미 URL 제공, 실제 호출은 안 되지만 에러 방지)
        // 테스트 데이터에 이미 지갑이 있으므로 실제로 호출되지 않음
        TronService tronService = new TronService(webClient, "http://test-tron-service:3000");
        service = new AdminMemberService(pool, tronService);
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
    
    // sanction_status 컬럼이 users 테이블에 없으므로 테스트 제거
    // @Test
    // void testUpdateSanctionStatus_SameStatus(VertxTestContext context) {
    //     // Given
    //     Long memberId = 1L;
    //     SanctionRequestDto request = SanctionRequestDto.builder()
    //         .sanctionStatus("WARNING")
    //         .build();
    //     
    //     // When - 실제 구현에서는 현재 상태를 조회하고 동일하면 해제
    //     // 여기서는 기본 동작 확인만
    //     assertThat(request.getSanctionStatus()).isEqualTo("WARNING");
    //     context.completeNow();
    // }
}

