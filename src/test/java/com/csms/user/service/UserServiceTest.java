package com.csms.user.service;

import com.csms.user.dto.CreateUserDto;
import com.csms.user.dto.LoginDto;
import com.csms.user.entities.User;
import com.csms.user.repository.UserRepository;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class UserServiceTest {
    
    @Mock
    private PgPool pool;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private JWTAuth jwtAuth;
    
    private UserService userService;
    private JsonObject jwtConfig;
    
    @BeforeEach
    void setUp() {
        jwtConfig = new JsonObject()
            .put("secret", "test-secret-key");
        
        userService = new UserService(
            pool,
            userRepository,
            jwtAuth,
            jwtConfig
        );
    }
    
    @Test
    void testCreateUser_Success(VertxTestContext context) {
        // Given
        CreateUserDto dto = new CreateUserDto();
        dto.setLoginId("testuser");
        dto.setPassword("password123");
        
        User user = User.builder()
            .id(1L)
            .loginId("testuser")
            .status("ACTIVE")
            .createdAt(LocalDateTime.now())
            .build();
        
        when(userRepository.createUser(any(), any())).thenReturn(Future.succeededFuture(user));
        
        // When
        userService.createUser(dto)
            .onSuccess(result -> {
                // Then
                assertThat(result).isNotNull();
                assertThat(result.getLoginId()).isEqualTo("testuser");
                assertThat(dto.getPasswordHash()).isNotNull(); // 비밀번호가 해시되었는지 확인
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testLogin_Success(VertxTestContext context) {
        // Given
        LoginDto dto = new LoginDto();
        dto.setLoginId("testuser");
        dto.setPassword("password123");
        
        User user = User.builder()
            .id(1L)
            .loginId("testuser")
            .passwordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy") // "password123" 해시
            .status("ACTIVE")
            .build();
        
        when(userRepository.getUserByLoginId(any(), eq("testuser"))).thenReturn(Future.succeededFuture(user));
        when(jwtAuth.generateToken(any(), any())).thenReturn("test-token");
        
        // When
        userService.login(dto)
            .onSuccess(response -> {
                // Then
                assertThat(response).isNotNull();
                assertThat(response.getUserId()).isEqualTo(1L);
                assertThat(response.getLoginId()).isEqualTo("testuser");
                assertThat(response.getAccessToken()).isNotNull();
                assertThat(response.getRefreshToken()).isNotNull();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testLogin_InvalidPassword(VertxTestContext context) {
        // Given
        LoginDto dto = new LoginDto();
        dto.setLoginId("testuser");
        dto.setPassword("wrong-password");
        
        User user = User.builder()
            .id(1L)
            .loginId("testuser")
            .passwordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy")
            .status("ACTIVE")
            .build();
        
        when(userRepository.getUserByLoginId(any(), eq("testuser"))).thenReturn(Future.succeededFuture(user));
        
        // When
        userService.login(dto)
            .onSuccess(response -> context.failNow("Should fail with invalid password"))
            .onFailure(error -> {
                // Then
                assertThat(error).isNotNull();
                context.completeNow();
            });
    }
}

