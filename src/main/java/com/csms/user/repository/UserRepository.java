package com.csms.user.repository;

import com.csms.common.repository.BaseRepository;
import com.csms.common.database.RowMapper;
import com.csms.user.entities.User;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class UserRepository extends BaseRepository {
    
    private final RowMapper<User> userMapper = row -> User.builder()
        .id(getLong(row, "id"))
        .loginId(getString(row, "login_id"))
        .passwordHash(getString(row, "password_hash"))
        .email(getString(row, "email"))
        .nickname(getString(row, "nickname"))
        .name(getString(row, "name"))
        .gender(getString(row, "gender"))
        .phone(getString(row, "phone"))
        .referralCode(getString(row, "referral_code"))
        .role(getInteger(row, "role"))
        .status(getString(row, "status"))
        .createdAt(getLocalDateTime(row, "created_at"))
        .updatedAt(getLocalDateTime(row, "updated_at"))
        .deletedAt(getLocalDateTime(row, "deleted_at"))
        .build();
    
    public Future<User> createUser(SqlClient client, com.csms.user.dto.CreateUserDto dto) {
        String sql = """
            INSERT INTO users (login_id, password_hash, status, created_at, updated_at)
            VALUES (:login_id, :password_hash, 'ACTIVE', NOW(), NOW())
            RETURNING *
            """;
        
        return query(client, sql, dto.toMap())
            .map(rows -> fetchOne(userMapper, rows))
            .onFailure(throwable -> log.error("사용자 생성 실패: {}", throwable.getMessage()));
    }
    
    public Future<User> getUserByLoginId(SqlClient client, String loginId) {
        String sql = """
            SELECT * FROM users 
            WHERE login_id = :login_id 
            AND deleted_at IS NULL
            """;
        
        return query(client, sql, Collections.singletonMap("login_id", loginId))
            .map(rows -> fetchOne(userMapper, rows))
            .onFailure(throwable -> log.error("사용자 조회 실패 - loginId: {}", loginId));
    }
    
    public Future<User> getUserById(SqlClient client, Long id) {
        String sql = """
            SELECT * FROM users 
            WHERE id = :id 
            AND deleted_at IS NULL
            """;
        
        return query(client, sql, Collections.singletonMap("id", id))
            .map(rows -> fetchOne(userMapper, rows))
            .onFailure(throwable -> log.error("사용자 조회 실패 - id: {}", id));
    }
}

