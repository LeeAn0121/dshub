# DSHub 설정 가이드

## 1. Google Cloud Console 설정

### 1-1. 프로젝트 생성
1. [Google Cloud Console](https://console.cloud.google.com) 접속
2. 새 프로젝트 생성: `DSHub`

### 1-2. Google Sheets API 활성화
1. "API 및 서비스" > "라이브러리"
2. "Google Sheets API" 검색 후 **사용 설정**

### 1-3. OAuth 2.0 클라이언트 ID 생성
1. "API 및 서비스" > "사용자 인증 정보"
2. "사용자 인증 정보 만들기" > "OAuth 클라이언트 ID"
3. 애플리케이션 유형: **Android**
4. 패키지 이름: `com.dshub.app`
5. SHA-1 인증서 지문 입력 (아래 명령어로 확인):

```bash
# 디버그 키스토어 SHA-1 확인
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### 1-4. OAuth 동의 화면 설정
1. "OAuth 동의 화면"에서 앱 정보 입력
2. 테스트 사용자에 사용할 Google 계정 추가

---

## 2. Google Sheets 설정

### 2-1. 시트 생성
1. [Google Sheets](https://sheets.google.com)에서 새 스프레드시트 생성
2. 시트 이름을 `기술지원`으로 설정 (또는 원하는 이름)
3. 앱이 자동으로 헤더를 생성하므로 별도 설정 불필요

### 2-2. 스프레드시트 ID 확인
- URL: `https://docs.google.com/spreadsheets/d/[여기가 ID]/edit`
- 해당 ID를 앱 설정 화면에 입력

---

## 3. 앱 설정

1. 앱 실행 후 Google 계정으로 로그인
2. 우측 상단 설정 아이콘 클릭
3. 스프레드시트 ID 입력 및 저장
4. 목록 화면에서 새로고침

---

## 4. Google Play Store 비공개 출시 (내부 테스트)

### 4-1. APK/AAB 서명 빌드
```bash
./gradlew bundleRelease
```

### 4-2. Play Console 설정
1. [Google Play Console](https://play.google.com/console) 접속
2. 앱 생성
3. "내부 테스트" 트랙 선택
4. AAB 파일 업로드: `app/build/outputs/bundle/release/app-release.aab`
5. 테스터 이메일 추가

---

## 시트 컬럼 구조 (A1~K1)

| 열 | 필드 |
|---|---|
| A | 등록일 |
| B | 진행단계 |
| C | 요청일 |
| D | 예정일 |
| E | 완료일 |
| F | 구분 |
| G | 현장명 |
| H | 담당 |
| I | 요청사항 |
| J | 처리내역 |
| K | 비고 |
