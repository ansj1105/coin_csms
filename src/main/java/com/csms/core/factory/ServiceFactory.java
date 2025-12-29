package com.csms.core.factory;

import com.csms.admin.handler.*;
import com.csms.admin.repository.*;
import com.csms.admin.service.*;
import com.csms.common.utils.RateLimiter;
import com.csms.currency.handler.CurrencyHandler;
import com.csms.currency.repository.CurrencyRepository;
import com.csms.currency.service.CurrencyService;
import com.csms.user.handler.UserHandler;
import com.csms.user.repository.UserRepository;
import com.csms.user.service.UserService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.pgclient.PgPool;
import io.vertx.ext.web.client.WebClient;
import io.vertx.redis.client.RedisAPI;

/**
 * 서비스 팩토리 - 의존성 주입을 위한 팩토리 패턴
 * 각 도메인별 서비스를 생성하고 의존성을 주입합니다.
 * DI Container 역할을 수행합니다.
 */
public interface ServiceFactory {
    
    PgPool getPool();
    
    JWTAuth getJwtAuth();
    
    WebClient getWebClient();
    
    JsonObject getConfig();
    
    JsonObject getJwtConfig();
    
    JsonObject getDatabaseConfig();
    
    JsonObject getRedisConfig();
    
    JsonObject getFrontendConfig();
    
    // ========== Repository 생성 메서드 ==========
    
    UserRepository getUserRepository();
    
    AdminRepository getAdminRepository();
    
    AdminMemberRepository getAdminMemberRepository();
    
    AdminMiningRepository getAdminMiningRepository();
    
    AdminDashboardRepository getAdminDashboardRepository();
    
    AdminFundsRepository getAdminFundsRepository();
    
    AdminReferralRepository getAdminReferralRepository();
    
    CurrencyRepository getCurrencyRepository();
    
    // ========== Service 생성 메서드 ==========
    
    UserService getUserService();
    
    AdminAuthService getAdminAuthService();
    
    AdminDashboardService getAdminDashboardService();
    
    AdminMemberService getAdminMemberService();
    
    AdminMiningService getAdminMiningService();
    
    AdminFundsService getAdminFundsService();
    
    AdminReferralService getAdminReferralService();
    
    CurrencyService getCurrencyService();
    
    // ========== Handler 생성 메서드 ==========
    
    UserHandler getUserHandler(Vertx vertx);
    
    AdminAuthHandler getAdminAuthHandler(Vertx vertx);
    
    AdminDashboardHandler getAdminDashboardHandler(Vertx vertx);
    
    AdminMemberHandler getAdminMemberHandler(Vertx vertx);
    
    AdminMiningHandler getAdminMiningHandler(Vertx vertx);
    
    AdminFundsHandler getAdminFundsHandler(Vertx vertx);
    
    AdminReferralHandler getAdminReferralHandler(Vertx vertx);
    
    CurrencyHandler getCurrencyHandler(Vertx vertx);
    
    // ========== Utility 생성 메서드 ==========
    
    RateLimiter getRateLimiter(RedisAPI redisApi);
}

