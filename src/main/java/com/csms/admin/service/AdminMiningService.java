package com.csms.admin.service;

import com.csms.admin.dto.MiningRecordListDto;
import com.csms.admin.repository.AdminMiningRepository;
import com.csms.common.service.BaseService;
import com.csms.common.utils.DateUtils;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
public class AdminMiningService extends BaseService {
    
    private final AdminMiningRepository repository;
    
    public AdminMiningService(PgPool pool, AdminMiningRepository repository) {
        super(pool);
        this.repository = repository;
    }
    
    public Future<MiningRecordListDto> getMiningRecords(
        Integer limit,
        Integer offset,
        String dateRange,
        String startDate,
        String endDate,
        String searchCategory,
        String searchKeyword,
        String activityStatus
    ) {
        // 기본값 설정
        if (limit == null || limit <= 0) limit = 20;
        if (offset == null || offset < 0) offset = 0;
        if (dateRange == null || dateRange.isEmpty()) dateRange = "7";
        
        // 날짜 범위 계산
        DateUtils.DateRange range = DateUtils.calculateDateRange(dateRange, startDate, endDate);
        LocalDateTime startDateTime = range.startDate().atStartOfDay();
        LocalDateTime endDateTime = range.endDate().atTime(23, 59, 59);
        
        // 검색어 길이 제한
        if (searchKeyword != null && searchKeyword.length() > 20) {
            searchKeyword = searchKeyword.substring(0, 20);
        }
        
        // 검색 카테고리 기본값
        if (searchCategory == null || searchCategory.isEmpty()) {
            searchCategory = "ALL";
        }
        
        // 활동상태 기본값
        if (activityStatus == null || activityStatus.isEmpty()) {
            activityStatus = "ALL";
        }
        
        return repository.getMiningRecords(
            limit,
            offset,
            startDateTime,
            endDateTime,
            searchCategory,
            searchKeyword,
            activityStatus
        );
    }
}

