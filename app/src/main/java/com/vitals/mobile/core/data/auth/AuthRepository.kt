package com.vitals.mobile.core.data.auth

import com.vitals.mobile.core.network.JwtUtils
import com.vitals.mobile.core.network.SessionTokenParser
import com.vitals.mobile.core.network.boolField
import com.vitals.mobile.core.network.objectField
import com.vitals.mobile.core.network.stringField
import com.vitals.mobile.core.session.ProfileRole
import com.vitals.mobile.core.session.Session
import com.vitals.mobile.core.session.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
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

    suspend fun getEsiaConfig(): EsiaConfigDto =
        runCatching { authApi.getEsiaConfig() }.getOrElse { EsiaConfigDto(enabled = false) }

    suspend fun getEsiaStatus(): EsiaStatusDto = authApi.getEsiaStatus()

    /** DEV stub: does not persist tokens so the UI can show existingAccount / devPassword first. */
    suspend fun stubRegister(request: EsiaStubRegisterRequest): EsiaAuthOutcome {
        val response = authApi.stubRegister(request)
        return parseEsiaAuthOutcome(response)
    }

    suspend fun stubLink(): EsiaAuthOutcome {
        val outcome = parseEsiaAuthOutcome(authApi.stubLink(JsonObject(emptyMap())))
        persistAuthOutcome(outcome)
        return outcome
    }

    suspend fun persistAuthOutcome(
        outcome: EsiaAuthOutcome,
        role: ProfileRole = ProfileRole.PATIENT,
    ): AuthResult = persistTokens(outcome.accessToken, outcome.refreshToken, outcome.publicIdHint, role)

    private fun parseEsiaAuthOutcome(response: JsonObject): EsiaAuthOutcome {
        val tokens = SessionTokenParser.extract(response)
        val accessToken = requireNotNull(tokens?.accessToken) { "Сервер не вернул токены доступа." }
        val esiaObj = response.objectField("esia")
        return EsiaAuthOutcome(
            accessToken = accessToken,
            refreshToken = tokens.refreshToken,
            publicIdHint = response.stringField("userPublicId", "user_public_id", "publicId"),
            esia = EsiaSyncResult(
                linked = esiaObj?.boolField("linked") ?: true,
                existingAccount = esiaObj?.boolField("existingAccount") ?: false,
                devPassword = esiaObj?.stringField("devPassword"),
                fullName = esiaObj?.stringField("fullName"),
            ),
        )
    }

    private suspend fun persistAuthResponse(
        response: JsonObject,
        role: ProfileRole,
    ): AuthResult {
        val tokens = SessionTokenParser.extract(response)
        val accessToken = requireNotNull(tokens?.accessToken) { "Auth response did not contain an access token" }
        return persistTokens(accessToken, tokens.refreshToken, null, role)
    }

    private suspend fun persistTokens(
        accessToken: String,
        refreshToken: String?,
        publicIdHint: String?,
        role: ProfileRole,
    ): AuthResult {
        val publicId = JwtUtils.publicId(accessToken) ?: publicIdHint
        val profileId = JwtUtils.profileId(accessToken)
        sessionManager.saveTokens(accessToken, refreshToken)
        sessionManager.saveIdentity(publicId = publicId, patientId = profileId, role = role)
        return AuthResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            publicId = publicId,
            profileId = profileId,
        )
    }

    private fun normalizePhone(phoneNumber: String): String = phoneNumber.filter { it.isDigit() }
}
