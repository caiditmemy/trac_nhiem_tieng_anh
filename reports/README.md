# Báo cáo kiểm thử Sprint 1 — Tùng (Tuần 6 + Tuần 7)

Sinh bởi `make report` (`scripts/report.py`). Không sửa tay.

| Thành phần | Công cụ | Số test | Lỗi | Bỏ qua | Độ phủ |
|---|---|---:|---:|---:|---|
| auth-service | JUnit 5 + Mockito / MockMvc | 44 | 0 | 0 | 75.3 % (JaCoCo, lines) |
| class-service | JUnit 5 + Mockito / MockMvc | 40 | 0 | 0 | 65.7 % (JaCoCo, lines) |
| exercise-service | JUnit 5 + Mockito / MockMvc | 17 | 0 | 0 | 72.7 % (JaCoCo, lines) |
| gateway | pytest | 52 | 0 | 0 | 94.1 % (pytest-cov, lines) |
| learning-service | pytest | 45 | 0 | 0 | 93.6 % (pytest-cov, lines) |
| web | Vitest + Testing Library | 62 | 0 | 0 | n/a |

Tổng: 260 test, 0 lỗi.

## Thư mục

- `junit/<thành phần>/`: kết quả từng test (XML).
- `coverage/<thành phần>/`: báo cáo JaCoCo (`index.html`) và pytest-cov (`html/`, `coverage.xml`).
- `sprint1-reports.tar.gz`: gói tất cả các thư mục trên.
