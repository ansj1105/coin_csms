package com.csms.admin.service;

import com.csms.admin.dto.MemberListDto;
import io.vertx.core.buffer.Buffer;
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
class AdminMemberExportServiceTest {
    
    @Mock
    private PgPool pool;
    
    private AdminMemberExportService exportService;
    
    @BeforeEach
    void setUp() {
        exportService = new AdminMemberExportService(pool);
    }
    
    @Test
    void testExportToExcel_NotNull(VertxTestContext context) {
        // Given
        String searchCategory = null;
        String searchKeyword = null;
        String activityStatus = null;
        String sanctionStatus = null;
        
        // When
        exportService.exportToExcel(searchCategory, searchKeyword, activityStatus, sanctionStatus)
            .onSuccess(buffer -> {
                // Then
                assertThat(buffer).isNotNull();
                assertThat(buffer.length()).isGreaterThan(0);
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
}

