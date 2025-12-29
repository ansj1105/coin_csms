package com.csms.admin.service;

import com.csms.admin.dto.AdminLoginDto;
import com.csms.admin.dto.AdminLoginResponseDto;
import com.csms.admin.entities.Admin;
import com.csms.admin.repository.AdminRepository;
import com.csms.common.enums.UserRole;
import com.csms.common.exceptions.BadRequestException;
import com.csms.common.exceptions.UnauthorizedException;
import com.csms.common.service.BaseService;
import com.csms.common.utils.AuthUtils;
import com.csms.common.utils.RateLimiter;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;

@Slf4j
public class AdminAuthService extends BaseService {
    
    private final AdminRepository adminRepository;
    private final JWTAuth jwtAuth;
    private final JsonObject jwtConfig;
    private final RateLimiter rateLimiter;
    
    public AdminAuthService(PgPool pool, 
                            AdminRepository adminRepository, 
                            JWTAuth jwtAuth, 
                            JsonObject jwtConfig,
                            RateLimiter rateLimiter) {
        super(pool);
        this.adminRepository = adminRepository;
        this.jwtAuth = jwtAuth;
        this.jwtConfig = jwtConfig;
        this.rateLimiter = rateLimiter;
    }
    
    public Future<AdminLoginResponseDto> login(AdminLoginDto dto, String clientIp) {
        log.info("Admin login attempt - adminId: {}, ip: {}", dto.getId(), clientIp);
        
        // Rate Limiting 체크 (5회 실패 시 30분 차단)
        Future<Boolean> rateLimitCheck;
        if (rateLimiter == null) {
            // Redis 연결 실패 시 Rate Limiting 없이 진행 (허용)
            log.warn("RateLimiter not available, skipping rate limit check - adminId: {}, ip: {}", 
                dto.getId(), clientIp);
            rateLimitCheck = Future.succeededFuture(true);
        } else {
            rateLimitCheck = rateLimiter.checkAdminLoginAttempt(clientIp)
                .recover(err -> {
                    log.error("Rate limiter check failed, allowing login - adminId: {}, ip: {}", 
                        dto.getId(), clientIp, err);
                    // Redis 오류 시 허용 (fail-open)
                    return Future.succeededFuture(true);
                });
        }
        
        return rateLimitCheck
            .compose(allowed -> {
                if (!allowed) {
                    log.warn("Admin login blocked by rate limiter - adminId: {}, ip: {}", 
                        dto.getId(), clientIp);
                    return Future.failedFuture(new BadRequestException(
                        "로그인 시도 횟수를 초과했습니다. 30분 후 다시 시도해주세요."));
                }
                
                return adminRepository.getAdminByLoginId(client, dto.getId())
                    .compose(admin -> {
                        if (admin == null) {
                            log.warn("Admin login failed - admin not found - adminId: {}, ip: {}", 
                                dto.getId(), clientIp);
                            if (rateLimiter != null) {
                                rateLimiter.recordAdminLoginFailure(clientIp);
                            }
                            return Future.failedFuture(new UnauthorizedException("Invalid admin credentials"));
                        }
                        
                        // 비밀번호 검증
                        if (!BCrypt.checkpw(dto.getPassword(), admin.getPasswordHash())) {
                            log.warn("Admin login failed - invalid password - adminId: {}, ip: {}", 
                                dto.getId(), clientIp);
                            if (rateLimiter != null) {
                                rateLimiter.recordAdminLoginFailure(clientIp);
                            }
                            return Future.failedFuture(new UnauthorizedException("Invalid admin credentials"));
                        }
                        
                        // 관리자 권한 확인
                        UserRole adminRole = admin.getRole() == 3 ? UserRole.SUPER_ADMIN : UserRole.ADMIN;
                        
                        // 관리자 토큰 생성 (1시간 만료)
                        String accessToken = AuthUtils.generateToken(jwtAuth, admin.getId(), adminRole, 3600);
                        
                        // 성공 시 실패 카운트 리셋
                        if (rateLimiter != null) {
                            rateLimiter.resetAdminLoginFailure(clientIp);
                        }
                        
                        // 관리자 로그인 로깅
                        log.info("Admin login successful - adminId: {}, role: {}, ip: {}", 
                            admin.getId(), adminRole, clientIp);
                        
                        return Future.succeededFuture(
                            AdminLoginResponseDto.builder()
                                .accessToken(accessToken)
                                .adminId(admin.getLoginId())
                                .build()
                        );
                    });
            })
            .onFailure(err -> {
                log.error("Admin login failed - adminId: {}, ip: {}", dto.getId(), clientIp, err);
            });
    }
}

