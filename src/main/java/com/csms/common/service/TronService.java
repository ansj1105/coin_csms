package com.csms.common.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;

/**
 * foxya-tron-service 호출을 위한 서비스
 * 참고: fox_coin 프로젝트의 WalletService.java
 */
@Slf4j
public class TronService {
    
    private final WebClient webClient;
    private final String tronServiceUrl;
    
    public TronService(WebClient webClient, String tronServiceUrl) {
        this.webClient = webClient;
        this.tronServiceUrl = tronServiceUrl;
    }
    
    /**
     * 지갑 생성
     * @param userId 사용자 ID
     * @param currencyCode 통화 코드 (예: TRX, USDT, KORI 등)
     * @return 생성된 지갑 주소
     */
    public Future<String> createWallet(Long userId, String currencyCode) {
        if (tronServiceUrl == null || tronServiceUrl.isEmpty()) {
            String errorMsg = "foxya-tron-service URL이 설정되지 않았습니다. config.json의 tron.serviceUrl을 확인해주세요.";
            log.error(errorMsg);
            return Future.failedFuture(errorMsg);
        }
        
        String url = tronServiceUrl + "/api/wallet/create";
        JsonObject requestBody = new JsonObject()
            .put("userId", userId)
            .put("currencyCode", currencyCode);
        
        log.info("foxya-tron-service 호출 - URL: {}, userId: {}, currencyCode: {}", url, userId, currencyCode);
        
        return webClient.postAbs(url)
            .sendJsonObject(requestBody)
            .compose(response -> {
                if (response.statusCode() == 200) {
                    JsonObject body = response.bodyAsJsonObject();
                    if (body != null && body.containsKey("address")) {
                        String address = body.getString("address");
                        log.info("지갑 생성 성공 - currencyCode: {}, address: {}", currencyCode, address);
                        return Future.succeededFuture(address);
                    } else {
                        String errorMsg = "foxya-tron-service 응답에 address가 없습니다. 응답: " + (body != null ? body.encode() : "null");
                        log.error(errorMsg);
                        return Future.failedFuture(errorMsg);
                    }
                } else {
                    String responseBody = "";
                    try {
                        if (response.body() != null) {
                            responseBody = response.bodyAsString();
                        }
                    } catch (Exception e) {
                        // 응답 본문 읽기 실패 시 무시
                    }
                    String errorMessage = String.format("foxya-tron-service 호출 실패 (status: %d, currencyCode: %s, response: %s)", 
                        response.statusCode(), currencyCode, responseBody);
                    log.error(errorMessage);
                    return Future.failedFuture(errorMessage);
                }
            })
            .recover(throwable -> {
                String errorMsg = String.format("foxya-tron-service 네트워크 오류 - currencyCode: %s, error: %s. 지갑 생성을 실패했습니다.", 
                    currencyCode, throwable.getMessage());
                log.error(errorMsg);
                return Future.failedFuture(errorMsg);
            });
    }
    
    /**
     * 코인 전송 (외부 전송)
     * 참고: fox_coin 프로젝트에서는 이벤트 기반으로 처리하지만, 
     * 직접 API 호출이 필요한 경우를 위해 구현
     * 
     * @param fromAddress 송신 주소
     * @param toAddress 수신 주소
     * @param amount 전송 금액
     * @param currencyCode 통화 코드
     * @return 트랜잭션 해시
     */
    public Future<String> transfer(String fromAddress, String toAddress, String amount, String currencyCode) {
        if (tronServiceUrl == null || tronServiceUrl.isEmpty()) {
            return Future.failedFuture("foxya-tron-service URL이 설정되지 않았습니다.");
        }
        
        String url = tronServiceUrl + "/api/transfer";
        JsonObject requestBody = new JsonObject()
            .put("fromAddress", fromAddress)
            .put("toAddress", toAddress)
            .put("amount", amount)
            .put("currencyCode", currencyCode);
        
        log.info("foxya-tron-service 전송 호출 - URL: {}, fromAddress: {}, toAddress: {}, amount: {}, currencyCode: {}", 
            url, fromAddress, toAddress, amount, currencyCode);
        
        return webClient.postAbs(url)
            .sendJsonObject(requestBody)
            .compose(response -> {
                if (response.statusCode() == 200) {
                    JsonObject body = response.bodyAsJsonObject();
                    if (body != null && body.containsKey("txHash")) {
                        String txHash = body.getString("txHash");
                        log.info("전송 성공 - currencyCode: {}, txHash: {}", currencyCode, txHash);
                        return Future.succeededFuture(txHash);
                    } else {
                        String errorMsg = "foxya-tron-service 응답에 txHash가 없습니다. 응답: " + (body != null ? body.encode() : "null");
                        log.error(errorMsg);
                        return Future.failedFuture(errorMsg);
                    }
                } else {
                    String responseBody = "";
                    try {
                        if (response.body() != null) {
                            responseBody = response.bodyAsString();
                        }
                    } catch (Exception e) {
                        // 응답 본문 읽기 실패 시 무시
                    }
                    String errorMessage = String.format("foxya-tron-service 전송 호출 실패 (status: %d, currencyCode: %s, response: %s)", 
                        response.statusCode(), currencyCode, responseBody);
                    log.error(errorMessage);
                    return Future.failedFuture(errorMessage);
                }
            })
            .recover(throwable -> {
                log.error("foxya-tron-service 전송 네트워크 오류 - currencyCode: {}, error: {}", currencyCode, throwable.getMessage());
                return Future.failedFuture("foxya-tron-service 연결 실패: " + throwable.getMessage());
            });
    }
    
    /**
     * 잔액 조회
     * @param address 지갑 주소
     * @param currencyCode 통화 코드 (선택사항, null이면 기본값 TRC-20)
     * @return 잔액 (문자열로 반환, BigDecimal로 변환 필요)
     */
    public Future<String> getBalance(String address, String currencyCode) {
        if (tronServiceUrl == null || tronServiceUrl.isEmpty()) {
            return Future.failedFuture("foxya-tron-service URL이 설정되지 않았습니다.");
        }
        
        // GET 방식 + Query Parameter 사용 (보고서 스펙에 맞춤)
        StringBuilder urlBuilder = new StringBuilder(tronServiceUrl + "/api/balance");
        urlBuilder.append("?address=").append(io.vertx.core.http.HttpServerRequest.encodeURIComponent(address));
        if (currencyCode != null && !currencyCode.isEmpty()) {
            urlBuilder.append("&currencyCode=").append(io.vertx.core.http.HttpServerRequest.encodeURIComponent(currencyCode));
        }
        String url = urlBuilder.toString();
        
        log.info("foxya-tron-service 잔액 조회 호출 - URL: {}, address: {}, currencyCode: {}", url, address, currencyCode);
        
        return webClient.getAbs(url)
            .send()
            .compose(response -> {
                if (response.statusCode() == 200) {
                    JsonObject body = response.bodyAsJsonObject();
                    if (body != null && body.containsKey("balance")) {
                        String balance = body.getString("balance");
                        log.info("잔액 조회 성공 - currencyCode: {}, address: {}, balance: {}", currencyCode, address, balance);
                        return Future.succeededFuture(balance);
                    } else {
                        String errorMsg = "foxya-tron-service 응답에 balance가 없습니다. 응답: " + (body != null ? body.encode() : "null");
                        log.error(errorMsg);
                        return Future.failedFuture(errorMsg);
                    }
                } else {
                    String responseBody = "";
                    try {
                        if (response.body() != null) {
                            responseBody = response.bodyAsString();
                        }
                    } catch (Exception e) {
                        // 응답 본문 읽기 실패 시 무시
                    }
                    String errorMessage = String.format("foxya-tron-service 잔액 조회 실패 (status: %d, currencyCode: %s, response: %s)", 
                        response.statusCode(), currencyCode, responseBody);
                    log.error(errorMessage);
                    return Future.failedFuture(errorMessage);
                }
            })
            .recover(throwable -> {
                log.error("foxya-tron-service 잔액 조회 네트워크 오류 - currencyCode: {}, error: {}", currencyCode, throwable.getMessage());
                return Future.failedFuture("foxya-tron-service 연결 실패: " + throwable.getMessage());
            });
    }
    
    /**
     * 트랜잭션 조회
     * 보고서 스펙: GET /api/tx/:txHash (TRON), GET /api/tx/btc/:txHash (BTC), GET /api/tx/eth/:txHash (ETH)
     * @param txHash 트랜잭션 해시
     * @param currencyCode 통화 코드 (TRX, BTC, ETH 등)
     * @return 트랜잭션 정보 (JSON)
     */
    public Future<JsonObject> getTransaction(String txHash, String currencyCode) {
        if (tronServiceUrl == null || tronServiceUrl.isEmpty()) {
            return Future.failedFuture("foxya-tron-service URL이 설정되지 않았습니다.");
        }
        
        // 통화 코드에 따라 엔드포인트 결정
        String endpoint;
        if ("BTC".equalsIgnoreCase(currencyCode)) {
            endpoint = "/api/tx/btc/" + txHash;
        } else if ("ETH".equalsIgnoreCase(currencyCode)) {
            endpoint = "/api/tx/eth/" + txHash;
        } else {
            // TRON (기본값)
            endpoint = "/api/tx/" + txHash;
        }
        
        String url = tronServiceUrl + endpoint;
        
        log.info("foxya-tron-service 트랜잭션 조회 호출 - URL: {}, txHash: {}, currencyCode: {}", url, txHash, currencyCode);
        
        return webClient.getAbs(url)
            .send()
            .compose(response -> {
                if (response.statusCode() == 200) {
                    JsonObject body = response.bodyAsJsonObject();
                    log.info("트랜잭션 조회 성공 - currencyCode: {}, txHash: {}", currencyCode, txHash);
                    return Future.succeededFuture(body);
                } else {
                    String responseBody = "";
                    try {
                        if (response.body() != null) {
                            responseBody = response.bodyAsString();
                        }
                    } catch (Exception e) {
                        // 응답 본문 읽기 실패 시 무시
                    }
                    String errorMessage = String.format("foxya-tron-service 트랜잭션 조회 실패 (status: %d, currencyCode: %s, txHash: %s, response: %s)", 
                        response.statusCode(), currencyCode, txHash, responseBody);
                    log.error(errorMessage);
                    return Future.failedFuture(errorMessage);
                }
            })
            .recover(throwable -> {
                log.error("foxya-tron-service 트랜잭션 조회 네트워크 오류 - currencyCode: {}, txHash: {}, error: {}", 
                    currencyCode, txHash, throwable.getMessage());
                return Future.failedFuture("foxya-tron-service 연결 실패: " + throwable.getMessage());
            });
    }
    
}

