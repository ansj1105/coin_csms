# Windows에서 Docker Desktop 설치 가이드

## 1. 시스템 요구사항 확인

- **Windows 10 64-bit**: Pro, Enterprise, 또는 Education (빌드 19041 이상)
- **Windows 11 64-bit**: Home 또는 Pro
- **WSL 2 기능 활성화 필요**
- **가상화 기능 활성화 필요** (BIOS에서)

## 2. WSL 2 설치

### 방법 1: PowerShell에서 자동 설치 (권장)

관리자 권한으로 PowerShell을 열고 다음 명령 실행:

```powershell
wsl --install
```

### 방법 2: 수동 설치

1. **Windows 기능 활성화**:
   - `Win + R` → `optionalfeatures` 입력
   - "Linux용 Windows 하위 시스템" 체크
   - "가상 머신 플랫폼" 체크
   - 재부팅

2. **WSL 2 업데이트**:
   ```powershell
   wsl --update
   wsl --set-default-version 2
   ```

## 3. Docker Desktop 다운로드 및 설치

### 다운로드

1. 공식 웹사이트 방문: https://www.docker.com/products/docker-desktop
2. "Download for Windows" 클릭
3. `Docker Desktop Installer.exe` 다운로드

### 설치

1. 다운로드한 `Docker Desktop Installer.exe` 실행
2. 설치 마법사 따라하기:
   - "Use WSL 2 instead of Hyper-V" 옵션 체크 (권장)
   - 설치 경로 선택 (기본값 사용 권장)
3. 설치 완료 후 **재부팅** (필요한 경우)

## 4. Docker Desktop 시작

1. 시작 메뉴에서 "Docker Desktop" 실행
2. 시스템 트레이에 고래 아이콘(🐳)이 나타날 때까지 대기
3. 고래 아이콘이 안정적으로 표시되면 Docker가 준비된 것입니다

## 5. 설치 확인

PowerShell에서 다음 명령 실행:

```powershell
# Docker 버전 확인
docker --version

# Docker Compose 버전 확인
docker compose version

# 또는 (구버전)
docker-compose --version

# 테스트 실행
docker run hello-world
```

성공적으로 실행되면 설치가 완료된 것입니다!

## 6. 문제 해결

### Docker 명령을 찾을 수 없는 경우

1. **Docker Desktop이 실행 중인지 확인**
   - 시스템 트레이에 고래 아이콘이 있는지 확인
   - 없으면 Docker Desktop을 시작

2. **PATH 환경 변수 확인**
   - Docker Desktop 설치 시 자동으로 PATH에 추가됩니다
   - 재부팅 후에도 문제가 있으면 Docker Desktop을 재시작

3. **PowerShell 재시작**
   - Docker Desktop 설치 후 PowerShell을 재시작해야 할 수 있습니다

### WSL 2 관련 오류

```powershell
# WSL 2 상태 확인
wsl --status

# WSL 2로 업데이트
wsl --set-default-version 2

# WSL 2 커널 업데이트
wsl --update
```

### 가상화 오류

BIOS에서 다음 설정 활성화:
- **Intel**: Virtualization Technology (VT-x)
- **AMD**: AMD-V
- **Hyper-V** (Windows Pro 이상)

## 7. Redis Cluster 시작

Docker Desktop이 실행 중이면 다음 명령으로 Redis Cluster를 시작할 수 있습니다:

```powershell
# PowerShell 스크립트 사용 (권장)
.\scripts\start-redis-cluster.ps1

# 또는 배치 파일 사용
scripts\start-redis-cluster.bat
```

## 참고 자료

- Docker Desktop 공식 문서: https://docs.docker.com/desktop/install/windows-install/
- WSL 2 설치 가이드: https://learn.microsoft.com/ko-kr/windows/wsl/install
- Docker Desktop 문제 해결: https://docs.docker.com/desktop/troubleshoot/

