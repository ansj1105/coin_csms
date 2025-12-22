package com.csms.admin.repository;

import com.csms.admin.dto.ReferralBonusDto;
import com.csms.common.repository.BaseRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AdminReferralBonusRepository extends BaseRepository {
    
    private final PgPool pool;
    
    public AdminReferralBonusRepository(PgPool pool) {
        this.pool = pool;
    }
    
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
                    // 기본값
                    return ReferralBonusDto.builder()
                        .isEnabled(true)
                        .distributionRate(5)
                        .build();
                }
            })
            .onFailure(throwable -> log.error("래퍼럴 보너스 조회 실패", throwable));
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
            .compose(rows -> Future.<Void>succeededFuture())
            .onFailure(throwable -> log.error("래퍼럴 보너스 업데이트 실패", throwable));
    }
}

