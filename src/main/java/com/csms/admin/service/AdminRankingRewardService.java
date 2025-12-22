package com.csms.admin.service;

import com.csms.admin.dto.RankingRewardDto;
import com.csms.admin.dto.UpdateRankingRewardRequestDto;
import com.csms.admin.repository.AdminRankingRewardRepository;
import com.csms.common.service.BaseService;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdminRankingRewardService extends BaseService {
    
    private final AdminRankingRewardRepository repository;
    
    public AdminRankingRewardService(PgPool pool, AdminRankingRewardRepository repository) {
        super(pool);
        this.repository = repository;
    }
    
    public Future<RankingRewardDto> getRankingReward() {
        return repository.getRankingReward();
    }
    
    public Future<Void> updateRankingReward(UpdateRankingRewardRequestDto request) {
        if (request.getType() == null || request.getType().isEmpty()) {
            return Future.failedFuture(new IllegalArgumentException("Type is required"));
        }
        
        if (!"REGIONAL".equals(request.getType()) && !"NATIONAL".equals(request.getType())) {
            return Future.failedFuture(new IllegalArgumentException("Type must be REGIONAL or NATIONAL"));
        }
        
        // 음수 체크
        if (request.getRank1() != null && request.getRank1() < 0) {
            return Future.failedFuture(new IllegalArgumentException("Rank1 must be non-negative"));
        }
        if (request.getRank2() != null && request.getRank2() < 0) {
            return Future.failedFuture(new IllegalArgumentException("Rank2 must be non-negative"));
        }
        if (request.getRank3() != null && request.getRank3() < 0) {
            return Future.failedFuture(new IllegalArgumentException("Rank3 must be non-negative"));
        }
        if (request.getRank4to10() != null && request.getRank4to10() < 0) {
            return Future.failedFuture(new IllegalArgumentException("Rank4to10 must be non-negative"));
        }
        
        return repository.updateRankingReward(
            request.getType(),
            request.getRank1(),
            request.getRank2(),
            request.getRank3(),
            request.getRank4to10()
        );
    }
}

