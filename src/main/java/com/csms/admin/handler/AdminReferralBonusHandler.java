package com.csms.admin.handler;

import com.csms.admin.dto.UpdateReferralBonusRequestDto;
import com.csms.admin.service.AdminReferralBonusService;
import com.csms.common.handler.BaseHandler;
import com.csms.common.utils.ErrorHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdminReferralBonusHandler extends BaseHandler {
    
    private final AdminReferralBonusService service;
    
    public AdminReferralBonusHandler(Vertx vertx, AdminReferralBonusService service) {
        super(vertx);
        this.service = service;
    }
    
    public Router getRouter() {
        Router router = Router.router(vertx);
        
        router.get("/referral-bonus").handler(this::getReferralBonus);
        router.patch("/referral-bonus").handler(this::updateReferralBonus);
        
        return router;
    }
    
    private void getReferralBonus(RoutingContext ctx) {
        try {
            service.getReferralBonus()
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
    
    private void updateReferralBonus(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            UpdateReferralBonusRequestDto request = body.mapTo(UpdateReferralBonusRequestDto.class);
            
            service.updateReferralBonus(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Referral bonus updated"));
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

