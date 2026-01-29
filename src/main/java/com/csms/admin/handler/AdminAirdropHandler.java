package com.csms.admin.handler;

import com.csms.admin.dto.*;
import com.csms.admin.service.AdminAirdropService;
import com.csms.common.enums.UserRole;
import com.csms.common.handler.BaseHandler;
import com.csms.common.utils.AuthUtils;
import com.csms.common.utils.ErrorHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdminAirdropHandler extends BaseHandler {
    
    private final AdminAirdropService service;
    private final JWTAuth jwtAuth;
    
    public AdminAirdropHandler(
        Vertx vertx,
        AdminAirdropService service,
        JWTAuth jwtAuth
    ) {
        super(vertx);
        this.service = service;
        this.jwtAuth = jwtAuth;
    }
    
    public Router getRouter() {
        Router router = Router.router(vertx);
        
        // 인증 및 권한 체크 미들웨어
        router.route().handler(JWTAuthHandler.create(jwtAuth));
        router.route().handler(AuthUtils.hasRole(UserRole.ADMIN, UserRole.SUPER_ADMIN));
        
        // Phase CRUD
        router.get("/phases").handler(this::getPhases);
        router.get("/phases/:id").handler(this::getPhaseById);
        router.post("/phases").handler(this::createPhase);
        router.patch("/phases/:id").handler(this::updatePhase);
        router.delete("/phases/:id").handler(this::deletePhase);
        
        // Transfer 조회
        router.get("/transfers").handler(this::getTransfers);
        
        return router;
    }
    
    private void getPhases(RoutingContext ctx) {
        try {
            Integer limit = getQueryParamAsInteger(ctx, "limit", 20);
            Integer offset = getQueryParamAsInteger(ctx, "offset", 0);
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            Long userId = getQueryParamAsLong(ctx, "userId");
            Integer phase = getQueryParamAsInteger(ctx, "phase");
            String status = ctx.queryParams().get("status");
            
            service.getPhases(limit, offset, searchCategory, searchKeyword, userId, phase, status)
                .onSuccess(result -> {
                    success(ctx, result);
                })
                .onFailure(err -> {
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ErrorHandler.handle(ctx);
        }
    }
    
    private void getPhaseById(RoutingContext ctx) {
        try {
            Long phaseId = Long.parseLong(ctx.pathParam("id"));
            
            service.getPhaseById(phaseId)
                .onSuccess(result -> {
                    success(ctx, result);
                })
                .onFailure(err -> {
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ErrorHandler.handle(ctx);
        }
    }
    
    private void createPhase(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            CreateAirdropPhaseRequestDto request = body.mapTo(CreateAirdropPhaseRequestDto.class);
            
            service.createPhase(request)
                .onSuccess(result -> {
                    success(ctx, result);
                })
                .onFailure(err -> {
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ErrorHandler.handle(ctx);
        }
    }
    
    private void updatePhase(RoutingContext ctx) {
        try {
            Long phaseId = Long.parseLong(ctx.pathParam("id"));
            JsonObject body = ctx.body().asJsonObject();
            UpdateAirdropPhaseRequestDto request = body.mapTo(UpdateAirdropPhaseRequestDto.class);
            
            service.updatePhase(phaseId, request)
                .onSuccess(result -> {
                    success(ctx, result);
                })
                .onFailure(err -> {
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ErrorHandler.handle(ctx);
        }
    }
    
    private void deletePhase(RoutingContext ctx) {
        try {
            Long phaseId = Long.parseLong(ctx.pathParam("id"));
            
            service.deletePhase(phaseId)
                .onSuccess(result -> {
                    success(ctx, null);
                })
                .onFailure(err -> {
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ErrorHandler.handle(ctx);
        }
    }
    
    private void getTransfers(RoutingContext ctx) {
        try {
            Integer limit = getQueryParamAsInteger(ctx, "limit", 20);
            Integer offset = getQueryParamAsInteger(ctx, "offset", 0);
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            Long userId = getQueryParamAsLong(ctx, "userId");
            String status = ctx.queryParams().get("status");
            
            service.getTransfers(limit, offset, searchCategory, searchKeyword, userId, status)
                .onSuccess(result -> {
                    success(ctx, result);
                })
                .onFailure(err -> {
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ErrorHandler.handle(ctx);
        }
    }
}
