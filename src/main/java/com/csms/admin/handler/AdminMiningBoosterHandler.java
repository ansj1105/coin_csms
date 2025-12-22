package com.csms.admin.handler;

import com.csms.admin.dto.UpdateBoosterRequestDto;
import com.csms.admin.service.AdminMiningBoosterService;
import com.csms.common.handler.BaseHandler;
import com.csms.common.utils.ErrorHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdminMiningBoosterHandler extends BaseHandler {
    
    private final AdminMiningBoosterService service;
    
    public AdminMiningBoosterHandler(Vertx vertx, AdminMiningBoosterService service) {
        super(vertx);
        this.service = service;
    }
    
    public Router getRouter() {
        Router router = Router.router(vertx);
        
        router.get("/booster").handler(this::getMiningBoosters);
        router.patch("/booster").handler(this::updateBooster);
        
        return router;
    }
    
    private void getMiningBoosters(RoutingContext ctx) {
        try {
            service.getMiningBoosters()
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
    
    private void updateBooster(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            UpdateBoosterRequestDto request = body.mapTo(UpdateBoosterRequestDto.class);
            
            service.updateBooster(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Booster updated"));
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

