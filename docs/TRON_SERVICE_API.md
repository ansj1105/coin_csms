# foxya-tron-service API 통합 문서

## 개요

`coin_csms` 프로젝트에서 `foxya-tron-service`를 호출하여 지갑 생성, 코인 전송, 잔액 조회 등의 기능을 수행합니다.

## 구현된 API

### 1. 지갑 생성 API

**엔드포인트**: `POST /api/wallet/create`

**요청 형식**:
```json
{
  "userId": 123,
  "currencyCode": "TRX"
}
```

**응답 형식**:
```json
{
  "address": "TXYZabc123def456..."
}
```

**구현 위치**:
- `src/main/java/com/csms/common/service/TronService.java` - `createWallet()` 메서드
- `src/main/java/com/csms/admin/repository/AdminMemberRepository.java` - `createWalletIfNotExists()` 메서드에서 사용

**사용 시나리오**:
- 관리자가 회원의 코인을 조정할 때 지갑이 없으면 자동으로 생성
- `AdminMemberService.adjustCoin()` 호출 시 지갑이 없으면 자동 생성

**설정**:
- `config.json`의 `tron.serviceUrl`에 `foxya-tron-service` URL 설정
- 예: `"serviceUrl": "http://foxya-tron-service:3000"`

## 구현되지 않은 API

### 1. 코인 전송 API

**엔드포인트**: `POST /api/transfer`

**요청 형식** (예상):
```json
{
  "fromAddress": "TXYZabc123...",
  "toAddress": "TXYZdef456...",
  "amount": "100.0",
  "currencyCode": "TRX"
}
```

**응답 형식** (예상):
```json
{
  "txHash": "0x1234567890abcdef..."
}
```

**현재 상태**:
- `TronService.transfer()` 메서드는 구현되어 있으나 실제로 사용되지 않음
- 외부 전송은 현재 이벤트 기반으로 처리됨 (Redis Stream)
- `foxya-tron-service`에 해당 API가 구현되어 있는지 확인 필요

**필요한 작업**:
1. `foxya-tron-service`에 `/api/transfer` 엔드포인트 구현 확인
2. 외부 전송 처리 로직에서 `TronService.transfer()` 호출 추가
3. 테스트 코드 작성

### 2. 잔액 조회 API

**엔드포인트**: `GET /api/balance?address=T...&currencyCode=USDT`

**요청 형식**:
- Query Parameter: `address` (필수), `currencyCode` (선택, 기본값: TRC-20)
- 예: `GET /api/balance?address=TXYZabc123...&currencyCode=USDT`
- 예: `GET /api/balance?address=TXYZabc123...` (currencyCode 생략 시 기본값)

**응답 형식**:
```json
{
  "balance": "1000.0"
}
```

**구현 위치**:
- `src/main/java/com/csms/common/service/TronService.java` - `getBalance()` 메서드
- **수정 완료**: POST → GET 방식으로 변경, Query Parameter 사용

**현재 상태**:
- ✅ `TronService.getBalance()` 메서드 구현 완료 (GET 방식)
- ⚠️ 실제로 사용되지 않음 (DB에서만 잔액 조회)
- 블록체인에서 실제 잔액을 조회하는 기능이 필요할 수 있음

**필요한 작업**:
1. 잔액 조회 시 블록체인에서 실제 잔액을 조회하는 기능 추가 (선택사항)
2. 테스트 코드 작성

### 3. 트랜잭션 조회 API

**엔드포인트**:
- TRON: `GET /api/tx/:txHash`
- BTC: `GET /api/tx/btc/:txHash`
- ETH: `GET /api/tx/eth/:txHash`

**요청 형식**:
- Path Parameter: `txHash` (트랜잭션 해시)
- Query Parameter: 없음

**응답 형식** (예상):
```json
{
  "txHash": "0x1234567890abcdef...",
  "status": "confirmed",
  "blockNumber": 12345,
  "confirmations": 20,
  ...
}
```

**구현 위치**:
- `src/main/java/com/csms/common/service/TronService.java` - `getTransaction()` 메서드
- **추가 완료**: 통화 코드에 따라 적절한 엔드포인트 호출

**현재 상태**:
- ✅ `TronService.getTransaction()` 메서드 구현 완료
- ⚠️ 실제로 사용되지 않음

**필요한 작업**:
1. 트랜잭션 조회 기능에서 `TronService.getTransaction()` 호출 추가
2. 테스트 코드 작성

## 설정

### config.json

```json
{
  "local": {
    "tron": {
      "serviceUrl": "http://foxya-tron-service:3000"
    }
  },
  "prod": {
    "tron": {
      "serviceUrl": "http://foxya-tron-service:3000"
    }
  }
}
```

## 참고

- `fox_coin` 프로젝트의 `WalletService.java`를 참고하여 구현
- `foxya-tron-service`는 Node.js 기반 서비스
- **중요**: 네트워크 오류 시 더미 주소를 생성하지 않습니다. 반드시 실패를 반환합니다. (상용 환경 안전성)

## 향후 개선 사항

1. **코인 전송 API 통합**: 외부 전송 처리 시 `TronService.transfer()` 호출
2. **잔액 조회 API 통합**: 블록체인에서 실제 잔액 조회 기능 추가
3. **에러 처리 개선**: 네트워크 오류 시 재시도 로직 추가
4. **로깅 개선**: API 호출 및 응답에 대한 상세 로깅
5. **테스트 코드 작성**: 각 API에 대한 통합 테스트 작성

