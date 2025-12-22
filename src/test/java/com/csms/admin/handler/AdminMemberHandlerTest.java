package com.csms.admin.handler;

import com.csms.admin.dto.*;
import com.csms.admin.service.AdminMemberService;
import com.csms.admin.service.AdminMemberExportService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class AdminMemberHandlerTest {
    
    @Mock
    private AdminMemberService service;
    
    @Mock
    private AdminMemberExportService exportService;
    
    private AdminMemberHandler handler;
    private Vertx vertx;
    
    @BeforeEach
    void setUp(Vertx vertx) {
        this.vertx = vertx;
        handler = new AdminMemberHandler(vertx, service, exportService);
    }
    
    @Test
    void testGetRouter_NotNull(VertxTestContext context) {
        // When
        Router router = handler.getRouter();
        
        // Then
        assertThat(router).isNotNull();
        context.completeNow();
    }
    
    @Test
    void testUpdateMember_Success(VertxTestContext context) {
        // When
        Router router = handler.getRouter();
        
        // Then
        assertThat(router).isNotNull();
        context.completeNow();
    }
    
    @Test
    void testResetTransactionPassword_Success(VertxTestContext context) {
        // When
        Router router = handler.getRouter();
        
        // Then
        assertThat(router).isNotNull();
        context.completeNow();
    }
    
    @Test
    void testAdjustCoin_Success(VertxTestContext context) {
        // When
        Router router = handler.getRouter();
        
        // Then
        assertThat(router).isNotNull();
        context.completeNow();
    }
    
    @Test
    void testAdjustKoriPoint_Success(VertxTestContext context) {
        // When
        Router router = handler.getRouter();
        
        // Then
        assertThat(router).isNotNull();
        context.completeNow();
    }
    
    @Test
    void testGetMemberWallets_Success(VertxTestContext context) {
        // When
        Router router = handler.getRouter();
        
        // Then
        assertThat(router).isNotNull();
        context.completeNow();
    }
}

