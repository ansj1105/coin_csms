package com.csms.admin.repository;

import com.csms.admin.dto.*;
import com.csms.common.database.RowMapper;
import com.csms.common.repository.BaseRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminMiningRepository extends BaseRepository {
    
    private final PgPool pool;
    
    public AdminMiningRepository(PgPool pool) {
        this.pool = pool;
    }
    
    private final RowMapper<MiningRecordDto> miningRecordMapper = row -> MiningRecordDto.builder()
        .id(getLong(row, "id"))
        .userId(getLong(row, "user_id"))
        .referrerNickname(getString(row, "referrer_nickname"))
        .nickname(getString(row, "nickname"))
        .email(getString(row, "email"))
        .level(getInteger(row, "level"))
        .miningStartTime(getLocalDateTime(row, "mining_start_time"))
        .miningEndTime(getLocalDateTime(row, "mining_end_time"))
        .miningAmount(getDouble(row, "mining_amount"))
        .cumulativeMiningAmount(getDouble(row, "cumulative_mining_amount"))
        .miningEfficiency(getInteger(row, "mining_efficiency"))
        .activityStatus(getString(row, "activity_status"))
        .build();
    
    public Future<MiningRecordListDto> getMiningRecords(
        Integer limit,
        Integer offset,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String searchCategory,
        String searchKeyword,
        String activityStatus
    ) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        // SELECT 절
        sql.append("""
            SELECT 
                mh.id,
                mh.user_id,
                referrer.nickname as referrer_nickname,
                u.nickname,
                u.email,
                u.level,
                mh.created_at as mining_start_time,
                mh.updated_at as mining_end_time,
                mh.amount as mining_amount,
                COALESCE(cumulative_stats.cumulative_amount, 0) as cumulative_mining_amount,
                COALESCE(mh.efficiency, 0) as mining_efficiency,
                u.status as activity_status
            FROM mining_history mh
            INNER JOIN users u ON u.id = mh.user_id
            LEFT JOIN referral_relations rr ON rr.referred_id = u.id AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL
            LEFT JOIN users referrer ON referrer.id = rr.referrer_id
            LEFT JOIN (
                SELECT 
                    user_id,
                    SUM(amount) OVER (PARTITION BY user_id ORDER BY created_at) as cumulative_amount
                FROM mining_history
                WHERE type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH')
            ) cumulative_stats ON cumulative_stats.user_id = mh.user_id
            WHERE mh.type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH')
            AND mh.created_at >= :start_date
            AND mh.created_at <= :end_date
            """);
        
        params.put("start_date", startDate);
        params.put("end_date", endDate);
        
        // 검색 조건
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ID" -> {
                    sql.append(" AND (u.id::text = :search_keyword OR u.login_id ILIKE :search_keyword_pattern)");
                    params.put("search_keyword", searchKeyword);
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "REFERRER" -> {
                    sql.append(" AND referrer.nickname ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "NICKNAME" -> {
                    sql.append(" AND u.nickname ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "EMAIL" -> {
                    sql.append(" AND u.email ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "LEVEL" -> {
                    try {
                        Integer level = Integer.parseInt(searchKeyword);
                        sql.append(" AND u.level = :search_level");
                        params.put("search_level", level);
                    } catch (NumberFormatException e) {
                        // 레벨 파싱 실패 시 무시
                    }
                }
            }
        }
        
        // 활동상태 필터
        if (activityStatus != null && !activityStatus.equals("ALL")) {
            sql.append(" AND u.status = :activity_status");
            params.put("activity_status", activityStatus);
        }
        
        // 정렬 및 페이지네이션
        sql.append(" ORDER BY mh.created_at DESC");
        
        // 총 개수 조회를 위한 별도 쿼리
        StringBuilder countSql = new StringBuilder();
        countSql.append("""
            SELECT COUNT(*) as total
            FROM mining_history mh
            INNER JOIN users u ON u.id = mh.user_id
            LEFT JOIN referral_relations rr ON rr.referred_id = u.id AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL
            LEFT JOIN users referrer ON referrer.id = rr.referrer_id
            WHERE mh.type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH')
            AND mh.created_at >= :start_date
            AND mh.created_at <= :end_date
            """);
        
        // 검색 조건 추가 (count 쿼리에도 동일하게 적용)
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ID" -> countSql.append(" AND (u.id::text = :search_keyword OR u.login_id ILIKE :search_keyword_pattern)");
                case "REFERRER" -> countSql.append(" AND referrer.nickname ILIKE :search_keyword_pattern");
                case "NICKNAME" -> countSql.append(" AND u.nickname ILIKE :search_keyword_pattern");
                case "EMAIL" -> countSql.append(" AND u.email ILIKE :search_keyword_pattern");
                case "LEVEL" -> {
                    try {
                        Integer level = Integer.parseInt(searchKeyword);
                        countSql.append(" AND u.level = :search_level");
                    } catch (NumberFormatException e) {
                        // 레벨 파싱 실패 시 무시
                    }
                }
            }
        }
        
        if (activityStatus != null && !activityStatus.equals("ALL")) {
            countSql.append(" AND u.status = :activity_status");
        }
        
        Map<String, Object> countParams = new HashMap<>(params);
        
        return query(pool, countSql.toString(), countParams)
            .compose(countRows -> {
                Integer totalValue = fetchOne(row -> getInteger(row, "total"), countRows);
                final Integer total = totalValue != null ? totalValue : 0;
                
                // 데이터 조회
                sql.append(" LIMIT :limit OFFSET :offset");
                params.put("limit", limit);
                params.put("offset", offset);
                
                return query(pool, sql.toString(), params)
                    .map(rows -> {
                        List<MiningRecordDto> records = new ArrayList<>();
                        for (var row : rows) {
                            records.add(miningRecordMapper.map(row));
                        }
                        return MiningRecordListDto.builder()
                            .records(records)
                            .total(total)
                            .limit(limit)
                            .offset(offset)
                            .build();
                    });
            })
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to get mining records", throwable);
            });
    }
    
    // ========== Mining Condition 관련 메서드 ==========
    
    public Future<MiningConditionDto> getMiningConditions() {
        String basicSql = """
            SELECT 
                is_enabled,
                base_time_enabled,
                base_time_minutes
            FROM mining_settings
            WHERE setting_type = 'BASIC'
            LIMIT 1
            """;
        
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
        
        String progressSql = """
            SELECT 
                setting_key,
                is_enabled,
                time_per_hour,
                coins_per_hour
            FROM mining_settings
            WHERE setting_type = 'PROGRESS'
            """;
        
        String levelLimitSql = """
            SELECT 
                level,
                daily_limit
            FROM mining_level_limits
            ORDER BY level
            """;
        
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
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to get mining conditions", throwable);
            });
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
            .compose(rows -> succeededVoid())
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to update basic conditions", throwable);
            });
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
            .compose(rows -> succeededVoid())
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to update mission - type: " + type, throwable);
            });
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
            .compose(rows -> succeededVoid())
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to update progress setting - key: " + settingKey, throwable);
            });
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
            .compose(rows -> succeededVoid())
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to update level limit - level: " + level, throwable);
            });
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
            .compose(rows -> succeededVoid())
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to update level limits enabled", throwable);
            });
    }
    
    // ========== Mining Booster 관련 메서드 ==========
    
    private final RowMapper<MiningBoosterDto.Booster> boosterMapper = row -> {
        String type = getString(row, "type");
        Integer efficiency = getInteger(row, "efficiency");
        Integer maxCount = getInteger(row, "max_count");
        Integer perUnitEfficiency = getInteger(row, "per_unit_efficiency");
        
        if (maxCount != null && perUnitEfficiency != null) {
            efficiency = maxCount * perUnitEfficiency;
        }
        
        String note = getString(row, "note");
        
        return MiningBoosterDto.Booster.builder()
            .type(type)
            .name(getString(row, "name"))
            .isEnabled(getBoolean(row, "is_enabled"))
            .efficiency(efficiency)
            .maxCount(maxCount)
            .perUnitEfficiency(perUnitEfficiency)
            .note(note)
            .build();
    };
    
    public Future<MiningBoosterDto> getMiningBoosters() {
        String sql = """
            SELECT 
                type,
                name,
                is_enabled,
                efficiency,
                max_count,
                per_unit_efficiency,
                note
            FROM mining_boosters
            ORDER BY type
            """;
        
        return query(pool, sql, new HashMap<>())
            .map(rows -> {
                List<MiningBoosterDto.Booster> boosters = new ArrayList<>();
                Integer totalEfficiency = 0;
                
                for (var row : rows) {
                    MiningBoosterDto.Booster booster = boosterMapper.map(row);
                    boosters.add(booster);
                    
                    if (Boolean.TRUE.equals(booster.getIsEnabled()) && booster.getEfficiency() != null) {
                        totalEfficiency += booster.getEfficiency();
                    }
                }
                
                if (boosters.isEmpty()) {
                    boosters = createDefaultBoosters();
                    totalEfficiency = calculateTotalEfficiency(boosters);
                }
                
                Double miningAmountPerMinute = 0.100;
                
                MiningBoosterDto.Summary summary = MiningBoosterDto.Summary.builder()
                    .totalEfficiency(totalEfficiency)
                    .miningAmountPerMinute(miningAmountPerMinute)
                    .build();
                
                return MiningBoosterDto.builder()
                    .boosters(boosters)
                    .summary(summary)
                    .build();
            })
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to get mining boosters", throwable);
            });
    }
    
    private List<MiningBoosterDto.Booster> createDefaultBoosters() {
        List<MiningBoosterDto.Booster> boosters = new ArrayList<>();
        
        boosters.add(MiningBoosterDto.Booster.builder()
            .type("SOCIAL_LINK")
            .name("카카오/구글/이메일/네이버 연동시")
            .isEnabled(true)
            .efficiency(5)
            .build());
        
        boosters.add(MiningBoosterDto.Booster.builder()
            .type("PHONE_VERIFICATION")
            .name("본인인증(휴대폰) 등록시")
            .isEnabled(true)
            .efficiency(5)
            .build());
        
        boosters.add(MiningBoosterDto.Booster.builder()
            .type("REVIEW")
            .name("리뷰 작성시")
            .isEnabled(true)
            .efficiency(10)
            .build());
        
        boosters.add(MiningBoosterDto.Booster.builder()
            .type("AGENCY")
            .name("에이전시 가입시")
            .isEnabled(true)
            .efficiency(80)
            .build());
        
        boosters.add(MiningBoosterDto.Booster.builder()
            .type("PREMIUM")
            .name("프리미엄 패키지 구독시")
            .isEnabled(true)
            .efficiency(40)
            .build());
        
        boosters.add(MiningBoosterDto.Booster.builder()
            .type("AD_VIEW")
            .name("광고시청 보너스")
            .isEnabled(true)
            .efficiency(25)
            .maxCount(5)
            .perUnitEfficiency(5)
            .note("시청횟수: 계정당 하루 최대 채굴효율(%): 1회당 +% 채굴효율 ex) 5회 x 5% = +25%")
            .build());
        
        boosters.add(MiningBoosterDto.Booster.builder()
            .type("REFERRER_REGISTRATION")
            .name("추천인 등록시")
            .isEnabled(true)
            .efficiency(5)
            .build());
        
        boosters.add(MiningBoosterDto.Booster.builder()
            .type("INVITATION_REWARD")
            .name("초대보상 보너스")
            .isEnabled(true)
            .efficiency(50)
            .maxCount(5)
            .perUnitEfficiency(10)
            .note("초대인원: 최대 몇명 채굴효율(%): 1명당 +% 채굴효율 ex) 5명 x 10% = +50%")
            .build());
        
        return boosters;
    }
    
    private Integer calculateTotalEfficiency(List<MiningBoosterDto.Booster> boosters) {
        return boosters.stream()
            .filter(b -> Boolean.TRUE.equals(b.getIsEnabled()) && b.getEfficiency() != null)
            .mapToInt(MiningBoosterDto.Booster::getEfficiency)
            .sum();
    }
    
    public Future<Void> updateBooster(
        String type,
        Boolean isEnabled,
        Integer efficiency,
        Integer maxCount,
        Integer perUnitEfficiency
    ) {
        Integer finalEfficiency = efficiency;
        if (maxCount != null && perUnitEfficiency != null) {
            finalEfficiency = maxCount * perUnitEfficiency;
        }
        
        String sql = """
            INSERT INTO mining_boosters (type, is_enabled, efficiency, max_count, per_unit_efficiency, updated_at)
            VALUES (:type, :is_enabled, :efficiency, :max_count, :per_unit_efficiency, NOW())
            ON CONFLICT (type) 
            DO UPDATE SET 
                is_enabled = COALESCE(:is_enabled, mining_boosters.is_enabled),
                efficiency = COALESCE(:efficiency, mining_boosters.efficiency),
                max_count = COALESCE(:max_count, mining_boosters.max_count),
                per_unit_efficiency = COALESCE(:per_unit_efficiency, mining_boosters.per_unit_efficiency),
                updated_at = NOW()
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        params.put("is_enabled", isEnabled);
        params.put("efficiency", finalEfficiency);
        params.put("max_count", maxCount);
        params.put("per_unit_efficiency", perUnitEfficiency);
        
        return query(pool, sql, params)
            .compose(rows -> succeededVoid())
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to update booster - type: " + type, throwable);
            });
    }
    
    // ========== Referral Bonus 관련 메서드 ==========
    
    public Future<ReferralBonusDto> getReferralBonus() {
        String sql = """
            SELECT 
                is_enabled,
                distribution_rate
            FROM referral_bonus_settings
            LIMIT 1
            """;
        
        return query(pool, sql, new HashMap<>())
            .map(rows -> {
                if (rows.size() > 0) {
                    var row = rows.iterator().next();
                    return ReferralBonusDto.builder()
                        .isEnabled(getBoolean(row, "is_enabled"))
                        .distributionRate(getInteger(row, "distribution_rate"))
                        .build();
                } else {
                    return ReferralBonusDto.builder()
                        .isEnabled(true)
                        .distributionRate(5)
                        .build();
                }
            })
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to get referral bonus", throwable);
            });
    }
    
    public Future<Void> updateReferralBonus(Boolean isEnabled, Integer distributionRate) {
        String sql = """
            INSERT INTO referral_bonus_settings (is_enabled, distribution_rate, updated_at)
            VALUES (:is_enabled, :distribution_rate, NOW())
            ON CONFLICT (id)
            DO UPDATE SET
                is_enabled = :is_enabled,
                distribution_rate = :distribution_rate,
                updated_at = NOW()
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("is_enabled", isEnabled);
        params.put("distribution_rate", distributionRate);
        
        return query(pool, sql, params)
            .compose(rows -> succeededVoid())
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to update referral bonus", throwable);
            });
    }
    
    // ========== Ranking Reward 관련 메서드 ==========
    
    public Future<RankingRewardDto> getRankingReward() {
        String sql = """
            SELECT 
                type,
                rank1,
                rank2,
                rank3,
                rank4to10
            FROM ranking_reward_settings
            ORDER BY type
            """;
        
        return query(pool, sql, new HashMap<>())
            .map(rows -> {
                RankingRewardDto.RegionalReward regional = null;
                RankingRewardDto.NationalReward national = null;
                
                for (var row : rows) {
                    String type = getString(row, "type");
                    Double rank1 = getDouble(row, "rank1");
                    Double rank2 = getDouble(row, "rank2");
                    Double rank3 = getDouble(row, "rank3");
                    Double rank4to10 = getDouble(row, "rank4to10");
                    
                    if ("REGIONAL".equals(type)) {
                        regional = RankingRewardDto.RegionalReward.builder()
                            .rank1(rank1)
                            .rank2(rank2)
                            .rank3(rank3)
                            .rank4to10(rank4to10)
                            .build();
                    } else if ("NATIONAL".equals(type)) {
                        national = RankingRewardDto.NationalReward.builder()
                            .rank1(rank1)
                            .rank2(rank2)
                            .rank3(rank3)
                            .rank4to10(rank4to10)
                            .build();
                    }
                }
                
                if (regional == null) {
                    regional = RankingRewardDto.RegionalReward.builder()
                        .rank1(10.00)
                        .rank2(5.00)
                        .rank3(2.50)
                        .rank4to10(0.80)
                        .build();
                }
                if (national == null) {
                    national = RankingRewardDto.NationalReward.builder()
                        .rank1(10.00)
                        .rank2(5.00)
                        .rank3(2.50)
                        .rank4to10(0.80)
                        .build();
                }
                
                return RankingRewardDto.builder()
                    .regional(regional)
                    .national(national)
                    .build();
            })
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to get ranking reward", throwable);
            });
    }
    
    public Future<Void> updateRankingReward(
        String type,
        Double rank1,
        Double rank2,
        Double rank3,
        Double rank4to10
    ) {
        String sql = """
            INSERT INTO ranking_reward_settings (type, rank1, rank2, rank3, rank4to10, updated_at)
            VALUES (:type, :rank1, :rank2, :rank3, :rank4to10, NOW())
            ON CONFLICT (type)
            DO UPDATE SET
                rank1 = COALESCE(:rank1, ranking_reward_settings.rank1),
                rank2 = COALESCE(:rank2, ranking_reward_settings.rank2),
                rank3 = COALESCE(:rank3, ranking_reward_settings.rank3),
                rank4to10 = COALESCE(:rank4to10, ranking_reward_settings.rank4to10),
                updated_at = NOW()
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        params.put("rank1", rank1);
        params.put("rank2", rank2);
        params.put("rank3", rank3);
        params.put("rank4to10", rank4to10);
        
        return query(pool, sql, params)
            .compose(rows -> succeededVoid())
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to update ranking reward - type: " + type, throwable);
            });
    }
    
    // ========== Mining History List 관련 메서드 ==========
    
    private final RowMapper<MiningHistoryListDto.MiningHistoryItem> itemMapper = row -> {
        String sanctionStatus = getString(row, "sanction_status");
        if (sanctionStatus == null) {
            sanctionStatus = "NONE";
        }
        
        return MiningHistoryListDto.MiningHistoryItem.builder()
            .id(getLong(row, "id"))
            .referrerNickname(getString(row, "referrer_nickname"))
            .nickname(getString(row, "nickname"))
            .miningEfficiency(getInteger(row, "mining_efficiency"))
            .level(getInteger(row, "level"))
            .invitationCode(getString(row, "invitation_code"))
            .teamMemberCount(getInteger(row, "team_member_count"))
            .totalMiningAmount(getDouble(row, "total_mining_amount"))
            .referralRevenue(getDouble(row, "referral_revenue"))
            .totalMinedHoldings(getDouble(row, "total_mined_holdings"))
            .activityStatus(getString(row, "activity_status"))
            .sanctionStatus(sanctionStatus)
            .build();
    };
    
    public Future<MiningHistoryListDto> getMiningHistoryList(
        Integer limit,
        Integer offset,
        String searchCategory,
        String searchKeyword,
        String sortType,
        String activityStatus,
        String sanctionStatus
    ) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        sql.append("""
            SELECT 
                u.id,
                referrer.nickname as referrer_nickname,
                u.nickname,
                COALESCE(avg_efficiency.mining_efficiency, 0) as mining_efficiency,
                u.level,
                u.referral_code as invitation_code,
                COALESCE(team_stats.team_member_count, 0) as team_member_count,
                COALESCE(mining_stats.total_mining_amount, 0) as total_mining_amount,
                COALESCE(revenue_stats.referral_revenue, 0) as referral_revenue,
                COALESCE(mining_stats.total_mining_amount, 0) + COALESCE(revenue_stats.referral_revenue, 0) as total_mined_holdings,
                u.status as activity_status,
                u.sanction_status
            FROM users u
            LEFT JOIN referral_relations rr ON rr.referred_id = u.id AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL
            LEFT JOIN users referrer ON referrer.id = rr.referrer_id
            LEFT JOIN (
                SELECT referrer_id, COUNT(DISTINCT referred_id) as team_member_count
                FROM referral_relations
                WHERE status = 'ACTIVE' AND deleted_at IS NULL
                GROUP BY referrer_id
            ) team_stats ON team_stats.referrer_id = u.id
            LEFT JOIN (
                SELECT user_id, SUM(amount) as total_mining_amount
                FROM mining_history
                WHERE type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH')
                GROUP BY user_id
            ) mining_stats ON mining_stats.user_id = u.id
            LEFT JOIN (
                SELECT user_id, SUM(amount) as referral_revenue
                FROM mining_history
                WHERE type = 'REFERRAL_REWARD'
                GROUP BY user_id
            ) revenue_stats ON revenue_stats.user_id = u.id
            LEFT JOIN (
                SELECT user_id, AVG(efficiency) as mining_efficiency
                FROM mining_history
                WHERE type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH')
                AND efficiency IS NOT NULL
                GROUP BY user_id
            ) avg_efficiency ON avg_efficiency.user_id = u.id
            WHERE (mining_stats.user_id IS NOT NULL OR revenue_stats.user_id IS NOT NULL)
            """);
        
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() &&
            searchCategory != null && !"ALL".equals(searchCategory)) {
            switch (searchCategory) {
                case "ID" -> {
                    sql.append(" AND (u.id::text LIKE :search_keyword OR u.login_id LIKE :search_keyword)");
                    params.put("search_keyword", "%" + searchKeyword.trim() + "%");
                }
                case "REFERRER" -> {
                    sql.append(" AND referrer.nickname LIKE :search_keyword");
                    params.put("search_keyword", "%" + searchKeyword.trim() + "%");
                }
                case "NICKNAME" -> {
                    sql.append(" AND u.nickname LIKE :search_keyword");
                    params.put("search_keyword", "%" + searchKeyword.trim() + "%");
                }
                case "LEVEL" -> {
                    try {
                        Integer level = Integer.parseInt(searchKeyword.trim());
                        sql.append(" AND u.level = :search_keyword");
                        params.put("search_keyword", level);
                    } catch (NumberFormatException e) {
                        sql.append(" AND 1=0");
                    }
                }
                case "INVITATION_CODE" -> {
                    sql.append(" AND u.referral_code LIKE :search_keyword");
                    params.put("search_keyword", "%" + searchKeyword.trim() + "%");
                }
            }
        }
        
        if (activityStatus != null && !"ALL".equals(activityStatus)) {
            sql.append(" AND u.status = :activity_status");
            params.put("activity_status", activityStatus);
        }
        
        if (sanctionStatus != null && !"ALL".equals(sanctionStatus)) {
            sql.append(" AND u.sanction_status = :sanction_status");
            params.put("sanction_status", sanctionStatus);
        }
        
        if (sortType != null) {
            switch (sortType) {
                case "LEVEL" -> sql.append(" ORDER BY u.level DESC");
                case "TEAM_MEMBER_COUNT" -> sql.append(" ORDER BY team_member_count DESC");
                default -> sql.append(" ORDER BY u.id DESC");
            }
        } else {
            sql.append(" ORDER BY u.id DESC");
        }
        
        StringBuilder countSql = new StringBuilder();
        countSql.append("""
            SELECT COUNT(DISTINCT u.id) as total
            FROM users u
            LEFT JOIN (
                SELECT user_id, SUM(amount) as total_mining_amount
                FROM mining_history
                WHERE type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH')
                GROUP BY user_id
            ) mining_stats ON mining_stats.user_id = u.id
            LEFT JOIN (
                SELECT user_id, SUM(amount) as referral_revenue
                FROM mining_history
                WHERE type = 'REFERRAL_REWARD'
                GROUP BY user_id
            ) revenue_stats ON revenue_stats.user_id = u.id
            LEFT JOIN referral_relations rr ON rr.referred_id = u.id AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL
            LEFT JOIN users referrer ON referrer.id = rr.referrer_id
            WHERE (mining_stats.user_id IS NOT NULL OR revenue_stats.user_id IS NOT NULL)
            """);
        
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() &&
            searchCategory != null && !"ALL".equals(searchCategory)) {
            switch (searchCategory) {
                case "ID" -> countSql.append(" AND (u.id::text LIKE :search_keyword OR u.login_id LIKE :search_keyword)");
                case "REFERRER" -> countSql.append(" AND referrer.nickname LIKE :search_keyword");
                case "NICKNAME" -> countSql.append(" AND u.nickname LIKE :search_keyword");
                case "LEVEL" -> {
                    try {
                        Integer level = Integer.parseInt(searchKeyword.trim());
                        countSql.append(" AND u.level = :search_keyword");
                    } catch (NumberFormatException e) {
                        countSql.append(" AND 1=0");
                    }
                }
                case "INVITATION_CODE" -> countSql.append(" AND u.referral_code LIKE :search_keyword");
            }
        }
        
        if (activityStatus != null && !"ALL".equals(activityStatus)) {
            countSql.append(" AND u.status = :activity_status");
        }
        
        if (sanctionStatus != null && !"ALL".equals(sanctionStatus)) {
            countSql.append(" AND u.sanction_status = :sanction_status");
        }
        
        return query(pool, countSql.toString(), params)
            .compose(countRows -> {
                Integer total = 0;
                if (countRows.size() > 0) {
                    total = getInteger(countRows.iterator().next(), "total");
                }
                final Integer finalTotal = total;
                
                sql.append(" LIMIT :limit OFFSET :offset");
                params.put("limit", limit);
                params.put("offset", offset);
                
                return query(pool, sql.toString(), params)
                    .map(rows -> {
                        List<MiningHistoryListDto.MiningHistoryItem> items = new ArrayList<>();
                        for (var row : rows) {
                            items.add(itemMapper.map(row));
                        }
                        
                        return MiningHistoryListDto.builder()
                            .items(items)
                            .total(finalTotal)
                            .limit(limit)
                            .offset(offset)
                            .build();
                    });
            })
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to get mining history list", throwable);
            });
    }
}

