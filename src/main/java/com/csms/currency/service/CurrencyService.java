package com.csms.currency.service;

import com.csms.common.service.BaseService;
import com.csms.currency.entities.Currency;
import com.csms.currency.repository.CurrencyRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;

import java.util.List;

public class CurrencyService extends BaseService {
    
    private final CurrencyRepository currencyRepository;
    
    public CurrencyService(PgPool pool, CurrencyRepository currencyRepository) {
        super(pool);
        this.currencyRepository = currencyRepository;
    }
    
    public Future<List<Currency>> getActiveCurrencies() {
        return currencyRepository.getActiveCurrencies(pool);
    }
}

