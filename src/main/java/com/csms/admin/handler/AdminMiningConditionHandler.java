package com.csms.admin.handler;

import com.csms.admin.dto.*;
import com.csms.admin.service.AdminMiningConditionService;
import com.csms.common.handler.BaseHandler;
import com.csms.common.utils.ErrorHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdminMiningConditionHandler extends BaseHandler {
    
    private final AdminMiningConditionService service;
    
    public AdminMiningConditionHandler(Vertx vertx, AdminMiningConditionService service) {
        super(vertx);
        this.service = service;
    }
    
    public Router getRouter() {
        Router router = Router.router(vertx);
        
        router.get("/conditions").handler(this::getMiningConditions);
        router.patch("/conditions/basic").handler(this::updateBasicConditions);
        router.patch("/conditions/progress").handler(this::updateProgressSetting);
        router.patch("/conditions/level-limit").handler(this::updateLevelLimit);
        router.patch("/conditions/level-limits-enabled").handler(this::updateLevelLimitsEnabled);
        
        return router;
    }
    
    private void getMiningConditions(RoutingContext ctx) {
        try {
            service.getMiningConditions()
                .onSuccess(result -> {
                    success(ctx, result);
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void updateBasicConditions(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            UpdateBasicConditionRequestDto request = body.mapTo(UpdateBasicConditionRequestDto.class);
            
            service.updateBasicConditions(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Basic conditions updated"));
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void updateProgressSetting(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            UpdateProgressSettingRequestDto request = body.mapTo(UpdateProgressSettingRequestDto.class);
            
            service.updateProgressSetting(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Progress setting updated"));
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void updateLevelLimit(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            UpdateLevelLimitRequestDto request = body.mapTo(UpdateLevelLimitRequestDto.class);
            
            service.updateLevelLimit(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Level limit updated"));
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void updateLevelLimitsEnabled(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            UpdateLevelLimitsEnabledRequestDto request = body.mapTo(UpdateLevelLimitsEnabledRequestDto.class);
            
            service.updateLevelLimitsEnabled(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Level limits enabled updated"));
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
}

