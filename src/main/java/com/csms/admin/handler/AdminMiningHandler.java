package com.csms.admin.handler;

import com.csms.admin.dto.MiningRecordListDto;
import com.csms.admin.service.AdminMiningService;
import com.csms.admin.service.AdminMiningExportService;
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
public class AdminMiningHandler extends BaseHandler {
    
    private final AdminMiningService service;
    private final AdminMiningExportService exportService;
    
    public AdminMiningHandler(Vertx vertx, AdminMiningService service, AdminMiningExportService exportService) {
        super(vertx);
        this.service = service;
        this.exportService = exportService;
    }
    
    public Router getRouter() {
        Router router = Router.router(vertx);
        
        router.get("/records").handler(this::getMiningRecords);
        router.get("/records/export").handler(this::exportMiningRecords);
        
        return router;
    }
    
    private void getMiningRecords(RoutingContext ctx) {
        try {
            Integer limit = getQueryParamAsInteger(ctx, "limit", 20);
            Integer offset = getQueryParamAsInteger(ctx, "offset", 0);
            String dateRange = ctx.queryParams().get("dateRange");
            String startDate = ctx.queryParams().get("startDate");
            String endDate = ctx.queryParams().get("endDate");
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String activityStatus = ctx.queryParams().get("activityStatus");
            
            service.getMiningRecords(
                limit,
                offset,
                dateRange,
                startDate,
                endDate,
                searchCategory,
                searchKeyword,
                activityStatus
            ).onSuccess(result -> {
                success(ctx, result);
            }).onFailure(throwable -> {
                ctx.fail(throwable);
                ErrorHandler.handle(ctx);
            });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
    
    private void exportMiningRecords(RoutingContext ctx) {
        try {
            String dateRange = ctx.queryParams().get("dateRange");
            String startDate = ctx.queryParams().get("startDate");
            String endDate = ctx.queryParams().get("endDate");
            String searchCategory = ctx.queryParams().get("searchCategory");
            String searchKeyword = ctx.queryParams().get("searchKeyword");
            String activityStatus = ctx.queryParams().get("activityStatus");
            
            exportService.exportToExcel(
                dateRange,
                startDate,
                endDate,
                searchCategory,
                searchKeyword,
                activityStatus
            ).onSuccess(buffer -> {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String filename = "mining_records_" + timestamp + ".xlsx";
                
                ctx.response()
                    .putHeader("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .putHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .putHeader("Content-Length", String.valueOf(buffer.length()))
                    .end(buffer);
            }).onFailure(throwable -> {
                ctx.fail(throwable);
                ErrorHandler.handle(ctx);
            });
        } catch (Exception e) {
            ctx.fail(e);
            ErrorHandler.handle(ctx);
        }
    }
}

