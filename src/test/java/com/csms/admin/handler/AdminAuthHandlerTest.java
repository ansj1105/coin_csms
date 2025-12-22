package com.csms.admin.handler;

import com.csms.admin.dto.AdminLoginDto;
import com.csms.admin.dto.AdminLoginResponseDto;
import com.csms.admin.service.AdminAuthService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class AdminAuthHandlerTest {
    
    @Mock
    private AdminAuthService adminAuthService;
    
    private AdminAuthHandler handler;
    private Vertx vertx;
    
    @BeforeEach
    void setUp(Vertx vertx) {
        this.vertx = vertx;
        handler = new AdminAuthHandler(vertx, adminAuthService);
    }
    
    @Test
    void testLoginEndpoint_Success(VertxTestContext context) {
        // Given
        AdminLoginResponseDto responseDto = AdminLoginResponseDto.builder()
            .accessToken("test-token")
            .adminId("admin1")
            .build();
        
        when(adminAuthService.login(any(), any())).thenReturn(Future.succeededFuture(responseDto));
        
        Router router = handler.getRouter();
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888)
            .compose(server -> {
                // When
                return WebClient.create(vertx)
                    .post(8888, "localhost", "/login")
                    .as(BodyCodec.jsonObject())
                    .sendJson(new JsonObject()
                        .put("id", "admin1")
                        .put("password", "password123"));
            })
            .onSuccess(response -> {
                // Then
                assertThat(response.statusCode()).isEqualTo(200);
                JsonObject body = response.body();
                assertThat(body.getString("status")).isEqualTo("OK");
                assertThat(body.getJsonObject("data").getString("adminId")).isEqualTo("admin1");
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
}

