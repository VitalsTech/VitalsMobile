# Архитектура VitalsMobile

## Обзор

Приложение построено как **одномодульный** Android-проект (`:app`) с разделением на:

- **`core`** — инфраструктура (сеть, сессия, дизайн-система, навигационные константы, data-слой);
- **`feature`** — экраны и ViewModel по пользовательским сценариям;
- **`di`** — модули Hilt.

Зависимости направлены внутрь: `feature` → `core`, не наоборот.

```
UI (Compose Screen)
        ↓
ViewModel (StateFlow)
        ↓
Repository
        ↓
Retrofit API  →  API Gateway
```

---

## Жизненный цикл приложения

1. `VitalsApplication` — точка входа Hilt (`@HiltAndroidApp`).
2. `MainActivity` — Compose host, edge-to-edge.
3. `VitalsApp`:
   - читает `AppViewModel.isLoggedIn` из `SessionManager`;
   - `null` → сплэш/лоадер;
   - `false` → `AuthScreen`;
   - `true` → `MainScaffold` (bottom bar + `NavHost`).

---

## Сессия и аутентификация

`SessionManager` (`core/session`) хранит в DataStore:

- `access_token`, `refresh_token`
- `public_id`, `patient_id`
- `role`, `device_fingerprint`
- опционально `triage_session_id` (если сохраняется в сессии)

Сетевой стек (`di/NetworkModule`):

| Клиент | Назначение |
|--------|------------|
| `@Named("plain")` OkHttp | refresh-токен, загрузка внешних ресурсов (QR fallback) без auth-цикла |
| Основной OkHttp | `AuthInterceptor` + `TokenAuthenticator` + logging |
| Retrofit | `baseUrl = ApiConfig.BASE_URL` |

При 401 authenticator обновляет токены через plain-клиент и повторяет запрос.  
При неудаче сессия очищается → UI возвращается на экран входа.

---

## Data-слой

Каждый домен обычно содержит:

- `*Api` — Retrofit interface (`api/v1/...`);
- `*Dto` — kotlinx.serialization модели с опциональными полями;
- `*Repository` — маппинг, объединение aliases (`patientId` + `publicId`), обработка списков.

Примеры доменов: `auth`, `users`, `doctors`, `triage`, `consultations`, `laborders`, `prescriptions`, `medicalrecords`, `routing`, `notifications`.

Общие типы — в `core/data/common` (`ChatMessageDto`, лейблы статусов консультаций/маршрута и т.д.).

Ответы gateway часто нестрого типизированы: репозитории используют гибкий разбор (`JsonElement`, `asArrayFlexible`), чтобы не падать на лишних полях или обёртках `{ items: [...] }`.

---

## Feature / UI

Соглашения:

- Один экран ≈ один `*Screen.kt` + `*ViewModel.kt` (+ опционально UiState data class в том же файле или рядом).
- Состояние: `private val _uiState = MutableStateFlow(...); val uiState = _uiState.asStateFlow()`.
- Навигация через `NavHostController` и хелперы `NavRoutes.*`.
- Общий чат: `feature/common/ChatBody` + `UiChatMessage` / `toUiChatMessage()`.

Дизайн-система (`core/designsystem`):

- цвета, типографика, shapes в `VitalsTheme`;
- кнопки, поля, chips, карточки, top/bottom bars, `ChatBubble`.

---

## Чат: polling и скролл

Как на VitalsWeb (интервал **2000 ms**):

- `ConsultationDetailViewModel` — чат конкретной консультации;
- `DoctorChatViewModel` — чат с врачом по активной сессии.

Пока `isSending == true`, тик опроса пропускается, чтобы не затирать optimistic message раньше ответа send.  
Job опроса отменяется в `onCleared()`.

`ChatBody` держит `LazyListState` и при смене последнего `id` / размера списка / `readAt` / `isThinking` прокручивает к последнему элементу (`animateScrollToItem` после готовности layout).

Метаданные пузыря:

- время `HH:mm` из `sentAt` / `createdAt` / `timestamp`;
- для своих: «Доставлено» или «Прочитано» (`readAt`).

---

## IME (клавиатура)

Проблема edge-to-edge: системный `adjustResize` сам по себе не поднимает Compose-контент над IME.

Решение:

1. Один раз `Modifier.imePadding()` на `NavHost` в `MainScaffold`.
2. Скрывать bottom bar, пока `WindowInsets.ime` > 0.
3. Не дублировать `imePadding` внутри `ChatBody`.
4. На экранах триажа скрывать нижние CTA и chips при открытой клавиатуре.

---

## Конфигурация сборки

- `API_BASE_URL` читается из `local.properties` в `app/build.gradle.kts` → `BuildConfig.API_BASE_URL`.
- `ApiConfig.BASE_URL` прокидывает значение в Retrofit.
- Cleartext HTTP разрешён для локальной разработки (`usesCleartextTraffic` + `network_security_config`).

---

## Зависимости DI

`ApiModule` предоставляет Retrofit-интерфейсы (`@Provides` / `@Singleton`).  
Репозитории и ViewModel инжектятся через конструктор (`@Inject` / `@HiltViewModel`).

Новый экран API:

1. Добавить `*Api` + DTO + Repository в `core/data/<domain>`.
2. Зарегистрировать API в `ApiModule`.
3. Создать feature Screen/ViewModel.
4. Зарегистрировать composable в `NavGraph` и константу в `NavRoutes`.
