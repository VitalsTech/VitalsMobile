package com.vitals.mobile.core.data.auth

import kotlinx.serialization.Serializable

enum class PreferredProfileType(val wireValue: String) {
    PATIENT("Patient"),
    DOCTOR("Doctor"),
}

@Serializable
data class LoginRequest(
    val phoneNumber: String,
    val password: String,
    val deviceFingerprint: String,
    val preferredProfileType: String,
)

@Serializable
data class PatientProfilePayload(val placeholder: String? = null)

@Serializable
data class DoctorProfilePayload(
    val specialization: String? = null,
    val licenseNumber: String? = null,
    val biography: String? = null,
)

@Serializable
data class RegisterRequest(
    val phoneNumber: String,
    val password: String,
    val email: String? = null,
    val firstName: String? = null,
    val secondName: String? = null,
    val surename: String? = null,
    val birthDate: String,
    val sex: String? = null,
    val patientProfile: PatientProfilePayload? = null,
    val doctorProfile: DoctorProfilePayload? = null,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
    val deviceFingerprint: String,
)

@Serializable
data class LogoutRequest(val refreshToken: String)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

@Serializable
data class ForgotPasswordRequest(
    val phoneNumber: String,
    val channel: String = "sms",
)

@Serializable
data class ResetPasswordRequest(
    val phoneNumber: String,
    val code: String,
    val newPassword: String,
)

/** Normalized result of a successful login/register, after loose-field extraction + JWT decoding. */
data class AuthResult(
    val accessToken: String,
    val refreshToken: String?,
    val publicId: String?,
    val profileId: String?,
)

@Serializable
data class EsiaConfigDto(
    val enabled: Boolean = false,
    val mode: String? = null,
    val configured: Boolean = true,
    val portal: String? = null,
    val redirectUri: String? = null,
)

fun EsiaConfigDto.isStubEnabled(): Boolean = enabled && mode == "stub"

@Serializable
data class EsiaStubRegisterRequest(
    val lastName: String,
    val firstName: String,
    val middleName: String? = null,
    val email: String,
    val phoneNumber: String,
)

@Serializable
data class EsiaStatusDto(
    val linked: Boolean = false,
    val linkedAt: String? = null,
    val snilsMasked: String? = null,
)

data class EsiaSyncResult(
    val linked: Boolean = true,
    val existingAccount: Boolean = false,
    val devPassword: String? = null,
    val fullName: String? = null,
)

data class EsiaAuthOutcome(
    val accessToken: String,
    val refreshToken: String?,
    val publicIdHint: String?,
    val esia: EsiaSyncResult,
)
