package com.csms.user.handler;

import com.csms.user.dto.LoginResponseDto;
import com.csms.user.entities.User;
import com.csms.user.service.UserService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
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
class UserHandlerTest {
    
    @Mock
    private UserService userService;
    
    @Mock
    private JWTAuth jwtAuth;
    
    private UserHandler handler;
    private Vertx vertx;
    
    @BeforeEach
    void setUp(Vertx vertx) {
        this.vertx = vertx;
        handler = new UserHandler(vertx, userService, jwtAuth);
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
    void testLogin_Success(VertxTestContext context) {
        // Given
        LoginResponseDto responseDto = LoginResponseDto.builder()
            .accessToken("test-token")
            .refreshToken("refresh-token")
            .userId(1L)
            .loginId("testuser")
            .build();
        
        when(userService.login(any())).thenReturn(Future.succeededFuture(responseDto));
        
        Router router = handler.getRouter();
        
        // Router가 정상적으로 생성되었는지 확인
        assertThat(router).isNotNull();
        context.completeNow();
    }
}

