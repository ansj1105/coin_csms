package com.csms.admin.service;

import com.csms.admin.dto.MiningRecordDto;
import com.csms.admin.dto.MiningRecordListDto;
import com.csms.admin.repository.AdminMiningRepository;
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
import java.util.List;

@Slf4j
public class AdminMiningExportService {
    
    private final AdminMiningRepository repository;
    
    public AdminMiningExportService(PgPool pool) {
        this.repository = new AdminMiningRepository(pool);
    }
    
    public Future<Buffer> exportToExcel(
        String dateRange,
        String startDate,
        String endDate,
        String searchCategory,
        String searchKeyword,
        String activityStatus
    ) {
        // 날짜 범위 계산
        com.csms.common.utils.DateUtils.DateRange range = 
            com.csms.common.utils.DateUtils.calculateDateRange(dateRange, startDate, endDate);
        LocalDateTime startDateTime = range.startDate.atStartOfDay();
        LocalDateTime endDateTime = range.endDate.atTime(23, 59, 59);
        
        // 모든 데이터 조회 (페이지네이션 없이)
        return repository.getMiningRecords(
            Integer.MAX_VALUE, // limit
            0, // offset
            startDateTime,
            endDateTime,
            searchCategory,
            searchKeyword,
            activityStatus
        ).map(miningRecordList -> {
            try {
                return createExcelFile(miningRecordList.getRecords());
            } catch (IOException e) {
                log.error("Failed to create Excel file", e);
                throw new RuntimeException("Failed to create Excel file", e);
            }
        });
    }
    
    private Buffer createExcelFile(List<MiningRecordDto> records) throws IOException {
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
            
            // ID
            Cell cell0 = row.createCell(colNum++);
            cell0.setCellValue(record.getId() != null ? record.getId() : 0);
            cell0.setCellStyle(dataStyle);
            
            // 회원ID
            Cell cell1 = row.createCell(colNum++);
            cell1.setCellValue(record.getUserId() != null ? record.getUserId() : 0);
            cell1.setCellStyle(dataStyle);
            
            // 추천인(부모)
            Cell cell2 = row.createCell(colNum++);
            cell2.setCellValue(record.getReferrerNickname() != null ? record.getReferrerNickname() : "");
            cell2.setCellStyle(dataStyle);
            
            // 닉네임
            Cell cell3 = row.createCell(colNum++);
            cell3.setCellValue(record.getNickname() != null ? record.getNickname() : "");
            cell3.setCellStyle(dataStyle);
            
            // 이메일
            Cell cell4 = row.createCell(colNum++);
            cell4.setCellValue(record.getEmail() != null ? record.getEmail() : "");
            cell4.setCellStyle(dataStyle);
            
            // 레벨
            Cell cell5 = row.createCell(colNum++);
            cell5.setCellValue(record.getLevel() != null ? record.getLevel() : 0);
            cell5.setCellStyle(dataStyle);
            
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
            
            // 채굴효율
            Cell cell10 = row.createCell(colNum++);
            cell10.setCellValue(record.getMiningEfficiency() != null ? record.getMiningEfficiency() : 0);
            cell10.setCellStyle(dataStyle);
            
            // 활동상태
            Cell cell11 = row.createCell(colNum++);
            cell11.setCellValue(record.getActivityStatus() != null ? record.getActivityStatus() : "");
            cell11.setCellStyle(dataStyle);
        }
        
        // 컬럼 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // 최소 너비 설정
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.max(currentWidth, 3000));
        }
        
        // 파일로 변환
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return Buffer.buffer(outputStream.toByteArray());
    }
}

