package com.csms.admin.service;

import com.csms.admin.dto.MiningHistoryListDto;
import com.csms.admin.repository.AdminMiningHistoryListRepository;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
public class AdminMiningHistoryListExportService {
    
    private final AdminMiningHistoryListRepository repository;
    
    public AdminMiningHistoryListExportService(PgPool pool) {
        this.repository = new AdminMiningHistoryListRepository(pool);
    }
    
    public Future<Buffer> exportToExcel(
        String searchCategory,
        String searchKeyword,
        String sortType,
        String activityStatus,
        String sanctionStatus
    ) {
        // 모든 데이터 조회 (페이지네이션 없이)
        return repository.getMiningHistoryList(
            Integer.MAX_VALUE,
            0,
            searchCategory,
            searchKeyword,
            sortType,
            activityStatus,
            sanctionStatus
        ).map(listDto -> {
            try {
                return createExcelFile(listDto);
            } catch (IOException e) {
                log.error("Failed to create Excel file", e);
                throw new RuntimeException("Failed to create Excel file", e);
            }
        });
    }
    
    private Buffer createExcelFile(MiningHistoryListDto listDto) throws IOException {
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

