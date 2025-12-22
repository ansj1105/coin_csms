package com.csms.admin.repository;

import com.csms.admin.dto.MemberDetailDto;
import com.csms.admin.dto.MemberListDto;
import com.csms.common.repository.BaseRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AdminMemberRepository extends BaseRepository {
    
    private final PgPool pool;
    
    public AdminMemberRepository(PgPool pool) {
        this.pool = pool;
    }
    
    public Future<MemberListDto> getMembers(
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
        return query(pool, countSql, params)
            .compose(countRows -> {
                Long total = 0L;
                if (countRows.size() > 0) {
                    total = getLong(countRows.iterator().next(), "total");
                }
                final Long finalTotal = total != null ? total : 0L;
                
                // 실제 데이터 조회
                return query(pool, sql.toString(), params)
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
    
    public Future<MemberDetailDto> getMemberDetail(Long memberId) {
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
        
        return query(pool, sql, params)
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
                    FROM wallets w
                    JOIN currency c ON c.id = w.currency_id
                    WHERE w.user_id = :user_id
                    ORDER BY c.code
                    """;
                
                Map<String, Object> walletParams = new HashMap<>();
                walletParams.put("user_id", memberId);
                
                return query(pool, walletSql, walletParams)
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
    
    public Future<Void> updateSanctionStatus(Long memberId, String sanctionStatus) {
        String sql = """
            UPDATE users
            SET sanction_status = :sanction_status,
                updated_at = NOW()
            WHERE id = :member_id
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("member_id", memberId);
        params.put("sanction_status", sanctionStatus);
        
        return query(pool, sql, params)
            .map(updateResult -> {
                if (updateResult.rowCount() == 0) {
                    throw new com.csms.common.exceptions.NotFoundException("Member not found");
                }
                return null;
            });
    }
    
    public Future<Void> updateMember(Long memberId, String phone, String email, Integer level) {
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
        
        return query(pool, sql.toString(), params)
            .map(updateResult -> {
                if (updateResult.rowCount() == 0) {
                    throw new com.csms.common.exceptions.NotFoundException("Member not found");
                }
                return null;
            });
    }
    
    public Future<Void> resetTransactionPassword(Long memberId) {
        // 거래 비밀번호를 "0000"으로 초기화
        // users 테이블에 transaction_password 필드가 있다고 가정
        // "0000"을 BCrypt로 해시화
        String passwordHash = org.mindrot.jbcrypt.BCrypt.hashpw("0000", org.mindrot.jbcrypt.BCrypt.gensalt());
        
        String sql = """
            UPDATE users
            SET transaction_password = :password_hash,
                updated_at = NOW()
            WHERE id = :member_id
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("member_id", memberId);
        params.put("password_hash", passwordHash);
        
        return query(pool, sql, params)
            .map(updateResult -> {
                if (updateResult.rowCount() == 0) {
                    throw new com.csms.common.exceptions.NotFoundException("Member not found");
                }
                return null;
            });
    }
    
    public Future<Void> adjustCoin(Long userId, String network, String token, Double amount, String type) {
        // 지갑 잔액 조정
        StringBuilder sql = new StringBuilder("""
            UPDATE wallets w
            SET balance = balance + :adjust_amount,
                updated_at = NOW()
            FROM currency c
            WHERE w.currency_id = c.id
            AND w.user_id = :user_id
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
        
        return query(pool, sql.toString(), params)
            .map(updateResult -> {
                if (updateResult.rowCount() == 0) {
                    throw new com.csms.common.exceptions.NotFoundException("Wallet not found");
                }
                return null;
            });
    }
    
    public Future<Void> adjustKoriPoint(Long userId, Double amount, String type) {
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
        
        return query(pool, sql.toString(), params)
            .map(updateResult -> {
                if (updateResult.rowCount() == 0) {
                    throw new com.csms.common.exceptions.NotFoundException("User not found");
                }
                return null;
            });
    }
    
    public Future<com.csms.admin.dto.WalletListDto> getMemberWallets(
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
            FROM wallets w
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
        return query(pool, countSql, params)
            .compose(countRows -> {
                Long total = 0L;
                if (countRows.size() > 0) {
                    total = getLong(countRows.iterator().next(), "total");
                }
                final Long finalTotal = total != null ? total : 0L;
                
                // 실제 데이터 조회
                return query(pool, sql.toString(), params)
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
}

