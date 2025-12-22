package com.csms.admin.handler;

import com.csms.admin.dto.AdminLoginDto;
import com.csms.admin.service.AdminAuthService;
import com.csms.common.handler.BaseHandler;
import com.csms.common.utils.JsonUtils;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.json.schema.SchemaParser;
import lombok.extern.slf4j.Slf4j;

import static io.vertx.ext.web.validation.builder.Bodies.json;
import static io.vertx.json.schema.common.dsl.Keywords.maxLength;
import static io.vertx.json.schema.common.dsl.Keywords.minLength;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;

@Slf4j
public class AdminAuthHandler extends BaseHandler {
    
    private final AdminAuthService adminAuthService;
    
    public AdminAuthHandler(Vertx vertx, AdminAuthService adminAuthService) {
        super(vertx);
        this.adminAuthService = adminAuthService;
    }
    
    @Override
    public Router getRouter() {
        Router router = Router.router(getVertx());
        
        SchemaParser parser = createSchemaParser();
        
        router.post("/login")
            .handler(loginValidation(parser))
            .handler(this::login);
        
        return router;
    }
    
    private ValidationHandler loginValidation(SchemaParser parser) {
        return ValidationHandler.builder(parser)
            .body(json(
                objectSchema()
                    .requiredProperty("id", stringSchema().with(minLength(1), maxLength(50)))
                    .requiredProperty("password", stringSchema().with(minLength(1), maxLength(100)))
                    .allowAdditionalProperties(false)
            ))
            .build();
    }
    
    private void login(RoutingContext ctx) {
        AdminLoginDto dto = getObjectMapper().convertValue(
            JsonUtils.getMapFromJsonObject(ctx.getBodyAsJson()),
            AdminLoginDto.class
        );
        
        // 클라이언트 IP 추출
        String clientIp = getClientIp(ctx);
        
        response(ctx, adminAuthService.login(dto, clientIp));
    }
    
    private String getClientIp(RoutingContext ctx) {
        // X-Forwarded-For 헤더 확인 (프록시 환경)
        String forwardedFor = ctx.request().getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        
        // X-Real-IP 헤더 확인
        String realIp = ctx.request().getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        
        // 직접 연결 IP
        return ctx.request().remoteAddress().host();
    }
}

