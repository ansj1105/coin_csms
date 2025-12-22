package com.csms.admin.handler;

import com.csms.admin.service.AdminDashboardService;
import com.csms.common.enums.UserRole;
import com.csms.common.handler.BaseHandler;
import com.csms.common.utils.AuthUtils;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdminDashboardHandler extends BaseHandler {
    
    private final AdminDashboardService dashboardService;
    private final JWTAuth jwtAuth;
    
    public AdminDashboardHandler(Vertx vertx, AdminDashboardService dashboardService, JWTAuth jwtAuth) {
        super(vertx);
        this.dashboardService = dashboardService;
        this.jwtAuth = jwtAuth;
    }
    
    @Override
    public Router getRouter() {
        Router router = Router.router(getVertx());
        
        router.get("/dashboard")
            .handler(JWTAuthHandler.create(jwtAuth))
            .handler(AuthUtils.hasRole(UserRole.ADMIN, UserRole.SUPER_ADMIN))
            .handler(this::getDashboard);
        
        return router;
    }
    
    private void getDashboard(RoutingContext ctx) {
        String dateRange = ctx.queryParams().get("dateRange");
        String startDate = ctx.queryParams().get("startDate");
        String endDate = ctx.queryParams().get("endDate");
        
        // 관리자 API 요청 로깅
        Long adminId = AuthUtils.getUserIdOf(ctx.user());
        log.info("Admin dashboard request - adminId: {}, dateRange: {}, startDate: {}, endDate: {}", 
            adminId, dateRange, startDate, endDate);
        
        response(ctx, dashboardService.getDashboardStats(dateRange, startDate, endDate));
    }
}

