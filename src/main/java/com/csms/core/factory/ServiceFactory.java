package com.csms.core.factory;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.pgclient.PgPool;
import io.vertx.ext.web.client.WebClient;

/**
 * 서비스 팩토리 - 의존성 주입을 위한 팩토리 패턴
 * 각 도메인별 서비스를 생성하고 의존성을 주입합니다.
 */
public interface ServiceFactory {
    
    PgPool getPool();
    
    JWTAuth getJwtAuth();
    
    WebClient getWebClient();
    
    JsonObject getConfig();
    
    JsonObject getJwtConfig();
    
    JsonObject getDatabaseConfig();
    
    JsonObject getRedisConfig();
    
    JsonObject getFrontendConfig();
}

