package com.vitals.mobile.core.data.users

import com.vitals.mobile.core.network.asArrayFlexible
import com.vitals.mobile.core.network.asObjectOrNull
import com.vitals.mobile.core.network.objectField
import com.vitals.mobile.core.network.stringField
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

data class PatientImportedFields(
    val insuranceNumber: String? = null,
    val snils: String? = null,
    val residenceAddress: String? = null,
)

@Singleton
class UsersRepository @Inject constructor(
    private val usersApi: UsersApi,
) {
    suspend fun getUser(publicId: String): UserDto = usersApi.getUser(publicId)

    suspend fun getPatientImportedFields(publicId: String): PatientImportedFields {
        val raw = runCatching { usersApi.getProfiles(publicId) }.getOrNull() ?: return PatientImportedFields()
        val profiles = raw.asArrayFlexible("items", "data", "profiles", "results")
        val patient = profiles
            .mapNotNull { it.asObjectOrNull() }
            .firstOrNull { profile ->
                profile.stringField("profileType", "profile_type").orEmpty()
                    .contains("patient", ignoreCase = true)
            }
        val data = patient?.objectField("data") ?: return PatientImportedFields()
        return PatientImportedFields(
            insuranceNumber = data.stringField("insuranceNumber", "oms", "omsNumber"),
            snils = data.stringField("snils"),
            residenceAddress = formatAddress(data.objectField("residenceAddress", "registrationAddress")),
        )
    }

    private fun formatAddress(address: JsonObject?): String? {
        if (address == null) return null
        val parts = listOfNotNull(
            address.stringField("postCode", "postalCode"),
            address.stringField("region"),
            address.stringField("city"),
            address.stringField("area"),
            address.stringField("street", "addressStr"),
            address.stringField("house")?.let { "д. $it" },
            address.stringField("flat")?.let { "кв. $it" },
        ).filter { it.isNotBlank() }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }
}

fun maskSensitive(value: String?): String? {
    val compact = value?.replace("\\s".toRegex(), "").orEmpty()
    if (compact.isEmpty()) return null
    if (compact.length <= 4) return "••••"
    return "•••${compact.takeLast(4)}"
}
