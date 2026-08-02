package com.vitals.mobile.core.navigation

/** Central registry of navigation routes for the patient app. */
object NavRoutes {
    /** Single screen hosting both the "Вход" and "Регистрация" tabs, matching the Figma card. */
    const val AUTH = "auth"

    // Bottom-nav roots
    const val PATH = "path"
    const val TRIAGE_CHAT = "triage_chat"
    const val DOCTORS = "doctors"
    const val PROFILE = "profile"
    const val MORE = "more"

    const val TRIAGE_ONBOARDING = "triage_onboarding"
    const val TRIAGE_RESULT = "triage_result/{sessionId}"
    fun triageResult(sessionId: String) = "triage_result/$sessionId"

    const val DOCTOR_DETAIL = "doctors/{doctorId}"
    fun doctorDetail(doctorId: String) = "doctors/$doctorId"

    const val DOCTOR_BOOK = "doctors/{doctorId}/book"
    fun doctorBook(doctorId: String) = "doctors/$doctorId/book"

    const val DOCTOR_CHAT = "doctors/{doctorId}/chat"
    fun doctorChat(doctorId: String) = "doctors/$doctorId/chat"

    const val PROFILE_EDIT = "profile/edit"

    const val DOCUMENTS = "documents"
    const val DOCUMENT_NEW = "documents/new"
    const val DOCUMENT_DETAIL = "documents/{documentId}"
    fun documentDetail(documentId: String) = "documents/$documentId"

    const val CONSULTATIONS = "consultations"
    const val CONSULTATION_DETAIL = "consultations/{sessionId}"
    fun consultationDetail(sessionId: String) = "consultations/$sessionId"

    const val TREATMENT = "treatment"
    const val LABS = "labs"
    const val NOTIFICATIONS = "notifications"
    const val SUPPORT = "support"
    const val HOUSE_CALL = "house_call"

    val bottomNavRoutes = setOf(PATH, TRIAGE_CHAT, DOCTORS, PROFILE, MORE)
}
