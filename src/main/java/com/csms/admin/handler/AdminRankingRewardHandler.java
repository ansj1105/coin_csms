package com.csms.admin.handler;

import com.csms.admin.dto.UpdateRankingRewardRequestDto;
import com.csms.admin.service.AdminRankingRewardService;
import com.csms.common.handler.BaseHandler;
import com.csms.common.utils.ErrorHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdminRankingRewardHandler extends BaseHandler {
    
    private final AdminRankingRewardService service;
    
    public AdminRankingRewardHandler(Vertx vertx, AdminRankingRewardService service) {
        super(vertx);
        this.service = service;
    }
    
    public Router getRouter() {
        Router router = Router.router(vertx);
        
        router.get("/ranking-reward").handler(this::getRankingReward);
        router.patch("/ranking-reward").handler(this::updateRankingReward);
        
        return router;
    }
    
    private void getRankingReward(RoutingContext ctx) {
        try {
            service.getRankingReward()
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
    
    private void updateRankingReward(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            UpdateRankingRewardRequestDto request = body.mapTo(UpdateRankingRewardRequestDto.class);
            
            service.updateRankingReward(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Ranking reward updated"));
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

