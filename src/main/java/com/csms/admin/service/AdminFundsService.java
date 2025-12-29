package com.csms.admin.service;

import com.csms.admin.dto.*;
import com.csms.admin.repository.AdminFundsRepository;
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
public class AdminFundsService extends BaseService {
    
    private final AdminFundsRepository repository;
    
    public AdminFundsService(PgPool pool) {
        super(pool);
        this.repository = new AdminFundsRepository();
    }
    
    /**
     * 출금신청 목록 조회
     */
    public Future<WithdrawalRequestListDto> getWithdrawalRequests(
        Integer limit,
        Integer offset,
        String dateRange,
        String startDate,
        String endDate,
        String network,
        String currencyCode,
        String searchCategory,
        String searchKeyword,
        String status
    ) {
        log.info("getWithdrawalRequests transaction started - limit: {}, offset: {}, dateRange: {}", 
            limit, offset, dateRange);
        
        final int finalLimit = (limit == null || limit <= 0) ? 20 : limit;
        final int finalOffset = (offset == null || offset < 0) ? 0 : offset;
        final String finalDateRange = (dateRange == null || dateRange.isEmpty()) ? "7" : dateRange;
        
        // 날짜 범위 계산
        DateUtils.DateRange range = DateUtils.calculateDateRange(finalDateRange, startDate, endDate);
        LocalDateTime startDateTime = range.startDate().atStartOfDay();
        LocalDateTime endDateTime = range.endDate().atTime(23, 59, 59);
        
        // "ALL" 처리
        if ("ALL".equals(finalDateRange)) {
            startDateTime = null;
            endDateTime = null;
        }
        
        return repository.getWithdrawalRequests(
            client,
            finalLimit,
            finalOffset,
            startDateTime,
            endDateTime,
            network,
            currencyCode,
            searchCategory,
            searchKeyword,
            status
        )
        .onSuccess(result -> {
            log.info("getWithdrawalRequests transaction completed - total: {}", result.getTotal());
        })
        .onFailure(err -> {
            log.error("getWithdrawalRequests transaction failed - limit: {}, offset: {}", finalLimit, finalOffset, err);
        });
    }
    
    /**
     * 출금신청 상태 업데이트
     */
    public Future<Void> updateWithdrawalStatus(Long id, String status) {
        log.info("updateWithdrawalStatus transaction started - id: {}, status: {}", id, status);
        
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            return Future.failedFuture(new IllegalArgumentException("Invalid status: " + status));
        }
        
        return repository.updateWithdrawalStatus(client, id, status)
            .onSuccess(result -> {
                log.info("updateWithdrawalStatus transaction completed - id: {}, status: {}", id, status);
            })
            .onFailure(err -> {
                log.error("updateWithdrawalStatus transaction failed - id: {}, status: {}", id, status, err);
            });
    }
    
    /**
     * 출금신청 목록 엑셀 다운로드
     */
    public Future<Buffer> exportWithdrawalRequests(
        String dateRange,
        String startDate,
        String endDate,
        String network,
        String currencyCode,
        String searchCategory,
        String searchKeyword,
        String status
    ) {
        log.info("exportWithdrawalRequests transaction started - dateRange: {}", dateRange);
        
        // 모든 데이터 조회
        return getWithdrawalRequests(
            Integer.MAX_VALUE,
            0,
            dateRange,
            startDate,
            endDate,
            network,
            currencyCode,
            searchCategory,
            searchKeyword,
            status
        )
        .map(listDto -> {
            try {
                log.debug("Creating Excel file for {} requests", listDto.getRequests().size());
                Buffer buffer = createWithdrawalRequestsExcelFile(listDto.getRequests());
                log.info("exportWithdrawalRequests transaction completed - requests: {}", listDto.getRequests().size());
                return buffer;
            } catch (IOException e) {
                log.error("exportWithdrawalRequests transaction failed - failed to create Excel file", e);
                throw new RuntimeException("Failed to create Excel file", e);
            }
        });
    }
    
    /**
     * 거래내역 목록 조회
     */
    public Future<TransactionHistoryListDto> getTransactionHistory(
        Integer limit,
        Integer offset,
        String dateRange,
        String startDate,
        String endDate,
        String transactionType,
        String currencyCode,
        String searchCategory,
        String searchKeyword,
        String status
    ) {
        log.info("getTransactionHistory transaction started - limit: {}, offset: {}, dateRange: {}", 
            limit, offset, dateRange);
        
        final int finalLimit = (limit == null || limit <= 0) ? 20 : limit;
        final int finalOffset = (offset == null || offset < 0) ? 0 : offset;
        final String finalDateRange = (dateRange == null || dateRange.isEmpty()) ? "7" : dateRange;
        
        // 날짜 범위 계산
        DateUtils.DateRange range = DateUtils.calculateDateRange(finalDateRange, startDate, endDate);
        LocalDateTime startDateTime = range.startDate().atStartOfDay();
        LocalDateTime endDateTime = range.endDate().atTime(23, 59, 59);
        
        // "ALL" 처리
        if ("ALL".equals(finalDateRange)) {
            startDateTime = null;
            endDateTime = null;
        }
        
        return repository.getTransactionHistory(
            client,
            finalLimit,
            finalOffset,
            startDateTime,
            endDateTime,
            transactionType,
            currencyCode,
            searchCategory,
            searchKeyword,
            status
        )
        .onSuccess(result -> {
            log.info("getTransactionHistory transaction completed - total: {}", result.getTotal());
        })
        .onFailure(err -> {
            log.error("getTransactionHistory transaction failed - limit: {}, offset: {}", finalLimit, finalOffset, err);
        });
    }
    
    /**
     * 거래내역 목록 엑셀 다운로드
     */
    public Future<Buffer> exportTransactionHistory(
        String dateRange,
        String startDate,
        String endDate,
        String transactionType,
        String currencyCode,
        String searchCategory,
        String searchKeyword,
        String status
    ) {
        log.info("exportTransactionHistory transaction started - dateRange: {}", dateRange);
        
        // 모든 데이터 조회
        return getTransactionHistory(
            Integer.MAX_VALUE,
            0,
            dateRange,
            startDate,
            endDate,
            transactionType,
            currencyCode,
            searchCategory,
            searchKeyword,
            status
        )
        .map(listDto -> {
            try {
                log.debug("Creating Excel file for {} transactions", listDto.getTransactions().size());
                Buffer buffer = createTransactionHistoryExcelFile(listDto.getTransactions());
                log.info("exportTransactionHistory transaction completed - transactions: {}", listDto.getTransactions().size());
                return buffer;
            } catch (IOException e) {
                log.error("exportTransactionHistory transaction failed - failed to create Excel file", e);
                throw new RuntimeException("Failed to create Excel file", e);
            }
        });
    }
    
    private Buffer createWithdrawalRequestsExcelFile(List<WithdrawalRequestDto> requests) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("출금신청");
        
        // 스타일 생성
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        // 헤더 생성
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "연도", "날짜", "시간", "주문번호", "ID", "닉네임", "유형", "자산", 
            "신청금액", "스프레드", "수수료(%)", "실시간 가격", "정산금액", "수수료 수익", "지갑주소", "상태"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 데이터 행 생성
        int rowNum = 1;
        for (WithdrawalRequestDto request : requests) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            
            createCell(row, colNum++, request.getYear(), dataStyle);
            createCell(row, colNum++, request.getDate(), dataStyle);
            createCell(row, colNum++, request.getTime(), dataStyle);
            createCell(row, colNum++, request.getOrderNumber(), dataStyle);
            createCell(row, colNum++, request.getLoginId(), dataStyle);
            createCell(row, colNum++, request.getNickname(), dataStyle);
            createCell(row, colNum++, request.getType(), dataStyle);
            createCell(row, colNum++, request.getAsset(), dataStyle);
            createCell(row, colNum++, request.getRequestAmount(), dataStyle);
            createCell(row, colNum++, request.getSpread(), dataStyle);
            createCell(row, colNum++, request.getFeeRate(), dataStyle);
            createCell(row, colNum++, request.getRealtimePrice(), dataStyle);
            createCell(row, colNum++, request.getSettlementAmount(), dataStyle);
            createCell(row, colNum++, request.getFeeRevenue(), dataStyle);
            createCell(row, colNum++, request.getWalletAddress(), dataStyle);
            createCell(row, colNum++, request.getStatus(), dataStyle);
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
    
    private Buffer createTransactionHistoryExcelFile(List<TransactionHistoryDto> transactions) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("거래내역");
        
        // 스타일 생성
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        // 헤더 생성
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "연도", "날짜", "시간", "주문번호", "ID", "닉네임", "유형", "자산", 
            "신청금액", "스프레드수수료", "수수료(%)", "실시간 가격", "정산금액", "수수료 수익", "지갑주소", "상태"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 데이터 행 생성
        int rowNum = 1;
        for (TransactionHistoryDto transaction : transactions) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            
            createCell(row, colNum++, transaction.getYear(), dataStyle);
            createCell(row, colNum++, transaction.getDate(), dataStyle);
            createCell(row, colNum++, transaction.getTime(), dataStyle);
            createCell(row, colNum++, transaction.getOrderNumber(), dataStyle);
            createCell(row, colNum++, transaction.getLoginId(), dataStyle);
            createCell(row, colNum++, transaction.getNickname(), dataStyle);
            createCell(row, colNum++, transaction.getType(), dataStyle);
            createCell(row, colNum++, transaction.getAsset(), dataStyle);
            createCell(row, colNum++, transaction.getRequestAmount(), dataStyle);
            createCell(row, colNum++, transaction.getSpread(), dataStyle);
            createCell(row, colNum++, transaction.getFeeRate(), dataStyle);
            createCell(row, colNum++, transaction.getRealtimePrice(), dataStyle);
            createCell(row, colNum++, transaction.getSettlementAmount(), dataStyle);
            createCell(row, colNum++, transaction.getFeeRevenue(), dataStyle);
            createCell(row, colNum++, transaction.getWalletAddress(), dataStyle);
            createCell(row, colNum++, transaction.getStatus(), dataStyle);
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

