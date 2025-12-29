package com.csms.admin.repository;

import com.csms.admin.dto.MemberDetailDto;
import com.csms.admin.dto.MemberListDto;
import com.csms.admin.dto.MiningHistoryDetailDto;
import com.csms.common.database.RowMapper;
import com.csms.common.repository.BaseRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminMemberRepository extends BaseRepository {
    
    private final PgPool pool;
    
    public AdminMemberRepository(PgPool pool) {
        this.pool = pool;
    }
    
    public Future<MemberListDto> getMembers(
        SqlClient client,
        Integer limit,
        Integer offset,
        String searchCategory,
        String searchKeyword,
        String activityStatus,
        String sanctionStatus
    ) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        // SELECT 절
        sql.append("""
            SELECT 
                u.id,
                u.login_id,
                u.nickname,
                u.email,
                u.level,
                u.referral_code as invitation_code,
                u.status as activity_status,
                u.sanction_status,
                u.created_at as registered_at,
                rr.referrer_id,
                referrer.nickname as referrer_nickname,
                COALESCE(team_stats.team_member_count, 0) as team_member_count,
                COALESCE(revenue_stats.referral_revenue, 0) as referral_revenue,
                COALESCE(mining_stats.total_mined_amount, 0) as total_mined_amount
            FROM users u
            LEFT JOIN referral_relations rr ON rr.referred_id = u.id AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL
            LEFT JOIN users referrer ON referrer.id = rr.referrer_id
            LEFT JOIN (
                SELECT referrer_id, COUNT(DISTINCT referred_id) as team_member_count
                FROM referral_relations
                WHERE status = 'ACTIVE' AND deleted_at IS NULL
                GROUP BY referrer_id
            ) team_stats ON team_stats.referrer_id = u.id
            LEFT JOIN (
                SELECT user_id, SUM(amount) as referral_revenue
                FROM mining_history
                WHERE type = 'REFERRAL_REWARD'
                GROUP BY user_id
            ) revenue_stats ON revenue_stats.user_id = u.id
            LEFT JOIN (
                SELECT user_id, SUM(amount) as total_mined_amount
                FROM mining_history
                WHERE type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH')
                GROUP BY user_id
            ) mining_stats ON mining_stats.user_id = u.id
            WHERE 1=1
            """);
        
        // 검색 조건
        if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchCategory != null) {
            switch (searchCategory) {
                case "ID" -> {
                    sql.append(" AND (u.id::text = :search_keyword OR u.login_id ILIKE :search_keyword_pattern)");
                    params.put("search_keyword", searchKeyword);
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "REFERRER" -> {
                    sql.append(" AND referrer.nickname ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "NICKNAME" -> {
                    sql.append(" AND u.nickname ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "EMAIL" -> {
                    sql.append(" AND u.email ILIKE :search_keyword_pattern");
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
                case "LEVEL" -> {
                    try {
                        Integer level = Integer.parseInt(searchKeyword);
                        sql.append(" AND u.level = :search_level");
                        params.put("search_level", level);
                    } catch (NumberFormatException e) {
                        // 레벨이 숫자가 아니면 조건 추가 안함
                    }
                }
                case "INVITATION_CODE" -> {
                    sql.append(" AND u.referral_code = :search_keyword");
                    params.put("search_keyword", searchKeyword);
                }
                case "ALL" -> {
                    sql.append("""
                        AND (
                            u.id::text = :search_keyword
                            OR u.login_id ILIKE :search_keyword_pattern
                            OR u.nickname ILIKE :search_keyword_pattern
                            OR u.email ILIKE :search_keyword_pattern
                            OR u.referral_code = :search_keyword
                            OR referrer.nickname ILIKE :search_keyword_pattern
                        )
                        """);
                    params.put("search_keyword", searchKeyword);
                    params.put("search_keyword_pattern", "%" + searchKeyword + "%");
                }
            }
        }
        
        // 필터 조건
        if (activityStatus != null && !activityStatus.equals("ALL")) {
            sql.append(" AND u.status = :activity_status");
            params.put("activity_status", activityStatus);
        }
        
        if (sanctionStatus != null && !sanctionStatus.equals("ALL")) {
            if (sanctionStatus.equals("NONE")) {
                sql.append(" AND (u.sanction_status IS NULL OR u.sanction_status = '')");
            } else {
                sql.append(" AND u.sanction_status = :sanction_status");
                params.put("sanction_status", sanctionStatus);
            }
        }
        
        // COUNT 쿼리
        String countSql = "SELECT COUNT(*) as total FROM (" + sql.toString() + ") as filtered";
        
        // 정렬 및 페이지네이션
        sql.append(" ORDER BY u.created_at DESC");
        sql.append(" LIMIT :limit OFFSET :offset");
        params.put("limit", limit);
        params.put("offset", offset);
        
        // COUNT 실행
        return query(client, countSql, params)
            .compose(countRows -> {
                Long total = 0L;
                if (countRows.size() > 0) {
                    total = getLong(countRows.iterator().next(), "total");
                }
                final Long finalTotal = total != null ? total : 0L;
                
                // 실제 데이터 조회
                return query(client, sql.toString(), params)
                    .map(rows -> {
                        List<MemberListDto.MemberInfo> members = new ArrayList<>();
                        for (var row : rows) {
                            members.add(MemberListDto.MemberInfo.builder()
                                .id(getLong(row, "id"))
                                .loginId(getString(row, "login_id"))
                                .referrerId(getLong(row, "referrer_id"))
                                .referrerNickname(getString(row, "referrer_nickname"))
                                .nickname(getString(row, "nickname"))
                                .email(getString(row, "email"))
                                .level(getInteger(row, "level"))
                                .invitationCode(getString(row, "invitation_code"))
                                .teamMemberCount(getInteger(row, "team_member_count"))
                                .referralRevenue(getDouble(row, "referral_revenue"))
                                .totalMinedAmount(getDouble(row, "total_mined_amount"))
                                .activityStatus(getString(row, "activity_status"))
                                .sanctionStatus(getString(row, "sanction_status"))
                                .registeredAt(getLocalDateTime(row, "registered_at"))
                                .build());
                        }
                        return MemberListDto.builder()
                            .members(members)
                            .total(finalTotal)
                            .limit(limit)
                            .offset(offset)
                            .build();
                    });
            });
    }
    
    public Future<MemberDetailDto> getMemberDetail(SqlClient client, Long memberId) {
        String sql = """
            SELECT 
                u.id,
                u.login_id,
                u.nickname,
                u.email,
                u.level,
                u.referral_code as invitation_code,
                u.status as activity_status,
                u.sanction_status,
                u.gender,
                u.age,
                u.real_name,
                u.birth_date,
                u.phone,
                u.kakao_id,
                u.created_at as registered_at,
                u.last_ip_address,
                u.last_login_at,
                rr.referrer_id,
                referrer.nickname as referrer_nickname,
                COALESCE(team_stats.team_member_count, 0) as team_member_count,
                COALESCE(revenue_stats.referral_revenue, 0) as referral_revenue,
                COALESCE(mining_stats.total_mined_amount, 0) as total_mined_amount,
                EXISTS(SELECT 1 FROM payment_deposits pd WHERE pd.user_id = u.id) as has_payment_history,
                EXISTS(SELECT 1 FROM token_deposits td WHERE td.user_id = u.id) as has_deposit_history
            FROM users u
            LEFT JOIN referral_relations rr ON rr.referred_id = u.id AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL
            LEFT JOIN users referrer ON referrer.id = rr.referrer_id
            LEFT JOIN (
                SELECT referrer_id, COUNT(DISTINCT referred_id) as team_member_count
                FROM referral_relations
                WHERE status = 'ACTIVE' AND deleted_at IS NULL
                GROUP BY referrer_id
            ) team_stats ON team_stats.referrer_id = u.id
            LEFT JOIN (
                SELECT user_id, SUM(amount) as referral_revenue
                FROM mining_history
                WHERE type = 'REFERRAL_REWARD'
                GROUP BY user_id
            ) revenue_stats ON revenue_stats.user_id = u.id
            LEFT JOIN (
                SELECT user_id, SUM(amount) as total_mined_amount
                FROM mining_history
                WHERE type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH')
                GROUP BY user_id
            ) mining_stats ON mining_stats.user_id = u.id
            WHERE u.id = :member_id
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("member_id", memberId);
        
        return query(client, sql, params)
            .compose(rows -> {
                if (rows.size() == 0) {
                    return Future.failedFuture(new com.csms.common.exceptions.NotFoundException("Member not found"));
                }
                
                var row = rows.iterator().next();
                
                // 지갑 정보 조회
                String walletSql = """
                    SELECT 
                        w.id,
                        c.code as currency_code,
                        w.address,
                        w.balance,
                        w.status
                    FROM user_wallets w
                    JOIN currency c ON c.id = w.currency_id
                    WHERE w.user_id = :user_id
                    ORDER BY c.code
                    """;
                
                Map<String, Object> walletParams = new HashMap<>();
                walletParams.put("user_id", memberId);
                
                return query(client, walletSql, walletParams)
                    .map(walletRows -> {
                        List<MemberDetailDto.WalletInfo> wallets = new ArrayList<>();
                        for (var walletRow : walletRows) {
                            wallets.add(MemberDetailDto.WalletInfo.builder()
                                .id(getLong(walletRow, "id"))
                                .currencyCode(getString(walletRow, "currency_code"))
                                .address(getString(walletRow, "address"))
                                .balance(getDouble(walletRow, "balance"))
                                .status(getString(walletRow, "status"))
                                .build());
                        }
                        
                        return MemberDetailDto.builder()
                            .id(getLong(row, "id"))
                            .loginId(getString(row, "login_id"))
                            .referrerId(getLong(row, "referrer_id"))
                            .referrerNickname(getString(row, "referrer_nickname"))
                            .nickname(getString(row, "nickname"))
                            .email(getString(row, "email"))
                            .level(getInteger(row, "level"))
                            .invitationCode(getString(row, "invitation_code"))
                            .teamMemberCount(getInteger(row, "team_member_count"))
                            .referralRevenue(getDouble(row, "referral_revenue"))
                            .totalMinedAmount(getDouble(row, "total_mined_amount"))
                            .activityStatus(getString(row, "activity_status"))
                            .sanctionStatus(getString(row, "sanction_status"))
                            .gender(getString(row, "gender"))
                            .age(getInteger(row, "age"))
                            .realName(getString(row, "real_name"))
                            .birthDate(getLocalDate(row, "birth_date"))
                            .phone(getString(row, "phone"))
                            .kakaoId(getString(row, "kakao_id"))
                            .registeredAt(getLocalDateTime(row, "registered_at"))
                            .hasPaymentHistory(getBoolean(row, "has_payment_history"))
                            .hasDepositHistory(getBoolean(row, "has_deposit_history"))
                            .lastIpAddress(getString(row, "last_ip_address"))
                            .lastLoginAt(getLocalDateTime(row, "last_login_at"))
                            .wallets(wallets)
                            .build();
                    });
            });
    }
    
    public Future<Void> updateSanctionStatus(SqlClient client, Long memberId, String sanctionStatus) {
        String sql = """
            UPDATE users
            SET sanction_status = :sanction_status,
                updated_at = NOW()
            WHERE id = :member_id
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("member_id", memberId);
        params.put("sanction_status", sanctionStatus);
        
        return query(client, sql, params)
            .map(updateResult -> {
                if (updateResult.rowCount() == 0) {
                    throw new com.csms.common.exceptions.NotFoundException("Member not found");
                }
                return null;
            });
    }
    
    public Future<Void> updateMember(SqlClient client, Long memberId, String phone, String email, Integer level) {
        StringBuilder sql = new StringBuilder("UPDATE users SET updated_at = NOW()");
        Map<String, Object> params = new HashMap<>();
        params.put("member_id", memberId);
        
        if (phone != null) {
            sql.append(", phone = :phone");
            params.put("phone", phone);
        }
        if (email != null) {
            sql.append(", email = :email");
            params.put("email", email);
        }
        if (level != null) {
            sql.append(", level = :level");
            params.put("level", level);
        }
        
        sql.append(" WHERE id = :member_id");
        
        return query(client, sql.toString(), params)
            .map(updateResult -> {
                if (updateResult.rowCount() == 0) {
                    throw new com.csms.common.exceptions.NotFoundException("Member not found");
                }
                return null;
            });
    }
    
    public Future<Void> resetTransactionPassword(SqlClient client, Long memberId) {
        // 거래 비밀번호를 "0000"으로 초기화
        // users 테이블에 transaction_password 필드가 있다고 가정
        // "0000"을 BCrypt로 해시화
        String passwordHash = org.mindrot.jbcrypt.BCrypt.hashpw("0000", org.mindrot.jbcrypt.BCrypt.gensalt());
        
        String sql = """
            UPDATE users
            SET transaction_password_hash = :password_hash,
                transaction_password = :password_hash,
                updated_at = NOW()
            WHERE id = :member_id
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("member_id", memberId);
        params.put("password_hash", passwordHash);
        
        return query(client, sql, params)
            .map(updateResult -> {
                if (updateResult.rowCount() == 0) {
                    throw new com.csms.common.exceptions.NotFoundException("Member not found");
                }
                return null;
            });
    }
    
    public Future<Void> adjustCoin(SqlClient client, Long userId, String network, String token, Double amount, String type) {
        // 지갑 잔액 조정
        StringBuilder sql = new StringBuilder("""
            UPDATE user_wallets w
            SET balance = balance + :adjust_amount,
                updated_at = NOW()
            WHERE w.user_id = :user_id
            AND w.currency_id IN (
                SELECT id FROM currency c
                WHERE 1=1
            """);
        
        Map<String, Object> params = new HashMap<>();
        params.put("user_id", userId);
        
        // ADD면 양수, WITHDRAW면 음수
        double adjustAmount = "ADD".equals(type) ? amount : -amount;
        params.put("adjust_amount", adjustAmount);
        
        if (network != null && !network.isEmpty()) {
            sql.append(" AND c.chain = :network");
            params.put("network", network);
        }
        
        if (token != null && !token.isEmpty()) {
            sql.append(" AND c.code = :token");
            params.put("token", token);
        }
        
        sql.append(")");
        
        return query(client, sql.toString(), params)
            .map(updateResult -> {
                if (updateResult.rowCount() == 0) {
                    throw new com.csms.common.exceptions.NotFoundException("Wallet not found");
                }
                return null;
            });
    }
    
    public Future<Void> adjustKoriPoint(SqlClient client, Long userId, Double amount, String type) {
        // KORI 포인트 조정 (users 테이블에 kori_points 필드가 있다고 가정)
        StringBuilder sql = new StringBuilder("""
            UPDATE users
            SET kori_points = kori_points + :adjust_amount,
                updated_at = NOW()
            WHERE id = :user_id
            """);
        
        Map<String, Object> params = new HashMap<>();
        params.put("user_id", userId);
        
        // ADD면 양수, WITHDRAW면 음수
        double adjustAmount = "ADD".equals(type) ? amount : -amount;
        params.put("adjust_amount", adjustAmount);
        
        return query(client, sql.toString(), params)
            .map(updateResult -> {
                if (updateResult.rowCount() == 0) {
                    throw new com.csms.common.exceptions.NotFoundException("User not found");
                }
                return null;
            });
    }
    
    public Future<com.csms.admin.dto.WalletListDto> getMemberWallets(
        SqlClient client,
        Long memberId,
        String network,
        String token,
        Integer limit,
        Integer offset
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                w.id,
                c.chain as network,
                c.code as token,
                w.address,
                w.balance
            FROM user_wallets w
            JOIN currency c ON c.id = w.currency_id
            WHERE w.user_id = :user_id
            """);
        
        Map<String, Object> params = new HashMap<>();
        params.put("user_id", memberId);
        
        if (network != null && !network.isEmpty()) {
            sql.append(" AND c.chain = :network");
            params.put("network", network);
        }
        
        if (token != null && !token.isEmpty()) {
            sql.append(" AND c.code = :token");
            params.put("token", token);
        }
        
        // COUNT 쿼리
        String countSql = "SELECT COUNT(*) as total FROM (" + sql.toString() + ") as filtered";
        
        // 정렬 및 페이지네이션
        sql.append(" ORDER BY c.chain, c.code");
        sql.append(" LIMIT :limit OFFSET :offset");
        params.put("limit", limit);
        params.put("offset", offset);
        
        // COUNT 실행
        return query(client, countSql, params)
            .compose(countRows -> {
                Long total = 0L;
                if (countRows.size() > 0) {
                    total = getLong(countRows.iterator().next(), "total");
                }
                final Long finalTotal = total != null ? total : 0L;
                
                // 실제 데이터 조회
                return query(client, sql.toString(), params)
                    .map(rows -> {
                        List<com.csms.admin.dto.WalletListDto.WalletInfo> wallets = new ArrayList<>();
                        for (var row : rows) {
                            wallets.add(com.csms.admin.dto.WalletListDto.WalletInfo.builder()
                                .id(getLong(row, "id"))
                                .network(getString(row, "network"))
                                .token(getString(row, "token"))
                                .address(getString(row, "address"))
                                .balance(getDouble(row, "balance"))
                                .build());
                        }
                        return com.csms.admin.dto.WalletListDto.builder()
                            .wallets(wallets)
                            .total(finalTotal)
                            .limit(limit)
                            .offset(offset)
                            .build();
                    });
            });
    }
    
    // ========== Mining History 관련 메서드 ==========
    
    private final RowMapper<MiningHistoryDetailDto.MiningHistoryRecord> recordMapper = row -> {
        LocalDateTime createdAt = getLocalDateTime(row, "created_at");
        String date = createdAt != null ? createdAt.toLocalDate().toString() : null;
        String time = createdAt != null ? createdAt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null;
        Integer year = createdAt != null ? createdAt.getYear() : null;
        
        // 기기정보 (IP 주소)
        String deviceInfo = getString(row, "device_info");
        String ipAddress = getString(row, "ip_address");
        String deviceInfoStr = "";
        if (deviceInfo != null && ipAddress != null) {
            deviceInfoStr = deviceInfo + " / " + ipAddress;
        } else if (deviceInfo != null) {
            deviceInfoStr = deviceInfo;
        } else if (ipAddress != null) {
            deviceInfoStr = ipAddress;
        }
        
        return MiningHistoryDetailDto.MiningHistoryRecord.builder()
            .id(getLong(row, "id"))
            .year(year)
            .date(date)
            .time(time)
            .level(getInteger(row, "level"))
            .miningType(getString(row, "mining_type"))
            .miningEfficiency(getInteger(row, "mining_efficiency"))
            .invitationCode(getString(row, "invitation_code"))
            .teamMemberCount(getInteger(row, "team_member_count"))
            .miningAmount(getDouble(row, "mining_amount"))
            .referralRevenue(getDouble(row, "referral_revenue"))
            .totalMinedHoldings(getDouble(row, "total_mined_holdings"))
            .deviceInfo(deviceInfoStr)
            .build();
    };
    
    public Future<MiningHistoryDetailDto> getMiningHistory(
        SqlClient client,
        Long userId,
        Integer limit,
        Integer offset,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        // 사용자 기본 정보 조회
        String userSql = """
            SELECT 
                u.id,
                u.nickname,
                u.level
            FROM users u
            WHERE u.id = :user_id
            """;
        
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("user_id", userId);
        
        return query(client, userSql, userParams)
            .compose(userRows -> {
                if (userRows.size() == 0) {
                    return Future.failedFuture(new com.csms.common.exceptions.NotFoundException("User not found"));
                }
                
                var userRow = userRows.iterator().next();
                String userNickname = getString(userRow, "nickname");
                Integer userLevel = getInteger(userRow, "level");
                
                // 채굴 내역 조회
                StringBuilder sql = new StringBuilder();
                Map<String, Object> params = new HashMap<>();
                
                sql.append("""
                    SELECT 
                        mh.id,
                        mh.created_at,
                        u.level,
                        mh.type as mining_type,
                        COALESCE(mh.efficiency, 0) as mining_efficiency,
                        u.referral_code as invitation_code,
                        COALESCE(team_stats.team_member_count, 0) as team_member_count,
                        CASE 
                            WHEN mh.type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH') THEN mh.amount
                            ELSE 0
                        END as mining_amount,
                        CASE 
                            WHEN mh.type = 'REFERRAL_REWARD' THEN mh.amount
                            ELSE 0
                        END as referral_revenue,
                        COALESCE(
                            (SELECT SUM(amount) 
                             FROM mining_history mh2 
                             WHERE mh2.user_id = mh.user_id 
                             AND mh2.type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH', 'REFERRAL_REWARD')
                             AND mh2.created_at <= mh.created_at), 0
                        ) as total_mined_holdings,
                        mh.device_info,
                        mh.ip_address
                    FROM mining_history mh
                    INNER JOIN users u ON u.id = mh.user_id
                    LEFT JOIN (
                        SELECT referrer_id, COUNT(DISTINCT referred_id) as team_member_count
                        FROM referral_relations
                        WHERE status = 'ACTIVE' AND deleted_at IS NULL
                        GROUP BY referrer_id
                    ) team_stats ON team_stats.referrer_id = u.id
                    WHERE mh.user_id = :user_id
                    """);
                
                params.put("user_id", userId);
                
                // 날짜 범위 필터
                if (startDate != null && endDate != null) {
                    sql.append(" AND mh.created_at >= :start_date AND mh.created_at <= :end_date");
                    params.put("start_date", startDate);
                    params.put("end_date", endDate);
                }
                
                sql.append(" ORDER BY mh.created_at DESC");
                
                // 총 개수 조회
                StringBuilder countSql = new StringBuilder();
                countSql.append("SELECT COUNT(*) as total FROM mining_history mh WHERE mh.user_id = :user_id");
                if (startDate != null && endDate != null) {
                    countSql.append(" AND mh.created_at >= :start_date AND mh.created_at <= :end_date");
                }
                
                return query(client, countSql.toString(), params)
                    .compose(countRows -> {
                        Integer total = 0;
                        if (countRows.size() > 0) {
                            total = getInteger(countRows.iterator().next(), "total");
                        }
                        final Integer finalTotal = total;
                        
                        // 데이터 조회
                        sql.append(" LIMIT :limit OFFSET :offset");
                        params.put("limit", limit);
                        params.put("offset", offset);
                        
                        return query(client, sql.toString(), params)
                            .compose(rows -> {
                                List<MiningHistoryDetailDto.MiningHistoryRecord> records = new ArrayList<>();
                                for (var row : rows) {
                                    records.add(recordMapper.map(row));
                                }
                                
                                // 요약 통계 조회
                                return getMiningHistorySummary(client, userId, startDate, endDate)
                                    .map(summary -> MiningHistoryDetailDto.builder()
                                        .userId(userId)
                                        .userNickname(userNickname)
                                        .userLevel(userLevel)
                                        .records(records)
                                        .total(finalTotal)
                                        .limit(limit)
                                        .offset(offset)
                                        .summary(summary)
                                        .build());
                            });
                    });
            });
    }
    
    private Future<MiningHistoryDetailDto.Summary> getMiningHistorySummary(SqlClient client, Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        sql.append("""
            SELECT 
                COALESCE(SUM(CASE WHEN type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH') THEN amount ELSE 0 END), 0) as total_mining_amount,
                COALESCE(SUM(CASE WHEN type = 'REFERRAL_REWARD' THEN amount ELSE 0 END), 0) as total_referral_revenue,
                COALESCE(SUM(CASE WHEN type IN ('BROADCAST_PROGRESS', 'BROADCAST_WATCH', 'REFERRAL_REWARD') THEN amount ELSE 0 END), 0) as total_mined_holdings
            FROM mining_history
            WHERE user_id = :user_id
            """);
        
        params.put("user_id", userId);
        
        if (startDate != null && endDate != null) {
            sql.append(" AND created_at >= :start_date AND created_at <= :end_date");
            params.put("start_date", startDate);
            params.put("end_date", endDate);
        }
        
        return query(client, sql.toString(), params)
            .map(rows -> {
                if (rows.size() == 0) {
                    return MiningHistoryDetailDto.Summary.builder()
                        .totalMiningAmount(0.0)
                        .totalReferralRevenue(0.0)
                        .totalMinedHoldings(0.0)
                        .build();
                }
                
                var row = rows.iterator().next();
                return MiningHistoryDetailDto.Summary.builder()
                    .totalMiningAmount(getDouble(row, "total_mining_amount"))
                    .totalReferralRevenue(getDouble(row, "total_referral_revenue"))
                    .totalMinedHoldings(getDouble(row, "total_mined_holdings"))
                    .build();
            });
    }
}

