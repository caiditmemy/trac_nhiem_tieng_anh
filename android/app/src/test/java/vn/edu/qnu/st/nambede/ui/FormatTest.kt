package vn.edu.qnu.st.nambede.ui

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.qnu.st.nambede.ui.common.formatDateTime
import vn.edu.qnu.st.nambede.ui.common.formatScore
import vn.edu.qnu.st.nambede.ui.common.isPast
import vn.edu.qnu.st.nambede.ui.common.parseInstant
import vn.edu.qnu.st.nambede.ui.common.shortDay
import vn.edu.qnu.st.nambede.ui.common.timeAgo
import vn.edu.qnu.st.nambede.ui.common.timeLeft

class FormatTest {

    private val vietnam = ZoneId.of("Asia/Ho_Chi_Minh")
    private val now = Instant.parse("2026-10-01T03:00:00Z")

    @Test
    fun scoresUseAVietnameseDecimalComma() {
        assertEquals("6,67", formatScore(6.67))
        assertEquals("10,00", formatScore(10.0))
    }

    @Test
    fun readsTheTimesTheServicesSend() {
        // Spring sends nanoseconds, FastAPI microseconds, both in UTC
        assertEquals(Instant.parse("2026-10-01T08:08:25.947746338Z"), parseInstant("2026-10-01T08:08:25.947746338Z"))
        assertEquals(Instant.parse("2026-10-01T08:00:00Z"), parseInstant("2026-10-01T08:00:00+00:00"))
        assertEquals("15:45 08/10/2026", formatDateTime("2026-10-08T08:45:00Z", vietnam))
    }

    @Test
    fun timeLeftCountsDaysHoursAndMinutes() {
        assertEquals("Còn 7 ngày", timeLeft("2026-10-08T03:00:00Z", now))
        assertEquals("Còn 5 giờ", timeLeft("2026-10-01T08:30:00Z", now))
        assertEquals("Còn 1 phút", timeLeft("2026-10-01T03:00:20Z", now))
        assertEquals("Đã quá hạn", timeLeft("2026-09-30T03:00:00Z", now))
        assertTrue(isPast("2026-09-30T03:00:00Z", now))
    }

    @Test
    fun weekDaysAreShortened() {
        assertEquals("28/09", shortDay("2026-09-28"))
    }

    @Test
    fun notificationTimesReadLikeTheWebBell() {
        assertEquals("vừa xong", timeAgo("2026-10-01T02:59:30Z", now, vietnam))
        assertEquals("5 phút trước", timeAgo("2026-10-01T02:55:00Z", now, vietnam))
        assertEquals("3 giờ trước", timeAgo("2026-10-01T00:00:00Z", now, vietnam))
        assertEquals("2 ngày trước", timeAgo("2026-09-29T03:00:00Z", now, vietnam))
        assertEquals("20/09/2026", timeAgo("2026-09-20T03:00:00Z", now, vietnam))
    }
}
