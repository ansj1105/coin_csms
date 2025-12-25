package com.csms.admin.service;

import com.csms.admin.dto.*;
import com.csms.admin.repository.AdminMiningRepository;
import com.csms.common.service.BaseService;
import com.csms.common.utils.DateUtils;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
        log.info("getMiningRecords transaction started - limit: {}, offset: {}, dateRange: {}, searchCategory: {}", 
            limit, offset, dateRange, searchCategory);
        
        // 기본값 설정
        final int finalLimit = (limit == null || limit <= 0) ? 20 : limit;
        final int finalOffset = (offset == null || offset < 0) ? 0 : offset;
        final String finalDateRange = (dateRange == null || dateRange.isEmpty()) ? "7" : dateRange;
        
        // 날짜 범위 계산
        DateUtils.DateRange range = DateUtils.calculateDateRange(finalDateRange, startDate, endDate);
        LocalDateTime startDateTime = range.startDate().atStartOfDay();
        LocalDateTime endDateTime = range.endDate().atTime(23, 59, 59);
        
        // 검색어 길이 제한
        final String finalSearchKeyword = (searchKeyword != null && searchKeyword.length() > 20) 
            ? searchKeyword.substring(0, 20) 
            : searchKeyword;
        
        // 검색 카테고리 기본값
        final String finalSearchCategory = (searchCategory == null || searchCategory.isEmpty()) ? "ALL" : searchCategory;
        
        // 활동상태 기본값
        final String finalActivityStatus = (activityStatus == null || activityStatus.isEmpty()) ? "ALL" : activityStatus;
        
        return repository.getMiningRecords(
            client,
            finalLimit,
            finalOffset,
            startDateTime,
            endDateTime,
            finalSearchCategory,
            finalSearchKeyword,
            finalActivityStatus
        )
        .onSuccess(result -> {
            log.info("getMiningRecords transaction completed - total: {}, returned: {}", 
                result.getTotal(), result.getRecords().size());
        })
        .onFailure(err -> {
            log.error("getMiningRecords transaction failed - limit: {}, offset: {}, dateRange: {}", 
                finalLimit, finalOffset, finalDateRange, err);
        });
    }
    
    // ========== Mining Condition 관련 메서드 ==========
    
    public Future<MiningConditionDto> getMiningConditions() {
        log.info("getMiningConditions transaction started");
        return repository.getMiningConditions(client)
            .onSuccess(result -> {
                log.info("getMiningConditions transaction completed - enabled: {}, missions: {}", 
                    result.getBasicConditions().getIsEnabled(), 
                    result.getBasicConditions().getMissions().size());
            })
            .onFailure(err -> {
                log.error("getMiningConditions transaction failed", err);
            });
    }
    
    public Future<Void> updateBasicConditions(UpdateBasicConditionRequestDto request) {
        log.info("updateBasicConditions transaction started - enabled: {}, baseTimeEnabled: {}, baseTimeMinutes: {}, missions: {}", 
            request.getIsEnabled(), request.getBaseTimeEnabled(), request.getBaseTimeMinutes(),
            request.getMissions() != null ? request.getMissions().size() : 0);
        
        return repository.updateBasicConditions(
            client,
            request.getIsEnabled(),
            request.getBaseTimeEnabled(),
            request.getBaseTimeMinutes()
        ).compose(v -> {
            if (request.getMissions() != null && !request.getMissions().isEmpty()) {
                List<Future<Void>> missionFutures = new ArrayList<>();
                for (UpdateBasicConditionRequestDto.MissionUpdate mission : request.getMissions()) {
                    missionFutures.add(repository.updateMission(
                        client,
                        mission.getType(),
                        mission.getRequiredCount(),
                        mission.getIsEnabled()
                    ));
                }
                return allVoid(missionFutures);
            }
            return succeededVoid();
        })
        .onSuccess(result -> {
            if (request.getMissions() != null && !request.getMissions().isEmpty()) {
                log.info("updateBasicConditions transaction completed - {} missions updated", request.getMissions().size());
            } else {
                log.info("updateBasicConditions transaction completed");
            }
        })
        .onFailure(err -> {
            log.error("updateBasicConditions transaction failed", err);
        });
    }
    
    public Future<Void> updateProgressSetting(UpdateProgressSettingRequestDto request) {
        log.info("updateProgressSetting transaction started - broadcastProgress: {}, broadcastListening: {}", 
            request.getBroadcastProgress() != null, request.getBroadcastListening() != null);
        
        List<Future<Void>> futures = new ArrayList<>();
        
        if (request.getBroadcastProgress() != null) {
            futures.add(repository.updateProgressSetting(client,
                "BROADCAST_PROGRESS",
                request.getBroadcastProgress().getIsEnabled(),
                request.getBroadcastProgress().getTimePerHour(),
                request.getBroadcastProgress().getCoinsPerHour()
            ));
        }
        
        if (request.getBroadcastListening() != null) {
            futures.add(repository.updateProgressSetting(client,
                "BROADCAST_LISTENING",
                request.getBroadcastListening().getIsEnabled(),
                request.getBroadcastListening().getTimePerHour(),
                request.getBroadcastListening().getCoinsPerHour()
            ));
        }
        
        if (futures.isEmpty()) {
            log.warn("updateProgressSetting transaction started with no settings to update");
            return succeededVoid()
                .onSuccess(result -> {
                    log.info("updateProgressSetting transaction completed - no settings to update");
                });
        }
        
        return allVoid(futures)
            .onSuccess(result -> {
                log.info("updateProgressSetting transaction completed - {} settings updated", futures.size());
            })
            .onFailure(err -> {
                log.error("updateProgressSetting transaction failed", err);
            });
    }
    
    public Future<Void> updateLevelLimit(UpdateLevelLimitRequestDto request) {
        log.info("updateLevelLimit transaction started - level: {}, dailyLimit: {}", 
            request.getLevel(), request.getDailyLimit());
        
        if (request.getLevel() == null || request.getLevel() < 1 || request.getLevel() > 9) {
            log.warn("updateLevelLimit transaction failed - invalid level: {}", request.getLevel());
            return Future.failedFuture(new IllegalArgumentException("Level must be between 1 and 9"));
        }
        if (request.getDailyLimit() == null || request.getDailyLimit() < 0) {
            log.warn("updateLevelLimit transaction failed - invalid dailyLimit: {}", request.getDailyLimit());
            return Future.failedFuture(new IllegalArgumentException("Daily limit must be non-negative"));
        }
        
        return repository.updateLevelLimit(client, request.getLevel(), request.getDailyLimit())
            .onSuccess(result -> {
                log.info("updateLevelLimit transaction completed - level: {}, dailyLimit: {}", 
                    request.getLevel(), request.getDailyLimit());
            })
            .onFailure(err -> {
                log.error("updateLevelLimit transaction failed - level: {}, dailyLimit: {}", 
                    request.getLevel(), request.getDailyLimit(), err);
            });
    }
    
    public Future<Void> updateLevelLimitsEnabled(UpdateLevelLimitsEnabledRequestDto request) {
        log.info("updateLevelLimitsEnabled transaction started - enabled: {}", request.getEnabled());
        return repository.updateLevelLimitsEnabled(client, request.getEnabled())
            .onSuccess(result -> {
                log.info("updateLevelLimitsEnabled transaction completed - enabled: {}", request.getEnabled());
            })
            .onFailure(err -> {
                log.error("updateLevelLimitsEnabled transaction failed - enabled: {}", request.getEnabled(), err);
            });
        }
        
    // ========== Mining Booster 관련 메서드 ==========
    
    public Future<MiningBoosterDto> getMiningBoosters() {
        log.info("getMiningBoosters transaction started");
        return repository.getMiningBoosters(client)
            .onSuccess(result -> {
                log.info("getMiningBoosters transaction completed - boosters: {}, totalEfficiency: {}", 
                    result.getBoosters().size(), result.getSummary().getTotalEfficiency());
            })
            .onFailure(err -> {
                log.error("getMiningBoosters transaction failed", err);
            });
    }
    
    public Future<Void> updateBooster(UpdateBoosterRequestDto request) {
        log.info("updateBooster transaction started - type: {}, enabled: {}, efficiency: {}, maxCount: {}, perUnitEfficiency: {}", 
            request.getType(), request.getIsEnabled(), request.getEfficiency(), 
            request.getMaxCount(), request.getPerUnitEfficiency());
        
        if (request.getType() == null || request.getType().isEmpty()) {
            log.warn("updateBooster transaction failed - type is required");
            return Future.failedFuture(new IllegalArgumentException("Type is required"));
        }
        
        return repository.updateBooster(client,
            request.getType(),
            request.getIsEnabled(),
            request.getEfficiency(),
            request.getMaxCount(),
            request.getPerUnitEfficiency()
        )
        .onSuccess(result -> {
            log.info("updateBooster transaction completed - type: {}", request.getType());
        })
        .onFailure(err -> {
            log.error("updateBooster transaction failed - type: {}", request.getType(), err);
        });
    }
    
    // ========== Referral Bonus 관련 메서드 ==========
    
    public Future<ReferralBonusDto> getReferralBonus() {
        log.info("getReferralBonus transaction started");
        return repository.getReferralBonus(client)
            .onSuccess(result -> {
                log.info("getReferralBonus transaction completed - enabled: {}, distributionRate: {}", 
                    result.getIsEnabled(), result.getDistributionRate());
            })
            .onFailure(err -> {
                log.error("getReferralBonus transaction failed", err);
            });
    }
    
    public Future<Void> updateReferralBonus(UpdateReferralBonusRequestDto request) {
        log.info("updateReferralBonus transaction started - enabled: {}, distributionRate: {}", 
            request.getIsEnabled(), request.getDistributionRate());
        
        if (request.getDistributionRate() != null && 
            (request.getDistributionRate() < 0 || request.getDistributionRate() > 100)) {
            log.warn("updateReferralBonus transaction failed - invalid distributionRate: {}", request.getDistributionRate());
            return Future.failedFuture(new IllegalArgumentException("Distribution rate must be between 0 and 100"));
        }
        
        return repository.updateReferralBonus(client,
            request.getIsEnabled(),
            request.getDistributionRate()
        )
        .onSuccess(result -> {
            log.info("updateReferralBonus transaction completed - enabled: {}, distributionRate: {}", 
                request.getIsEnabled(), request.getDistributionRate());
        })
        .onFailure(err -> {
            log.error("updateReferralBonus transaction failed", err);
        });
    }
    
    // ========== Ranking Reward 관련 메서드 ==========
    
    public Future<RankingRewardDto> getRankingReward() {
        log.info("getRankingReward transaction started");
        return repository.getRankingReward(client)
            .onSuccess(result -> {
                log.info("getRankingReward transaction completed - regional: {}, national: {}", 
                    result.getRegional() != null, result.getNational() != null);
            })
            .onFailure(err -> {
                log.error("getRankingReward transaction failed", err);
            });
    }
    
    public Future<Void> updateRankingReward(UpdateRankingRewardRequestDto request) {
        log.info("updateRankingReward transaction started - type: {}, rank1: {}, rank2: {}, rank3: {}, rank4to10: {}", 
            request.getType(), request.getRank1(), request.getRank2(), 
            request.getRank3(), request.getRank4to10());
        
        if (request.getType() == null || request.getType().isEmpty()) {
            log.warn("updateRankingReward transaction failed - type is required");
            return Future.failedFuture(new IllegalArgumentException("Type is required"));
        }
        
        if (!"REGIONAL".equals(request.getType()) && !"NATIONAL".equals(request.getType())) {
            log.warn("updateRankingReward transaction failed - invalid type: {}", request.getType());
            return Future.failedFuture(new IllegalArgumentException("Type must be REGIONAL or NATIONAL"));
        }
        
        if (request.getRank1() != null && request.getRank1() < 0) {
            log.warn("updateRankingReward transaction failed - invalid rank1: {}", request.getRank1());
            return Future.failedFuture(new IllegalArgumentException("Rank1 must be non-negative"));
        }
        if (request.getRank2() != null && request.getRank2() < 0) {
            log.warn("updateRankingReward transaction failed - invalid rank2: {}", request.getRank2());
            return Future.failedFuture(new IllegalArgumentException("Rank2 must be non-negative"));
        }
        if (request.getRank3() != null && request.getRank3() < 0) {
            log.warn("updateRankingReward transaction failed - invalid rank3: {}", request.getRank3());
            return Future.failedFuture(new IllegalArgumentException("Rank3 must be non-negative"));
        }
        if (request.getRank4to10() != null && request.getRank4to10() < 0) {
            log.warn("updateRankingReward transaction failed - invalid rank4to10: {}", request.getRank4to10());
            return Future.failedFuture(new IllegalArgumentException("Rank4to10 must be non-negative"));
        }
        
        return repository.updateRankingReward(client,
            request.getType(),
            request.getRank1(),
            request.getRank2(),
            request.getRank3(),
            request.getRank4to10()
        )
        .onSuccess(result -> {
            log.info("updateRankingReward transaction completed - type: {}", request.getType());
        })
        .onFailure(err -> {
            log.error("updateRankingReward transaction failed - type: {}", request.getType(), err);
        });
        }
        
    // ========== Mining History List 관련 메서드 ==========
    
    public Future<MiningHistoryListDto> getMiningHistoryList(
        Integer limit,
        Integer offset,
        String searchCategory,
        String searchKeyword,
        String sortType,
        String activityStatus,
        String sanctionStatus
    ) {
        log.info("getMiningHistoryList transaction started - limit: {}, offset: {}, searchCategory: {}, sortType: {}, activityStatus: {}, sanctionStatus: {}", 
            limit, offset, searchCategory, sortType, activityStatus, sanctionStatus);
        
        final int finalLimit = (limit == null || limit <= 0) ? 20 : limit;
        final int finalOffset = (offset == null || offset < 0) ? 0 : offset;
        final String finalSearchCategory = (searchCategory == null || searchCategory.isEmpty()) ? "ALL" : searchCategory;
        final String finalActivityStatus = (activityStatus == null || activityStatus.isEmpty()) ? "ALL" : activityStatus;
        final String finalSanctionStatus = (sanctionStatus == null || sanctionStatus.isEmpty()) ? "ALL" : sanctionStatus;
        final String finalSearchKeyword = (searchKeyword != null && searchKeyword.length() > 20) 
            ? searchKeyword.substring(0, 20) 
            : searchKeyword;
        
        return repository.getMiningHistoryList(
            client,
            finalLimit,
            finalOffset,
            finalSearchCategory,
            finalSearchKeyword,
            sortType,
            finalActivityStatus,
            finalSanctionStatus
        )
        .onSuccess(result -> {
            log.info("getMiningHistoryList transaction completed - total: {}, returned: {}", 
                result.getTotal(), result.getItems().size());
        })
        .onFailure(err -> {
            log.error("getMiningHistoryList transaction failed - limit: {}, offset: {}", finalLimit, finalOffset, err);
        });
    }
    
    // ========== Export 관련 메서드 ==========
    
    public Future<Buffer> exportMiningRecords(
        String dateRange,
        String startDate,
        String endDate,
        String searchCategory,
        String searchKeyword,
        String activityStatus
    ) {
        log.info("exportMiningRecords transaction started - dateRange: {}, searchCategory: {}", dateRange, searchCategory);
        
        // 날짜 범위 계산
        DateUtils.DateRange range = DateUtils.calculateDateRange(dateRange, startDate, endDate);
        LocalDateTime startDateTime = range.startDate().atStartOfDay();
        LocalDateTime endDateTime = range.endDate().atTime(23, 59, 59);
        
        // 모든 데이터 조회 (페이지네이션 없이)
        return repository.getMiningRecords(client,
            Integer.MAX_VALUE,
            0,
            startDateTime,
            endDateTime,
            searchCategory,
            searchKeyword,
            activityStatus
        )
        .map(miningRecordList -> {
            try {
                log.debug("Creating Excel file for {} records", miningRecordList.getRecords().size());
                Buffer buffer = createMiningRecordsExcelFile(miningRecordList.getRecords());
                log.info("exportMiningRecords transaction completed - records: {}", miningRecordList.getRecords().size());
                return buffer;
            } catch (IOException e) {
                log.error("exportMiningRecords transaction failed - failed to create Excel file", e);
                throw new RuntimeException("Failed to create Excel file", e);
            }
        });
    }
    
    public Future<Buffer> exportMiningHistoryList(
        String searchCategory,
        String searchKeyword,
        String sortType,
        String activityStatus,
        String sanctionStatus
    ) {
        log.info("exportMiningHistoryList transaction started - searchCategory: {}, activityStatus: {}, sanctionStatus: {}", 
            searchCategory, activityStatus, sanctionStatus);
        
        // 모든 데이터 조회 (페이지네이션 없이)
        return repository.getMiningHistoryList(
            client,
            Integer.MAX_VALUE,
            0,
            searchCategory,
            searchKeyword,
            sortType,
            activityStatus,
            sanctionStatus
        )
        .map(listDto -> {
            try {
                log.debug("Creating Excel file for {} items", listDto.getItems().size());
                Buffer buffer = createMiningHistoryListExcelFile(listDto);
                log.info("exportMiningHistoryList transaction completed - items: {}", listDto.getItems().size());
                return buffer;
            } catch (IOException e) {
                log.error("exportMiningHistoryList transaction failed - failed to create Excel file", e);
                throw new RuntimeException("Failed to create Excel file", e);
            }
        });
    }
    
    private Buffer createMiningRecordsExcelFile(List<MiningRecordDto> records) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("채굴 기록");
        
        // 헤더 스타일
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        
        // 데이터 스타일
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        
        // 날짜 포맷
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.cloneStyleFrom(dataStyle);
        CreationHelper createHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        
        // 숫자 포맷
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(dataStyle);
        numberStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.0000"));
        
        // 헤더 생성
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "ID", "회원ID", "추천인(부모)", "닉네임", "이메일", "레벨",
            "채굴 시작 시간", "채굴 종료 시간", "채굴량", "누적 채굴량", "채굴효율", "활동상태"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 데이터 행 생성
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int rowNum = 1;
        for (MiningRecordDto record : records) {
            Row row = sheet.createRow(rowNum++);
            
            int colNum = 0;
            
            createCell(row, colNum++, record.getId(), dataStyle);
            createCell(row, colNum++, record.getUserId(), dataStyle);
            createCell(row, colNum++, record.getReferrerNickname(), dataStyle);
            createCell(row, colNum++, record.getNickname(), dataStyle);
            createCell(row, colNum++, record.getEmail(), dataStyle);
            createCell(row, colNum++, record.getLevel(), dataStyle);
            
            // 채굴 시작 시간
            Cell cell6 = row.createCell(colNum++);
            if (record.getMiningStartTime() != null) {
                cell6.setCellValue(record.getMiningStartTime().format(formatter));
            } else {
                cell6.setCellValue("");
            }
            cell6.setCellStyle(dataStyle);
            
            // 채굴 종료 시간
            Cell cell7 = row.createCell(colNum++);
            if (record.getMiningEndTime() != null) {
                cell7.setCellValue(record.getMiningEndTime().format(formatter));
            } else {
                cell7.setCellValue("");
            }
            cell7.setCellStyle(dataStyle);
            
            // 채굴량
            Cell cell8 = row.createCell(colNum++);
            cell8.setCellValue(record.getMiningAmount() != null ? record.getMiningAmount() : 0.0);
            cell8.setCellStyle(numberStyle);
            
            // 누적 채굴량
            Cell cell9 = row.createCell(colNum++);
            cell9.setCellValue(record.getCumulativeMiningAmount() != null ? record.getCumulativeMiningAmount() : 0.0);
            cell9.setCellStyle(numberStyle);
            
            createCell(row, colNum++, record.getMiningEfficiency(), dataStyle);
            createCell(row, colNum++, record.getActivityStatus(), dataStyle);
        }
        
        // 컬럼 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.max(currentWidth, 3000));
        }
        
        // 파일로 변환
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return Buffer.buffer(outputStream.toByteArray());
    }
    
    private Buffer createMiningHistoryListExcelFile(MiningHistoryListDto listDto) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("채굴 내역 목록");
        
        // 헤더 스타일
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        
        // 데이터 스타일
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        
        // 숫자 포맷
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(dataStyle);
        CreationHelper createHelper = workbook.getCreationHelper();
        numberStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.0000"));
        
        // 헤더 생성
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "ID", "추천인(부모)", "닉네임", "채굴효율(%)", "레벨", "초대코드",
            "팀원수", "총 채굴량", "래퍼럴수익", "총채굴 보유량", "활동상태", "상태"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 데이터 행 생성
        int rowNum = 1;
        for (MiningHistoryListDto.MiningHistoryItem item : listDto.getItems()) {
            Row row = sheet.createRow(rowNum++);
            
            int colNum = 0;
            
            createCell(row, colNum++, item.getId(), dataStyle);
            createCell(row, colNum++, item.getReferrerNickname(), dataStyle);
            createCell(row, colNum++, item.getNickname(), dataStyle);
            createCell(row, colNum++, item.getMiningEfficiency(), dataStyle);
            createCell(row, colNum++, item.getLevel(), dataStyle);
            createCell(row, colNum++, item.getInvitationCode(), dataStyle);
            createCell(row, colNum++, item.getTeamMemberCount(), dataStyle);
            
            // 숫자 셀
            Cell cell8 = row.createCell(colNum++);
            cell8.setCellValue(item.getTotalMiningAmount() != null ? item.getTotalMiningAmount() : 0.0);
            cell8.setCellStyle(numberStyle);
            
            Cell cell9 = row.createCell(colNum++);
            cell9.setCellValue(item.getReferralRevenue() != null ? item.getReferralRevenue() : 0.0);
            cell9.setCellStyle(numberStyle);
            
            Cell cell10 = row.createCell(colNum++);
            cell10.setCellValue(item.getTotalMinedHoldings() != null ? item.getTotalMinedHoldings() : 0.0);
            cell10.setCellStyle(numberStyle);
            
            createCell(row, colNum++, item.getActivityStatus(), dataStyle);
            createCell(row, colNum++, item.getSanctionStatus(), dataStyle);
        }
        
        // 컬럼 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.max(currentWidth, 3000));
        }
        
        // 파일로 변환
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return Buffer.buffer(outputStream.toByteArray());
    }
    
    private void createCell(Row row, int colNum, Object value, CellStyle style) {
        Cell cell = row.createCell(colNum);
        if (value != null) {
            if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue());
            } else {
                cell.setCellValue(value.toString());
            }
        }
        cell.setCellStyle(style);
    }
}

