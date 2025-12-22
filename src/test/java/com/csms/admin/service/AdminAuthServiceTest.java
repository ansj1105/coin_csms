package com.csms.admin.service;

import com.csms.admin.dto.AdminLoginDto;
import com.csms.admin.entities.Admin;
import com.csms.admin.repository.AdminRepository;
import com.csms.common.utils.RateLimiter;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class AdminAuthServiceTest {
    
    @Mock
    private PgPool pool;
    
    @Mock
    private AdminRepository adminRepository;
    
    @Mock
    private JWTAuth jwtAuth;
    
    @Mock
    private RateLimiter rateLimiter;
    
    private AdminAuthService adminAuthService;
    private JsonObject jwtConfig;
    
    @BeforeEach
    void setUp() {
        jwtConfig = new JsonObject()
            .put("secret", "test-secret-key");
        
        adminAuthService = new AdminAuthService(
            pool,
            adminRepository,
            jwtAuth,
            jwtConfig,
            rateLimiter
        );
    }
    
    @Test
    void testLogin_Success(VertxTestContext context) {
        // Given
        AdminLoginDto dto = new AdminLoginDto();
        dto.setId("admin1");
        dto.setPassword("password123");
        
        Admin admin = Admin.builder()
            .id(1L)
            .loginId("admin1")
            .passwordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy") // "password123" 해시
            .role(2)
            .status("ACTIVE")
            .build();
        
        when(rateLimiter.checkAdminLoginAttempt(any())).thenReturn(Future.succeededFuture(true));
        when(adminRepository.getAdminByLoginId(any(), eq("admin1"))).thenReturn(Future.succeededFuture(admin));
        when(jwtAuth.generateToken(any(), any())).thenReturn("test-token");
        
        // When
        adminAuthService.login(dto, "127.0.0.1")
            .onSuccess(response -> {
                // Then
                assertThat(response).isNotNull();
                assertThat(response.getAdminId()).isEqualTo("admin1");
                assertThat(response.getAccessToken()).isNotNull();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testLogin_InvalidCredentials(VertxTestContext context) {
        // Given
        AdminLoginDto dto = new AdminLoginDto();
        dto.setId("admin1");
        dto.setPassword("wrong-password");
        
        Admin admin = Admin.builder()
            .id(1L)
            .loginId("admin1")
            .passwordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy")
            .role(2)
            .status("ACTIVE")
            .build();
        
        when(rateLimiter.checkAdminLoginAttempt(any())).thenReturn(Future.succeededFuture(true));
        when(adminRepository.getAdminByLoginId(any(), eq("admin1"))).thenReturn(Future.succeededFuture(admin));
        
        // When
        adminAuthService.login(dto, "127.0.0.1")
            .onSuccess(response -> context.failNow("Should fail with invalid credentials"))
            .onFailure(error -> {
                // Then
                assertThat(error).isNotNull();
                context.completeNow();
            });
    }
    
    @Test
    void testLogin_RateLimitExceeded(VertxTestContext context) {
        // Given
        AdminLoginDto dto = new AdminLoginDto();
        dto.setId("admin1");
        dto.setPassword("password123");
        
        when(rateLimiter.checkAdminLoginAttempt(any())).thenReturn(Future.succeededFuture(false));
        
        // When
        adminAuthService.login(dto, "127.0.0.1")
            .onSuccess(response -> context.failNow("Should fail with rate limit"))
            .onFailure(error -> {
                // Then
                assertThat(error).isNotNull();
                context.completeNow();
            });
    }
}

