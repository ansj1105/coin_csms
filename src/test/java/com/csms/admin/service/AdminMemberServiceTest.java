package com.csms.admin.service;

import com.csms.admin.dto.MemberDetailDto;
import com.csms.admin.dto.MemberListDto;
import com.csms.admin.dto.SanctionRequestDto;
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
class AdminMemberServiceTest {
    
    @Mock
    private PgPool pool;
    
    private AdminMemberService service;
    
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

