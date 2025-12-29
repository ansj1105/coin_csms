package com.csms.admin.service;

import com.csms.admin.dto.*;
import com.csms.admin.repository.AdminReferralRepository;
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
import java.util.List;

@Slf4j
public class AdminReferralService extends BaseService {
    
    private final AdminReferralRepository repository;
    
    public AdminReferralService(PgPool pool) {
        super(pool);
        this.repository = new AdminReferralRepository();
    }
    
    /**
     * 래퍼럴 거래내역 목록 조회
     */
    public Future<ReferralTransactionHistoryListDto> getReferralTransactionHistory(
        Integer limit,
        Integer offset,
        String dateRange,
        String startDate,
        String endDate,
        String searchCategory,
        String searchKeyword,
        String sortType
    ) {
        log.info("getReferralTransactionHistory transaction started - limit: {}, offset: {}, dateRange: {}", 
            limit, offset, dateRange);
        
        final int finalLimit = (limit == null || limit <= 0) ? 20 : limit;
        final int finalOffset = (offset == null || offset < 0) ? 0 : offset;
        final String finalDateRange = (dateRange == null || dateRange.isEmpty()) ? "7" : dateRange;
        
        // 날짜 범위 계산
        DateUtils.DateRange range = DateUtils.calculateDateRange(finalDateRange, startDate, endDate);
        LocalDateTime startDateTime = range.startDate().atStartOfDay();
        LocalDateTime endDateTime = range.endDate().atTime(23, 59, 59);
        
        return repository.getReferralTransactionHistory(
            client,
            finalLimit,
            finalOffset,
            startDateTime,
            endDateTime,
            searchCategory,
            searchKeyword,
            sortType
        )
        .onSuccess(result -> {
            log.info("getReferralTransactionHistory transaction completed - total: {}", result.getTotal());
        })
        .onFailure(err -> {
            log.error("getReferralTransactionHistory transaction failed - limit: {}, offset: {}", finalLimit, finalOffset, err);
        });
    }
    
    /**
     * 래퍼럴 거래내역 목록 엑셀 다운로드
     */
    public Future<Buffer> exportReferralTransactionHistory(
        String dateRange,
        String startDate,
        String endDate,
        String searchCategory,
        String searchKeyword,
        String sortType
    ) {
        log.info("exportReferralTransactionHistory transaction started - dateRange: {}", dateRange);
        
        // 모든 데이터 조회
        return getReferralTransactionHistory(
            Integer.MAX_VALUE,
            0,
            dateRange,
            startDate,
            endDate,
            searchCategory,
            searchKeyword,
            sortType
        )
        .map(listDto -> {
            try {
                log.debug("Creating Excel file for {} transactions", listDto.getTransactions().size());
                Buffer buffer = createReferralTransactionHistoryExcelFile(listDto.getTransactions());
                log.info("exportReferralTransactionHistory transaction completed - transactions: {}", listDto.getTransactions().size());
                return buffer;
            } catch (IOException e) {
                log.error("exportReferralTransactionHistory transaction failed - failed to create Excel file", e);
                throw new RuntimeException("Failed to create Excel file", e);
            }
        });
    }
    
    /**
     * 래퍼럴 트리구조 목록 조회
     */
    public Future<ReferralTreeListDto> getReferralTree(
        Integer limit,
        Integer offset,
        String dateRange,
        String startDate,
        String endDate,
        String searchCategory,
        String searchKeyword,
        String activityStatus,
        String sortType
    ) {
        log.info("getReferralTree transaction started - limit: {}, offset: {}, dateRange: {}", 
            limit, offset, dateRange);
        
        final int finalLimit = (limit == null || limit <= 0) ? 20 : limit;
        final int finalOffset = (offset == null || offset < 0) ? 0 : offset;
        final String finalDateRange = (dateRange == null || dateRange.isEmpty()) ? "7" : dateRange;
        
        // 날짜 범위 계산
        DateUtils.DateRange range = DateUtils.calculateDateRange(finalDateRange, startDate, endDate);
        LocalDateTime startDateTime = range.startDate().atStartOfDay();
        LocalDateTime endDateTime = range.endDate().atTime(23, 59, 59);
        
        return repository.getReferralTree(
            client,
            finalLimit,
            finalOffset,
            startDateTime,
            endDateTime,
            searchCategory,
            searchKeyword,
            activityStatus,
            sortType
        )
        .onSuccess(result -> {
            log.info("getReferralTree transaction completed - total: {}", result.getTotal());
        })
        .onFailure(err -> {
            log.error("getReferralTree transaction failed - limit: {}, offset: {}", finalLimit, finalOffset, err);
        });
    }
    
    /**
     * 래퍼럴 트리구조 목록 엑셀 다운로드
     */
    public Future<Buffer> exportReferralTree(
        String dateRange,
        String startDate,
        String endDate,
        String searchCategory,
        String searchKeyword,
        String activityStatus,
        String sortType
    ) {
        log.info("exportReferralTree transaction started - dateRange: {}", dateRange);
        
        // 모든 데이터 조회
        return getReferralTree(
            Integer.MAX_VALUE,
            0,
            dateRange,
            startDate,
            endDate,
            searchCategory,
            searchKeyword,
            activityStatus,
            sortType
        )
        .map(listDto -> {
            try {
                log.debug("Creating Excel file for {} members", listDto.getMembers().size());
                Buffer buffer = createReferralTreeExcelFile(listDto.getMembers());
                log.info("exportReferralTree transaction completed - members: {}", listDto.getMembers().size());
                return buffer;
            } catch (IOException e) {
                log.error("exportReferralTree transaction failed - failed to create Excel file", e);
                throw new RuntimeException("Failed to create Excel file", e);
            }
        });
    }
    
    private Buffer createReferralTransactionHistoryExcelFile(List<ReferralTransactionHistoryDto> transactions) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("래퍼럴 거래내역");
        
        // 스타일 생성
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        // 헤더 생성
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "ID", "추천인(부모)", "초대코드", "닉네임", "레벨", "팀원 수", 
            "래퍼럴수익 전", "래퍼럴수익", "래퍼럴 수익 후", "시간날짜"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 데이터 행 생성
        int rowNum = 1;
        for (ReferralTransactionHistoryDto transaction : transactions) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            
            createCell(row, colNum++, transaction.getId(), dataStyle);
            createCell(row, colNum++, transaction.getReferrerNickname(), dataStyle);
            createCell(row, colNum++, transaction.getInvitationCode(), dataStyle);
            createCell(row, colNum++, transaction.getNickname(), dataStyle);
            createCell(row, colNum++, transaction.getLevel(), dataStyle);
            createCell(row, colNum++, transaction.getTeamMemberCount(), dataStyle);
            createCell(row, colNum++, transaction.getReferralRevenueBefore(), dataStyle);
            createCell(row, colNum++, transaction.getReferralRevenue(), dataStyle);
            createCell(row, colNum++, transaction.getReferralRevenueAfter(), dataStyle);
            createCell(row, colNum++, transaction.getDate() + " " + transaction.getTime(), dataStyle);
        }
        
        // 컬럼 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        
        return Buffer.buffer(out.toByteArray());
    }
    
    private Buffer createReferralTreeExcelFile(List<ReferralTreeMemberDto> members) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("래퍼럴 트리구조");
        
        // 스타일 생성
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        // 헤더 생성
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "ID", "추천인(부모)", "초대코드", "닉네임", "레벨", "팀원 수", 
            "레퍼럴 총수익", "래퍼럴 수익", "래퍼럴 등록일", "활동상태"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 데이터 행 생성 (평면화)
        int rowNum = 1;
        for (ReferralTreeMemberDto member : members) {
            // 상위 회원
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            
            createCell(row, colNum++, member.getId(), dataStyle);
            createCell(row, colNum++, member.getReferrerNickname(), dataStyle);
            createCell(row, colNum++, member.getInvitationCode(), dataStyle);
            createCell(row, colNum++, member.getNickname(), dataStyle);
            createCell(row, colNum++, member.getLevel(), dataStyle);
            createCell(row, colNum++, member.getTeamMemberCount(), dataStyle);
            createCell(row, colNum++, member.getTotalReferralRevenue(), dataStyle);
            createCell(row, colNum++, "", dataStyle); // 상위 회원은 래퍼럴 수익 없음
            createCell(row, colNum++, "", dataStyle); // 상위 회원은 등록일 없음
            createCell(row, colNum++, member.getActivityStatus(), dataStyle);
            
            // 하부 회원들
            if (member.getChildren() != null) {
                for (ReferralTreeMemberDto child : member.getChildren()) {
                    Row childRow = sheet.createRow(rowNum++);
                    colNum = 0;
                    
                    createCell(childRow, colNum++, child.getId(), dataStyle);
                    createCell(childRow, colNum++, child.getReferrerNickname(), dataStyle);
                    createCell(childRow, colNum++, child.getInvitationCode(), dataStyle);
                    createCell(childRow, colNum++, child.getNickname(), dataStyle);
                    createCell(childRow, colNum++, child.getLevel(), dataStyle);
                    createCell(childRow, colNum++, child.getTeamMemberCount(), dataStyle);
                    createCell(childRow, colNum++, "", dataStyle); // 하부 회원은 총수익 없음
                    createCell(childRow, colNum++, child.getReferralRevenue(), dataStyle);
                    createCell(childRow, colNum++, child.getReferralRegistrationDate(), dataStyle);
                    createCell(childRow, colNum++, child.getActivityStatus(), dataStyle);
                }
            }
        }
        
        // 컬럼 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        
        return Buffer.buffer(out.toByteArray());
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private void createCell(Row row, int colNum, Object value, CellStyle style) {
        Cell cell = row.createCell(colNum);
        if (value != null) {
            if (value instanceof String) {
                cell.setCellValue((String) value);
            } else if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue());
            } else {
                cell.setCellValue(value.toString());
            }
        }
        cell.setCellStyle(style);
    }
}

