package com.csms.admin.repository;

import com.csms.admin.dto.MiningConditionDto;
import com.csms.common.repository.BaseRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AdminMiningConditionRepository extends BaseRepository {
    
    private final PgPool pool;
    
    public AdminMiningConditionRepository(PgPool pool) {
        this.pool = pool;
    }
    
    public Future<MiningConditionDto> getMiningConditions() {
        // 기본 조건 조회
        String basicSql = """
            SELECT 
                is_enabled,
                base_time_enabled,
                base_time_minutes
            FROM mining_settings
            WHERE setting_type = 'BASIC'
            LIMIT 1
            """;
        
        // 미션 조회
        String missionSql = """
            SELECT 
                type,
                name,
                required_count,
                is_enabled,
                has_input
            FROM mining_missions
            ORDER BY type
            """;
        
        // 진행 설정 조회
        String progressSql = """
            SELECT 
                setting_key,
                is_enabled,
                time_per_hour,
                coins_per_hour
            FROM mining_settings
            WHERE setting_type = 'PROGRESS'
            """;
        
        // 레벨별 한도 조회
        String levelLimitSql = """
            SELECT 
                level,
                daily_limit
            FROM mining_level_limits
            ORDER BY level
            """;
        
        // 레벨 한도 활성화 여부
        String levelLimitEnabledSql = """
            SELECT 
                is_enabled
            FROM mining_settings
            WHERE setting_type = 'LEVEL_LIMIT'
            LIMIT 1
            """;
        
        return query(pool, basicSql, new HashMap<>())
            .compose(basicRows -> {
                MiningConditionDto.BasicConditions basicConditions = null;
                if (basicRows.size() > 0) {
                    var row = basicRows.iterator().next();
                    basicConditions = MiningConditionDto.BasicConditions.builder()
                        .isEnabled(getBoolean(row, "is_enabled"))
                        .baseTimeEnabled(getBoolean(row, "base_time_enabled"))
                        .baseTimeMinutes(getInteger(row, "base_time_minutes"))
                        .missions(new ArrayList<>())
                        .build();
                } else {
                    // 기본값
                    basicConditions = MiningConditionDto.BasicConditions.builder()
                        .isEnabled(true)
                        .baseTimeEnabled(false)
                        .baseTimeMinutes(30)
                        .missions(new ArrayList<>())
                        .build();
                }
                
                final MiningConditionDto.BasicConditions finalBasicConditions = basicConditions;
                
                return query(pool, missionSql, new HashMap<>())
                    .compose(missionRows -> {
                        List<MiningConditionDto.Mission> missions = new ArrayList<>();
                        for (var row : missionRows) {
                            missions.add(MiningConditionDto.Mission.builder()
                                .type(getString(row, "type"))
                                .name(getString(row, "name"))
                                .requiredCount(getInteger(row, "required_count"))
                                .isEnabled(getBoolean(row, "is_enabled"))
                                .hasInput(getBoolean(row, "has_input"))
                                .build());
                        }
                        
                        // 미션이 없으면 기본 미션 생성
                        if (missions.isEmpty()) {
                            missions.add(MiningConditionDto.Mission.builder()
                                .type("RANDOM_CALL")
                                .name("랜덤통화")
                                .requiredCount(2)
                                .isEnabled(true)
                                .hasInput(true)
                                .build());
                            missions.add(MiningConditionDto.Mission.builder()
                                .type("LIVE_AD")
                                .name("라이브내 광고시청")
                                .requiredCount(1)
                                .isEnabled(true)
                                .hasInput(true)
                                .build());
                            missions.add(MiningConditionDto.Mission.builder()
                                .type("COMMUNITY_POST")
                                .name("커뮤니티 게시글")
                                .requiredCount(1)
                                .isEnabled(true)
                                .hasInput(true)
                                .build());
                            missions.add(MiningConditionDto.Mission.builder()
                                .type("DAILY_CHECKIN")
                                .name("일일 출석체크")
                                .requiredCount(1)
                                .isEnabled(true)
                                .hasInput(false)
                                .build());
                        }
                        
                        finalBasicConditions.setMissions(missions);
                        
                        return query(pool, progressSql, new HashMap<>())
                            .compose(progressRows -> {
                                MiningConditionDto.BroadcastSetting broadcastProgress = null;
                                MiningConditionDto.BroadcastSetting broadcastListening = null;
                                
                                for (var row : progressRows) {
                                    String key = getString(row, "setting_key");
                                    if ("BROADCAST_PROGRESS".equals(key)) {
                                        broadcastProgress = MiningConditionDto.BroadcastSetting.builder()
                                            .isEnabled(getBoolean(row, "is_enabled"))
                                            .timePerHour(getInteger(row, "time_per_hour"))
                                            .coinsPerHour(getDouble(row, "coins_per_hour"))
                                            .build();
                                    } else if ("BROADCAST_LISTENING".equals(key)) {
                                        broadcastListening = MiningConditionDto.BroadcastSetting.builder()
                                            .isEnabled(getBoolean(row, "is_enabled"))
                                            .timePerHour(getInteger(row, "time_per_hour"))
                                            .coinsPerHour(getDouble(row, "coins_per_hour"))
                                            .build();
                                    }
                                }
                                
                                // 기본값
                                if (broadcastProgress == null) {
                                    broadcastProgress = MiningConditionDto.BroadcastSetting.builder()
                                        .isEnabled(true)
                                        .timePerHour(15)
                                        .coinsPerHour(0.002)
                                        .build();
                                }
                                if (broadcastListening == null) {
                                    broadcastListening = MiningConditionDto.BroadcastSetting.builder()
                                        .isEnabled(true)
                                        .timePerHour(15)
                                        .coinsPerHour(0.002)
                                        .build();
                                }
                                
                                MiningConditionDto.ProgressSettings progressSettings = 
                                    MiningConditionDto.ProgressSettings.builder()
                                        .broadcastProgress(broadcastProgress)
                                        .broadcastListening(broadcastListening)
                                        .build();
                                
                                return query(pool, levelLimitSql, new HashMap<>())
                                    .compose(levelLimitRows -> {
                                        List<MiningConditionDto.LevelLimit> levelLimits = new ArrayList<>();
                                        for (var row : levelLimitRows) {
                                            levelLimits.add(MiningConditionDto.LevelLimit.builder()
                                                .level(getInteger(row, "level"))
                                                .dailyLimit(getDouble(row, "daily_limit"))
                                                .build());
                                        }
                                        
                                        return query(pool, levelLimitEnabledSql, new HashMap<>())
                                            .map(enabledRows -> {
                                                Boolean levelLimitsEnabled = true;
                                                if (enabledRows.size() > 0) {
                                                    var row = enabledRows.iterator().next();
                                                    levelLimitsEnabled = getBoolean(row, "is_enabled");
                                                }
                                                
                                                return MiningConditionDto.builder()
                                                    .basicConditions(finalBasicConditions)
                                                    .progressSettings(progressSettings)
                                                    .levelLimits(levelLimits)
                                                    .levelLimitsEnabled(levelLimitsEnabled)
                                                    .build();
                                            });
                                    });
                            });
                    });
            })
            .onFailure(throwable -> log.error("채굴 조건 조회 실패", throwable));
    }
    
    public Future<Void> updateBasicConditions(
        Boolean isEnabled,
        Boolean baseTimeEnabled,
        Integer baseTimeMinutes
    ) {
        String sql = """
            INSERT INTO mining_settings (setting_type, is_enabled, base_time_enabled, base_time_minutes, updated_at)
            VALUES ('BASIC', :is_enabled, :base_time_enabled, :base_time_minutes, NOW())
            ON CONFLICT (setting_type) 
            DO UPDATE SET 
                is_enabled = :is_enabled,
                base_time_enabled = :base_time_enabled,
                base_time_minutes = :base_time_minutes,
                updated_at = NOW()
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("is_enabled", isEnabled);
        params.put("base_time_enabled", baseTimeEnabled);
        params.put("base_time_minutes", baseTimeMinutes);
        
        return query(pool, sql, params)
            .compose(rows -> Future.<Void>succeededFuture())
            .onFailure(throwable -> log.error("기본 조건 업데이트 실패", throwable));
    }
    
    public Future<Void> updateMission(String type, Integer requiredCount, Boolean isEnabled) {
        String sql = """
            INSERT INTO mining_missions (type, required_count, is_enabled, updated_at)
            VALUES (:type, :required_count, :is_enabled, NOW())
            ON CONFLICT (type) 
            DO UPDATE SET 
                required_count = :required_count,
                is_enabled = :is_enabled,
                updated_at = NOW()
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        params.put("required_count", requiredCount);
        params.put("is_enabled", isEnabled);
        
        return query(pool, sql, params)
            .compose(rows -> Future.<Void>succeededFuture())
            .onFailure(throwable -> log.error("미션 업데이트 실패 - type: {}", type, throwable));
    }
    
    public Future<Void> updateProgressSetting(
        String settingKey,
        Boolean isEnabled,
        Integer timePerHour,
        Double coinsPerHour
    ) {
        String sql = """
            INSERT INTO mining_settings (setting_type, setting_key, is_enabled, time_per_hour, coins_per_hour, updated_at)
            VALUES ('PROGRESS', :setting_key, :is_enabled, :time_per_hour, :coins_per_hour, NOW())
            ON CONFLICT (setting_type, setting_key) 
            DO UPDATE SET 
                is_enabled = :is_enabled,
                time_per_hour = :time_per_hour,
                coins_per_hour = :coins_per_hour,
                updated_at = NOW()
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("setting_key", settingKey);
        params.put("is_enabled", isEnabled);
        params.put("time_per_hour", timePerHour);
        params.put("coins_per_hour", coinsPerHour);
        
        return query(pool, sql, params)
            .compose(rows -> Future.<Void>succeededFuture())
            .onFailure(throwable -> log.error("진행 설정 업데이트 실패 - key: {}", settingKey, throwable));
    }
    
    public Future<Void> updateLevelLimit(Integer level, Double dailyLimit) {
        String sql = """
            INSERT INTO mining_level_limits (level, daily_limit, updated_at)
            VALUES (:level, :daily_limit, NOW())
            ON CONFLICT (level) 
            DO UPDATE SET 
                daily_limit = :daily_limit,
                updated_at = NOW()
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("level", level);
        params.put("daily_limit", dailyLimit);
        
        return query(pool, sql, params)
            .compose(rows -> Future.<Void>succeededFuture())
            .onFailure(throwable -> log.error("레벨 한도 업데이트 실패 - level: {}", level, throwable));
    }
    
    public Future<Void> updateLevelLimitsEnabled(Boolean enabled) {
        String sql = """
            INSERT INTO mining_settings (setting_type, is_enabled, updated_at)
            VALUES ('LEVEL_LIMIT', :is_enabled, NOW())
            ON CONFLICT (setting_type) 
            DO UPDATE SET 
                is_enabled = :is_enabled,
                updated_at = NOW()
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("is_enabled", enabled);
        
        return query(pool, sql, params)
            .compose(rows -> Future.<Void>succeededFuture())
            .onFailure(throwable -> log.error("레벨 한도 활성화 업데이트 실패", throwable));
    }
}

