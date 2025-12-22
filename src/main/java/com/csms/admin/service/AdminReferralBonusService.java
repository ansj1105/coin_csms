package com.csms.admin.service;

import com.csms.admin.dto.ReferralBonusDto;
import com.csms.admin.dto.UpdateReferralBonusRequestDto;
import com.csms.admin.repository.AdminReferralBonusRepository;
import com.csms.common.service.BaseService;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdminReferralBonusService extends BaseService {
    
    private final AdminReferralBonusRepository repository;
    
    public AdminReferralBonusService(PgPool pool, AdminReferralBonusRepository repository) {
        super(pool);
        this.repository = repository;
    }
    
    public Future<ReferralBonusDto> getReferralBonus() {
        return repository.getReferralBonus();
    }
    
    public Future<Void> updateReferralBonus(UpdateReferralBonusRequestDto request) {
        if (request.getDistributionRate() != null && 
            (request.getDistributionRate() < 0 || request.getDistributionRate() > 100)) {
            return Future.failedFuture(new IllegalArgumentException("Distribution rate must be between 0 and 100"));
        }
        
        return repository.updateReferralBonus(
            request.getIsEnabled(),
            request.getDistributionRate()
        );
    }
}

