package com.csms.common.utils;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class RateLimiterTest {
    
    @Mock
    private RedisAPI redisApi;
    
    @Mock
    private Response response;
    
    private RateLimiter rateLimiter;
    
    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter(redisApi);
    }
    
    @Test
    void testCheckAdminLoginAttempt_Allowed(VertxTestContext context) {
        // Given
        when(redisApi.exists(any())).thenReturn(Future.succeededFuture(response));
        when(response.toInteger()).thenReturn(0); // 차단되지 않음
        
        // When
        rateLimiter.checkAdminLoginAttempt("127.0.0.1")
            .onSuccess(allowed -> {
                // Then
                assertThat(allowed).isTrue();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testCheckAdminLoginAttempt_Blocked(VertxTestContext context) {
        // Given
        when(redisApi.exists(any())).thenReturn(Future.succeededFuture(response));
        when(response.toInteger()).thenReturn(1); // 차단됨
        
        // When
        rateLimiter.checkAdminLoginAttempt("127.0.0.1")
            .onSuccess(allowed -> {
                // Then
                assertThat(allowed).isFalse();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testCheckAdminLoginAttempt_RedisFailure(VertxTestContext context) {
        // Given
        when(redisApi.exists(any())).thenReturn(Future.failedFuture(new RuntimeException("Redis error")));
        
        // When
        rateLimiter.checkAdminLoginAttempt("127.0.0.1")
            .onSuccess(allowed -> {
                // Then - fail-open: Redis 오류 시 허용
                assertThat(allowed).isTrue();
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
}

