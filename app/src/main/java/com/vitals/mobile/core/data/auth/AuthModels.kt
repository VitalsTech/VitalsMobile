package com.vitals.mobile.core.data.auth

import kotlinx.serialization.SerialName
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
