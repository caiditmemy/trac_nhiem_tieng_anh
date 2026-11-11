package vn.edu.qnu.st.nambede.data

/** Vietnamese message for a failed call, like errorText() in the web app. `overrides` rewords statuses for one screen. */
fun errorText(error: Throwable, overrides: Map<Int, String> = emptyMap()): String {
    val status = (error as? ApiException)?.status ?: return "Đã có lỗi xảy ra. Vui lòng thử lại."
    overrides[status]?.let { return it }
    return when (status) {
        0 -> "Không kết nối được máy chủ. Kiểm tra mạng và địa chỉ gateway."
        400 -> "Dữ liệu không hợp lệ: ${error.message}"
        401 -> "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
        403 -> "Bạn không có quyền thực hiện thao tác này."
        404 -> "Không tìm thấy dữ liệu."
        409 -> "Dữ liệu đã tồn tại."
        429 -> "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau ít phút."
        502, 503, 504 -> "Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau."
        else -> "Đã có lỗi xảy ra. Vui lòng thử lại."
    }
}
