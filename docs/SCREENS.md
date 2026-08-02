# Карта экранов VitalsMobile

Маршруты — `core/navigation/NavRoutes.kt`.  
Граф — `core/navigation/NavGraph.kt`.

## Нижняя навигация

| UI | Route | Screen | ViewModel |
|----|-------|--------|-----------|
| Путь | `path` | `PathScreen` | `PathViewModel` |
| ИИ | `triage_chat` | `AiAssistantScreen` | `AiAssistantViewModel` |
| Врачи | `doctors` | `DoctorsListScreen` | `DoctorsListViewModel` |
| Профиль | `profile` | `ProfileScreen` | `ProfileViewModel` |
| Ещё | `more` | экран меню «Ещё» | — / локальная навигация |

## Аутентификация

| Route | Screen | ViewModel | Примечание |
|-------|--------|-----------|------------|
| `auth` | `AuthScreen` | `AuthViewModel` | Вне основного scaffold, пока нет сессии |

## Триаж

| Route | Screen | ViewModel |
|-------|--------|-----------|
| `triage_onboarding` | `TriageOnboardingScreen` | `TriageViewModel` |
| `triage_result/{sessionId}` | `TriageResultScreen` | `TriageResultViewModel` |
| `triage_chat` | `AiAssistantScreen` | `AiAssistantViewModel` |

Результат триажа и ассистент работают с `TriageRepository` / сессией triage.

## Врачи

| Route | Screen | ViewModel |
|-------|--------|-----------|
| `doctors` | список | `DoctorsListViewModel` |
| `doctors/{doctorId}` | `DoctorDetailScreen` | `DoctorDetailViewModel` |
| `doctors/{doctorId}/book` | `DoctorBookScreen` | `DoctorBookViewModel` |
| `doctors/{doctorId}/chat` | `DoctorChatScreen` | `DoctorChatViewModel` |

`DoctorChatViewModel`: резолвит активную консультацию с этим врачом, polling сообщений 2 с.

## Консультации

| Route | Screen | ViewModel |
|-------|--------|-----------|
| `consultations` | `MyConsultationsScreen` | `MyConsultationsViewModel` |
| `consultations/{sessionId}` | `ConsultationDetailScreen` | `ConsultationDetailViewModel` |

Секции списка: активные записи, открытые чаты, завершённые.  
Детали: статус, протокол (если есть), чат с polling.

## Анализы и рецепты

| Route | Screen | ViewModel |
|-------|--------|-----------|
| `labs` | `LabsScreen` | `LabsViewModel` |
| `prescriptions/{prescriptionId}` | `PrescriptionDetailScreen` | `PrescriptionDetailViewModel` |

QR: `PrescriptionsRepository.loadQr` → поле `qrCodeBase64Png` (+ fallback URL).

## Профиль и сводка

| Route | Screen | ViewModel |
|-------|--------|-----------|
| `profile` | `ProfileScreen` | `ProfileViewModel` |
| `profile/edit` | `ProfileEditScreen` | `ProfileEditViewModel` |
| `medical_overview` | `MedicalOverviewScreen` | `MedicalOverviewViewModel` |
| `treatment` | `TreatmentScreen` | `TreatmentViewModel` |

## Документы

| Route | Screen | ViewModel |
|-------|--------|-----------|
| `documents` | список | `DocumentsViewModel` |
| `documents/new` | создание | `DocumentNewViewModel` |
| `documents/{documentId}` | детали | `DocumentDetailViewModel` |

## Прочее (`feature/misc` и соседние)

| Route | Screen | ViewModel |
|-------|--------|-----------|
| `notifications` | уведомления | `NotificationsViewModel` |
| `support` | поддержка | `SupportViewModel` |
| `house_call` | вызов на дом | `HouseCallViewModel` |

---

## Соответствие VitalsWeb (пациент)

| Mobile | Web (примерно) |
|--------|----------------|
| `path` | `/patient` (Home / путь) |
| `triage_chat` / triage | `/patient/triage`, `/patient/ai` |
| `doctors*` | `/patient/doctors/*` |
| `consultations*` | `/patient/consultations/*` |
| `labs` + prescription detail | анализы/рецепты + модалка QR на web |
| `medical_overview` | сводка / medical overview |
| `documents*` | `/patient/documents` |
| `profile*` | профиль пациента |

Логику шагов пути, статусов консультаций и формата сообщений чата намеренно держим близко к web (`PathViewModel`, `ChatMessageLabels`, polling 2 с).

---

## Общие UI-блоки

| Компонент | Где | Использование |
|-----------|-----|----------------|
| `ChatBody` | `feature/common` | Триаж, ИИ, чат врача, детали консультации |
| `ChatBubble` | designsystem | Пузырь + время/статус |
| `VitalsSelectableChip` | designsystem | Слоты записи к врачу |
| `VitalsPrimaryButton` / Secondary | designsystem | CTA на экранах |
| `VitalsBottomNavBar` | designsystem | Нижняя панель |
