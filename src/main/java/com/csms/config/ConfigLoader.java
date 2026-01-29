package com.csms.config;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigLoader {
    
    // 설정 파일 경로 (환경변수로 오버라이드 가능)
    private static final String DEFAULT_CONFIG_PATH = "config.json";
    private static final String DEV_CONFIG_PATH = "src/main/resources/config.json";
    
    /**
     * 환경별 설정 로드
     * 환경 변수 ENV 또는 config.json의 env 필드로 환경 결정
     * 기본값: local
     */
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
                // 기본 경로에서 실패하면 개발 경로 시도
                log.warn("Config not found at {}, trying dev path: {}", finalConfigPath, DEV_CONFIG_PATH);
                return vertx.fileSystem().readFile(DEV_CONFIG_PATH);
            })
            .map(buffer -> {
                // BOM(Byte Order Mark) 제거
                String jsonString = buffer.toString();
                if (jsonString.startsWith("\uFEFF")) {
                    jsonString = jsonString.substring(1);
                }
                JsonObject fullConfig = new JsonObject(jsonString);
                
                String env = System.getenv("APP_ENV");
                if (env == null || env.isEmpty()) {
                    env = System.getenv("ENV");
                }
                if (env == null || env.isEmpty()) {
                    env = fullConfig.getString("env", "local");
                }
                
                log.info("Loading config for environment: {}", env);
                
                // 해당 환경의 설정 추출
                JsonObject envConfig = fullConfig.getJsonObject(env);
                if (envConfig == null) {
                    log.warn("Environment '{}' not found in config, using 'local'", env);
                    envConfig = fullConfig.getJsonObject("local");
                }
                
                // env 필드 추가
                envConfig.put("env", env);
                
                // 환경 변수로 민감한 정보 오버라이드
                overrideWithEnvVars(envConfig);
                
                return envConfig;
            })
            .onSuccess(config -> log.info("Config loaded successfully"))
            .onFailure(throwable -> log.error("Failed to load config", throwable));
    }
    
    /**
     * 테스트용: 특정 파일과 환경의 설정 로드
     */
    public static Future<JsonObject> loadForEnv(Vertx vertx, String configPath, String env) {
        return vertx.fileSystem()
            .readFile(configPath)
            .map(buffer -> {
                // BOM(Byte Order Mark) 제거
                String jsonString = buffer.toString();
                if (jsonString.startsWith("\uFEFF")) {
                    jsonString = jsonString.substring(1);
                }
                JsonObject fullConfig = new JsonObject(jsonString);
                
                log.info("Loading config for environment: {} from {}", env, configPath);
                
                JsonObject envConfig = fullConfig.getJsonObject(env);
                if (envConfig == null) {
                    throw new IllegalArgumentException("Environment '" + env + "' not found in config at " + configPath);
                }
                
                envConfig.put("env", env);
                
                // 환경 변수로 민감한 정보 오버라이드
                overrideWithEnvVars(envConfig);
                
                return envConfig;
            })
            .onSuccess(config -> log.info("Config loaded successfully for env: {}", env))
            .onFailure(throwable -> log.error("Failed to load config for env: {} from {}", env, configPath, throwable));
    }
    
    /**
     * 테스트용: 특정 환경의 설정 로드 (기본 경로)
     */
    public static Future<JsonObject> loadForEnv(Vertx vertx, String env) {
        return loadForEnv(vertx, "src/main/resources/config.json", env);
    }
    
    /**
     * 환경 변수로 민감한 정보 오버라이드
     * 환경 변수가 설정되어 있으면 config.json의 값을 덮어씁니다.
     */
    private static void overrideWithEnvVars(JsonObject config) {
        // Database 설정
        JsonObject dbConfig = config.getJsonObject("database");
        if (dbConfig != null) {
            overrideIfSet(dbConfig, "host", "DB_HOST");
            overrideIfSet(dbConfig, "port", "DB_PORT", Integer::parseInt);
            overrideIfSet(dbConfig, "database", "DB_DATABASE");
            overrideIfSet(dbConfig, "user", "DB_USER");
            overrideIfSet(dbConfig, "password", "DB_PASSWORD");
        }
        
        // JWT 설정
        JsonObject jwtConfig = config.getJsonObject("jwt");
        if (jwtConfig != null) {
            overrideIfSet(jwtConfig, "secret", "JWT_SECRET");
        }
        
        // SMTP 설정
        JsonObject smtpConfig = config.getJsonObject("smtp");
        if (smtpConfig != null) {
            overrideIfSet(smtpConfig, "username", "SMTP_USERNAME");
            overrideIfSet(smtpConfig, "password", "SMTP_PASSWORD");
        }
        
        // Google OAuth 설정
        JsonObject googleConfig = config.getJsonObject("google");
        if (googleConfig != null) {
            overrideIfSet(googleConfig, "clientSecret", "GOOGLE_CLIENT_SECRET");
            overrideIfSet(googleConfig, "clientId", "GOOGLE_CLIENT_ID");
            overrideIfSet(googleConfig, "redirectUri", "GOOGLE_REDIRECT_URI");
        }
        
        // Monitoring 설정
        JsonObject monitoringConfig = config.getJsonObject("monitoring");
        if (monitoringConfig != null) {
            overrideIfSet(monitoringConfig, "apiKey", "MONITORING_API_KEY");
        }
    }
    
    private static void overrideIfSet(JsonObject config, String key, String envVar) {
        String value = System.getenv(envVar);
        if (value != null && !value.trim().isEmpty()) {
            config.put(key, value);
            log.debug("Overriding {} with environment variable {}", key, envVar);
        }
    }
    
    private static void overrideIfSet(JsonObject config, String key, String envVar, java.util.function.Function<String, Object> converter) {
        String value = System.getenv(envVar);
        if (value != null && !value.trim().isEmpty()) {
            try {
                config.put(key, converter.apply(value));
                log.debug("Overriding {} with environment variable {}", key, envVar);
            } catch (Exception e) {
                log.warn("Failed to convert environment variable {} for {}: {}", envVar, key, e.getMessage());
            }
        }
    }
}

