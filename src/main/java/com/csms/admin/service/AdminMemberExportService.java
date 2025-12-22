package com.csms.admin.service;

import com.csms.admin.dto.MemberListDto;
import com.csms.admin.repository.AdminMemberRepository;
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
public class AdminMemberExportService {
    
    private final AdminMemberRepository repository;
    
    public AdminMemberExportService(PgPool pool) {
        this.repository = new AdminMemberRepository(pool);
    }
    
    public Future<Buffer> exportToExcel(
        String searchCategory,
        String searchKeyword,
        String activityStatus,
        String sanctionStatus
    ) {
        // 모든 데이터 조회 (페이지네이션 없이)
        return repository.getMembers(
            Integer.MAX_VALUE, // limit
            0, // offset
            searchCategory,
            searchKeyword,
            activityStatus,
            sanctionStatus
        ).map(memberList -> {
            try {
                return createExcelFile(memberList.getMembers());
            } catch (IOException e) {
                log.error("Failed to create Excel file", e);
                throw new RuntimeException("Failed to create Excel file", e);
            }
        });
    }
    
    private Buffer createExcelFile(List<MemberListDto.MemberInfo> members) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("회원 목록");
        
        // 스타일 생성
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);
        
        // 헤더 행 생성
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "ID", "로그인ID", "추천인ID", "추천인닉네임", "닉네임", "이메일", "레벨", 
            "초대코드", "팀원수", "래퍼럴수익", "총채굴보유량", 
            "활동상태", "제재상태", "가입일"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 데이터 행 생성
        int rowNum = 1;
        for (MemberListDto.MemberInfo member : members) {
            Row row = sheet.createRow(rowNum++);
            
            int colNum = 0;
            createCell(row, colNum++, member.getId(), dataStyle);
            createCell(row, colNum++, member.getLoginId(), dataStyle);
            createCell(row, colNum++, member.getReferrerId(), dataStyle);
            createCell(row, colNum++, member.getReferrerNickname(), dataStyle);
            createCell(row, colNum++, member.getNickname(), dataStyle);
            createCell(row, colNum++, member.getEmail(), dataStyle);
            createCell(row, colNum++, member.getLevel(), dataStyle);
            createCell(row, colNum++, member.getInvitationCode(), dataStyle);
            createCell(row, colNum++, member.getTeamMemberCount(), dataStyle);
            createCell(row, colNum++, member.getReferralRevenue(), dataStyle);
            createCell(row, colNum++, member.getTotalMinedAmount(), dataStyle);
            createCell(row, colNum++, member.getActivityStatus(), dataStyle);
            createCell(row, colNum++, member.getSanctionStatus(), dataStyle);
            
            // 날짜 셀
            Cell dateCell = row.createCell(colNum++);
            if (member.getRegisteredAt() != null) {
                dateCell.setCellValue(member.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            dateCell.setCellStyle(dateStyle);
        }
        
        // 컬럼 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // 최소 너비 설정
            if (sheet.getColumnWidth(i) < 2000) {
                sheet.setColumnWidth(i, 2000);
            }
            // 최대 너비 제한 (너무 넓어지지 않도록)
            if (sheet.getColumnWidth(i) > 15000) {
                sheet.setColumnWidth(i, 15000);
            }
        }
        
        // Excel 파일을 바이트 배열로 변환
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return Buffer.buffer(outputStream.toByteArray());
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
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }
    
    private void createCell(Row row, int colNum, Object value, CellStyle style) {
        Cell cell = row.createCell(colNum);
        if (value != null) {
            if (value instanceof Long) {
                cell.setCellValue(((Long) value).doubleValue());
            } else if (value instanceof Integer) {
                cell.setCellValue(((Integer) value).doubleValue());
            } else if (value instanceof Double) {
                cell.setCellValue((Double) value);
            } else {
                cell.setCellValue(value.toString());
            }
        }
        cell.setCellStyle(style);
    }
}

