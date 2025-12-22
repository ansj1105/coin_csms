package com.csms.common.utils;

import io.vertx.core.Future;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Rate Limiting 유틸리티
 * Redis를 사용하여 로그인 시도 제한 관리
 */
@Slf4j
public class RateLimiter {
    
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_SECONDS = 1800; // 30분
    private static final String ADMIN_LOGIN_PREFIX = "admin:login:fail:";
    private static final String ADMIN_LOGIN_BLOCK_PREFIX = "admin:login:block:";
    
    private final RedisAPI redisApi;
    
    public RateLimiter(RedisAPI redisApi) {
        this.redisApi = redisApi;
    }
    
    /**
     * 관리자 로그인 시도 체크
     * @param clientIp 클라이언트 IP
     * @return 허용 여부
     */
    public Future<Boolean> checkAdminLoginAttempt(String clientIp) {
        String blockKey = ADMIN_LOGIN_BLOCK_PREFIX + clientIp;
        
        return redisApi.exists(List.of(blockKey))
            .map(response -> {
                if (response.toInteger() == 1) {
                    log.warn("Admin login blocked for IP: {}", clientIp);
                    return false;
                }
                return true;
            })
            .recover(err -> {
                log.error("Failed to check admin login attempt", err);
                // Redis 오류 시 허용 (fail-open)
                return Future.succeededFuture(true);
            });
    }
    
    /**
     * 관리자 로그인 실패 기록
     * @param clientIp 클라이언트 IP
     */
    public void recordAdminLoginFailure(String clientIp) {
        String failKey = ADMIN_LOGIN_PREFIX + clientIp;
        
        redisApi.incr(failKey)
            .compose(response -> {
                int attempts = response.toInteger();
                
                // 실패 횟수 설정 (30분 TTL)
                redisApi.expire(failKey, String.valueOf(BLOCK_DURATION_SECONDS));
                
                // 5회 실패 시 차단
                if (attempts >= MAX_LOGIN_ATTEMPTS) {
                    String blockKey = ADMIN_LOGIN_BLOCK_PREFIX + clientIp;
                    return redisApi.setex(blockKey, String.valueOf(BLOCK_DURATION_SECONDS), "1")
                        .map(v -> {
                            log.warn("Admin login blocked for IP: {} after {} attempts", clientIp, attempts);
                            return v;
                        });
                }
                
                return Future.succeededFuture();
            })
            .onFailure(err -> log.error("Failed to record admin login failure", err));
    }
    
    /**
     * 관리자 로그인 실패 카운트 리셋
     * @param clientIp 클라이언트 IP
     */
    public void resetAdminLoginFailure(String clientIp) {
        String failKey = ADMIN_LOGIN_PREFIX + clientIp;
        String blockKey = ADMIN_LOGIN_BLOCK_PREFIX + clientIp;
        
        redisApi.del(Arrays.asList(failKey, blockKey))
            .onFailure(err -> log.error("Failed to reset admin login failure", err));
    }
}

