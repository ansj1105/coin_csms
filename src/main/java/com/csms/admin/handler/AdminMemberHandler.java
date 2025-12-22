package com.csms.admin.handler;

import com.csms.admin.dto.MemberDetailDto;
import com.csms.admin.dto.MemberListDto;
import com.csms.admin.dto.SanctionRequestDto;
import com.csms.admin.service.AdminMemberService;
import com.csms.common.handler.BaseHandler;
import com.csms.common.utils.ErrorHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdminMemberHandler extends BaseHandler {
    
    private final AdminMemberService service;
    
    public AdminMemberHandler(Vertx vertx, AdminMemberService service) {
        super(vertx);
        this.service = service;
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
        // TODO: 엑셀/CSV 다운로드 구현
        // Apache POI 또는 OpenCSV 사용
        ctx.fail(501, new UnsupportedOperationException("Export feature not implemented yet"));
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

