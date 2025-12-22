package com.csms.admin.handler;

import com.csms.admin.service.AdminAuthService;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class AdminAuthHandlerTest {
    
    @Mock
    private AdminAuthService adminAuthService;
    
    private AdminAuthHandler handler;
    private Vertx vertx;
    
    @BeforeEach
    void setUp(Vertx vertx) {
        this.vertx = vertx;
        handler = new AdminAuthHandler(vertx, adminAuthService);
    }
    
    @Test
    void testGetRouter_NotNull(VertxTestContext context) {
        // When
        Router router = handler.getRouter();
        
        // Then
        assertThat(router).isNotNull();
        context.completeNow();
    }
}

