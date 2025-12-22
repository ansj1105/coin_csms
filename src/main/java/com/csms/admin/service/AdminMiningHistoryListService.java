package com.csms.admin.service;

import com.csms.admin.dto.MiningHistoryListDto;
import com.csms.admin.repository.AdminMiningHistoryListRepository;
import com.csms.common.service.BaseService;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdminMiningHistoryListService extends BaseService {
    
    private final AdminMiningHistoryListRepository repository;
    
    public AdminMiningHistoryListService(PgPool pool, AdminMiningHistoryListRepository repository) {
        super(pool);
        this.repository = repository;
    }
    
    public Future<MiningHistoryListDto> getMiningHistoryList(
        Integer limit,
        Integer offset,
        String searchCategory,
        String searchKeyword,
        String sortType,
        String activityStatus,
        String sanctionStatus
    ) {
        // 기본값 설정
        if (limit == null || limit <= 0) limit = 20;
        if (offset == null || offset < 0) offset = 0;
        if (searchCategory == null || searchCategory.isEmpty()) searchCategory = "ALL";
        if (activityStatus == null || activityStatus.isEmpty()) activityStatus = "ALL";
        if (sanctionStatus == null || sanctionStatus.isEmpty()) sanctionStatus = "ALL";
        
        // 검색어 길이 제한 (20자)
        if (searchKeyword != null && searchKeyword.length() > 20) {
            searchKeyword = searchKeyword.substring(0, 20);
        }
        
        return repository.getMiningHistoryList(
            limit,
            offset,
            searchCategory,
            searchKeyword,
            sortType,
            activityStatus,
            sanctionStatus
        );
    }
}

