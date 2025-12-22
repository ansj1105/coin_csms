package com.csms.admin.handler;

import com.csms.admin.dto.MiningHistoryDetailDto;
import com.csms.admin.service.AdminMiningHistoryService;
import com.csms.admin.service.AdminMiningHistoryExportService;
import com.csms.common.handler.BaseHandler;
import com.csms.common.utils.ErrorHandler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class AdminMiningHistoryHandler extends BaseHandler {
    
    private final AdminMiningHistoryService service;
    private final AdminMiningHistoryExportService exportService;
    
    public AdminMiningHistoryHandler(Vertx vertx, AdminMiningHistoryService service, AdminMiningHistoryExportService exportService) {
        super(vertx);
        this.service = service;
        this.exportService = exportService;
    }
    
    public Router getRouter() {
        Router router = Router.router(vertx);
        
        router.get("/:userId/mining-history").handler(this::getMiningHistory);
        router.get("/:userId/mining-history/export").handler(this::exportMiningHistory);
        
        return router;
    }
    
    private void getMiningHistory(RoutingContext ctx) {
        try {
            Long userId = Long.parseLong(ctx.pathParam("userId"));
            Integer limit = getQueryParamAsInteger(ctx, "limit", 20);
            Integer offset = getQueryParamAsInteger(ctx, "offset", 0);
            String dateRange = ctx.queryParams().get("dateRange");
            
            service.getMiningHistory(userId, limit, offset, dateRange)
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
            
            exportService.exportToExcel(userId, dateRange)
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

