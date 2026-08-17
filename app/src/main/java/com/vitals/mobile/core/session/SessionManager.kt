package com.vitals.mobile.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "vitals_session")

enum class ProfileRole(val wireValue: String) {
    PATIENT("Patient"),
    DOCTOR("Doctor");

    companion object {
        fun fromWire(value: String?): ProfileRole = if (value.equals("Doctor", ignoreCase = true)) DOCTOR else PATIENT
    }
}

data class Session(
    val accessToken: String?,
    val refreshToken: String?,
    val publicId: String?,
    val patientId: String?,
    val role: ProfileRole?,
    val deviceFingerprint: String,
    /** Active AI triage session id (mirrors web localStorage). */
    val triageSessionId: String? = null,
) {
    val isLoggedIn: Boolean get() = !accessToken.isNullOrBlank()
}

/**
 * Persists the authenticated session, mirroring the `vitals.*` localStorage keys used by VitalsWeb:
 * accessToken, refreshToken, publicId, patientId (ProfileId), role, deviceFingerprint.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val PUBLIC_ID = stringPreferencesKey("public_id")
        val PATIENT_ID = stringPreferencesKey("patient_id")
        val ROLE = stringPreferencesKey("role")
        val DEVICE_FINGERPRINT = stringPreferencesKey("device_fingerprint")
        val TRIAGE_SESSION_ID = stringPreferencesKey("triage_session_id")
        /** One-shot flag: open ИИ tab and create a fresh triage session (like web `?new=1`). */
        val PENDING_NEW_TRIAGE = booleanPreferencesKey("pending_new_triage")
    }

    val sessionFlow: Flow<Session> = context.sessionDataStore.data.map { prefs ->
        Session(
            accessToken = prefs[Keys.ACCESS_TOKEN],
            refreshToken = prefs[Keys.REFRESH_TOKEN],
            publicId = prefs[Keys.PUBLIC_ID],
            patientId = prefs[Keys.PATIENT_ID],
            role = prefs[Keys.ROLE]?.let { ProfileRole.fromWire(it) },
            deviceFingerprint = prefs[Keys.DEVICE_FINGERPRINT] ?: DeviceFingerprint.generate(),
            triageSessionId = prefs[Keys.TRIAGE_SESSION_ID],
        )
    }

    suspend fun currentSession(): Session = sessionFlow.first()

    suspend fun getOrCreateDeviceFingerprint(): String {
        val existing = context.sessionDataStore.data.first()[Keys.DEVICE_FINGERPRINT]
        if (existing != null) return existing
        val generated = DeviceFingerprint.generate()
        context.sessionDataStore.edit { it[Keys.DEVICE_FINGERPRINT] = generated }
        return generated
    }

    suspend fun getAccessTokenBlocking(): String? = context.sessionDataStore.data.first()[Keys.ACCESS_TOKEN]

    suspend fun getRefreshTokenBlocking(): String? = context.sessionDataStore.data.first()[Keys.REFRESH_TOKEN]

    suspend fun saveTokens(accessToken: String, refreshToken: String?) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            if (refreshToken != null) prefs[Keys.REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun saveIdentity(publicId: String?, patientId: String?, role: ProfileRole?) {
        context.sessionDataStore.edit { prefs ->
            if (publicId != null) prefs[Keys.PUBLIC_ID] = publicId
            if (patientId != null) prefs[Keys.PATIENT_ID] = patientId
            if (role != null) prefs[Keys.ROLE] = role.wireValue
        }
    }

    suspend fun saveTriageSessionId(sessionId: String?) {
        context.sessionDataStore.edit { prefs ->
            if (sessionId.isNullOrBlank()) prefs.remove(Keys.TRIAGE_SESSION_ID)
            else prefs[Keys.TRIAGE_SESSION_ID] = sessionId
        }
    }

    /**
     * Clears the stored triage session and marks that the ИИ screen should open a brand-new one.
     * Consumed by [consumePendingNewTriage] when AiAssistant becomes visible.
     */
    suspend fun requestNewTriage() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(Keys.TRIAGE_SESSION_ID)
            prefs[Keys.PENDING_NEW_TRIAGE] = true
        }
    }

    /** Returns true once if [requestNewTriage] was called; clears the flag. */
    suspend fun consumePendingNewTriage(): Boolean {
        val pending = context.sessionDataStore.data.first()[Keys.PENDING_NEW_TRIAGE] == true
        if (pending) {
            context.sessionDataStore.edit { it.remove(Keys.PENDING_NEW_TRIAGE) }
        }
        return pending
    }

    suspend fun clear() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.PUBLIC_ID)
            prefs.remove(Keys.PATIENT_ID)
            prefs.remove(Keys.ROLE)
            prefs.remove(Keys.TRIAGE_SESSION_ID)
            prefs.remove(Keys.PENDING_NEW_TRIAGE)
            // Keep the device fingerprint stable across logins/logouts.
        }
    }
}
