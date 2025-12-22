package com.csms.currency.service;

import com.csms.currency.entities.Currency;
import com.csms.currency.repository.CurrencyRepository;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class CurrencyServiceTest {
    
    @Mock
    private PgPool pool;
    
    @Mock
    private CurrencyRepository currencyRepository;
    
    private CurrencyService currencyService;
    
    @BeforeEach
    void setUp() {
        currencyService = new CurrencyService(pool, currencyRepository);
    }
    
    @Test
    void testGetActiveCurrencies_Success(VertxTestContext context) {
        // Given
        List<Currency> currencies = Arrays.asList(
            Currency.builder()
                .id(1)
                .code("TRX")
                .name("TRON")
                .chain("TRON")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
            Currency.builder()
                .id(2)
                .code("USDT")
                .name("Tether USD")
                .chain("TRON")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
        );
        
        when(currencyRepository.getActiveCurrencies(any())).thenReturn(Future.succeededFuture(currencies));
        
        // When
        currencyService.getActiveCurrencies()
            .onSuccess(result -> {
                // Then
                assertThat(result).isNotNull();
                assertThat(result.size()).isEqualTo(2);
                assertThat(result.get(0).getCode()).isEqualTo("TRX");
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
    
    @Test
    void testGetActiveCurrencies_Empty(VertxTestContext context) {
        // Given
        when(currencyRepository.getActiveCurrencies(any())).thenReturn(Future.succeededFuture(List.of()));
        
        // When
        currencyService.getActiveCurrencies()
            .onSuccess(result -> {
                // Then
                assertThat(result).isNotNull();
                assertThat(result.size()).isEqualTo(0);
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
}

