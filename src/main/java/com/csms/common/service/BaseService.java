package com.csms.common.service;

import io.vertx.pgclient.PgPool;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseService {
    protected final PgPool pool;
}

