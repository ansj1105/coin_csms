package com.csms.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.RedisReplicas;
import lombok.extern.slf4j.Slf4j;

/**
 * 이벤트 처리 Verticle
 * Redis Pub/Sub를 통한 이벤트 기반 아키텍처 지원
 * 
 * Redis 구성 모드:
 * - standalone: 단일 Redis 인스턴스 (기본값)
 * - cluster: Redis Cluster (3+ Master 노드)
 * - sentinel: Redis Sentinel (Master-Slave + Failover)
 */
@Slf4j
public class EventVerticle extends AbstractVerticle {
    
    private Redis redisClient;
    private Redis subscriberClient;
    private RedisAPI redisApi;
    
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        log.info("Starting EventVerticle...");
        
        JsonObject redisConfig = config().getJsonObject("redis", new JsonObject());
        String mode = redisConfig.getString("mode", "standalone");
        
        // Redis 클라이언트 옵션 생성
        RedisOptions options = createRedisOptions(redisConfig, mode);
        
        redisClient = Redis.createClient(vertx, options);
        
        // Pub/Sub 전용 클라이언트 (Cluster 모드에서는 별도 연결 필요)
        RedisOptions subscriberOptions = createRedisOptions(redisConfig, mode);
        subscriberClient = Redis.createClient(vertx, subscriberOptions);
        
        redisClient.connect()
            .map(conn -> {
                log.info("Redis connected successfully (mode: {})", mode);
                redisApi = RedisAPI.api(conn);
                
                // 이벤트 구독 시작
                subscribeToEvents();
                
                return (Void) null;
            })
            .recover(err -> {
                log.warn("Failed to connect to Redis for EventVerticle, continuing without event pub/sub", err);
                // Redis 연결 실패해도 서버는 시작 (이벤트 기능만 비활성화)
                return Future.<Void>succeededFuture();
            })
            .onComplete(result -> {
                if (result.succeeded()) {
                    startPromise.complete();
                } else {
                    startPromise.fail(result.cause());
                }
            });
    }
    
    /**
     * Redis 모드에 따른 옵션 생성
     */
    private RedisOptions createRedisOptions(JsonObject redisConfig, String mode) {
        RedisOptions options = new RedisOptions();
        
        String password = redisConfig.getString("password");
        if (password != null && !password.isEmpty()) {
            options.setPassword(password);
        }
        
        switch (mode) {
            case "cluster" -> {
                options.setType(RedisClientType.CLUSTER);
                options.setUseReplicas(RedisReplicas.SHARE);
                
                JsonArray nodes = redisConfig.getJsonArray("nodes", new JsonArray());
                if (nodes.isEmpty()) {
                    options.addConnectionString("redis://localhost:7001");
                    options.addConnectionString("redis://localhost:7002");
                    options.addConnectionString("redis://localhost:7003");
                } else {
                    for (int i = 0; i < nodes.size(); i++) {
                        options.addConnectionString(nodes.getString(i));
                    }
                }
                log.info("Redis Cluster mode configured with {} nodes", 
                    nodes.isEmpty() ? 3 : nodes.size());
            }
            
            case "sentinel" -> {
                options.setType(RedisClientType.SENTINEL);
                options.setMasterName(redisConfig.getString("masterName", "mymaster"));
                options.setRole(io.vertx.redis.client.RedisRole.MASTER);
                
                JsonArray sentinels = redisConfig.getJsonArray("sentinels", new JsonArray());
                if (sentinels.isEmpty()) {
                    options.addConnectionString("redis://localhost:26379");
                } else {
                    for (int i = 0; i < sentinels.size(); i++) {
                        options.addConnectionString(sentinels.getString(i));
                    }
                }
                log.info("Redis Sentinel mode configured with master: {}", 
                    redisConfig.getString("masterName", "mymaster"));
            }
            
            default -> {
                options.setType(RedisClientType.STANDALONE);
                String host = redisConfig.getString("host", "localhost");
                int port = redisConfig.getInteger("port", 6379);
                options.setConnectionString("redis://" + host + ":" + port);
                log.info("Redis Standalone mode configured: {}:{}", host, port);
            }
        }
        
        options.setMaxPoolSize(redisConfig.getInteger("maxPoolSize", 8));
        options.setMaxPoolWaiting(redisConfig.getInteger("maxPoolWaiting", 32));
        options.setPoolRecycleTimeout(redisConfig.getInteger("poolRecycleTimeout", 15000));
        
        return options;
    }
    
    /**
     * 이벤트 구독
     */
    private void subscribeToEvents() {
        // TODO: 이벤트 구독 로직 구현
        log.info("Event subscriptions initialized");
    }
    
    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        log.info("Stopping EventVerticle...");
        
        if (redisClient != null) {
            redisClient.close();
        }
        if (subscriberClient != null) {
            subscriberClient.close();
        }
        
        stopPromise.complete();
    }
}

