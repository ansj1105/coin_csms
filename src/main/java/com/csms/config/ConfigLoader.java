package com.csms.config;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigLoader {
    
    private static final String DEFAULT_CONFIG_PATH = "config.json";
    private static final String DEV_CONFIG_PATH = "src/main/resources/config.json";
    
    public static Future<JsonObject> load(Vertx vertx) {
        String configPath = System.getenv("CONFIG_PATH");
        if (configPath == null || configPath.isEmpty()) {
            configPath = DEFAULT_CONFIG_PATH;
        }
        
        final String finalConfigPath = configPath;
        log.info("Loading config from: {}", finalConfigPath);
        
        return vertx.fileSystem()
            .readFile(finalConfigPath)
            .recover(err -> {
                log.warn("Config not found at {}, trying dev path: {}", finalConfigPath, DEV_CONFIG_PATH);
                return vertx.fileSystem().readFile(DEV_CONFIG_PATH);
            })
            .map(buffer -> {
                JsonObject fullConfig = new JsonObject(buffer.toString());
                
                String env = System.getenv("APP_ENV");
                if (env == null || env.isEmpty()) {
                    env = System.getenv("ENV");
                }
                if (env == null || env.isEmpty()) {
                    env = fullConfig.getString("env", "local");
                }
                
                log.info("Loading config for environment: {}", env);
                
                JsonObject envConfig = fullConfig.getJsonObject(env);
                if (envConfig == null) {
                    log.warn("Environment '{}' not found in config, using 'local'", env);
                    envConfig = fullConfig.getJsonObject("local");
                }
                
                if (envConfig == null) {
                    throw new IllegalStateException("No configuration found for environment: " + env);
                }
                
                envConfig.put("env", env);
                return envConfig;
            })
            .onSuccess(config -> log.info("Config loaded successfully"))
            .onFailure(throwable -> log.error("Failed to load config", throwable));
    }
    
    public static Future<JsonObject> loadForEnv(Vertx vertx, String configPath, String env) {
        return vertx.fileSystem()
            .readFile(configPath)
            .map(buffer -> {
                JsonObject fullConfig = new JsonObject(buffer.toString());
                
                log.info("Loading config for environment: {} from {}", env, configPath);
                
                JsonObject envConfig = fullConfig.getJsonObject(env);
                if (envConfig == null) {
                    throw new IllegalArgumentException("Environment '" + env + "' not found in config at " + configPath);
                }
                
                envConfig.put("env", env);
                return envConfig;
            })
            .onSuccess(config -> log.info("Config loaded successfully for env: {}", env))
            .onFailure(throwable -> log.error("Failed to load config for env: {} from {}", env, configPath, throwable));
    }
    
    public static Future<JsonObject> loadForEnv(Vertx vertx, String env) {
        return loadForEnv(vertx, "src/main/resources/config.json", env);
    }
}

