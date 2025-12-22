package com.csms.admin.repository;

import com.csms.admin.dto.MiningBoosterDto;
import com.csms.common.database.RowMapper;
import com.csms.common.repository.BaseRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AdminMiningBoosterRepository extends BaseRepository {
    
    private final PgPool pool;
    
    public AdminMiningBoosterRepository(PgPool pool) {
        this.pool = pool;
    }
    
    private final RowMapper<MiningBoosterDto.Booster> boosterMapper = row -> {
        String type = getString(row, "type");
        Integer efficiency = getInteger(row, "efficiency");
        Integer maxCount = getInteger(row, "max_count");
        Integer perUnitEfficiency = getInteger(row, "per_unit_efficiency");
        
        // 복합 부스터의 경우 efficiency 자동 계산
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
                    
                    // 활성화된 부스터의 효율 합산
                    if (Boolean.TRUE.equals(booster.getIsEnabled()) && booster.getEfficiency() != null) {
                        totalEfficiency += booster.getEfficiency();
                    }
                }
                
                // 기본 부스터가 없으면 생성
                if (boosters.isEmpty()) {
                    boosters = createDefaultBoosters();
                    totalEfficiency = calculateTotalEfficiency(boosters);
                }
                
                // miningAmountPerMinute 계산 (예시: 기본 채굴량 * (1 + totalEfficiency / 100))
                // 실제 계산 로직은 비즈니스 요구사항에 따라 조정 필요
                Double miningAmountPerMinute = 0.100; // 기본값, 실제로는 설정에서 가져와야 함
                
                MiningBoosterDto.Summary summary = MiningBoosterDto.Summary.builder()
                    .totalEfficiency(totalEfficiency)
                    .miningAmountPerMinute(miningAmountPerMinute)
                    .build();
                
                return MiningBoosterDto.builder()
                    .boosters(boosters)
                    .summary(summary)
                    .build();
            })
            .onFailure(throwable -> log.error("채굴 부스터 조회 실패", throwable));
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
        // 복합 부스터의 경우 efficiency 자동 계산
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
            .compose(rows -> Future.succeededFuture())
            .onFailure(throwable -> log.error("부스터 업데이트 실패 - type: {}", type, throwable));
    }
}

