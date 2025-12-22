package com.csms.currency.handler;

import com.csms.common.handler.BaseHandler;
import com.csms.currency.service.CurrencyService;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class CurrencyHandler extends BaseHandler {
    
    private final CurrencyService currencyService;
    
    public CurrencyHandler(Vertx vertx, CurrencyService currencyService) {
        super(vertx);
        this.currencyService = currencyService;
    }
    
    @Override
    public Router getRouter() {
        Router router = Router.router(getVertx());
        
        // 공개 API (인증 불필요)
        router.get("/").handler(this::getCurrencies);
        
        return router;
    }
    
    private void getCurrencies(RoutingContext ctx) {
        response(ctx, currencyService.getActiveCurrencies());
    }
}

