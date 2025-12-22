package com.csms.common.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class JsonUtils {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
    
    public static Map<String, Object> getMapFromJsonObject(JsonObject json) {
        Map<String, Object> map = new HashMap<>();
        if (json != null) {
            json.forEach(entry -> map.put(entry.getKey(), entry.getValue()));
        }
        return map;
    }
    
    public static JsonObject getQueryAsJson(io.vertx.ext.web.RoutingContext ctx) {
        io.vertx.core.MultiMap multiMap = ctx.queryParams();
        JsonObject json = new JsonObject();
        multiMap.forEach(entry -> json.put(entry.getKey(), entry.getValue()));
        return json;
    }
}

