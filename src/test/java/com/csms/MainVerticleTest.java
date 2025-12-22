package com.csms;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MainVerticle 테스트
 * 단위 테스트: 실제 서버 배포 없이 Verticle 인스턴스 생성만 확인
 */
@ExtendWith(VertxExtension.class)
class MainVerticleTest {
    
    @Test
    void verticle_instantiation(Vertx vertx, VertxTestContext testContext) {
        // Given
        MainVerticle verticle = new MainVerticle();
        
        // Then
        assertThat(verticle).isNotNull();
        testContext.completeNow();
    }
    
    @Test
    void verticle_with_config_instantiation(Vertx vertx, VertxTestContext testContext) {
        // Given
        MainVerticle verticle = new MainVerticle("src/test/resources/config.json", "test");
        
        // Then
        assertThat(verticle).isNotNull();
        testContext.completeNow();
    }
}

