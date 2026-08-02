package com.vitals.mobile.core.data.users

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val publicId: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val secondName: String? = null,
    val surename: String? = null,
    val birthDate: String? = null,
    val sex: String? = null,
    val activeProfileId: String? = null,
) {
    val fullName: String
        get() = listOfNotNull(secondName, firstName, surename).filter { it.isNotBlank() }.joinToString(" ")
        .ifBlank { phoneNumber.orEmpty() }
}

@Serializable
data class UserProfileDto(
    val id: String? = null,
    val profileId: String? = null,
    val profileType: String? = null,
    val isActive: Boolean? = null,
)

@Serializable
data class AddProfileRequest(
    val publicId: String,
    val profileType: String? = null,
)

@Serializable
data class SwitchProfileRequest(
    val publicId: String,
    val profileId: String,
)
