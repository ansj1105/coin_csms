package com.csms.verticle;

import com.csms.admin.handler.*;
import com.csms.admin.repository.*;
import com.csms.admin.service.*;
import com.csms.common.utils.ErrorHandler;
import com.csms.common.utils.RateLimiter;
import com.csms.core.factory.DefaultServiceFactory;
import com.csms.core.factory.ServiceFactory;
import com.csms.currency.handler.CurrencyHandler;
import com.csms.currency.repository.CurrencyRepository;
import com.csms.currency.service.CurrencyService;
import com.csms.user.handler.UserHandler;
import com.csms.user.repository.UserRepository;
import com.csms.user.service.UserService;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.RedisRole;
import lombok.extern.slf4j.Slf4j;

/**
 * API Verticle - HTTP API 서버를 담당하는 Verticle
 * 개선사항:
 * - ServiceFactory를 통한 의존성 주입
 * - 모듈화된 라우터 등록
 * - 전역 핸들러 설정
 */
@Slf4j
public class ApiVerticle extends AbstractVerticle {
    
    static {
        DatabindCodec.mapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    private ServiceFactory serviceFactory;
    private RedisAPI redisApi;
    
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        log.info("Starting ApiVerticle...");
        
        JsonObject config = config();
        JsonObject httpConfig = config.getJsonObject("http", new JsonObject());
        int port = httpConfig.getInteger("port", 8080);
        
        // ServiceFactory 초기화
        serviceFactory = DefaultServiceFactory.create(vertx, config);
        
        // Redis 연결 (RateLimiter용)
        connectRedis(config)
            .compose(v -> {
                // Router 생성
                Router mainRouter = Router.router(vertx);
                
                // 전역 핸들러 설정
                setupGlobalHandlers(mainRouter);
                
                // 도메인별 라우터 등록
                registerRouters(mainRouter);
                
                // HTTP 서버 시작
                HttpServerOptions serverOptions = new HttpServerOptions()
                    .setCompressionSupported(true);
                
                return vertx.createHttpServer(serverOptions)
                    .requestHandler(mainRouter)
                    .listen(port);
            })
            .onSuccess(http -> {
                log.info("HTTP API server started on port {}", port);
                startPromise.complete();
            })
            .onFailure(throwable -> {
                log.error("Failed to start HTTP server", throwable);
                startPromise.fail(throwable);
            });
    }
    
    /**
     * 전역 핸들러 설정
     */
    private void setupGlobalHandlers(Router router) {
        // CORS
        router.route().handler(CorsHandler.create()
            .addRelativeOrigin(".*")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.DELETE)
            .allowedMethod(HttpMethod.PATCH)
            .allowedMethod(HttpMethod.OPTIONS)
            .allowedHeader("Content-Type")
            .allowedHeader("Authorization")
            .allowedHeader("Accept")
            .allowedHeader("Origin")
            .allowedHeader("X-Requested-With")
            .exposedHeader("Content-Length")
            .exposedHeader("Content-Type"));
        
        // Body Handler
        router.route().handler(BodyHandler.create());
        
        // Request 로깅
        router.route().handler(ctx -> {
            log.info("[REQUEST] {} {} from {}", 
                ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());
            ctx.next();
        });
        
        // Failure Handler
        router.route().failureHandler(ErrorHandler::handle);
        
        // Health check
        router.get("/health").handler(ctx -> {
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(Json.encode(new JsonObject()
                    .put("status", "UP")
                    .put("timestamp", System.currentTimeMillis())));
        });
        
        // Swagger UI
        router.get("/api-docs").handler(ctx -> {
            ctx.response()
                .putHeader("Content-Type", "text/html; charset=utf-8")
                .end(getSwaggerHtml());
        });
        
        // OpenAPI Spec
        router.get("/openapi.yaml").handler(ctx -> {
            vertx.fileSystem().readFile("openapi.yaml")
                .recover(err -> vertx.fileSystem().readFile("src/main/resources/openapi.yaml"))
                .onSuccess(buffer -> {
                    ctx.response()
                        .putHeader("Content-Type", "application/x-yaml; charset=utf-8")
                        .end(buffer);
                })
                .onFailure(err -> {
                    log.error("Failed to load openapi.yaml", err);
                    ctx.response()
                        .setStatusCode(404)
                        .end("OpenAPI spec not found");
                });
        });
    }
    
    private String getSwaggerHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>CSMS API Documentation</title>
                <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5.10.0/swagger-ui.css">
                <style>
                    html { box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll; }
                    *, *:before, *:after { box-sizing: inherit; }
                    body { margin:0; padding:0; background: #fafafa; }
                </style>
            </head>
            <body>
                <div id="swagger-ui"></div>
                <script src="https://unpkg.com/swagger-ui-dist@5.10.0/swagger-ui-bundle.js"></script>
                <script src="https://unpkg.com/swagger-ui-dist@5.10.0/swagger-ui-standalone-preset.js"></script>
                <script>
                window.onload = function() {
                    window.ui = SwaggerUIBundle({
                        url: "/openapi.yaml",
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        presets: [
                            SwaggerUIBundle.presets.apis,
                            SwaggerUIStandalonePreset
                        ],
                        plugins: [
                            SwaggerUIBundle.plugins.DownloadUrl
                        ],
                        layout: "StandaloneLayout",
                        validatorUrl: null
                    });
                };
                </script>
            </body>
            </html>
            """;
    }
    
    /**
     * Redis 연결
     */
    private Future<Void> connectRedis(JsonObject config) {
        JsonObject redisConfig = config.getJsonObject("redis", new JsonObject());
        String mode = redisConfig.getString("mode", "standalone");
        
        RedisOptions options = new RedisOptions();
        
        String password = redisConfig.getString("password");
        if (password != null && !password.isEmpty()) {
            options.setPassword(password);
        }
        
        switch (mode) {
            case "cluster" -> {
                options.setType(RedisClientType.CLUSTER);
                JsonArray nodes = redisConfig.getJsonArray("nodes", new JsonArray());
                if (nodes.isEmpty()) {
                    options.addConnectionString("redis://localhost:7001");
                    options.addConnectionString("redis://localhost:7002");
                    options.addConnectionString("redis://localhost:7003");
                } else {
                    for (int i = 0; i < nodes.size(); i++) {
                        options.addConnectionString(nodes.getString(i));
                    }
                }
            }
            case "sentinel" -> {
                options.setType(RedisClientType.SENTINEL);
                options.setMasterName(redisConfig.getString("masterName", "mymaster"));
                options.setRole(RedisRole.MASTER);
                JsonArray sentinels = redisConfig.getJsonArray("sentinels", new JsonArray());
                if (sentinels.isEmpty()) {
                    options.addConnectionString("redis://localhost:26379");
                } else {
                    for (int i = 0; i < sentinels.size(); i++) {
                        options.addConnectionString(sentinels.getString(i));
                    }
                }
            }
            default -> {
                options.setType(RedisClientType.STANDALONE);
                String host = redisConfig.getString("host", "localhost");
                int port = redisConfig.getInteger("port", 6379);
                options.setConnectionString("redis://" + host + ":" + port);
            }
        }
        
        options.setMaxPoolSize(redisConfig.getInteger("maxPoolSize", 8));
        options.setMaxPoolWaiting(redisConfig.getInteger("maxPoolWaiting", 32));
        
        Redis redisClient = Redis.createClient(vertx, options);
        
        return redisClient.connect()
            .map(conn -> {
                log.info("Redis connected for RateLimiter");
                redisApi = RedisAPI.api(conn);
                return (Void) null;
            })
            .recover(err -> {
                log.warn("Failed to connect Redis for RateLimiter, continuing without rate limiting", err);
                // Redis 연결 실패해도 서버는 시작 (RateLimiter는 null 체크 필요)
                return Future.<Void>succeededFuture();
            });
    }
    
    /**
     * 도메인별 라우터 등록
     * 각 도메인 모듈의 Handler를 등록합니다.
     */
    private void registerRouters(Router mainRouter) {
        // User 도메인
        UserRepository userRepository = new UserRepository();
        UserService userService = new UserService(
            serviceFactory.getPool(),
            userRepository,
            serviceFactory.getJwtAuth(),
            serviceFactory.getJwtConfig()
        );
        UserHandler userHandler = new UserHandler(
            vertx,
            userService,
            serviceFactory.getJwtAuth()
        );
        mainRouter.mountSubRouter("/api/v1/users", userHandler.getRouter());
        
        // Admin 도메인
        AdminRepository adminRepository = new AdminRepository();
        // RateLimiter는 Redis 연결이 실패해도 null일 수 있으므로 체크 필요
        RateLimiter rateLimiter = redisApi != null 
            ? new RateLimiter(redisApi)
            : null; // Redis 연결 실패 시 null (RateLimiter에서 null 체크 필요)
        AdminAuthService adminAuthService = new AdminAuthService(
            serviceFactory.getPool(),
            adminRepository,
            serviceFactory.getJwtAuth(),
            serviceFactory.getJwtConfig(),
            rateLimiter
        );
        AdminAuthHandler adminAuthHandler = new AdminAuthHandler(
            vertx,
            adminAuthService
        );
        mainRouter.mountSubRouter("/api/v1/admin/auth", adminAuthHandler.getRouter());
        
        // Admin Dashboard
        AdminDashboardService adminDashboardService = new AdminDashboardService(
            serviceFactory.getPool()
        );
        AdminDashboardHandler adminDashboardHandler = new AdminDashboardHandler(
            vertx,
            adminDashboardService,
            serviceFactory.getJwtAuth()
        );
        mainRouter.mountSubRouter("/api/v1/admin", adminDashboardHandler.getRouter());
        
        // Admin Member 도메인 (통합)
        AdminMemberService adminMemberService = new AdminMemberService(
            serviceFactory.getPool()
        );
        AdminMemberExportService adminMemberExportService = new AdminMemberExportService(
            serviceFactory.getPool()
        );
        AdminMiningHistoryRepository adminMiningHistoryRepository = new AdminMiningHistoryRepository(
            serviceFactory.getPool()
        );
        AdminMiningHistoryService adminMiningHistoryService = new AdminMiningHistoryService(
            serviceFactory.getPool(),
            adminMiningHistoryRepository
        );
        AdminMiningHistoryExportService adminMiningHistoryExportService = new AdminMiningHistoryExportService(
            serviceFactory.getPool()
        );
        AdminMemberHandler adminMemberHandler = new AdminMemberHandler(
            vertx,
            adminMemberService,
            adminMemberExportService,
            adminMiningHistoryService,
            adminMiningHistoryExportService
        );
        mainRouter.mountSubRouter("/api/v1/admin/members", adminMemberHandler.getRouter());
        
        // Admin Mining 도메인 (통합)
        AdminMiningRepository adminMiningRepository = new AdminMiningRepository(
            serviceFactory.getPool()
        );
        AdminMiningService adminMiningService = new AdminMiningService(
            serviceFactory.getPool(),
            adminMiningRepository
        );
        AdminMiningExportService adminMiningExportService = new AdminMiningExportService(
            serviceFactory.getPool()
        );
        AdminMiningConditionRepository adminMiningConditionRepository = new AdminMiningConditionRepository(
            serviceFactory.getPool()
        );
        AdminMiningConditionService adminMiningConditionService = new AdminMiningConditionService(
            serviceFactory.getPool(),
            adminMiningConditionRepository
        );
        AdminMiningBoosterRepository adminMiningBoosterRepository = new AdminMiningBoosterRepository(
            serviceFactory.getPool()
        );
        AdminMiningBoosterService adminMiningBoosterService = new AdminMiningBoosterService(
            serviceFactory.getPool(),
            adminMiningBoosterRepository
        );
        AdminMiningHistoryListRepository adminMiningHistoryListRepository = new AdminMiningHistoryListRepository(
            serviceFactory.getPool()
        );
        AdminMiningHistoryListService adminMiningHistoryListService = new AdminMiningHistoryListService(
            serviceFactory.getPool(),
            adminMiningHistoryListRepository
        );
        AdminMiningHistoryListExportService adminMiningHistoryListExportService = new AdminMiningHistoryListExportService(
            serviceFactory.getPool()
        );
        // Admin Referral Bonus & Ranking Reward (같은 경로에 마운트되어 있음)
        AdminReferralBonusRepository adminReferralBonusRepository = new AdminReferralBonusRepository(
            serviceFactory.getPool()
        );
        AdminReferralBonusService adminReferralBonusService = new AdminReferralBonusService(
            serviceFactory.getPool(),
            adminReferralBonusRepository
        );
        AdminRankingRewardRepository adminRankingRewardRepository = new AdminRankingRewardRepository(
            serviceFactory.getPool()
        );
        AdminRankingRewardService adminRankingRewardService = new AdminRankingRewardService(
            serviceFactory.getPool(),
            adminRankingRewardRepository
        );
        AdminMiningHandler adminMiningHandler = new AdminMiningHandler(
            vertx,
            adminMiningService,
            adminMiningExportService,
            adminMiningConditionService,
            adminMiningBoosterService,
            adminMiningHistoryListService,
            adminMiningHistoryListExportService,
            adminReferralBonusService,
            adminRankingRewardService
        );
        mainRouter.mountSubRouter("/api/v1/admin/mining", adminMiningHandler.getRouter());
        
        // Currency 도메인
        CurrencyRepository currencyRepository = new CurrencyRepository();
        CurrencyService currencyService = new CurrencyService(
            serviceFactory.getPool(),
            currencyRepository
        );
        CurrencyHandler currencyHandler = new CurrencyHandler(
            vertx,
            currencyService
        );
        mainRouter.mountSubRouter("/api/v1/currencies", currencyHandler.getRouter());
        
        log.info("Routers registered");
    }
    
    protected ServiceFactory getServiceFactory() {
        return serviceFactory;
    }
}

