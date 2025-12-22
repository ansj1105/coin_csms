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
    
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        log.info("Starting ApiVerticle...");
        
        JsonObject config = config();
        JsonObject httpConfig = config.getJsonObject("http", new JsonObject());
        int port = httpConfig.getInteger("port", 8080);
        
        // ServiceFactory 초기화
        serviceFactory = DefaultServiceFactory.create(vertx, config);
        
        // Router 생성
        Router mainRouter = Router.router(vertx);
        
        // 전역 핸들러 설정
        setupGlobalHandlers(mainRouter);
        
        // 도메인별 라우터 등록
        registerRouters(mainRouter);
        
        // HTTP 서버 시작
        HttpServerOptions serverOptions = new HttpServerOptions()
            .setCompressionSupported(true);
        
        vertx.createHttpServer(serverOptions)
            .requestHandler(mainRouter)
            .listen(port, http -> {
                if (http.succeeded()) {
                    log.info("HTTP API server started on port {}", port);
                    startPromise.complete();
                } else {
                    log.error("Failed to start HTTP server", http.cause());
                    startPromise.fail(http.cause());
                }
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
    }
    
    /**
     * 도메인별 라우터 등록
     * 각 도메인 모듈의 Handler를 등록합니다.
     */
    private void registerRouters(Router mainRouter) {
        // User 도메인 등록 예시
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
        
        log.info("Routers registered");
    }
    
    protected ServiceFactory getServiceFactory() {
        return serviceFactory;
    }
}

