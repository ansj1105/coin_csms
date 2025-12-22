package com.csms.admin.service;

import com.csms.admin.dto.*;
import com.csms.admin.repository.AdminMiningConditionRepository;
import com.csms.common.service.BaseService;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AdminMiningConditionService extends BaseService {
    
    private final AdminMiningConditionRepository repository;
    
    public AdminMiningConditionService(PgPool pool, AdminMiningConditionRepository repository) {
        super(pool);
        this.repository = repository;
    }
    
    public Future<MiningConditionDto> getMiningConditions() {
        return repository.getMiningConditions();
    }
    
    public Future<Void> updateBasicConditions(UpdateBasicConditionRequestDto request) {
        // 기본 조건 업데이트
        return repository.updateBasicConditions(
            request.getIsEnabled(),
            request.getBaseTimeEnabled(),
            request.getBaseTimeMinutes()
        ).compose(v -> {
            // 미션 업데이트
            if (request.getMissions() != null) {
                List<Future<Void>> missionFutures = new ArrayList<>();
                for (UpdateBasicConditionRequestDto.MissionUpdate mission : request.getMissions()) {
                    missionFutures.add(repository.updateMission(
                        mission.getType(),
                        mission.getRequiredCount(),
                        mission.getIsEnabled()
                    ));
                }
                return Future.all(missionFutures).map((Void) null);
            }
            return Future.succeededFuture();
        });
    }
    
    public Future<Void> updateProgressSetting(UpdateProgressSettingRequestDto request) {
        List<Future<Void>> futures = new ArrayList<>();
        
        if (request.getBroadcastProgress() != null) {
            futures.add(repository.updateProgressSetting(
                "BROADCAST_PROGRESS",
                request.getBroadcastProgress().getIsEnabled(),
                request.getBroadcastProgress().getTimePerHour(),
                request.getBroadcastProgress().getCoinsPerHour()
            ));
        }
        
        if (request.getBroadcastListening() != null) {
            futures.add(repository.updateProgressSetting(
                "BROADCAST_LISTENING",
                request.getBroadcastListening().getIsEnabled(),
                request.getBroadcastListening().getTimePerHour(),
                request.getBroadcastListening().getCoinsPerHour()
            ));
        }
        
        return Future.all(futures).map((Void) null);
    }
    
    public Future<Void> updateLevelLimit(UpdateLevelLimitRequestDto request) {
        if (request.getLevel() == null || request.getLevel() < 1 || request.getLevel() > 9) {
            return Future.failedFuture(new IllegalArgumentException("Level must be between 1 and 9"));
        }
        if (request.getDailyLimit() == null || request.getDailyLimit() < 0) {
            return Future.failedFuture(new IllegalArgumentException("Daily limit must be non-negative"));
        }
        
        return repository.updateLevelLimit(request.getLevel(), request.getDailyLimit());
    }
    
    public Future<Void> updateLevelLimitsEnabled(UpdateLevelLimitsEnabledRequestDto request) {
        return repository.updateLevelLimitsEnabled(request.getEnabled());
    }
}

