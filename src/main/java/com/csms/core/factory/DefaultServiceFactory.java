package com.csms.core.factory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Getter
public class DefaultServiceFactory implements ServiceFactory {
    
    private final Vertx vertx;
    private final JsonObject config;
    private final PgPool pool;
    private final JWTAuth jwtAuth;
    private final WebClient webClient;
    
    public static DefaultServiceFactory create(Vertx vertx, JsonObject config) {
        JsonObject databaseConfig = config.getJsonObject("database", new JsonObject());
        JsonObject jwtConfig = config.getJsonObject("jwt", new JsonObject());
        
        PgPool pool = createPgPool(vertx, databaseConfig);
        JWTAuth jwtAuth = createJwtAuth(vertx, jwtConfig);
        WebClient webClient = WebClient.create(vertx);
        
        return new DefaultServiceFactory(vertx, config, pool, jwtAuth, webClient);
    }
    
    private static PgPool createPgPool(Vertx vertx, JsonObject config) {
        PgConnectOptions connectOptions = new PgConnectOptions()
            .setHost(config.getString("host"))
            .setPort(config.getInteger("port"))
            .setDatabase(config.getString("database"))
            .setUser(config.getString("user"))
            .setPassword(config.getString("password"));
        
        PoolOptions poolOptions = new PoolOptions()
            .setMaxSize(config.getInteger("pool_size", 10))
            .setIdleTimeout(config.getInteger("idle_timeout", 60))
            .setPoolCleanerPeriod(config.getInteger("pool_cleaner_period", 60));
        
        return PgPool.pool(vertx, connectOptions, poolOptions);
    }
    
    private static JWTAuth createJwtAuth(Vertx vertx, JsonObject config) {
        String secret = config.getString("secret");
        
        return JWTAuth.create(vertx, new JWTAuthOptions()
            .addPubSecKey(new PubSecKeyOptions()
                .setAlgorithm("HS256")
                .setBuffer(secret)));
    }
    
    @Override
    public JsonObject getJwtConfig() {
        return config.getJsonObject("jwt", new JsonObject());
    }
    
    @Override
    public JsonObject getDatabaseConfig() {
        return config.getJsonObject("database", new JsonObject());
    }
    
    @Override
    public JsonObject getRedisConfig() {
        return config.getJsonObject("redis", new JsonObject());
    }
    
    @Override
    public JsonObject getFrontendConfig() {
        return config.getJsonObject("frontend", new JsonObject());
    }
}

