package com.csms.admin.service;

import com.csms.admin.dto.DashboardStatsDto;
import com.csms.admin.repository.AdminDashboardRepository;
import com.csms.common.service.BaseService;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class AdminDashboardService extends BaseService {
    
    private final AdminDashboardRepository repository;
    
    public AdminDashboardService(PgPool pool, AdminDashboardRepository repository) {
        super(pool);
        this.repository = repository;
    }
    
    public Future<DashboardStatsDto> getDashboardStats(String dateRange, String startDate, String endDate) {
        log.debug("getDashboardStats called - dateRange: {}, startDate: {}, endDate: {}", 
            dateRange, startDate, endDate);
        
        // 날짜 범위 계산
        DateRange range = calculateDateRange(dateRange, startDate, endDate);
        log.debug("Calculated date range - start: {}, end: {}", range.startDate, range.endDate);
        
        return Future.all(
            getTotalStats(range),
            getChartData(range),
            getTopMembers(),
            getNotifications(),
            getTopReferrers()
        )
        .map(result -> {
            DashboardStatsDto.StatsDto stats = result.resultAt(0);
            List<DashboardStatsDto.ChartDataDto> chartData = result.resultAt(1);
            List<DashboardStatsDto.TopMemberDto> topMembers = result.resultAt(2);
            List<DashboardStatsDto.NotificationDto> notifications = result.resultAt(3);
            List<DashboardStatsDto.TopReferrerDto> topReferrers = result.resultAt(4);
            
            log.info("getDashboardStats succeeded - chartData: {}, topMembers: {}, notifications: {}, topReferrers: {}", 
                chartData.size(), topMembers.size(), notifications.size(), topReferrers.size());
            
            return DashboardStatsDto.builder()
                .stats(stats)
                .chartData(chartData)
                .topMembers(topMembers)
                .notifications(notifications)
                .topReferrers(topReferrers)
                .build();
        })
        .onFailure(err -> {
            log.error("getDashboardStats failed - dateRange: {}, startDate: {}, endDate: {}", 
                dateRange, startDate, endDate, err);
        });
    }
    
    private Future<DashboardStatsDto.StatsDto> getTotalStats(DateRange range) {
        // 오늘 날짜 계산
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(23, 59, 59);
        
        return repository.getTotalStats(client, range.startDate, range.endDate, todayStart, todayEnd);
    }
    
    private Future<List<DashboardStatsDto.ChartDataDto>> getChartData(DateRange range) {
        return repository.getChartData(client, range.startDate, range.endDate);
    }
    
    private Future<List<DashboardStatsDto.TopMemberDto>> getTopMembers() {
        return repository.getTopMembers(client);
    }
    
    private DateRange calculateDateRange(String dateRange, String startDate, String endDate) {
        LocalDate end = LocalDate.now();
        LocalDate start;
        
        if ("custom".equals(dateRange) && startDate != null && endDate != null) {
            start = LocalDate.parse(startDate);
            end = LocalDate.parse(endDate);
        } else {
            int days = switch (dateRange != null ? dateRange : "7") {
                case "14" -> 14;
                case "30" -> 30;
                case "180" -> 180;
                case "365" -> 365;
                default -> 7;
            };
            start = end.minusDays(days);
        }
        
        // 최대 365일 제한
        if (start.isBefore(end.minusDays(365))) {
            start = end.minusDays(365);
        }
        
        return new DateRange(
            start.atStartOfDay(),
            end.atTime(23, 59, 59)
        );
    }
    
    private Future<List<DashboardStatsDto.NotificationDto>> getNotifications() {
        return repository.getNotifications(client);
    }
    
    private Future<List<DashboardStatsDto.TopReferrerDto>> getTopReferrers() {
        return repository.getTopReferrers(client);
    }
    
    private record DateRange(LocalDateTime startDate, LocalDateTime endDate) {}
}
