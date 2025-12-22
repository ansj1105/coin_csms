package com.csms.admin.handler;

import com.csms.admin.dto.*;
import com.csms.admin.service.*;
import com.csms.common.handler.BaseHandler;
import com.csms.common.utils.ErrorHandler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class AdminMemberHandler extends BaseHandler {
    
    private final AdminMemberService service;
    private final AdminMemberExportService exportService;
    private final AdminMiningHistoryService miningHistoryService;
    private final AdminMiningHistoryExportService miningHistoryExportService;
    
    public AdminMemberHandler(
        Vertx vertx,
        AdminMemberService service,
        AdminMemberExportService exportService,
        AdminMiningHistoryService miningHistoryService,
        AdminMiningHistoryExportService miningHistoryExportService
    ) {
        super(vertx);
        this.service = service;
        this.exportService = exportService;
        this.miningHistoryService = miningHistoryService;
        this.miningHistoryExportService = miningHistoryExportService;
    }
    
    public Router getRouter() {
        Router router = Router.router(vertx);
        
        router.get("/").handler(this::getMembers);
        router.get("/:id").handler(this::getMemberDetail);
        router.patch("/:id").handler(this::updateMember);
        router.patch("/:id/sanction").handler(this::updateSanctionStatus);
        router.post("/:id/reset-transaction-password").handler(this::resetTransactionPassword);
        router.post("/coins/adjust").handler(this::adjustCoin);
        router.post("/kori-points/adjust").handler(this::adjustKoriPoint);
        router.get("/:id/wallets").handler(this::getMemberWallets);
        router.get("/export").handler(this::exportMembers);
        
        // Mining History (회원별 채굴 내역)
        router.get("/:userId/mining-history").handler(this::getMiningHistory);
        router.get("/:userId/mining-history/export").handler(this::exportMiningHistory);
        
        return router;
    }
    
    private void getMembers(RoutingContext ctx) {
        try {
            Integer limit = getQueryParamAsInteger(ctx, "limit", 20);
            Integer offset = getQueryParamAsInteger(ctx, "offset", 0);
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String activityStatus = ctx.queryParams().get("activityStatus");
            String sanctionStatus = ctx.queryParams().get("sanctionStatus");
            
            service.getMembers(limit, offset, searchCategory, searchKeyword, activityStatus, sanctionStatus)
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
    
    private void getMemberDetail(RoutingContext ctx) {
        try {
            Long memberId = Long.parseLong(ctx.pathParam("id"));
            
            service.getMemberDetail(memberId)
                .onSuccess(result -> {
                    success(ctx, result);
                })
                .onFailure(err -> {
                    ErrorHandler.handle(ctx);
                });
        } catch (NumberFormatException e) {
            ctx.fail(400, new IllegalArgumentException("Invalid member ID"));
        } catch (Exception e) {
            ErrorHandler.handle(ctx);
        }
    }
    
    private void updateMember(RoutingContext ctx) {
        try {
            Long memberId = Long.parseLong(ctx.pathParam("id"));
            
            JsonObject body = ctx.body().asJsonObject();
            UpdateMemberRequestDto request = body.mapTo(UpdateMemberRequestDto.class);
            
            service.updateMember(memberId, request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Member updated"));
                })
                .onFailure(err -> {
                    ErrorHandler.handle(ctx);
                });
        } catch (NumberFormatException e) {
            ctx.fail(400, new IllegalArgumentException("Invalid member ID"));
        } catch (Exception e) {
            ErrorHandler.handle(ctx);
        }
    }
    
    private void updateSanctionStatus(RoutingContext ctx) {
        try {
            Long memberId = Long.parseLong(ctx.pathParam("id"));
            
            JsonObject body = ctx.body().asJsonObject();
            SanctionRequestDto request = body.mapTo(SanctionRequestDto.class);
            
            service.updateSanctionStatus(memberId, request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Sanction status updated"));
                })
                .onFailure(err -> {
                    ErrorHandler.handle(ctx);
                });
        } catch (NumberFormatException e) {
            ctx.fail(400, new IllegalArgumentException("Invalid member ID"));
        } catch (Exception e) {
            ErrorHandler.handle(ctx);
        }
    }
    
    private void resetTransactionPassword(RoutingContext ctx) {
        try {
            Long memberId = Long.parseLong(ctx.pathParam("id"));
            
            service.resetTransactionPassword(memberId)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Transaction password reset"));
                })
                .onFailure(err -> {
                    ErrorHandler.handle(ctx);
                });
        } catch (NumberFormatException e) {
            ctx.fail(400, new IllegalArgumentException("Invalid member ID"));
        } catch (Exception e) {
            ErrorHandler.handle(ctx);
        }
    }
    
    private void adjustCoin(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            CoinAdjustRequestDto request = body.mapTo(CoinAdjustRequestDto.class);
            
            service.adjustCoin(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "Coin adjusted"));
                })
                .onFailure(err -> {
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ErrorHandler.handle(ctx);
        }
    }
    
    private void adjustKoriPoint(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            KoriPointAdjustRequestDto request = body.mapTo(KoriPointAdjustRequestDto.class);
            
            service.adjustKoriPoint(request)
                .onSuccess(result -> {
                    success(ctx, new JsonObject().put("message", "KORI point adjusted"));
                })
                .onFailure(err -> {
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            ErrorHandler.handle(ctx);
        }
    }
    
    private void getMemberWallets(RoutingContext ctx) {
        try {
            Long memberId = Long.parseLong(ctx.pathParam("id"));
            String network = ctx.queryParams().get("network");
            String token = ctx.queryParams().get("token");
            Integer limit = getQueryParamAsInteger(ctx, "limit", 20);
            Integer offset = getQueryParamAsInteger(ctx, "offset", 0);
            
            service.getMemberWallets(memberId, network, token, limit, offset)
                .onSuccess(result -> {
                    success(ctx, result);
                })
                .onFailure(err -> {
                    ErrorHandler.handle(ctx);
                });
        } catch (NumberFormatException e) {
            ctx.fail(400, new IllegalArgumentException("Invalid member ID"));
        } catch (Exception e) {
            ErrorHandler.handle(ctx);
        }
    }
    
    private void exportMembers(RoutingContext ctx) {
        try {
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String activityStatus = ctx.queryParams().get("activityStatus");
            String sanctionStatus = ctx.queryParams().get("sanctionStatus");
            
            exportService.exportToExcel(searchCategory, searchKeyword, activityStatus, sanctionStatus)
                .onSuccess(buffer -> {
                    // 파일명 생성 (현재 날짜시간 포함)
                    String fileName = "members_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
                    
                    ctx.response()
                        .setStatusCode(200)
                        .putHeader("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .putHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                        .putHeader("Content-Length", String.valueOf(buffer.length()))
                        .end(buffer);
                })
                .onFailure(err -> {
                    log.error("Failed to export members", err);
                    ErrorHandler.handle(ctx);
                });
        } catch (Exception e) {
            log.error("Error in exportMembers", e);
            ErrorHandler.handle(ctx);
        }
    }
    
    // Mining History (회원별 채굴 내역)
    private void getMiningHistory(RoutingContext ctx) {
        try {
            Long userId = Long.parseLong(ctx.pathParam("userId"));
            Integer limit = getQueryParamAsInteger(ctx, "limit", 20);
            Integer offset = getQueryParamAsInteger(ctx, "offset", 0);
            String dateRange = ctx.queryParams().get("dateRange");
            
            miningHistoryService.getMiningHistory(userId, limit, offset, dateRange)
                .onSuccess(result -> {
                    success(ctx, result);
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (NumberFormatException e) {
            ctx.fail(400, new IllegalArgumentException("Invalid user ID"));
            ErrorHandler.handle(ctx);
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void exportMiningHistory(RoutingContext ctx) {
        try {
            Long userId = Long.parseLong(ctx.pathParam("userId"));
            String dateRange = ctx.queryParams().get("dateRange");
            
            miningHistoryExportService.exportToExcel(userId, dateRange)
                .onSuccess(buffer -> {
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    String filename = "mining_history_" + userId + "_" + timestamp + ".xlsx";
                    
                    ctx.response()
                        .putHeader("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .putHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                        .putHeader("Content-Length", String.valueOf(buffer.length()))
                        .end(buffer);
                })
                .onFailure(throwable -> {
                    ctx.fail(throwable);
                    ErrorHandler.handle(ctx);
                });
        } catch (NumberFormatException e) {
            ctx.fail(400, new IllegalArgumentException("Invalid user ID"));
            ErrorHandler.handle(ctx);
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
}

