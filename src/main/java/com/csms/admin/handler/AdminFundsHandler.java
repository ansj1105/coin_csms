package com.csms.admin.handler;

import com.csms.admin.dto.UpdateWithdrawalStatusRequestDto;
import com.csms.admin.service.AdminFundsService;
import com.csms.common.enums.UserRole;
import com.csms.common.handler.BaseHandler;
import com.csms.common.utils.AuthUtils;
import com.csms.common.utils.ErrorHandler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class AdminFundsHandler extends BaseHandler {
    
    private final AdminFundsService fundsService;
    private final JWTAuth jwtAuth;
    
    public AdminFundsHandler(Vertx vertx, AdminFundsService fundsService, JWTAuth jwtAuth) {
        super(vertx);
        this.fundsService = fundsService;
        this.jwtAuth = jwtAuth;
    }
    
    @Override
    public Router getRouter() {
        Router router = Router.router(getVertx());
        
        // 인증 및 권한 체크 미들웨어
        router.route().handler(JWTAuthHandler.create(jwtAuth));
        router.route().handler(AuthUtils.hasRole(UserRole.ADMIN, UserRole.SUPER_ADMIN));
        
        // 출금신청 관리
        router.get("/withdrawal-request").handler(this::getWithdrawalRequests);
        router.patch("/withdrawal-request/:id/status").handler(this::updateWithdrawalStatus);
        router.get("/withdrawal-request/export").handler(this::exportWithdrawalRequests);
        
        // 거래내역 관리
        router.get("/transaction-history").handler(this::getTransactionHistory);
        router.get("/transaction-history/export").handler(this::exportTransactionHistory);
        
        return router;
    }
    
    private void getWithdrawalRequests(RoutingContext ctx) {
        try {
            Integer limit = getQueryParamAsInteger(ctx, "limit", 20);
            Integer offset = getQueryParamAsInteger(ctx, "offset", 0);
            String dateRange = ctx.queryParams().get("dateRange");
            String startDate = ctx.queryParams().get("startDate");
            String endDate = ctx.queryParams().get("endDate");
            String network = ctx.queryParams().get("network");
            String currencyCode = ctx.queryParams().get("currencyCode");
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String status = ctx.queryParams().get("status");
            
            fundsService.getWithdrawalRequests(
                limit, offset, dateRange, startDate, endDate,
                network, currencyCode, searchCategory, searchKeyword, status
            )
            .onSuccess(result -> {
                success(ctx, result);
            })
            .onFailure(err -> {
                log.error("Failed to get withdrawal requests", err);
                ErrorHandler.handle(ctx);
            });
        } catch (Exception e) {
            log.error("Error in getWithdrawalRequests", e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void updateWithdrawalStatus(RoutingContext ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            
            JsonObject body = ctx.body().asJsonObject();
            UpdateWithdrawalStatusRequestDto request = body.mapTo(UpdateWithdrawalStatusRequestDto.class);
            
            fundsService.updateWithdrawalStatus(id, request.getStatus())
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("status", "OK"));
                })
                .onFailure(err -> {
                    log.error("Failed to update withdrawal status", err);
                    ErrorHandler.handle(ctx);
                });
        } catch (NumberFormatException e) {
            ctx.fail(400, new IllegalArgumentException("Invalid withdrawal request ID"));
        } catch (Exception e) {
            log.error("Error in updateWithdrawalStatus", e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void exportWithdrawalRequests(RoutingContext ctx) {
        try {
            String dateRange = ctx.queryParams().get("dateRange");
            String startDate = ctx.queryParams().get("startDate");
            String endDate = ctx.queryParams().get("endDate");
            String network = ctx.queryParams().get("network");
            String currencyCode = ctx.queryParams().get("currencyCode");
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String status = ctx.queryParams().get("status");
            
            fundsService.exportWithdrawalRequests(
                dateRange, startDate, endDate,
                network, currencyCode, searchCategory, searchKeyword, status
            )
            .onSuccess(buffer -> {
                String fileName = "withdrawal_requests_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
                
                ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .putHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .putHeader("Content-Length", String.valueOf(buffer.length()))
                    .end(buffer);
            })
            .onFailure(err -> {
                log.error("Failed to export withdrawal requests", err);
                ErrorHandler.handle(ctx);
            });
        } catch (Exception e) {
            log.error("Error in exportWithdrawalRequests", e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void getTransactionHistory(RoutingContext ctx) {
        try {
            Integer limit = getQueryParamAsInteger(ctx, "limit", 20);
            Integer offset = getQueryParamAsInteger(ctx, "offset", 0);
            String dateRange = ctx.queryParams().get("dateRange");
            String startDate = ctx.queryParams().get("startDate");
            String endDate = ctx.queryParams().get("endDate");
            String transactionType = ctx.queryParams().get("transactionType");
            String currencyCode = ctx.queryParams().get("currencyCode");
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String status = ctx.queryParams().get("status");
            
            fundsService.getTransactionHistory(
                limit, offset, dateRange, startDate, endDate,
                transactionType, currencyCode, searchCategory, searchKeyword, status
            )
            .onSuccess(result -> {
                success(ctx, result);
            })
            .onFailure(err -> {
                log.error("Failed to get transaction history", err);
                ErrorHandler.handle(ctx);
            });
        } catch (Exception e) {
            log.error("Error in getTransactionHistory", e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void exportTransactionHistory(RoutingContext ctx) {
        try {
            String dateRange = ctx.queryParams().get("dateRange");
            String startDate = ctx.queryParams().get("startDate");
            String endDate = ctx.queryParams().get("endDate");
            String transactionType = ctx.queryParams().get("transactionType");
            String currencyCode = ctx.queryParams().get("currencyCode");
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String status = ctx.queryParams().get("status");
            
            fundsService.exportTransactionHistory(
                dateRange, startDate, endDate,
                transactionType, currencyCode, searchCategory, searchKeyword, status
            )
            .onSuccess(buffer -> {
                String fileName = "transaction_history_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
                
                ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .putHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .putHeader("Content-Length", String.valueOf(buffer.length()))
                    .end(buffer);
            })
            .onFailure(err -> {
                log.error("Failed to export transaction history", err);
                ErrorHandler.handle(ctx);
            });
        } catch (Exception e) {
            log.error("Error in exportTransactionHistory", e);
            ErrorHandler.handle(ctx);
        }
    }
}

