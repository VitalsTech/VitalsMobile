package com.vitals.mobile.core.data.laborders

import kotlinx.serialization.Serializable

enum class LabOrderStatus(val wireValue: String) {
    ORDERED("Ordered"),
    IN_PROGRESS("InProgress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromWire(value: String?): LabOrderStatus? =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) }
    }
}

@Serializable
data class LabTestItem(
    val name: String? = null,
    val testName: String? = null,
    val code: String? = null,
    val testCode: String? = null,
) {
    val resolvedName: String get() = testName?.trim()?.takeIf { it.isNotEmpty() }
        ?: name?.trim()?.takeIf { it.isNotEmpty() }
        ?: "Анализ"
}

@Serializable
data class CreateLabOrderRequest(
    val patientId: String,
    val consultationId: String? = null,
    val tests: List<LabTestItem> = emptyList(),
    val items: List<LabTestItem> = emptyList(),
)

@Serializable
data class StartLabOrderRequest(val externalLabOrderId: String? = null)

@Serializable
data class CancelLabOrderRequest(val reason: String)

@Serializable
data class LabResultItem(
    val testName: String? = null,
    val value: String? = null,
    val unit: String? = null,
    val referenceRange: String? = null,
)

@Serializable
data class LabResultsRequest(
    val markCompleted: Boolean = true,
    val results: List<LabResultItem>,
)

@Serializable
data class LabOrderDto(
    val id: String? = null,
    val labOrderId: String? = null,
    val patientId: String? = null,
    val status: String? = null,
    /** Legacy / create payload shape. */
    val tests: List<LabTestItem>? = null,
    /** Gateway list shape (web `items` + `testName`). */
    val items: List<LabTestItem>? = null,
    val results: List<LabResultItem>? = null,
    val createdAt: String? = null,
    val orderedAt: String? = null,
) {
    val resolvedId: String get() = labOrderId ?: id.orEmpty()

    val resolvedStatus: LabOrderStatus? get() = LabOrderStatus.fromWire(status)

    fun formatTestNames(): String {
        val names = (items ?: tests).orEmpty().map { it.resolvedName }.filter { it.isNotBlank() }
        return if (names.isNotEmpty()) names.joinToString(", ") else "Направление на анализы"
    }
}
