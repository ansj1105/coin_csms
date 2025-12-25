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
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.redis.client.RedisAPI;
import io.vertx.sqlclient.PoolOptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class DefaultServiceFactory implements ServiceFactory {
    
    private final Vertx vertx;
    private final JsonObject config;
    private final PgPool pool;
    private final JWTAuth jwtAuth;
    private final WebClient webClient;
    
    // Lazy initialization을 위한 필드들
    private UserRepository userRepository;
    private AdminRepository adminRepository;
    private AdminMemberRepository adminMemberRepository;
    private AdminMiningRepository adminMiningRepository;
    private CurrencyRepository currencyRepository;
    
    private UserService userService;
    private AdminAuthService adminAuthService;
    private AdminDashboardService adminDashboardService;
    private AdminMemberService adminMemberService;
    private AdminMiningService adminMiningService;
    private CurrencyService currencyService;
    
    public DefaultServiceFactory(Vertx vertx, JsonObject config, PgPool pool, JWTAuth jwtAuth, WebClient webClient) {
        this.vertx = vertx;
        this.config = config;
        this.pool = pool;
        this.jwtAuth = jwtAuth;
        this.webClient = webClient;
    }
    
    public static DefaultServiceFactory create(Vertx vertx, JsonObject config) {
        JsonObject databaseConfig = config.getJsonObject("database", new JsonObject());
        JsonObject jwtConfig = config.getJsonObject("jwt", new JsonObject());
        
        PgPool pool = createPgPool(vertx, databaseConfig);
        JWTAuth jwtAuth = createJwtAuth(vertx, jwtConfig);
        WebClient webClient = WebClient.create(vertx);
        
        return new DefaultServiceFactory(vertx, config, pool, jwtAuth, webClient);
    }
    
    private static PgPool createPgPool(Vertx vertx, JsonObject config) {
        PgConnectOptions connectOptions = new PgConnectOptions()
            .setHost(config.getString("host"))
            .setPort(config.getInteger("port"))
            .setDatabase(config.getString("database"))
            .setUser(config.getString("user"))
            .setPassword(config.getString("password"));
        
        PoolOptions poolOptions = new PoolOptions()
            .setMaxSize(config.getInteger("pool_size", 10))
            .setIdleTimeout(config.getInteger("idle_timeout", 60))
            .setPoolCleanerPeriod(config.getInteger("pool_cleaner_period", 60));
        
        return PgPool.pool(vertx, connectOptions, poolOptions);
    }
    
    private static JWTAuth createJwtAuth(Vertx vertx, JsonObject config) {
        String secret = config.getString("secret");
        
        return JWTAuth.create(vertx, new JWTAuthOptions()
            .addPubSecKey(new PubSecKeyOptions()
                .setAlgorithm("HS256")
                .setBuffer(secret)));
    }
    
    @Override
    public JsonObject getJwtConfig() {
        return config.getJsonObject("jwt", new JsonObject());
    }
    
    @Override
    public JsonObject getDatabaseConfig() {
        return config.getJsonObject("database", new JsonObject());
    }
    
    @Override
    public JsonObject getRedisConfig() {
        return config.getJsonObject("redis", new JsonObject());
    }
    
    @Override
    public JsonObject getFrontendConfig() {
        return config.getJsonObject("frontend", new JsonObject());
    }
    
    // ========== Repository 생성 메서드 (Lazy Initialization) ==========
    
    @Override
    public UserRepository getUserRepository() {
        if (userRepository == null) {
            userRepository = new UserRepository();
        }
        return userRepository;
    }
    
    @Override
    public AdminRepository getAdminRepository() {
        if (adminRepository == null) {
            adminRepository = new AdminRepository();
        }
        return adminRepository;
    }
    
    @Override
    public AdminMemberRepository getAdminMemberRepository() {
        if (adminMemberRepository == null) {
            adminMemberRepository = new AdminMemberRepository(pool);
        }
        return adminMemberRepository;
    }
    
    @Override
    public AdminMiningRepository getAdminMiningRepository() {
        if (adminMiningRepository == null) {
            adminMiningRepository = new AdminMiningRepository(pool);
        }
        return adminMiningRepository;
    }
    
    @Override
    public CurrencyRepository getCurrencyRepository() {
        if (currencyRepository == null) {
            currencyRepository = new CurrencyRepository();
        }
        return currencyRepository;
    }
    
    // ========== Service 생성 메서드 (Lazy Initialization) ==========
    
    @Override
    public UserService getUserService() {
        if (userService == null) {
            userService = new UserService(
                pool,
                getUserRepository(),
                jwtAuth,
                getJwtConfig()
            );
        }
        return userService;
    }
    
    @Override
    public AdminAuthService getAdminAuthService() {
        if (adminAuthService == null) {
            RateLimiter rateLimiter = null; // Redis 연결 후 설정 필요
            adminAuthService = new AdminAuthService(
                pool,
                getAdminRepository(),
                jwtAuth,
                getJwtConfig(),
                rateLimiter
            );
        }
        return adminAuthService;
    }
    
    @Override
    public AdminDashboardService getAdminDashboardService() {
        if (adminDashboardService == null) {
            adminDashboardService = new AdminDashboardService(pool);
        }
        return adminDashboardService;
    }
    
    @Override
    public AdminMemberService getAdminMemberService() {
        if (adminMemberService == null) {
            adminMemberService = new AdminMemberService(pool);
        }
        return adminMemberService;
    }
    
    @Override
    public AdminMiningService getAdminMiningService() {
        if (adminMiningService == null) {
            adminMiningService = new AdminMiningService(
                pool,
                getAdminMiningRepository()
            );
        }
        return adminMiningService;
    }
    
    @Override
    public CurrencyService getCurrencyService() {
        if (currencyService == null) {
            currencyService = new CurrencyService(
                pool,
                getCurrencyRepository()
            );
        }
        return currencyService;
    }
    
    // ========== Handler 생성 메서드 ==========
    
    @Override
    public UserHandler getUserHandler(Vertx vertx) {
        return new UserHandler(
            vertx,
            getUserService(),
            jwtAuth
        );
    }
    
    @Override
    public AdminAuthHandler getAdminAuthHandler(Vertx vertx) {
        return new AdminAuthHandler(
            vertx,
            getAdminAuthService()
        );
    }
    
    @Override
    public AdminDashboardHandler getAdminDashboardHandler(Vertx vertx) {
        return new AdminDashboardHandler(
            vertx,
            getAdminDashboardService(),
            jwtAuth
        );
    }
    
    @Override
    public AdminMemberHandler getAdminMemberHandler(Vertx vertx) {
        return new AdminMemberHandler(
            vertx,
            getAdminMemberService()
        );
    }
    
    @Override
    public AdminMiningHandler getAdminMiningHandler(Vertx vertx) {
        return new AdminMiningHandler(
            vertx,
            getAdminMiningService()
        );
    }
    
    @Override
    public CurrencyHandler getCurrencyHandler(Vertx vertx) {
        return new CurrencyHandler(
            vertx,
            getCurrencyService()
        );
    }
    
    // ========== Utility 생성 메서드 ==========
    
    @Override
    public RateLimiter getRateLimiter(RedisAPI redisApi) {
        if (redisApi == null) {
            return null;
        }
        return new RateLimiter(redisApi);
    }
    
    // AdminAuthService의 RateLimiter 설정을 위한 메서드
    public void setAdminAuthServiceRateLimiter(RateLimiter rateLimiter) {
        // RateLimiter는 생성 시점에 설정되어야 하므로 재생성 필요
        // 또는 AdminAuthService에 setter 추가 필요
        // 현재는 생성자에서만 설정 가능하므로 재생성
        adminAuthService = new AdminAuthService(
            pool,
            getAdminRepository(),
            jwtAuth,
            getJwtConfig(),
            rateLimiter
        );
    }
}

