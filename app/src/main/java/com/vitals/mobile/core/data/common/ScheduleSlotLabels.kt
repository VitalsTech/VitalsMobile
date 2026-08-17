package com.vitals.mobile.core.data.common

import com.vitals.mobile.core.data.doctors.ScheduleSlotDto
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Mirrors VitalsWeb `scheduleSlot.ts` helpers. */
object ScheduleSlotLabels {
    private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", Locale("ru"))
    private val DAY_FMT = DateTimeFormatter.ofPattern("E, dd.MM", Locale("ru"))
    private val DAY_TIME_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", Locale("ru"))

    fun startIso(slot: ScheduleSlotDto): String =
        slot.startsAt ?: slot.startAt ?: slot.startTime ?: slot.start ?: ""

    fun endIso(slot: ScheduleSlotDto): String =
        slot.endsAt ?: slot.endAt ?: slot.endTime ?: slot.end ?: ""

    fun isAvailable(slot: ScheduleSlotDto): Boolean {
        if (slot.isBooked == true) return false
        val status = slot.status?.lowercase().orEmpty()
        if (status == "booked" || status == "closed") return false
        return slot.isAvailable ?: slot.available ?: true
    }

    fun formatTime(slot: ScheduleSlotDto): String = formatTimeIso(startIso(slot))

    fun formatTimeIso(iso: String): String {
        val dateTime = parse(iso) ?: return if (iso.matches(Regex("""^\d{1,2}:\d{2}$"""))) iso else "-"
        return dateTime.format(TIME_FMT)
    }

    fun formatDayLabel(date: LocalDate): String =
        date.format(DAY_FMT).replaceFirstChar { it.uppercaseChar() }

    /** Human-readable date+time for consultation cards (web `formatDayTime`). */
    fun formatDayTime(iso: String?): String {
        if (iso.isNullOrBlank()) return "-"
        val dateTime = parse(iso) ?: return iso
        return dateTime.format(DAY_TIME_FMT)
    }

    fun localDate(slot: ScheduleSlotDto, zone: ZoneId = ZoneId.systemDefault()): LocalDate? {
        val dateTime = parse(startIso(slot)) ?: return null
        return dateTime.atZone(zone).toLocalDate()
    }

    fun formatRange(slot: ScheduleSlotDto): String {
        val start = formatTimeIso(startIso(slot))
        val end = formatTimeIso(endIso(slot))
        return if (end != "-" && end.isNotBlank()) "$start – $end" else start
    }

    fun isInFuture(slot: ScheduleSlotDto, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val start = parse(startIso(slot)) ?: return true
        return start.atZone(zone).toInstant().isAfter(Instant.now())
    }

    private fun parse(raw: String): LocalDateTime? {
        if (raw.isBlank()) return null
        return runCatching { OffsetDateTime.parse(raw).toLocalDateTime() }.getOrNull()
            ?: runCatching { Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDateTime() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(raw) }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(raw.substringBefore('.').replace(' ', 'T'))
            }.getOrNull()
    }
}
