package com.csms.admin.handler;

import com.csms.admin.service.AdminReferralService;
import com.csms.common.enums.UserRole;
import com.csms.common.handler.BaseHandler;
import com.csms.common.utils.AuthUtils;
import com.csms.common.utils.ErrorHandler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class AdminReferralHandler extends BaseHandler {
    
    private final AdminReferralService referralService;
    private final JWTAuth jwtAuth;
    
    public AdminReferralHandler(Vertx vertx, AdminReferralService referralService, JWTAuth jwtAuth) {
        super(vertx);
        this.referralService = referralService;
        this.jwtAuth = jwtAuth;
    }
    
    @Override
    public Router getRouter() {
        Router router = Router.router(getVertx());
        
        // 인증 및 권한 체크 미들웨어
        router.route().handler(JWTAuthHandler.create(jwtAuth));
        router.route().handler(AuthUtils.hasRole(UserRole.ADMIN, UserRole.SUPER_ADMIN));
        
        // 래퍼럴 거래내역 관리
        router.get("/transaction-history").handler(this::getReferralTransactionHistory);
        router.get("/transaction-history/export").handler(this::exportReferralTransactionHistory);
        
        // 래퍼럴 트리구조 관리
        router.get("/tree").handler(this::getReferralTree);
        router.get("/tree/export").handler(this::exportReferralTree);
        
        return router;
    }
    
    private void getReferralTransactionHistory(RoutingContext ctx) {
        try {
            Integer limit = getQueryParamAsInteger(ctx, "limit", 20);
            Integer offset = getQueryParamAsInteger(ctx, "offset", 0);
            String dateRange = ctx.queryParams().get("dateRange");
            String startDate = ctx.queryParams().get("startDate");
            String endDate = ctx.queryParams().get("endDate");
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String sortType = ctx.queryParams().get("sortType");
            
            referralService.getReferralTransactionHistory(
                limit, offset, dateRange, startDate, endDate,
                searchCategory, searchKeyword, sortType
            )
            .onSuccess(result -> {
                success(ctx, result);
            })
            .onFailure(err -> {
                log.error("Failed to get referral transaction history", err);
                ErrorHandler.handle(ctx);
            });
        } catch (Exception e) {
            log.error("Error in getReferralTransactionHistory", e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void exportReferralTransactionHistory(RoutingContext ctx) {
        try {
            String dateRange = ctx.queryParams().get("dateRange");
            String startDate = ctx.queryParams().get("startDate");
            String endDate = ctx.queryParams().get("endDate");
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String sortType = ctx.queryParams().get("sortType");
            
            referralService.exportReferralTransactionHistory(
                dateRange, startDate, endDate,
                searchCategory, searchKeyword, sortType
            )
            .onSuccess(buffer -> {
                String fileName = "referral_transaction_history_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
                
                ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .putHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .putHeader("Content-Length", String.valueOf(buffer.length()))
                    .end(buffer);
            })
            .onFailure(err -> {
                log.error("Failed to export referral transaction history", err);
                ErrorHandler.handle(ctx);
            });
        } catch (Exception e) {
            log.error("Error in exportReferralTransactionHistory", e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void getReferralTree(RoutingContext ctx) {
        try {
            Integer limit = getQueryParamAsInteger(ctx, "limit", 20);
            Integer offset = getQueryParamAsInteger(ctx, "offset", 0);
            String dateRange = ctx.queryParams().get("dateRange");
            String startDate = ctx.queryParams().get("startDate");
            String endDate = ctx.queryParams().get("endDate");
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String activityStatus = ctx.queryParams().get("activityStatus");
            String sortType = ctx.queryParams().get("sortType");
            
            referralService.getReferralTree(
                limit, offset, dateRange, startDate, endDate,
                searchCategory, searchKeyword, activityStatus, sortType
            )
            .onSuccess(result -> {
                success(ctx, result);
            })
            .onFailure(err -> {
                log.error("Failed to get referral tree", err);
                ErrorHandler.handle(ctx);
            });
        } catch (Exception e) {
            log.error("Error in getReferralTree", e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void exportReferralTree(RoutingContext ctx) {
        try {
            String dateRange = ctx.queryParams().get("dateRange");
            String startDate = ctx.queryParams().get("startDate");
            String endDate = ctx.queryParams().get("endDate");
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String activityStatus = ctx.queryParams().get("activityStatus");
            String sortType = ctx.queryParams().get("sortType");
            
            referralService.exportReferralTree(
                dateRange, startDate, endDate,
                searchCategory, searchKeyword, activityStatus, sortType
            )
            .onSuccess(buffer -> {
                String fileName = "referral_tree_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
                
                ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .putHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .putHeader("Content-Length", String.valueOf(buffer.length()))
                    .end(buffer);
            })
            .onFailure(err -> {
                log.error("Failed to export referral tree", err);
                ErrorHandler.handle(ctx);
            });
        } catch (Exception e) {
            log.error("Error in exportReferralTree", e);
            ErrorHandler.handle(ctx);
        }
    }
}

