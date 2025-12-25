package com.csms.common.service;

import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlClient;

import java.util.List;

public abstract class BaseService {
    protected final PgPool pool;
    protected final SqlClient client;
    
    public BaseService(PgPool pool) {
        this.pool = pool;
        this.client = pool; // PgPool은 SqlClient를 구현함
    }
    
    /**
     * 여러 Future<Void>를 실행하고 결과를 Future<Void>로 변환
     * @param futures 실행할 Future 목록
     * @return 모든 Future가 완료되면 성공하는 Future<Void>
     */
    protected Future<Void> allVoid(List<Future<Void>> futures) {
        return Future.all(futures).map(ignored -> null);
    }
    
    /**
     * 성공한 Future<Void>를 반환
     * @return 성공한 Future<Void>
     */
    protected Future<Void> succeededVoid() {
        return Future.succeededFuture();
    }
}

