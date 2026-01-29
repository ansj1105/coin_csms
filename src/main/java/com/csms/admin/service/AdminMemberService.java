package com.csms.admin.service;

import com.csms.admin.dto.*;
import com.csms.admin.repository.AdminMemberRepository;
import com.csms.common.service.BaseService;
import com.csms.common.service.TronService;
import com.csms.common.utils.DateUtils;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mindrot.jbcrypt.BCrypt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class AdminMemberService extends com.csms.common.service.BaseService {
    
    private final AdminMemberRepository repository;
    private final TronService tronService;
    
    public AdminMemberService(PgPool pool, TronService tronService) {
        super(pool);
        this.repository = new AdminMemberRepository(pool, tronService);
        this.tronService = tronService;
    }
    
    public Future<MemberListDto> getMembers(
        Integer limit,
        Integer offset,
        String searchCategory,
        String searchKeyword,
        String activityStatus,
        String sanctionStatus
    ) {
        log.info("getMembers transaction started - limit: {}, offset: {}, searchCategory: {}, activityStatus: {}, sanctionStatus: {}", 
            limit, offset, searchCategory, activityStatus, sanctionStatus);
        
        // 기본값 설정
        final int finalLimit = (limit == null || limit <= 0) ? 20 : limit;
        final int finalOffset = (offset == null || offset < 0) ? 0 : offset;
        
        // 검색어 길이 제한
        final String finalSearchKeyword = (searchKeyword != null && searchKeyword.length() > 20) 
            ? searchKeyword.substring(0, 20) 
            : searchKeyword;
        
        return repository.getMembers(client, finalLimit, finalOffset, searchCategory, finalSearchKeyword, activityStatus, sanctionStatus)
            .onSuccess(result -> {
                log.info("getMembers transaction completed - total: {}, returned: {}", 
                    result.getTotal(), result.getMembers().size());
            })
            .onFailure(err -> {
                log.error("getMembers transaction failed - limit: {}, offset: {}", finalLimit, finalOffset, err);
            });
    }
    
    public Future<MemberDetailDto> getMemberDetail(Long memberId) {
        log.info("getMemberDetail transaction started - memberId: {}", memberId);
        return repository.getMemberDetail(client, memberId)
            .onSuccess(result -> {
                log.info("getMemberDetail transaction completed - memberId: {}, nickname: {}", 
                    memberId, result.getNickname());
            })
            .onFailure(err -> {
                log.error("getMemberDetail transaction failed - memberId: {}", memberId, err);
            });
    }
    
    public Future<Void> updateSanctionStatus(Long memberId, SanctionRequestDto request) {
        log.info("updateSanctionStatus transaction started - memberId: {}, sanctionStatus: {}", 
            memberId, request.getSanctionStatus());
        
        final String requestSanctionStatus = request.getSanctionStatus();
        
        // 현재 제재 상태 조회
        return repository.getMemberDetail(client, memberId)
            .compose(member -> {
                String currentSanction = member.getSanctionStatus();
                
                // 현재 제재 상태와 동일하면 해제 (null)
                String finalSanctionStatus;
                if (requestSanctionStatus != null && requestSanctionStatus.equals(currentSanction)) {
                    finalSanctionStatus = null;
                } else {
                    // null이면 빈 문자열로 변환 (DB에서 null로 저장)
                    finalSanctionStatus = (requestSanctionStatus == null || requestSanctionStatus.trim().isEmpty()) 
                        ? null 
                        : requestSanctionStatus;
                }
                
                return repository.updateSanctionStatus(client, memberId, finalSanctionStatus);
            })
            .onSuccess(result -> {
                log.info("updateSanctionStatus transaction completed - memberId: {}", memberId);
            })
            .onFailure(err -> {
                log.error("updateSanctionStatus transaction failed - memberId: {}", memberId, err);
            });
    }
    
    public Future<Void> updateMember(Long memberId, UpdateMemberRequestDto request) {
        log.info("updateMember transaction started - memberId: {}, phone: {}, level: {}", 
            memberId, request.getPhone() != null ? "***" : null, request.getLevel());
        
        return repository.updateMember(
            client,
            memberId,
            request.getPhone(),
            request.getLevel()
        )
        .onSuccess(result -> {
            log.info("updateMember transaction completed - memberId: {}", memberId);
        })
        .onFailure(err -> {
            log.error("updateMember transaction failed - memberId: {}", memberId, err);
        });
    }
    
    public Future<Void> resetTransactionPassword(Long memberId) {
        log.info("resetTransactionPassword transaction started - memberId: {}", memberId);
        return repository.resetTransactionPassword(client, memberId)
            .onSuccess(result -> {
                log.info("resetTransactionPassword transaction completed - memberId: {}", memberId);
            })
            .onFailure(err -> {
                log.error("resetTransactionPassword transaction failed - memberId: {}", memberId, err);
            });
    }
    
    public Future<Void> adjustCoin(CoinAdjustRequestDto request) {
        log.info("adjustCoin transaction started - userId: {}, type: {}, amount: {}, network: {}, token: {}", 
            request.getUserId(), request.getType(), request.getAmount(), request.getNetwork(), request.getToken());
        
        if (request.getUserId() == null) {
            log.warn("adjustCoin transaction failed - userId is required");
            return Future.failedFuture(new IllegalArgumentException("userId is required"));
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            log.warn("adjustCoin transaction failed - invalid amount: {}", request.getAmount());
            return Future.failedFuture(new IllegalArgumentException("amount must be positive"));
        }
        if (request.getType() == null || (!request.getType().equals("ADD") && !request.getType().equals("WITHDRAW"))) {
            log.warn("adjustCoin transaction failed - invalid type: {}", request.getType());
            return Future.failedFuture(new IllegalArgumentException("type must be ADD or WITHDRAW"));
        }
        
        return repository.adjustCoin(
            client,
            request.getUserId(),
            request.getNetwork(),
            request.getToken(),
            request.getAmount(),
            request.getType()
        )
        .onSuccess(result -> {
            log.info("adjustCoin transaction completed - userId: {}, type: {}, amount: {}", 
                request.getUserId(), request.getType(), request.getAmount());
        })
        .onFailure(err -> {
            log.error("adjustCoin transaction failed - userId: {}, type: {}, amount: {}", 
                request.getUserId(), request.getType(), request.getAmount(), err);
        });
    }
    
    public Future<Void> adjustKoriPoint(KoriPointAdjustRequestDto request) {
        log.info("adjustKoriPoint transaction started - userId: {}, type: {}, amount: {}", 
            request.getUserId(), request.getType(), request.getAmount());
        
        if (request.getUserId() == null) {
            log.warn("adjustKoriPoint transaction failed - userId is required");
            return Future.failedFuture(new IllegalArgumentException("userId is required"));
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            log.warn("adjustKoriPoint transaction failed - invalid amount: {}", request.getAmount());
            return Future.failedFuture(new IllegalArgumentException("amount must be positive"));
        }
        if (request.getType() == null || (!request.getType().equals("ADD") && !request.getType().equals("WITHDRAW"))) {
            log.warn("adjustKoriPoint transaction failed - invalid type: {}", request.getType());
            return Future.failedFuture(new IllegalArgumentException("type must be ADD or WITHDRAW"));
        }
        
        return repository.adjustKoriPoint(
            client,
            request.getUserId(),
            request.getAmount(),
            request.getType()
        )
        .onSuccess(result -> {
            log.info("adjustKoriPoint transaction completed - userId: {}, type: {}, amount: {}", 
                request.getUserId(), request.getType(), request.getAmount());
        })
        .onFailure(err -> {
            log.error("adjustKoriPoint transaction failed - userId: {}, type: {}, amount: {}", 
                request.getUserId(), request.getType(), request.getAmount(), err);
        });
    }
    
    public Future<WalletListDto> getMemberWallets(
        Long memberId,
        String network,
        String token,
        Integer limit,
        Integer offset
    ) {
        log.info("getMemberWallets transaction started - memberId: {}, network: {}, token: {}, limit: {}, offset: {}", 
            memberId, network, token, limit, offset);
        
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        if (offset == null || offset < 0) {
            offset = 0;
        }
        
        return repository.getMemberWallets(client, memberId, network, token, limit, offset)
            .onSuccess(result -> {
                log.info("getMemberWallets transaction completed - memberId: {}, total: {}, returned: {}", 
                    memberId, result.getTotal(), result.getWallets().size());
            })
            .onFailure(err -> {
                log.error("getMemberWallets transaction failed - memberId: {}", memberId, err);
            });
    }
    
    // ========== Mining History 관련 메서드 ==========
    
    public Future<MiningHistoryDetailDto> getMiningHistory(
        Long userId,
        Integer limit,
        Integer offset,
        String dateRange
    ) {
        log.info("getMiningHistory transaction started - userId: {}, limit: {}, offset: {}, dateRange: {}", 
            userId, limit, offset, dateRange);
        
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
        
        return repository.getMiningHistory(client, userId, limit, offset, startDate, endDate)
            .onSuccess(result -> {
                log.info("getMiningHistory transaction completed - userId: {}, total: {}, returned: {}", 
                    userId, result.getTotal(), result.getRecords().size());
            })
            .onFailure(err -> {
                log.error("getMiningHistory transaction failed - userId: {}", userId, err);
            });
    }
    
    // ========== Export 관련 메서드 ==========
    
    public Future<Buffer> exportMembers(
        String searchCategory,
        String searchKeyword,
        String activityStatus,
        String sanctionStatus
    ) {
        log.info("exportMembers transaction started - searchCategory: {}, activityStatus: {}, sanctionStatus: {}", 
            searchCategory, activityStatus, sanctionStatus);
        
        // 모든 데이터 조회 (페이지네이션 없이)
        return repository.getMembers(client,
            Integer.MAX_VALUE,
            0,
            searchCategory,
            searchKeyword,
            activityStatus,
            sanctionStatus
        )
        .map(memberList -> {
            try {
                log.debug("Creating Excel file for {} members", memberList.getMembers().size());
                Buffer buffer = createMembersExcelFile(memberList.getMembers());
                log.info("exportMembers transaction completed - members: {}", memberList.getMembers().size());
                return buffer;
            } catch (IOException e) {
                log.error("exportMembers transaction failed - failed to create Excel file", e);
                throw new RuntimeException("Failed to create Excel file", e);
            }
        });
    }
    
    public Future<Buffer> exportMiningHistory(Long userId, String dateRange) {
        log.info("exportMiningHistory transaction started - userId: {}, dateRange: {}", userId, dateRange);
        
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
            client,
            userId,
            Integer.MAX_VALUE,
            0,
            startDate,
            endDate
        )
        .map(historyDetail -> {
            try {
                log.debug("Creating Excel file for {} records", historyDetail.getRecords().size());
                Buffer buffer = createMiningHistoryExcelFile(historyDetail);
                log.info("exportMiningHistory transaction completed - userId: {}, records: {}", 
                    userId, historyDetail.getRecords().size());
                return buffer;
            } catch (IOException e) {
                log.error("exportMiningHistory transaction failed - failed to create Excel file", e);
                throw new RuntimeException("Failed to create Excel file", e);
            }
        });
    }
    
    private Buffer createMembersExcelFile(List<MemberListDto.MemberInfo> members) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("회원 목록");
        
        // 스타일 생성
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);
        
        // 헤더 행 생성
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "ID", "로그인ID", "추천인ID", "추천인닉네임", "닉네임", "레벨", 
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
            // email 컬럼 제거 (users 테이블에 email 컬럼 없음)
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
            if (sheet.getColumnWidth(i) < 2000) {
                sheet.setColumnWidth(i, 2000);
            }
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
    
    private Buffer createMiningHistoryExcelFile(MiningHistoryDetailDto historyDetail) throws IOException {
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

