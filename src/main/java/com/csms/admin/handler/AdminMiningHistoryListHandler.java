package com.csms.admin.handler;

import com.csms.admin.dto.MiningHistoryListDto;
import com.csms.admin.service.AdminMiningHistoryListService;
import com.csms.admin.service.AdminMiningHistoryListExportService;
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
public class AdminMiningHistoryListHandler extends BaseHandler {
    
    private final AdminMiningHistoryListService service;
    private final AdminMiningHistoryListExportService exportService;
    
    public AdminMiningHistoryListHandler(Vertx vertx, AdminMiningHistoryListService service, AdminMiningHistoryListExportService exportService) {
        super(vertx);
        this.service = service;
        this.exportService = exportService;
    }
    
    public Router getRouter() {
        Router router = Router.router(vertx);
        
        router.get("/history").handler(this::getMiningHistoryList);
        router.get("/history/export").handler(this::exportMiningHistoryList);
        
        return router;
    }
    
    private void getMiningHistoryList(RoutingContext ctx) {
        try {
            Integer limit = getQueryParamAsInteger(ctx, "limit", 20);
            Integer offset = getQueryParamAsInteger(ctx, "offset", 0);
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String sortType = ctx.queryParams().get("sortType");
            String activityStatus = ctx.queryParams().get("activityStatus");
            String sanctionStatus = ctx.queryParams().get("sanctionStatus");
            
            service.getMiningHistoryList(limit, offset, searchCategory, searchKeyword, sortType, activityStatus, sanctionStatus)
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
    
    private void exportMiningHistoryList(RoutingContext ctx) {
        try {
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String sortType = ctx.queryParams().get("sortType");
            String activityStatus = ctx.queryParams().get("activityStatus");
            String sanctionStatus = ctx.queryParams().get("sanctionStatus");
            
            exportService.exportToExcel(searchCategory, searchKeyword, sortType, activityStatus, sanctionStatus)
                .onSuccess(buffer -> {
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    String filename = "mining_history_list_" + timestamp + ".xlsx";
                    
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
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
}

