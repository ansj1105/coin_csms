package com.csms.admin.service;

import com.csms.admin.dto.MiningHistoryDetailDto;
import com.csms.admin.repository.AdminMiningHistoryRepository;
import com.csms.common.service.BaseService;
import com.csms.common.utils.DateUtils;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
public class AdminMiningHistoryService extends BaseService {
    
    private final AdminMiningHistoryRepository repository;
    
    public AdminMiningHistoryService(PgPool pool, AdminMiningHistoryRepository repository) {
        super(pool);
        this.repository = repository;
    }
    
    public Future<MiningHistoryDetailDto> getMiningHistory(
        Long userId,
        Integer limit,
        Integer offset,
        String dateRange
    ) {
        // 기본값 설정
        if (limit == null || limit <= 0) limit = 20;
        if (offset == null || offset < 0) offset = 0;
        if (dateRange == null || dateRange.isEmpty()) dateRange = "ALL";
        
        // 날짜 범위 계산
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        
        if (!"ALL".equals(dateRange)) {
            LocalDate end = LocalDate.now();
            LocalDate start;
            
            switch (dateRange) {
                case "TODAY" -> {
                    start = end;
                }
                case "7" -> {
                    start = end.minusDays(7);
                }
                case "14" -> {
                    start = end.minusDays(14);
                }
                case "30" -> {
                    start = end.minusDays(30);
                }
                case "365" -> {
                    start = end.minusDays(365);
                }
                default -> {
                    start = end.minusDays(7);
                }
            }
            
            startDate = start.atStartOfDay();
            endDate = end.atTime(23, 59, 59);
        }
        
        return repository.getMiningHistory(userId, limit, offset, startDate, endDate);
    }
}

