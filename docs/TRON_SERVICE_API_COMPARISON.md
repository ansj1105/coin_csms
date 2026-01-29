# foxya-tron-service API 스펙 비교 문서

## 📋 현재 구현 vs 보고서 스펙 비교

### 1. 지갑 생성 API ✅

**보고서 스펙:**
- `POST /api/wallet/create`
- Request: `{ "userId": 123, "currencyCode": "TRX" }`
- Response: `{ "address": "T..." }`

**현재 구현:**
- ✅ `POST /api/wallet/create` - 일치
- ✅ Request Body: `{ "userId": 123, "currencyCode": "TRX" }` - 일치
- ✅ Response: `{ "address": "T..." }` - 일치

**결과**: ✅ **완벽히 일치**

---

### 2. 잔액 조회 API ❌

**보고서 스펙:**
- `GET /api/balance?address=T...&currencyCode=USDT`
- `GET /api/balance?address=T...` (기본값: TRC-20 토큰)
- Query Parameter 방식

**현재 구현:**
- ❌ `POST /api/balance` - **HTTP 메서드 불일치**
- ❌ Request Body: `{ "address": "...", "currencyCode": "..." }` - **요청 방식 불일치**

**결과**: ❌ **HTTP 메서드와 요청 방식이 불일치 (GET + Query Parameter 필요)**

---

### 3. 코인 전송 API ✅

**보고서 스펙:**
- `POST /api/transfer`
- Request: `{ "fromAddress": "...", "toAddress": "...", "amount": "...", "currencyCode": "..." }`
- Response: `{ "txHash": "..." }`

**현재 구현:**
- ✅ `POST /api/transfer` - 일치
- ✅ Request Body: `{ "fromAddress": "...", "toAddress": "...", "amount": "...", "currencyCode": "..." }` - 일치
- ✅ Response: `{ "txHash": "..." }` - 일치

**결과**: ✅ **완벽히 일치**

---

### 4. 트랜잭션 조회 API ❌

**보고서 스펙:**
- `GET /api/tx/:txHash` (TRON)
- `GET /api/tx/btc/:txHash` (BTC)
- `GET /api/tx/eth/:txHash` (ETH)

**현재 구현:**
- ❌ **구현되지 않음**

**결과**: ❌ **구현 필요**

---

## 🔧 수정 필요 사항

### 1. 잔액 조회 API 수정 (우선순위: 높음)

**현재 코드:**
```java
// POST 방식으로 구현됨
String url = tronServiceUrl + "/api/balance";
return webClient.postAbs(url)
    .sendJsonObject(requestBody)
```

**수정 필요:**
```java
// GET 방식 + Query Parameter로 변경
String url = tronServiceUrl + "/api/balance?address=" + address + "&currencyCode=" + currencyCode;
return webClient.getAbs(url)
    .send()
```

### 2. 트랜잭션 조회 API 추가 (우선순위: 중간)

**추가 필요:**
- `getTransaction(String txHash, String currencyCode)` 메서드
- TRON: `GET /api/tx/:txHash`
- BTC: `GET /api/tx/btc/:txHash`
- ETH: `GET /api/tx/eth/:txHash`

---

## 📝 요약

| API | 보고서 스펙 | 현재 구현 | 상태 |
|-----|------------|----------|------|
| 지갑 생성 | `POST /api/wallet/create` | `POST /api/wallet/create` | ✅ 일치 |
| 잔액 조회 | `GET /api/balance?address=...&currencyCode=...` | `GET /api/balance?address=...&currencyCode=...` | ✅ 일치 (수정 완료) |
| 코인 전송 | `POST /api/transfer` | `POST /api/transfer` | ✅ 일치 |
| 트랜잭션 조회 | `GET /api/tx/:txHash` (TRON/BTC/ETH) | `GET /api/tx/:txHash` (통화별 분기) | ✅ 일치 (추가 완료) |

## ✅ 수정 완료 사항

### 1. 잔액 조회 API 수정 완료 ✅
- **변경 전**: `POST /api/balance` (Request Body)
- **변경 후**: `GET /api/balance?address=...&currencyCode=...` (Query Parameter)
- **파일**: `src/main/java/com/csms/common/service/TronService.java` - `getBalance()` 메서드

### 2. 트랜잭션 조회 API 추가 완료 ✅
- **추가**: `getTransaction(String txHash, String currencyCode)` 메서드
- **엔드포인트**:
  - TRON: `GET /api/tx/:txHash`
  - BTC: `GET /api/tx/btc/:txHash`
  - ETH: `GET /api/tx/eth/:txHash`
- **파일**: `src/main/java/com/csms/common/service/TronService.java`

