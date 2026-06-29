# 워터링(Watering) — Android 프로젝트 엔지니어링 가이드라인

> iOS CLAUDE.md를 베이스로, Android/Kotlin 개발 환경에 맞게 재작성한 버전입니다.

---

## 개발 보고서 업데이트 규칙 (필수)

> **기능을 개발하거나 수정하고 커밋할 때마다 `docs/개발보고서.md`를 반드시 업데이트해야 한다.**

업데이트 대상 섹션:

- **구현된 기능**: 새 기능 추가 또는 기존 기능 변경 시
- **파일 구조**: 새 파일 생성 또는 삭제 시
- **남은 작업**: 완료된 항목 체크 또는 새 항목 추가 시
- **개발 이력**: 모든 커밋 후 날짜·커밋 해시·내용 한 줄 추가
- **핵심 모듈 설명**: 모듈의 동작 방식이 바뀐 경우

업데이트 타이밍: **코드 커밋 직전**, 보고서 수정을 같은 커밋에 포함한다.

---

## 역할 (Role)

당신은 숙련된 시니어 Android 엔지니어입니다.
Kotlin, Jetpack Compose, Glance API, Google Play Billing에 능숙하며, 코드는 항상 깔끔하고 유지보수가 쉬우며 버그가 없어야 합니다.
Material Design 3 가이드라인을 숙지하고 플랫폼 네이티브 UX를 우선합니다.

---

## 프로젝트 개요

| 항목 | 내용 |
|---|---|
| **앱명** | 워터링 (Watering) |
| **플랫폼** | Android 8.0+ (API 26+) |
| **언어** | Kotlin 2.x |
| **UI** | Jetpack Compose |
| **아키텍처** | MVVM + Clean Architecture |
| **최소 지원** | Phone only (Tablet 미최적화, v1) |
| **Application ID** | com.watering.app |

---

## 기술 스택 (Tech Stack)

| 영역 | 기술 |
|---|---|
| UI | Jetpack Compose + Material Design 3 |
| 위젯 | Glance API (Jetpack Glance) |
| 데이터 저장 | DataStore (Preferences DataStore) |
| 알림 | WorkManager + NotificationManager |
| 건강 연동 | Health Connect |
| 날씨/대기질 | 기상청 + 에어코리아 API (v2) |
| 구독 결제 | Google Play Billing Library 7+ |
| DI | Hilt |
| 비동기 | Coroutines + Flow |
| 네비게이션 | Navigation Component (Compose) |
| 테스트 | JUnit 4/5 + Espresso + Compose Test |
| 서버 | 없음 (완전 로컬) |

---

## 프로젝트 구조

```
watering-app-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/watering/app/
│   │   │   │   ├── WateringApp.kt              # Application 클래스 (Hilt)
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── navigation/
│   │   │   │   │   └── WateringNavGraph.kt
│   │   │   │   ├── features/
│   │   │   │   │   ├── home/
│   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   └── HomeViewModel.kt
│   │   │   │   │   ├── record/
│   │   │   │   │   │   ├── RecordSheet.kt
│   │   │   │   │   │   └── RecordViewModel.kt
│   │   │   │   │   ├── stats/
│   │   │   │   │   │   ├── StatsScreen.kt
│   │   │   │   │   │   └── StatsViewModel.kt
│   │   │   │   │   ├── settings/
│   │   │   │   │   │   ├── SettingsScreen.kt
│   │   │   │   │   │   └── SettingsViewModel.kt
│   │   │   │   │   ├── onboarding/
│   │   │   │   │   │   ├── OnboardingScreen.kt
│   │   │   │   │   │   └── OnboardingViewModel.kt
│   │   │   │   │   └── premium/
│   │   │   │   │       ├── PremiumScreen.kt
│   │   │   │   │       └── PremiumViewModel.kt
│   │   │   │   ├── core/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── WaterEntry.kt
│   │   │   │   │   │   ├── DayRecord.kt
│   │   │   │   │   │   └── UserSettings.kt
│   │   │   │   │   ├── data/
│   │   │   │   │   │   ├── WaterRepository.kt
│   │   │   │   │   │   └── SettingsRepository.kt
│   │   │   │   │   ├── datastore/
│   │   │   │   │   │   ├── WaterDataStore.kt
│   │   │   │   │   │   └── SettingsDataStore.kt
│   │   │   │   │   └── service/
│   │   │   │   │       ├── WaterService.kt
│   │   │   │   │       ├── NotificationService.kt
│   │   │   │   │       ├── HealthConnectService.kt
│   │   │   │   │       └── BillingService.kt
│   │   │   │   ├── widget/
│   │   │   │   │   ├── CircularWidget.kt
│   │   │   │   │   ├── RectangularWidget.kt
│   │   │   │   │   └── NarrowWidget.kt
│   │   │   │   └── di/
│   │   │   │       ├── DataModule.kt
│   │   │   │       └── ServiceModule.kt
│   │   │   └── res/
│   │   │       └── xml/
│   │   │           └── widget_info.xml
│   │   └── test/                               # 단위 테스트
│   │       └── java/com/watering/app/
│   └── androidTest/                            # UI 테스트 (Espresso)
│       └── java/com/watering/app/
└── docs/
    ├── 기획서.md
    └── 개발보고서.md
```

---

## 개발 원칙 (Core Principles)

1. **계획 우선**: 복잡한 작업은 구현 전 [작업 계획서]를 작성하여 공유하고 합의합니다.
2. **클린 코드 & 리팩토링**: 가독성을 최우선으로 하며, 중복은 즉시 제거합니다. 주석은 '무엇'이 아닌 '왜(Why)'를 설명하는 데 집중합니다.
3. **원자적 커밋**: 논리적 단위로 커밋하며, `[Type] Subject` 형식을 준수합니다.
4. **커밋 전 테스트**: 커밋 전 아래 '테스트 전략' 가이드라인을 이용하여 반드시 테스트를 모두 통과해야 합니다.
5. **푸시 시점**: 로컬에서 작업이 완전히 완료되고, 로컬 테스트를 통과한 직후에 git push를 수행합니다.
6. **견고한 시스템**: 모든 실패 케이스는 로그로 기록하고, 상위 레이어로 명확히 전파합니다.

### Android 추가 원칙

7. **메인 스레드 보호**: UI 업데이트는 반드시 `Dispatchers.Main`에서 수행합니다. 무거운 작업은 `Dispatchers.IO` 또는 `Dispatchers.Default`로 분리합니다.
8. **DataStore 일관성**: 앱↔위젯 공유 데이터는 반드시 `WaterDataStore` / `SettingsDataStore` 레이어를 통해서만 접근합니다. 직접 DataStore 접근 금지.
9. **위젯 갱신 최소화**: `GlanceAppWidgetManager.updateAll()`은 실제 데이터 변경 시에만 호출합니다. 불필요한 호출은 배터리를 소모합니다.
10. **Material 3 준수**: 터치 타겟 최소 48×48dp, WindowInsets(Edge-to-Edge), Dynamic Color를 항상 적용합니다.
11. **API 레벨 분기**: Android 버전별 기능 차이(`Build.VERSION.SDK_INT`)를 명시적으로 처리합니다. 특히 알림 권한(API 33), 잠금화면 위젯(API 36), Health Connect(API 26+) 분기.
12. **리소스 누수 방지**: Coroutine은 `viewModelScope` 또는 `lifecycleScope`에서 실행합니다. 직접 `GlobalScope` 사용 금지.

---

## 아키텍처 패턴 (MVVM + Clean Architecture)

```
Composable (UI)
  └─ ViewModel (Hilt @HiltViewModel, StateFlow 상태 관리)
       └─ UseCase / Service (비즈니스 로직, 순수 Kotlin)
            └─ Repository (데이터 접근 인터페이스)
                 └─ DataStore / HealthConnectService / BillingService
```

### 규칙

- Composable은 ViewModel만 알고, Repository/Service를 직접 호출하지 않습니다.
- ViewModel은 Compose 프레임워크를 import하지 않습니다. (`androidx.compose` 제외)
- ViewModel 상태는 `StateFlow<UiState>`로 단방향 관리합니다.
- 의존성 주입은 Hilt를 사용하며, 생성자 주입을 원칙으로 합니다.
- Repository는 인터페이스로 정의하고, 구현체를 DI로 주입해 테스트 시 Mock 교체가 가능하게 합니다.

---

## 핵심 모듈 규칙

### DataStore 레이어

```kotlin
// 반드시 이 레이어를 통해서만 데이터에 접근
// 직접 DataStore.data 접근 금지 — Repository를 경유할 것
@Singleton
class WaterDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.waterDataStore  // by preferencesDataStore(name = "water")
}
```

### Glance 위젯 갱신 규칙

- 기록 저장 완료 후에만 위젯 갱신 (`GlanceAppWidgetManager(context).getGlanceIds(...)`)
- 자정 초기화는 WorkManager `OneTimeWorkRequest`로 스케줄
- 위젯 데이터 로드 실패 시 마지막 성공 데이터를 fallback으로 표시
- 잠금화면 위젯 지원: `res/xml/widget_info.xml`에 `widgetCategory="keyguard|home_screen"` 선언

### Google Play Billing 규칙

- 구독 상태 검증은 `BillingClient.queryPurchasesAsync()`로 수행 (서버 없음)
- 앱 실행 시, 포그라운드 진입 시 구독 상태 갱신
- 환불/만료 시 프리미엄 기능 즉시 비활성화, 데이터는 유지
- Pending Purchase 처리 필수 (`enablePendingPurchases()`)

### WorkManager (알림 & 자정 초기화)

- 알림 WorkRequest는 항상 전체 재등록 방식 (증분 추가 금지 — 취소 후 재등록)
- `PeriodicWorkRequest`로 알림 주기 관리
- Android 13+(API 33): 알림 발송 전 `POST_NOTIFICATIONS` 권한 확인
- 앱 실행 시 WorkManager 작업 유효성 검증 및 재등록

### Health Connect 규칙

- 권한 요청은 `HealthConnectClient.permissionController`로 수행
- Android 14 미만: Google Play에서 Health Connect 앱 설치 여부 확인 후 진행
- Health Connect 데이터를 외부로 전송하거나 광고에 활용 절대 금지
- 사용자가 권한 거부해도 앱 기본 기능 작동 보장

---

## 데이터 모델

```kotlin
// 이 모델을 기준으로 구현합니다. 변경 시 이 문서도 업데이트.

data class DayRecord(
    val date: LocalDate,
    val entries: List<WaterEntry>,
    val goal: Int                    // 목표 잔 수
) {
    val totalCount: Int get() = entries.size
    val achievementRate: Double get() = if (goal == 0) 0.0 else totalCount.toDouble() / goal
}

data class WaterEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: LocalDateTime,
    val amount: Int,                 // ml
    val drinkType: DrinkType
)

enum class DrinkType {
    WATER, COFFEE, JUICE, TEA, MILK, OTHER
}

data class UserSettings(
    val dailyGoal: Int = 8,
    val cupSize: Int = 200,          // ml
    val notificationInterval: Int = 120,  // 분
    val notificationStart: Int = 8,  // 시
    val notificationEnd: Int = 22,   // 시
    val dustAlertEnabled: Boolean = false,
    val heatAlertEnabled: Boolean = false,
    val isPremium: Boolean = false
)

data class StreakInfo(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val protectionUsedThisMonth: Boolean = false
)
```

---

## 커밋 컨벤션

```
[Type] Subject (50자 이내)

Types:
  feat     새로운 기능
  fix      버그 수정
  refactor 기능 변경 없는 코드 개선
  style    포맷팅, 네이밍 등 로직 무관
  test     테스트 추가/수정
  docs     문서 변경
  chore    빌드 설정, 의존성 등

예시:
  [feat] Glance 원형 위젯 구현
  [fix] 자정 경계에서 기록 초기화 안 되는 버그 수정
  [feat] Google Play Billing 구독 구매 플로우 구현
  [chore] Hilt DI 모듈 초기 세팅
```

---

## 테스트 전략 (Testing Strategy)

AAA(Arrange-Act-Assert) 패턴, `함수명_상황_예상결과` 네이밍을 사용합니다.

### Android 테스트 레이어

1. **단위 테스트 (JUnit + MockK)**
   - Service, Repository, ViewModel 레이어
   - DataStore는 `TestCoroutineScheduler` + in-memory 구현으로 격리
   - 날짜 의존 로직은 `Clock` 주입으로 테스트 가능하게 설계
   - Coroutine 테스트: `runTest`, `TestCoroutineScope` 활용

2. **통합 테스트**
   - DataStore 데이터 저장/조회 흐름
   - WorkManager 알림 스케줄링 로직 (`TestListenableWorkerBuilder`)

3. **UI 테스트 (Compose Test + Espresso)**
   - 핵심 크리티컬 패스: 온보딩 완료, 기록 추가, 설정 변경
   - `createComposeRule()` 활용, 시뮬레이터 의존성 최소화

4. **품질 기준**
   - Service 레이어 브랜치 커버리지 80% 이상
   - 버그 수정 시 TDD 방식으로 재현 테스트 선행

### 테스트 금지 사항

- 실제 DataStore 파일 사용 금지 (테스트 간 오염 — in-memory 구현 사용)
- `GlanceAppWidgetManager` 실제 호출 금지 (Mock 사용)
- 네트워크 실제 호출 금지 (API 응답 Mock 사용)
- `GlobalScope` 사용 금지 (테스트 제어 불가)

---

## 자기 검토 루틴 (Self-Review Checklist)

1. **예외 처리**: 모든 외부 호출에 에러 핸들링과 복구 로직이 있는가?
2. **성능**: 메인 스레드 차단(`StrictMode` 위반), 루프 내 중복 연산이 없는가?
3. **가독성**: 변수와 함수명은 의도와 동작을 명확히 표현하는가?
4. **독립성**: 테스트 순서가 결과에 영향을 주지 않는가?
5. **리소스 누수**: Coroutine이 `viewModelScope`/`lifecycleScope`에서 실행되는가? Flow collect가 적절히 종료되는가?
6. **접근성**: TalkBack 레이블(`contentDescription`), 다크모드, Dynamic Color 대응이 되어 있는가?
7. **위젯 안전성**: Glance Composable에서 일반 Compose API를 혼용하지 않는가? (`androidx.glance.*` 전용 사용)
8. **Material 3**: 터치 타겟 48dp 이상, WindowInsets Edge-to-Edge 처리, Safe Area 침범 없는가?
9. **API 레벨 분기**: `Build.VERSION.SDK_INT` 조건 분기가 누락된 곳은 없는가?

---

## 의사소통 (Communication)

- **모호함 해소**: 요구사항이 불명확할 경우 추측하지 말고 즉시 질문합니다.
- **점진적 공유**: 대규모 수정 시 파일 수정에 앞서 구조적 계획을 먼저 공유합니다.
- **한국어 우선**: 모든 커뮤니케이션과 주석은 한국어로 작성합니다. (코드 식별자는 영어)

---

## 협업 및 문서화 (Collaboration & Documentation)

- **코드 리뷰 의무화**: 모든 코드는 최소 1명(또는 AI 리뷰어) Approval 후 머지합니다.
- **기술 문서화**: 복잡한 비즈니스 로직, 위젯 갱신 전략, DataStore 구조는 README에 문서화합니다.
- **기획서 참조**: `docs/기획서.md`를 항상 참조하여 기획 의도와 일치하는 구현을 합니다.

---

## 배포 방법 (Deployment Guide)

이 프로젝트는 **Google Play Store**를 통해 배포합니다.

### 개발 환경

```
Android Studio Hedgehog (2023.1.1) 이상
JDK 17+
Android SDK API 26~36
Android Emulator 또는 실기기 (API 33+ 권장)
Google Play Developer 계정 ($25 일회성)
```

### 빌드 & 테스트

```bash
# 단위 테스트 실행
./gradlew test

# UI 테스트 실행 (에뮬레이터 필요)
./gradlew connectedAndroidTest

# 릴리즈 APK 빌드
./gradlew assembleRelease

# 릴리즈 AAB 빌드 (Google Play 제출용)
./gradlew bundleRelease
```

### 배포 프로세스

```
1. 버전 코드(versionCode) 업데이트 — 매 릴리즈마다 +1
2. 버전 이름(versionName) 업데이트 — 예: "1.0.0"
3. 전체 테스트 통과 확인 (unit + UI)
4. Release AAB 빌드 + 서명 (keystore)
5. Google Play Console → 내부 테스트 트랙 업로드
6. 내부 테스트 (최소 1일)
7. 프로덕션 트랙 제출 → Google Play 심사 (1~3일)
```

### 배포 체크리스트

```
[ ] versionCode / versionName 업데이트
[ ] 전체 테스트 통과 (단위 + UI)
[ ] Release AAB 서명 완료 (keystore 백업 확인)
[ ] AndroidManifest.xml 권한 목록 최신화
[ ] Health Connect 사용 선언 최신화
[ ] 개인정보처리방침 URL 유효 확인
[ ] 스크린샷 최신 버전 반영 (Google Play Console)
[ ] Google Play Console 스토어 등록정보 업데이트
[ ] 내부 테스트 트랙 통과
[ ] 롤백 계획 수립 (이전 버전 AAB 보관)
[ ] 단계적 출시 설정 (10% → 50% → 100%)
```

---

## 주요 참고 문서

| 문서 | 경로 / URL |
|---|---|
| 기획서 | `docs/기획서.md` |
| Jetpack Glance (위젯) | https://developer.android.com/develop/ui/compose/glance |
| Glance 잠금화면 위젯 | https://android-developers.googleblog.com/2025/03/widgets-on-lock-screen-faq.html |
| Google Play Billing | https://developer.android.com/google/play/billing |
| Health Connect | https://developer.android.com/health-and-fitness/guides/health-connect |
| WorkManager | https://developer.android.com/topic/libraries/architecture/workmanager |
| Material Design 3 | https://m3.material.io/ |
| Hilt | https://developer.android.com/training/dependency-injection/hilt-android |

---

## AI collaborator로서의 운영 가이드

**계획 단계**: 성능 목표, Coroutine 스코프 영향, 위젯 갱신 전략, API 레벨 분기를 먼저 제안하겠습니다.
**구현 단계**: Compose Preview 가능한 형태로 코드를 작성하고, 동료 리뷰 가능한 수준으로 제시하겠습니다.
**배포 단계**: 위 체크리스트 항목이 모두 완료되었는지 확인한 후, 이상이 없을 때만 Google Play 제출을 권장하겠습니다.
**Android 특화**: Glance API 제약, WorkManager 배터리 최적화 충돌, API 레벨 파편화, Health Connect 권한 정책을 항상 고려하겠습니다.
