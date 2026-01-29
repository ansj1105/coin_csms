package com.csms.admin.handler;

import com.csms.admin.dto.*;
import com.csms.common.HandlerTestBase;
import com.csms.common.dto.ApiResponse;
import com.csms.common.enums.UserRole;
import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AdminAirdropHandlerTest extends HandlerTestBase {

    public AdminAirdropHandlerTest() {
        super("/api/v1/admin/airdrop");
    }

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        super.init(testContext);
    }

    @Test
    void testGetPhases_Success(VertxTestContext context) {
        String token = getAccessTokenOfAdmin(3L); // admin_user (ID:3)
        reqGet(getUrl("/phases"))
            .bearerTokenAuthentication(token)
            .send()
            .onSuccess(res -> {
                expectSuccess(res);
                TypeReference<ApiResponse<AirdropPhaseListDto>> typeRef = 
                    new TypeReference<ApiResponse<AirdropPhaseListDto>>() {};
                AirdropPhaseListDto result = expectSuccessAndGetResponse(res, typeRef);
                assertNotNull(result);
                assertNotNull(result.getPhases());
                context.completeNow();
            })
            .onFailure(context::failNow);
    }

    @Test
    void testGetPhases_WithFilters(VertxTestContext context) {
        String token = getAccessTokenOfAdmin(3L);
        reqGet(getUrl("/phases?limit=10&offset=0&phase=1&status=PROCESSING"))
            .bearerTokenAuthentication(token)
            .send()
            .onSuccess(res -> {
                expectSuccess(res);
                TypeReference<ApiResponse<AirdropPhaseListDto>> typeRef = 
                    new TypeReference<ApiResponse<AirdropPhaseListDto>>() {};
                AirdropPhaseListDto result = expectSuccessAndGetResponse(res, typeRef);
                assertNotNull(result);
                context.completeNow();
            })
            .onFailure(context::failNow);
    }

    @Test
    void testGetPhaseById_Success(VertxTestContext context) {
        // 먼저 Phase를 생성
        CreateAirdropPhaseRequestDto createRequest = new CreateAirdropPhaseRequestDto();
        createRequest.setUserId(1L);
        createRequest.setPhase(1);
        createRequest.setAmount(new BigDecimal("1000.0"));
        createRequest.setDaysRemaining(30);

        String token = getAccessTokenOfAdmin(3L);
        reqPost(getUrl("/phases"))
            .bearerTokenAuthentication(token)
            .sendJson(JsonObject.mapFrom(createRequest))
            .onSuccess(createRes -> {
                expectSuccess(createRes);
                TypeReference<ApiResponse<AirdropPhaseDto>> createTypeRef = 
                    new TypeReference<ApiResponse<AirdropPhaseDto>>() {};
                AirdropPhaseDto createdPhase = expectSuccessAndGetResponse(createRes, createTypeRef);
                
                // 생성된 Phase 조회
                reqGet(getUrl("/phases/" + createdPhase.getId()))
                    .bearerTokenAuthentication(token)
                    .send()
                    .onSuccess(getRes -> {
                        expectSuccess(getRes);
                        TypeReference<ApiResponse<AirdropPhaseDto>> typeRef = 
                            new TypeReference<ApiResponse<AirdropPhaseDto>>() {};
                        AirdropPhaseDto result = expectSuccessAndGetResponse(getRes, typeRef);
                        assertNotNull(result);
                        assertEquals(createdPhase.getId(), result.getId());
                        context.completeNow();
                    })
                    .onFailure(context::failNow);
            })
            .onFailure(context::failNow);
    }

    @Test
    void testGetPhaseById_NotFound(VertxTestContext context) {
        String token = getAccessTokenOfAdmin(3L);
        reqGet(getUrl("/phases/999999"))
            .bearerTokenAuthentication(token)
            .send()
            .onSuccess(res -> {
                expectError(res, 404);
                context.completeNow();
            })
            .onFailure(context::failNow);
    }

    @Test
    void testCreatePhase_Success(VertxTestContext context) {
        CreateAirdropPhaseRequestDto request = new CreateAirdropPhaseRequestDto();
        request.setUserId(1L);
        request.setPhase(1);
        request.setAmount(new BigDecimal("1000.0"));
        request.setDaysRemaining(30);

        reqPost(getUrl("/phases"))
            .sendJson(JsonObject.mapFrom(request))
            .onSuccess(res -> {
                expectSuccess(res);
                TypeReference<ApiResponse<AirdropPhaseDto>> typeRef = 
                    new TypeReference<ApiResponse<AirdropPhaseDto>>() {};
                AirdropPhaseDto result = expectSuccessAndGetResponse(res, typeRef);
                assertNotNull(result);
                assertEquals(request.getUserId(), result.getUserId());
                assertEquals(request.getPhase(), result.getPhase());
                assertEquals(0, request.getAmount().compareTo(result.getAmount()));
                context.completeNow();
            })
            .onFailure(context::failNow);
    }

    @Test
    void testCreatePhase_WithUnlockDate(VertxTestContext context) {
        CreateAirdropPhaseRequestDto request = new CreateAirdropPhaseRequestDto();
        request.setUserId(1L);
        request.setPhase(2);
        request.setAmount(new BigDecimal("2000.0"));
        request.setUnlockDate(LocalDateTime.now().plusDays(60));

        String token = getAccessTokenOfAdmin(3L);
        reqPost(getUrl("/phases"))
            .bearerTokenAuthentication(token)
            .sendJson(JsonObject.mapFrom(request))
            .onSuccess(res -> {
                expectSuccess(res);
                TypeReference<ApiResponse<AirdropPhaseDto>> typeRef = 
                    new TypeReference<ApiResponse<AirdropPhaseDto>>() {};
                AirdropPhaseDto result = expectSuccessAndGetResponse(res, typeRef);
                assertNotNull(result);
                assertNotNull(result.getUnlockDate());
                context.completeNow();
            })
            .onFailure(context::failNow);
    }

    @Test
    void testCreatePhase_InvalidPhase(VertxTestContext context) {
        CreateAirdropPhaseRequestDto request = new CreateAirdropPhaseRequestDto();
        request.setUserId(1L);
        request.setPhase(6); // Invalid: should be 1-5
        request.setAmount(new BigDecimal("1000.0"));
        request.setDaysRemaining(30);

        String token = getAccessTokenOfAdmin(3L);
        reqPost(getUrl("/phases"))
            .bearerTokenAuthentication(token)
            .sendJson(JsonObject.mapFrom(request))
            .onSuccess(res -> {
                expectError(res, 400);
                context.completeNow();
            })
            .onFailure(context::failNow);
    }

    @Test
    void testUpdatePhase_Success(VertxTestContext context) {
        // 먼저 Phase 생성
        CreateAirdropPhaseRequestDto createRequest = new CreateAirdropPhaseRequestDto();
        createRequest.setUserId(1L);
        createRequest.setPhase(1);
        createRequest.setAmount(new BigDecimal("1000.0"));
        createRequest.setDaysRemaining(30);

        String token = getAccessTokenOfAdmin(3L);
        reqPost(getUrl("/phases"))
            .bearerTokenAuthentication(token)
            .sendJson(JsonObject.mapFrom(createRequest))
            .onSuccess(createRes -> {
                expectSuccess(createRes);
                TypeReference<ApiResponse<AirdropPhaseDto>> createTypeRef = 
                    new TypeReference<ApiResponse<AirdropPhaseDto>>() {};
                AirdropPhaseDto createdPhase = expectSuccessAndGetResponse(createRes, createTypeRef);
                
                // Phase 수정
                UpdateAirdropPhaseRequestDto updateRequest = new UpdateAirdropPhaseRequestDto();
                updateRequest.setAmount(new BigDecimal("2000.0"));
                updateRequest.setDaysRemaining(60);
                updateRequest.setStatus("PROCESSING");
                reqPatch(getUrl("/phases/" + createdPhase.getId()))
                    .bearerTokenAuthentication(token)
                    .sendJson(JsonObject.mapFrom(updateRequest))
                    .onSuccess(updateRes -> {
                        expectSuccess(updateRes);
                        TypeReference<ApiResponse<AirdropPhaseDto>> typeRef = 
                            new TypeReference<ApiResponse<AirdropPhaseDto>>() {};
                        AirdropPhaseDto result = expectSuccessAndGetResponse(updateRes, typeRef);
                        assertNotNull(result);
                        assertEquals(0, new BigDecimal("2000.0").compareTo(result.getAmount()));
                        context.completeNow();
                    })
                    .onFailure(context::failNow);
            })
            .onFailure(context::failNow);
    }

    @Test
    void testDeletePhase_Success(VertxTestContext context) {
        // 먼저 Phase 생성
        CreateAirdropPhaseRequestDto createRequest = new CreateAirdropPhaseRequestDto();
        createRequest.setUserId(1L);
        createRequest.setPhase(1);
        createRequest.setAmount(new BigDecimal("1000.0"));
        createRequest.setDaysRemaining(30);

        String token = getAccessTokenOfAdmin(3L);
        reqPost(getUrl("/phases"))
            .bearerTokenAuthentication(token)
            .sendJson(JsonObject.mapFrom(createRequest))
            .onSuccess(createRes -> {
                expectSuccess(createRes);
                TypeReference<ApiResponse<AirdropPhaseDto>> createTypeRef = 
                    new TypeReference<ApiResponse<AirdropPhaseDto>>() {};
                AirdropPhaseDto createdPhase = expectSuccessAndGetResponse(createRes, createTypeRef);
                
                // Phase 삭제
                reqDelete(getUrl("/phases/" + createdPhase.getId()))
                    .bearerTokenAuthentication(token)
                    .send()
                    .onSuccess(deleteRes -> {
                        expectSuccess(deleteRes);
                        
                        // 삭제 후 조회 시 404
                        reqGet(getUrl("/phases/" + createdPhase.getId()))
                            .bearerTokenAuthentication(token)
                            .send()
                            .onSuccess(getRes -> {
                                expectError(getRes, 404);
                                context.completeNow();
                            })
                            .onFailure(context::failNow);
                    })
                    .onFailure(context::failNow);
            })
            .onFailure(context::failNow);
    }

    @Test
    void testGetTransfers_Success(VertxTestContext context) {
        String token = getAccessTokenOfAdmin(3L);
        reqGet(getUrl("/transfers"))
            .bearerTokenAuthentication(token)
            .send()
            .onSuccess(res -> {
                expectSuccess(res);
                TypeReference<ApiResponse<AirdropTransferListDto>> typeRef = 
                    new TypeReference<ApiResponse<AirdropTransferListDto>>() {};
                AirdropTransferListDto result = expectSuccessAndGetResponse(res, typeRef);
                assertNotNull(result);
                assertNotNull(result.getTransfers());
                context.completeNow();
            })
            .onFailure(context::failNow);
    }

    @Test
    void testGetTransfers_WithFilters(VertxTestContext context) {
        String token = getAccessTokenOfAdmin(3L);
        reqGet(getUrl("/transfers?limit=10&offset=0&status=COMPLETED"))
            .bearerTokenAuthentication(token)
            .send()
            .onSuccess(res -> {
                expectSuccess(res);
                TypeReference<ApiResponse<AirdropTransferListDto>> typeRef = 
                    new TypeReference<ApiResponse<AirdropTransferListDto>>() {};
                AirdropTransferListDto result = expectSuccessAndGetResponse(res, typeRef);
                assertNotNull(result);
                context.completeNow();
            })
            .onFailure(context::failNow);
    }
}
