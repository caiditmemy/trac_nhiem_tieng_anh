package vn.edu.qnu.st.nambede.ui.common

import androidx.compose.ui.graphics.Color
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import vn.edu.qnu.st.nambede.ui.theme.Amber
import vn.edu.qnu.st.nambede.ui.theme.Emerald
import vn.edu.qnu.st.nambede.ui.theme.Rose

private val VIETNAMESE = Locale.forLanguageTag("vi-VN")
private val DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
private val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** The services send UTC times such as 2026-10-08T07:45:01Z (also with an offset or nanoseconds). */
fun parseInstant(iso: String): Instant = OffsetDateTime.parse(iso).toInstant()

/** 6.67 gives "6,67", like the web app. */
fun formatScore(score: Double): String = String.format(VIETNAMESE, "%.2f", score)

fun formatDateTime(iso: String, zone: ZoneId = ZoneId.systemDefault()): String =
    runCatching { DATE_TIME.format(parseInstant(iso).atZone(zone)) }.getOrDefault(iso)

/** "Còn 3 ngày", "Còn 5 giờ", "Còn 12 phút", "Đã quá hạn". */
fun timeLeft(dueAt: String, now: Instant = Instant.now()): String {
    val left = Duration.between(now, runCatching { parseInstant(dueAt) }.getOrDefault(now))
    if (left.isNegative || left.isZero) return "Đã quá hạn"
    val hours = left.toHours()
    return when {
        hours >= 24 -> "Còn ${hours / 24} ngày"
        hours >= 1 -> "Còn $hours giờ"
        else -> "Còn ${maxOf(1, left.toMinutes())} phút"
    }
}

fun isPast(iso: String, now: Instant = Instant.now()): Boolean = runCatching { !parseInstant(iso).isAfter(now) }.getOrDefault(false)

/** "vừa xong", "5 phút trước", "3 giờ trước", "2 ngày trước", then the date, like the web app's notification bell. */
fun timeAgo(iso: String, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String {
    val time = runCatching { parseInstant(iso) }.getOrNull() ?: return iso
    val minutes = Duration.between(time, now).toMinutes()
    return when {
        minutes < 1 -> "vừa xong"
        minutes < 60 -> "$minutes phút trước"
        minutes < 24 * 60 -> "${minutes / 60} giờ trước"
        minutes < 7 * 24 * 60 -> "${minutes / (24 * 60)} ngày trước"
        else -> DATE.format(time.atZone(zone))
    }
}

/** Green from 8, amber from 5, red below. */
fun scoreColor(score: Double): Color = when {
    score >= 8 -> Emerald
    score >= 5 -> Amber
    else -> Rose
}

/** Weekly ranking dates "2026-09-28" shown as "28/09". */
fun shortDay(isoDate: String): String = isoDate.split("-").let { if (it.size == 3) "${it[2]}/${it[1]}" else isoDate }
