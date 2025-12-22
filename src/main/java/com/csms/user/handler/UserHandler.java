package com.csms.user.handler;

import com.csms.common.handler.BaseHandler;
import com.csms.common.utils.AuthUtils;
import com.csms.common.utils.JsonUtils;
import com.csms.common.enums.UserRole;
import com.csms.user.dto.CreateUserDto;
import com.csms.user.dto.LoginDto;
import com.csms.user.service.UserService;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.json.schema.SchemaParser;
import lombok.extern.slf4j.Slf4j;

import static io.vertx.ext.web.validation.builder.Bodies.json;
import static io.vertx.json.schema.common.dsl.Keywords.maxLength;
import static io.vertx.json.schema.common.dsl.Keywords.minLength;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;

@Slf4j
public class UserHandler extends BaseHandler {
    
    private final UserService userService;
    private final JWTAuth jwtAuth;
    
    public UserHandler(Vertx vertx, UserService userService, JWTAuth jwtAuth) {
        super(vertx);
        this.userService = userService;
        this.jwtAuth = jwtAuth;
    }
    
    @Override
    public Router getRouter() {
        Router router = Router.router(getVertx());
        
        SchemaParser parser = createSchemaParser();
        
        // 공개 API (인증 불필요)
        router.post("/register")
            .handler(registerValidation(parser))
            .handler(this::register);
        
        router.post("/login")
            .handler(loginValidation(parser))
            .handler(this::login);
        
        // 인증 필요 API
        router.get("/:id")
            .handler(JWTAuthHandler.create(jwtAuth))
            .handler(AuthUtils.hasRole(UserRole.USER, UserRole.ADMIN))
            .handler(this::getUser);
        
        return router;
    }
    
    private ValidationHandler registerValidation(SchemaParser parser) {
        return ValidationHandler.builder(parser)
            .body(json(
                objectSchema()
                    .requiredProperty("loginId", stringSchema().with(minLength(3), maxLength(50)))
                    .requiredProperty("password", stringSchema().with(minLength(8), maxLength(20)))
                    .allowAdditionalProperties(false)
            ))
            .build();
    }
    
    private ValidationHandler loginValidation(SchemaParser parser) {
        return ValidationHandler.builder(parser)
            .body(json(
                objectSchema()
                    .requiredProperty("loginId", stringSchema().with(minLength(3), maxLength(50)))
                    .requiredProperty("password", stringSchema().with(minLength(8), maxLength(20)))
                    .allowAdditionalProperties(false)
            ))
            .build();
    }
    
    private void register(RoutingContext ctx) {
        CreateUserDto dto = getObjectMapper().convertValue(
            JsonUtils.getMapFromJsonObject(ctx.getBodyAsJson()),
            CreateUserDto.class
        );
        
        response(ctx, userService.createUser(dto));
    }
    
    private void login(RoutingContext ctx) {
        LoginDto dto = getObjectMapper().convertValue(
            JsonUtils.getMapFromJsonObject(ctx.getBodyAsJson()),
            LoginDto.class
        );
        
        response(ctx, userService.login(dto));
    }
    
    private void getUser(RoutingContext ctx) {
        String idParam = ctx.pathParam("id");
        if (idParam == null || idParam.isEmpty() || !idParam.matches("^\\d+$")) {
            ctx.fail(404, new com.csms.common.exceptions.NotFoundException("사용자를 찾을 수 없습니다."));
            return;
        }
        
        try {
            Long id = Long.valueOf(idParam);
            response(ctx, userService.getUserById(id));
        } catch (NumberFormatException e) {
            ctx.fail(404, new com.csms.common.exceptions.NotFoundException("사용자를 찾을 수 없습니다."));
        }
    }
}

