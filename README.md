# VitalsMobile

Android-приложение пациента для цифровой медицинской экосистемы **Vitals**.  
Покрывает тот же пациентский сценарий, что и раздел `/patient` в [VitalsWeb](../VitalsWeb): ИИ-триаж, маршрут лечения, запись к врачу, чат консультации, анализы, рецепты (с QR), документы и профиль.

Стек и UI выровнены под макеты Figma и поведение web-клиента; бэкенд — API Gateway Vitals.

---

## Стек

| Область | Технологии |
|--------|------------|
| Язык / UI | Kotlin, Jetpack Compose, Material 3 |
| Архитектура | Feature-пакеты + MVVM (`ViewModel` + `StateFlow`) |
| DI | Hilt |
| Навигация | Navigation Compose |
| Сеть | Retrofit 2, OkHttp, Kotlinx Serialization |
| Сессия | DataStore Preferences (JWT + profile ids) |
| minSdk / targetSdk | 26 / 35 |
| JDK | **17** (AGP 8.7 + Gradle не работают на JVM 23–25) |

---

## Требования

- Android Studio (рекомендуется) с JDK **17** или **21**
- Эмулятор Android или устройство
- Запущенный **Vitals API Gateway** (по умолчанию порт `5080`)

В `gradle.properties` можно указать путь к JDK:

```properties
org.gradle.java.home=/path/to/jdk-17
```

---

## Быстрый старт

1. Клонировать репозиторий и открыть папку `VitalsMobile` в Android Studio.
2. Дождаться Gradle Sync.
3. При необходимости задать URL API в `local.properties` (файл не коммитится):

```properties
## Эмулятор → хост-машина
API_BASE_URL=http://10.0.2.2:5080/

## Физическое устройство в той же сети (IP компьютера)
# API_BASE_URL=http://192.168.1.10:5080/
```

Если `API_BASE_URL` не задан, используется `http://10.0.2.2:5080/` (стандартный адрес хоста с эмулятора).

4. Запустить конфигурацию `app` (Run ▶).

Сборка из CLI (при корректном JDK):

```bash
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

---

## Роль и вход

Приложение **только для пациента** (в отличие от web, где есть и врач).

- Экран `Auth` — вкладки «Вход» / «Регистрация».
- После логина в DataStore сохраняются `accessToken`, `refreshToken`, `publicId`, `patientId`, `role`.
- OkHttp: `AuthInterceptor` добавляет Bearer-токен; `TokenAuthenticator` обновляет сессию через refresh.
- `AppViewModel` / `VitalsApp` переключают экран загрузки → auth → основной scaffold с нижней навигацией.

---

## Навигация

Корневые вкладки нижней панели (`NavRoutes.bottomNavRoutes`):

| Вкладка | Маршрут | Назначение |
|---------|---------|------------|
| Путь | `path` | Маршрут лечения (4 шага) |
| ИИ | `triage_chat` | Чат ИИ-ассистента / триаж |
| Врачи | `doctors` | Список, карточка, запись, чат |
| Профиль | `profile` | Данные пациента, выход |
| Ещё | `more` | Документы, консультации, анализы, поддержка… |

Дополнительные экраны (примеры):

- `doctors/{id}`, `doctors/{id}/book`, `doctors/{id}/chat`
- `consultations`, `consultations/{sessionId}`
- `labs`, `prescriptions/{prescriptionId}`
- `medical_overview`, `documents`, `triage_result/{sessionId}`

Маршруты собраны в `core/navigation/NavRoutes.kt`, граф — в `NavGraph.kt`, shell — в `VitalsApp.kt`.

---

## Архитектура пакетов

```
com.vitals.mobile
├── MainActivity / VitalsApplication
├── di/                     # Hilt: NetworkModule, ApiModule
├── core/
│   ├── data/               # Retrofit API + Repository по доменам
│   │   ├── auth, users, doctors, triage, consultations
│   │   ├── laborders, prescriptions, medicalrecords
│   │   ├── routing, notifications, common
│   ├── network/            # ApiConfig, interceptors, JSON helpers
│   ├── session/            # SessionManager (DataStore)
│   ├── designsystem/       # Тема, компоненты (ChatBubble, chips, buttons…)
│   └── navigation/         # Routes, NavHost, AppViewModel
└── feature/
    ├── auth, path, triage, aiassistant
    ├── doctors, consultations, labs, prescriptions
    ├── documents, overview, profile, treatment, misc
    └── common/             # ChatBody, UiChatMessage
```

Паттерн экрана:

1. `*Screen` (Compose) подписывается на `uiState`.
2. `*ViewModel` вызывает репозитории, обновляет `MutableStateFlow`.
3. Репозиторий ходит в Retrofit API и нормализует «мягкие» ответы gateway (как на web).

Дизайн-система: `VitalsTheme` + компоненты в `core/designsystem/components` — переиспользуются между фичами.

---

## Функциональность (пациент)

### Маршрут («Путь»)

Четыре шага, логика как на web Home:

1. ИИ-триаж  
2. Консультация врача  
3. Анализы  
4. Получение рецепта  

Текущий шаг — первый незавершённый; если все done — остаётся шаг 4.  
Кнопка продолжения ведёт в триаж / врачей / консультации / анализы / сводку.

### ИИ-триаж

- Чат с сессией triage API.
- Быстрые фразы, завершение триажа → экран результата / маршрут.
- Общий UI: `feature/common/ChatBody`.

### Врачи и запись

- Список и карточка врача.
- Запись: выбор даты и слота (chips).
- Чат с врачом по активной консультации.

### Консультации

- Список: активные / чаты / завершённые.
- Детали сессии + чат.
- **Polling сообщений каждые 2 с** (как web) в `DoctorChatViewModel` и `ConsultationDetailViewModel`.
- Автопрокрутка вниз при новых сообщениях; у своих — время и «Доставлено» / «Прочитано».

### Анализы и рецепты

- Направления из lab-orders, протокола и рекомендаций маршрута.
- Рецепты кликабельны → детали, отправка в аптеку, отмена, **QR** (`qrCodeBase64Png` с бэкенда).

### Прочее

- Медицинская сводка (анамнез, диагнозы, лекарства, недавние анализы).
- Документы, уведомления, поддержка, вызов на дом (экраны в `feature/misc` и соседних пакетах).
- Редактирование профиля.

---

## Backend / API

- Базовый URL: `BuildConfig.API_BASE_URL` ← `local.properties` / дефолт эмулятора.
- Клиент: Retrofit + kotlinx.serialization (`ignoreUnknownKeys`, lenient).
- JWT refresh через отдельный `@Named("plain")` OkHttpClient (без рекурсии authenticator).
- Для dev разрешён cleartext HTTP (`network_security_config.xml`).
- Ответы gateway часто «мягкие» — парсинг через гибкие хелперы (`asArrayFlexible` и т.п.), как в VitalsWeb.

Связанные репозитории экосистемы:

- `VitalsBackend` — сервисы и API Gateway  
- `VitalsWeb` — эталон UX/логики пациента  

---

## UI и IME

- `enableEdgeToEdge` + `imePadding()` на `NavHost`.
- При открытой клавиатуре нижняя навигация скрывается, чтобы поле ввода не перекрывалось и не было двойного отступа.
- В чатах вторичные CTA («Завершить триаж») и quick-replies прячутся, пока открыта клавиатура.

---

## Конфигурация сети (шпаргалка)

| Где крутится приложение | Куда смотрит API |
|-------------------------|------------------|
| Android Emulator | `http://10.0.2.2:5080/` |
| Устройство в LAN | `http://<IP-хоста>:5080/` |
| Прод / HTTPS | `https://api.example.com/` |

URL **обязан** заканчиваться на `/` (требование Retrofit `baseUrl`).

---

## Структура документации в репозитории

| Файл | Содержание |
|------|------------|
| [README.md](./README.md) | Этот обзор, запуск, фичи |
| [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) | Слои, сессия, чат, DI |
| [docs/SCREENS.md](./docs/SCREENS.md) | Карта экранов и ViewModel |

---

## Известные ограничения

- Только роль пациента (нет doctor UI).
- Чат без WebSocket — polling ~2 с.
- Release minify выключен (`isMinifyEnabled = false`).
- Сборка ожидается через Android Studio / JDK 17–21; системный Java 25 часто ломает Gradle.
