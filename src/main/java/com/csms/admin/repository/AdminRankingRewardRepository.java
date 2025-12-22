package com.csms.admin.repository;

import com.csms.admin.dto.RankingRewardDto;
import com.csms.common.repository.BaseRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AdminRankingRewardRepository extends BaseRepository {
    
    private final PgPool pool;
    
    public AdminRankingRewardRepository(PgPool pool) {
        this.pool = pool;
    }
    
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
                
                // 기본값
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
            .onFailure(throwable -> log.error("랭킹 보상 조회 실패", throwable));
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
            .compose(rows -> Future.<Void>succeededFuture())
            .onFailure(throwable -> log.error("랭킹 보상 업데이트 실패 - type: {}", type, throwable));
    }
}

