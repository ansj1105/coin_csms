package com.csms.admin.service;

import com.csms.admin.dto.MiningBoosterDto;
import com.csms.admin.dto.UpdateBoosterRequestDto;
import com.csms.admin.repository.AdminMiningBoosterRepository;
import com.csms.common.service.BaseService;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdminMiningBoosterService extends BaseService {
    
    private final AdminMiningBoosterRepository repository;
    
    public AdminMiningBoosterService(PgPool pool, AdminMiningBoosterRepository repository) {
        super(pool);
        this.repository = repository;
    }
    
    public Future<MiningBoosterDto> getMiningBoosters() {
        return repository.getMiningBoosters();
    }
    
    public Future<Void> updateBooster(UpdateBoosterRequestDto request) {
        if (request.getType() == null || request.getType().isEmpty()) {
            return Future.failedFuture(new IllegalArgumentException("Type is required"));
        }
        
        return repository.updateBooster(
            request.getType(),
            request.getIsEnabled(),
            request.getEfficiency(),
            request.getMaxCount(),
            request.getPerUnitEfficiency()
        );
    }
}

