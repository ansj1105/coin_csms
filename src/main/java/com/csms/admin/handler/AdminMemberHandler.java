package com.csms.admin.handler;

import com.csms.admin.dto.MemberDetailDto;
import com.csms.admin.dto.MemberListDto;
import com.csms.admin.dto.SanctionRequestDto;
import com.csms.admin.service.AdminMemberService;
import com.csms.admin.service.AdminMemberExportService;
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
    
    public AdminMemberHandler(Vertx vertx, AdminMemberService service, AdminMemberExportService exportService) {
        super(vertx);
        this.service = service;
        this.exportService = exportService;
    }
    
    public Router getRouter() {
        Router router = Router.router(vertx);
        
        router.get("/").handler(this::getMembers);
        router.get("/:id").handler(this::getMemberDetail);
        router.patch("/:id/sanction").handler(this::updateSanctionStatus);
        router.get("/export").handler(this::exportMembers);
        
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
    
    private Integer getQueryParamAsInteger(RoutingContext ctx, String param, Integer defaultValue) {
        String value = ctx.queryParams().get(param);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

