package com.csms.admin.repository;

import com.csms.admin.entities.Admin;
import com.csms.common.database.RowMapper;
import com.csms.common.repository.BaseRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlClient;

import java.util.Collections;

public class AdminRepository extends BaseRepository {
    
    private final RowMapper<Admin> adminMapper = row -> Admin.builder()
        .id(getLong(row, "id"))
        .loginId(getString(row, "login_id"))
        .passwordHash(getString(row, "password_hash"))
        .role(getInteger(row, "role"))
        .status(getString(row, "status"))
        .createdAt(getLocalDateTime(row, "created_at"))
        .updatedAt(getLocalDateTime(row, "updated_at"))
        .build();
    
    public Future<Admin> getAdminByLoginId(SqlClient client, String loginId) {
        String sql = """
            SELECT * FROM users 
            WHERE login_id = :login_id 
            AND role IN (2, 3)
            AND status = 'ACTIVE'
            """;
        
        return query(client, sql, Collections.singletonMap("login_id", loginId))
            .map(rows -> fetchOne(adminMapper, rows))
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to get admin by loginId: " + loginId, throwable);
            });
    }
    
    public Future<Admin> getAdminById(SqlClient client, Long id) {
        String sql = """
            SELECT * FROM users 
            WHERE id = :id 
            AND role IN (2, 3)
            AND status = 'ACTIVE'
            """;
        
        return query(client, sql, Collections.singletonMap("id", id))
            .map(rows -> fetchOne(adminMapper, rows))
            .onFailure(throwable -> {
                throw new com.csms.common.exceptions.InternalServerException("Failed to get admin by id: " + id, throwable);
            });
    }
}

