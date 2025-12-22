package com.csms.admin.service;

import com.csms.admin.dto.MiningHistoryDetailDto;
import com.csms.admin.repository.AdminMiningHistoryRepository;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class AdminMiningHistoryExportService {
    
    private final AdminMiningHistoryRepository repository;
    
    public AdminMiningHistoryExportService(PgPool pool) {
        this.repository = new AdminMiningHistoryRepository(pool);
    }
    
    public Future<Buffer> exportToExcel(Long userId, String dateRange) {
        // 날짜 범위 계산
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        
        if (!"ALL".equals(dateRange) && dateRange != null && !dateRange.isEmpty()) {
            LocalDate end = LocalDate.now();
            LocalDate start;
            
            switch (dateRange) {
                case "TODAY" -> start = end;
                case "7" -> start = end.minusDays(7);
                case "14" -> start = end.minusDays(14);
                case "30" -> start = end.minusDays(30);
                case "365" -> start = end.minusDays(365);
                default -> start = end.minusDays(7);
            }
            
            startDate = start.atStartOfDay();
            endDate = end.atTime(23, 59, 59);
        }
        
        // 모든 데이터 조회 (페이지네이션 없이)
        return repository.getMiningHistory(
            userId,
            Integer.MAX_VALUE,
            0,
            startDate,
            endDate
        ).map(historyDetail -> {
            try {
                return createExcelFile(historyDetail);
            } catch (IOException e) {
                log.error("Failed to create Excel file", e);
                throw new RuntimeException("Failed to create Excel file", e);
            }
        });
    }
    
    private Buffer createExcelFile(MiningHistoryDetailDto historyDetail) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("채굴 내역");
        
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
            "연도", "날짜", "시간", "레벨", "채굴유형", "채굴효율(%)", "초대코드", 
            "팀원수", "채굴량", "래퍼럴수익", "총채굴 보유량", "기기정보(IP)"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 데이터 행 생성
        int rowNum = 1;
        for (MiningHistoryDetailDto.MiningHistoryRecord record : historyDetail.getRecords()) {
            Row row = sheet.createRow(rowNum++);
            
            int colNum = 0;
            
            createCell(row, colNum++, record.getYear(), dataStyle);
            createCell(row, colNum++, record.getDate(), dataStyle);
            createCell(row, colNum++, record.getTime(), dataStyle);
            createCell(row, colNum++, record.getLevel(), dataStyle);
            createCell(row, colNum++, record.getMiningType(), dataStyle);
            createCell(row, colNum++, record.getMiningEfficiency(), dataStyle);
            createCell(row, colNum++, record.getInvitationCode(), dataStyle);
            createCell(row, colNum++, record.getTeamMemberCount(), dataStyle);
            
            // 숫자 셀
            Cell cell8 = row.createCell(colNum++);
            cell8.setCellValue(record.getMiningAmount() != null ? record.getMiningAmount() : 0.0);
            cell8.setCellStyle(numberStyle);
            
            Cell cell9 = row.createCell(colNum++);
            cell9.setCellValue(record.getReferralRevenue() != null ? record.getReferralRevenue() : 0.0);
            cell9.setCellStyle(numberStyle);
            
            Cell cell10 = row.createCell(colNum++);
            cell10.setCellValue(record.getTotalMinedHoldings() != null ? record.getTotalMinedHoldings() : 0.0);
            cell10.setCellStyle(numberStyle);
            
            createCell(row, colNum++, record.getDeviceInfo(), dataStyle);
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

