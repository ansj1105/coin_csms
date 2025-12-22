package com.csms.verticle;

import com.csms.common.utils.ErrorHandler;
import com.csms.core.factory.DefaultServiceFactory;
import com.csms.core.factory.ServiceFactory;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
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
    private io.vertx.redis.client.RedisAPI redisApi;
    
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
    private io.vertx.core.Future<Void> connectRedis(JsonObject config) {
        JsonObject redisConfig = config.getJsonObject("redis", new JsonObject());
        String mode = redisConfig.getString("mode", "standalone");
        
        io.vertx.redis.client.RedisOptions options = new io.vertx.redis.client.RedisOptions();
        
        String password = redisConfig.getString("password");
        if (password != null && !password.isEmpty()) {
            options.setPassword(password);
        }
        
        switch (mode) {
            case "cluster" -> {
                options.setType(io.vertx.redis.client.RedisClientType.CLUSTER);
                io.vertx.core.json.JsonArray nodes = redisConfig.getJsonArray("nodes", new io.vertx.core.json.JsonArray());
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
                options.setType(io.vertx.redis.client.RedisClientType.SENTINEL);
                options.setMasterName(redisConfig.getString("masterName", "mymaster"));
                options.setRole(io.vertx.redis.client.RedisRole.MASTER);
                io.vertx.core.json.JsonArray sentinels = redisConfig.getJsonArray("sentinels", new io.vertx.core.json.JsonArray());
                if (sentinels.isEmpty()) {
                    options.addConnectionString("redis://localhost:26379");
                } else {
                    for (int i = 0; i < sentinels.size(); i++) {
                        options.addConnectionString(sentinels.getString(i));
                    }
                }
            }
            default -> {
                options.setType(io.vertx.redis.client.RedisClientType.STANDALONE);
                String host = redisConfig.getString("host", "localhost");
                int port = redisConfig.getInteger("port", 6379);
                options.setConnectionString("redis://" + host + ":" + port);
            }
        }
        
        options.setMaxPoolSize(redisConfig.getInteger("maxPoolSize", 8));
        options.setMaxPoolWaiting(redisConfig.getInteger("maxPoolWaiting", 32));
        
        io.vertx.redis.client.Redis redisClient = io.vertx.redis.client.Redis.createClient(vertx, options);
        
        return redisClient.connect()
            .map(conn -> {
                log.info("Redis connected for RateLimiter");
                redisApi = io.vertx.redis.client.RedisAPI.api(conn);
                return (Void) null;
            })
            .recover(err -> {
                log.warn("Failed to connect Redis for RateLimiter, continuing without rate limiting", err);
                // Redis 연결 실패해도 서버는 시작 (RateLimiter는 null 체크 필요)
                return io.vertx.core.Future.<Void>succeededFuture();
            });
    }
    
    /**
     * 도메인별 라우터 등록
     * 각 도메인 모듈의 Handler를 등록합니다.
     */
    private void registerRouters(Router mainRouter) {
        // User 도메인
        com.csms.user.repository.UserRepository userRepository = new com.csms.user.repository.UserRepository();
        com.csms.user.service.UserService userService = new com.csms.user.service.UserService(
            serviceFactory.getPool(),
            userRepository,
            serviceFactory.getJwtAuth(),
            serviceFactory.getJwtConfig()
        );
        com.csms.user.handler.UserHandler userHandler = new com.csms.user.handler.UserHandler(
            vertx,
            userService,
            serviceFactory.getJwtAuth()
        );
        mainRouter.mountSubRouter("/api/v1/users", userHandler.getRouter());
        
        // Admin 도메인
        com.csms.admin.repository.AdminRepository adminRepository = new com.csms.admin.repository.AdminRepository();
        // RateLimiter는 Redis 연결이 실패해도 null일 수 있으므로 체크 필요
        com.csms.common.utils.RateLimiter rateLimiter = redisApi != null 
            ? new com.csms.common.utils.RateLimiter(redisApi)
            : null; // Redis 연결 실패 시 null (RateLimiter에서 null 체크 필요)
        com.csms.admin.service.AdminAuthService adminAuthService = new com.csms.admin.service.AdminAuthService(
            serviceFactory.getPool(),
            adminRepository,
            serviceFactory.getJwtAuth(),
            serviceFactory.getJwtConfig(),
            rateLimiter
        );
        com.csms.admin.handler.AdminAuthHandler adminAuthHandler = new com.csms.admin.handler.AdminAuthHandler(
            vertx,
            adminAuthService
        );
        mainRouter.mountSubRouter("/api/v1/admin/auth", adminAuthHandler.getRouter());
        
        // Admin Dashboard
        com.csms.admin.service.AdminDashboardService adminDashboardService = new com.csms.admin.service.AdminDashboardService(
            serviceFactory.getPool()
        );
        com.csms.admin.handler.AdminDashboardHandler adminDashboardHandler = new com.csms.admin.handler.AdminDashboardHandler(
            vertx,
            adminDashboardService,
            serviceFactory.getJwtAuth()
        );
        mainRouter.mountSubRouter("/api/v1/admin", adminDashboardHandler.getRouter());
        
        // Admin Member 도메인
        com.csms.admin.service.AdminMemberService adminMemberService = new com.csms.admin.service.AdminMemberService(
            serviceFactory.getPool()
        );
        com.csms.admin.service.AdminMemberExportService adminMemberExportService = new com.csms.admin.service.AdminMemberExportService(
            serviceFactory.getPool()
        );
        com.csms.admin.handler.AdminMemberHandler adminMemberHandler = new com.csms.admin.handler.AdminMemberHandler(
            vertx,
            adminMemberService,
            adminMemberExportService
        );
        mainRouter.mountSubRouter("/api/v1/admin/members", adminMemberHandler.getRouter());
        
        // Admin Mining History 도메인 (회원별 채굴 내역)
        com.csms.admin.repository.AdminMiningHistoryRepository adminMiningHistoryRepository = new com.csms.admin.repository.AdminMiningHistoryRepository(
            serviceFactory.getPool()
        );
        com.csms.admin.service.AdminMiningHistoryService adminMiningHistoryService = new com.csms.admin.service.AdminMiningHistoryService(
            serviceFactory.getPool(),
            adminMiningHistoryRepository
        );
        com.csms.admin.service.AdminMiningHistoryExportService adminMiningHistoryExportService = new com.csms.admin.service.AdminMiningHistoryExportService(
            serviceFactory.getPool()
        );
        com.csms.admin.handler.AdminMiningHistoryHandler adminMiningHistoryHandler = new com.csms.admin.handler.AdminMiningHistoryHandler(
            vertx,
            adminMiningHistoryService,
            adminMiningHistoryExportService
        );
        mainRouter.mountSubRouter("/api/v1/admin/members", adminMiningHistoryHandler.getRouter());
        
        // Admin Mining 도메인
        com.csms.admin.repository.AdminMiningRepository adminMiningRepository = new com.csms.admin.repository.AdminMiningRepository(
            serviceFactory.getPool()
        );
        com.csms.admin.service.AdminMiningService adminMiningService = new com.csms.admin.service.AdminMiningService(
            serviceFactory.getPool(),
            adminMiningRepository
        );
        com.csms.admin.service.AdminMiningExportService adminMiningExportService = new com.csms.admin.service.AdminMiningExportService(
            serviceFactory.getPool()
        );
        com.csms.admin.handler.AdminMiningHandler adminMiningHandler = new com.csms.admin.handler.AdminMiningHandler(
            vertx,
            adminMiningService,
            adminMiningExportService
        );
        mainRouter.mountSubRouter("/api/v1/admin/mining", adminMiningHandler.getRouter());
        
        // Admin Mining Condition 도메인
        com.csms.admin.repository.AdminMiningConditionRepository adminMiningConditionRepository = new com.csms.admin.repository.AdminMiningConditionRepository(
            serviceFactory.getPool()
        );
        com.csms.admin.service.AdminMiningConditionService adminMiningConditionService = new com.csms.admin.service.AdminMiningConditionService(
            serviceFactory.getPool(),
            adminMiningConditionRepository
        );
        com.csms.admin.handler.AdminMiningConditionHandler adminMiningConditionHandler = new com.csms.admin.handler.AdminMiningConditionHandler(
            vertx,
            adminMiningConditionService
        );
        mainRouter.mountSubRouter("/api/v1/admin/mining", adminMiningConditionHandler.getRouter());
        
        // Admin Mining History List 도메인 (채굴 내역 목록)
        com.csms.admin.repository.AdminMiningHistoryListRepository adminMiningHistoryListRepository = new com.csms.admin.repository.AdminMiningHistoryListRepository(
            serviceFactory.getPool()
        );
        com.csms.admin.service.AdminMiningHistoryListService adminMiningHistoryListService = new com.csms.admin.service.AdminMiningHistoryListService(
            serviceFactory.getPool(),
            adminMiningHistoryListRepository
        );
        com.csms.admin.service.AdminMiningHistoryListExportService adminMiningHistoryListExportService = new com.csms.admin.service.AdminMiningHistoryListExportService(
            serviceFactory.getPool()
        );
        com.csms.admin.handler.AdminMiningHistoryListHandler adminMiningHistoryListHandler = new com.csms.admin.handler.AdminMiningHistoryListHandler(
            vertx,
            adminMiningHistoryListService,
            adminMiningHistoryListExportService
        );
        mainRouter.mountSubRouter("/api/v1/admin/mining", adminMiningHistoryListHandler.getRouter());
        
        // Admin Mining Booster 도메인
        com.csms.admin.repository.AdminMiningBoosterRepository adminMiningBoosterRepository = new com.csms.admin.repository.AdminMiningBoosterRepository(
            serviceFactory.getPool()
        );
        com.csms.admin.service.AdminMiningBoosterService adminMiningBoosterService = new com.csms.admin.service.AdminMiningBoosterService(
            serviceFactory.getPool(),
            adminMiningBoosterRepository
        );
        com.csms.admin.handler.AdminMiningBoosterHandler adminMiningBoosterHandler = new com.csms.admin.handler.AdminMiningBoosterHandler(
            vertx,
            adminMiningBoosterService
        );
        mainRouter.mountSubRouter("/api/v1/admin/mining", adminMiningBoosterHandler.getRouter());
        
        // Currency 도메인
        com.csms.currency.repository.CurrencyRepository currencyRepository = new com.csms.currency.repository.CurrencyRepository();
        com.csms.currency.service.CurrencyService currencyService = new com.csms.currency.service.CurrencyService(
            serviceFactory.getPool(),
            currencyRepository
        );
        com.csms.currency.handler.CurrencyHandler currencyHandler = new com.csms.currency.handler.CurrencyHandler(
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

