package vn.edu.qnu.st.nambede.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import vn.edu.qnu.st.nambede.ui.profile.initialsOf
import vn.edu.qnu.st.nambede.ui.profile.passwordProblem

class ProfileTest {

    @Test
    fun passwordChecksMatchTheWebForm() {
        assertEquals("Nhập mật khẩu hiện tại.", passwordProblem("", "Moi@2026", "Moi@2026"))
        assertEquals("Mật khẩu mới phải có ít nhất 6 ký tự.", passwordProblem("Student@123", "12345", "12345"))
        assertEquals("Nhập lại mật khẩu mới chưa khớp.", passwordProblem("Student@123", "Moi@2026", "Moi@2027"))
        assertEquals("Mật khẩu mới phải khác mật khẩu hiện tại.", passwordProblem("Student@123", "Student@123", "Student@123"))
        assertNull(passwordProblem("Student@123", "Moi@2026", "Moi@2026"))
    }

    @Test
    fun initialsUseTheLastTwoWords() {
        assertEquals("VA", initialsOf("Nguyễn Văn A"))
        assertEquals("B", initialsOf("  B  "))
        assertEquals("?", initialsOf(""))
    }
}
