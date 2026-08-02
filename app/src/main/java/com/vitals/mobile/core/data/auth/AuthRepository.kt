package com.vitals.mobile.core.data.auth

import com.vitals.mobile.core.network.JwtUtils
import com.vitals.mobile.core.network.SessionTokenParser
import com.vitals.mobile.core.session.ProfileRole
import com.vitals.mobile.core.session.Session
import com.vitals.mobile.core.session.SessionManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager,
) {
    val sessionFlow: Flow<Session> = sessionManager.sessionFlow

    suspend fun login(phoneNumber: String, password: String, role: ProfileRole): AuthResult {
        val fingerprint = sessionManager.getOrCreateDeviceFingerprint()
        val response = authApi.login(
            LoginRequest(
                phoneNumber = normalizePhone(phoneNumber),
                password = password,
                deviceFingerprint = fingerprint,
                preferredProfileType = role.wireValue,
            ),
        )
        return persistAuthResponse(response, role)
    }

    suspend fun register(payload: RegisterRequest, role: ProfileRole): AuthResult {
        sessionManager.getOrCreateDeviceFingerprint()
        val response = authApi.register(payload)
        val tokens = SessionTokenParser.extract(response)
        return if (tokens?.accessToken != null) {
            persistAuthResponse(response, role)
        } else {
            // Some backends return an empty body on register and expect a follow-up login call.
            login(payload.phoneNumber, payload.password, role)
        }
    }

    suspend fun logout() {
        val refreshToken = sessionManager.currentSession().refreshToken
        if (refreshToken != null) {
            runCatching { authApi.logout(LogoutRequest(refreshToken)) }
        }
        sessionManager.clear()
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        authApi.changePassword(ChangePasswordRequest(currentPassword, newPassword))
    }

    suspend fun forgotPassword(phoneNumber: String, channel: String = "sms") {
        authApi.forgotPassword(ForgotPasswordRequest(normalizePhone(phoneNumber), channel))
    }

    suspend fun resetPassword(phoneNumber: String, code: String, newPassword: String) {
        authApi.resetPassword(ResetPasswordRequest(normalizePhone(phoneNumber), code, newPassword))
    }

    private suspend fun persistAuthResponse(response: kotlinx.serialization.json.JsonObject, role: ProfileRole): AuthResult {
        val tokens = SessionTokenParser.extract(response)
        val accessToken = requireNotNull(tokens?.accessToken) { "Auth response did not contain an access token" }
        val publicId = JwtUtils.publicId(accessToken)
        val profileId = JwtUtils.profileId(accessToken)

        sessionManager.saveTokens(accessToken, tokens?.refreshToken)
        sessionManager.saveIdentity(publicId = publicId, patientId = profileId, role = role)

        return AuthResult(
            accessToken = accessToken,
            refreshToken = tokens?.refreshToken,
            publicId = publicId,
            profileId = profileId,
        )
    }

    /** Backend expects digits-only phone numbers (no leading `+`), matching VitalsWeb's normalization. */
    private fun normalizePhone(phoneNumber: String): String = phoneNumber.filter { it.isDigit() }
}
