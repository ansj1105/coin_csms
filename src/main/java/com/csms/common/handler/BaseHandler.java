package com.csms.common.handler;

import com.csms.common.dto.ApiResponse;
import com.csms.common.utils.JsonUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseHandler {
    
    private static final String DEFAULT_SUCCESS_MESSAGE = "요청이 완료되었습니다.";
    private static final String DEFAULT_FAIL_MESSAGE = "요청이 실패했습니다.";
    private static final String JSON_CONTENT_TYPE = "application/json";
    
    protected final Vertx vertx;
    
    public Vertx getVertx() {
        return vertx;
    }
    
    protected SchemaParser createSchemaParser() {
        return SchemaParser.createDraft7SchemaParser(
            SchemaRouter.create(getVertx(), new SchemaRouterOptions())
        );
    }
    
    public abstract Router getRouter();
    
    /**
     * Query Parameters를 JsonObject로 변환 (참고 프로젝트 패턴)
     */
    public static JsonObject getQueryAsJson(RoutingContext ctx) {
        MultiMap multiMap = ctx.queryParams();
        JsonObject json = new JsonObject();
        multiMap.forEach(entry -> json.put(entry.getKey(), entry.getValue()));
        return json;
    }
    
    /**
     * ObjectMapper를 static으로 제공 (참고 프로젝트 패턴)
     */
    public static ObjectMapper getObjectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    /**
     * ObjectMapper를 인스턴스 메서드로도 제공 (하위 호환성)
     */
    protected ObjectMapper getObjectMapperInstance() {
        return getObjectMapper();
    }
    
    protected <T> void response(RoutingContext ctx, Future<T> future) {
        response(ctx, future, result -> result);
    }
    
    protected <T, R> void response(RoutingContext ctx, Future<T> future, Function<T, R> mapper) {
        future
            .onSuccess(result -> {
                try {
                    Object obj = mapper.apply(result);
                    success(ctx, obj);
                } catch (Exception e) {
                    log.error("Error mapping response", e);
                    ctx.fail(e);
                }
            })
            .onFailure(ctx::fail);
    }
    
    protected void success(RoutingContext ctx, Object data) {
        ApiResponse<?> response = ApiResponse.success(data);
        
        ctx.response()
            .setChunked(true)
            .setStatusCode(HttpResponseStatus.OK.code())
            .putHeader(HttpHeaders.CONTENT_TYPE, JSON_CONTENT_TYPE)
            .end(Json.encode(response));
    }
    
    protected void success(RoutingContext ctx, String message, Object data) {
        ApiResponse<?> response = ApiResponse.success(message, data);
        
        ctx.response()
            .setChunked(true)
            .setStatusCode(HttpResponseStatus.OK.code())
            .putHeader(HttpHeaders.CONTENT_TYPE, JSON_CONTENT_TYPE)
            .end(Json.encode(response));
    }
    
    /**
     * 실패 응답 (참고 프로젝트 패턴: Object를 받도록 개선)
     */
    protected void fail(RoutingContext ctx, Object data) {
        String message = DEFAULT_FAIL_MESSAGE;
        if (data instanceof String) {
            message = (String) data;
            data = null;
        }
        
        ApiResponse<?> response = ApiResponse.<Object>builder()
            .status("FAIL")
            .message(message)
            .data(data)
            .build();
        
        ctx.response()
            .setChunked(true)
            .setStatusCode(HttpResponseStatus.OK.code())
            .putHeader(HttpHeaders.CONTENT_TYPE, JSON_CONTENT_TYPE)
            .end(Json.encode(response));
    }
    
    /**
     * String 메시지를 받는 fail 메서드 (하위 호환성 유지)
     */
    protected void fail(RoutingContext ctx, String message) {
        fail(ctx, (Object) message);
    }
    
    protected Integer getQueryParamAsInteger(RoutingContext ctx, String param, Integer defaultValue) {
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

