package com.vitals.mobile.core.data.laborders

import kotlinx.serialization.Serializable

enum class LabOrderStatus(val wireValue: String) {
    ORDERED("Ordered"),
    IN_PROGRESS("InProgress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromWire(value: String?): LabOrderStatus? = entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) }
    }
}

@Serializable
data class LabTestItem(
    val name: String,
    val code: String? = null,
)

@Serializable
data class CreateLabOrderRequest(
    val patientId: String,
    val consultationId: String? = null,
    val tests: List<LabTestItem>,
)

@Serializable
data class StartLabOrderRequest(val externalLabOrderId: String? = null)

@Serializable
data class CancelLabOrderRequest(val reason: String)

@Serializable
data class LabResultItem(
    val testName: String,
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
    val patientId: String? = null,
    val status: String? = null,
    val tests: List<LabTestItem>? = null,
    val results: List<LabResultItem>? = null,
    val createdAt: String? = null,
) {
    val resolvedStatus: LabOrderStatus? get() = LabOrderStatus.fromWire(status)
}
