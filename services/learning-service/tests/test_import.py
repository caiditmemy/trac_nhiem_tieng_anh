"""Lecturers import questions from an Excel, CSV or JSON file laid out like the template."""

import io
import json

import pytest
from openpyxl import Workbook, load_workbook

from learning_service import question_import

TEACHER = ("3", "TEACHER")
STUDENT = ("2", "STUDENT")


@pytest.fixture
def h(auth_header):
    return lambda who: auth_header(*who)


@pytest.fixture
def topic(bank, h):
    """A topic that already has one question."""
    topic = bank.post("/topics", json={"name": "Thì hiện tại đơn"}, headers=h(TEACHER)).json()
    body = {
        "topicId": topic["id"],
        "content": "She ___ to school every day.",
        "optionA": "go",
        "optionB": "goes",
        "optionC": "gone",
        "optionD": "going",
        "correctOption": "B",
    }
    assert bank.post("/questions", json=body, headers=h(TEACHER)).status_code == 201
    return topic


def workbook(*rows) -> bytes:
    book = Workbook()
    sheet = book.active
    sheet.append(question_import.COLUMNS)
    for row in rows:
        sheet.append(list(row))
    buffer = io.BytesIO()
    book.save(buffer)
    return buffer.getvalue()


def huge_dimension(row=1_000_000, column=1) -> bytes:
    """A tiny workbook whose single stray cell makes the declared dimension enormous."""
    book = Workbook()
    sheet = book.active
    sheet.append(question_import.COLUMNS)
    sheet.cell(row=row, column=column, value="x")
    buffer = io.BytesIO()
    book.save(buffer)
    return buffer.getvalue()


def no_option_d() -> bytes:
    """A workbook whose header has no 'Đáp án D' column."""
    book = Workbook()
    sheet = book.active
    sheet.append([name for name in question_import.COLUMNS if name != "Đáp án D"])
    sheet.append(["Q", "a", "b", "c", "A", None])
    buffer = io.BytesIO()
    book.save(buffer)
    return buffer.getvalue()


def upload(client, h, topic_id, data, filename="cau-hoi.xlsx", dry_run=True, who=TEACHER):
    return client.post(
        f"/questions/import?topicId={topic_id}&dryRun={'true' if dry_run else 'false'}",
        files={"file": (filename, data, "application/octet-stream")},
        headers=h(who),
    )


ROWS = (
    ("Q1: He ___ tea every morning.", "drink", "drinks", "drank", "drinking", "b", "Chủ ngữ số ít."),  # row 2, lower-case answer is fine
    ("Q2: missing C", "a", "b", None, "d", "A", None),  # row 3
    ("Q3: wrong answer letter", "a", "b", "c", "d", "E", None),  # row 4
    (None, None, None, None, None, None, None),  # row 5, empty: skipped
    ("she ___ to school   every day.", "go", "goes", "gone", "going", "B", None),  # row 6, already in the topic
    ("Q1: He ___ tea every morning.", "drink", "drinks", "drank", "drinking", "B", None),  # row 7, repeated in the file
    ("1 + 1 = ?", 1, 2, 3, 4, "B", None),  # row 8, numbers become text
)


def test_template_has_the_expected_columns_and_examples(bank, h):
    response = bank.get("/questions/import/template", headers=h(TEACHER))
    assert response.status_code == 200
    assert response.headers["content-type"] == question_import.XLSX_TYPE
    sheet = load_workbook(io.BytesIO(response.content)).active
    rows = list(sheet.iter_rows(values_only=True))
    assert list(rows[0]) == question_import.COLUMNS
    assert len(rows) == 3
    assert bank.get("/questions/import/template", headers=h(STUDENT)).status_code == 403


def test_preview_reports_each_row_and_saves_nothing(bank, h, topic):
    response = upload(bank, h, topic["id"], workbook(*ROWS))
    assert response.status_code == 200
    body = response.json()
    assert (body["dryRun"], body["total"], body["valid"], body["imported"]) == (True, 6, 2, 0)
    assert [(row["row"], row["problems"]) for row in body["rows"]] == [
        (2, []),
        (3, ["option_c_missing"]),
        (4, ["correct_option_invalid"]),
        (6, ["duplicate"]),
        (7, ["duplicate"]),
        (8, []),
    ]
    assert body["rows"][0]["correctOption"] == "B"
    assert (body["rows"][5]["optionA"], body["rows"][5]["optionD"]) == ("1", "4")
    assert len(bank.get(f"/questions?topicId={topic['id']}", headers=h(TEACHER)).json()) == 1


def test_import_saves_the_valid_rows_into_the_topic(bank, h, topic):
    response = upload(bank, h, topic["id"], workbook(*ROWS), dry_run=False)
    assert response.status_code == 200
    assert (response.json()["valid"], response.json()["imported"]) == (2, 2)
    questions = bank.get(f"/questions?topicId={topic['id']}", headers=h(TEACHER)).json()
    assert len(questions) == 3
    imported = next(question for question in questions if question["content"].startswith("Q1"))
    assert (imported["correctOption"], imported["explanation"], imported["createdBy"]) == ("B", "Chủ ngữ số ít.", 3)
    # Importing the same file again adds nothing: every row is now a duplicate or broken
    again = upload(bank, h, topic["id"], workbook(*ROWS), dry_run=False).json()
    assert (again["valid"], again["imported"]) == (0, 0)


def test_a_broken_row_does_not_block_a_correct_copy_later(bank, h, topic):
    rows = (("New question", "a", "b", None, "d", "A", None), ("New question", "a", "b", "c", "d", "A", None))
    body = upload(bank, h, topic["id"], workbook(*rows)).json()
    assert [row["problems"] for row in body["rows"]] == [["option_c_missing"], []]


def test_csv_files_saved_by_excel_work_too(bank, h, topic):
    text = "Câu hỏi,Đáp án A,Đáp án B,Đáp án C,Đáp án D,Đáp án đúng,Giải thích\nThey ___ football yesterday.,play,plays,played,playing,C,Quá khứ đơn.\n"
    response = upload(bank, h, topic["id"], text.encode("utf-8-sig"), filename="cau-hoi.csv", dry_run=False)
    assert response.status_code == 200
    assert response.json()["imported"] == 1
    assert upload(bank, h, topic["id"], "Câu hỏi\nđ".encode("utf-16"), filename="cu.csv").status_code == 400


@pytest.mark.parametrize(
    ("filename", "data", "message"),
    [
        ("cau-hoi.txt", b"hello", "Only .xlsx, .csv and .json files are supported"),
        ("cau-hoi.xlsx", b"not a workbook", "The file could not be read as an Excel workbook"),
        ("cau-hoi.xlsx", workbook(), "The file has no questions"),
        ("cau-hoi.xlsx", b"", "The file could not be read as an Excel workbook"),
        ("cau-hoi.xlsx", no_option_d(), "Missing column: Đáp án D"),
        ("cau-hoi.csv", b"Cau hoi,Dap an\nx,y\n", "Missing column: Câu hỏi, Đáp án A, Đáp án B, Đáp án C, Đáp án D, Đáp án đúng"),
        ("cau-hoi.json", b"{not json", "The file could not be read as JSON"),
        ("cau-hoi.json", '{"Câu hỏi": "x"}'.encode(), "The JSON file must contain an array of questions"),
        ("cau-hoi.json", b'["just text"]', "Every JSON item must be an object with the question\'s fields"),
        ("cau-hoi.json", b"[]", "The file has no questions"),
        ("cau-hoi.json", json.dumps([{"Câu hỏi": "Q", "Đáp án A": "a", "Đáp án B": "b", "Đáp án C": "c", "Giải thích": "x"}]).encode(), "Missing column: Đáp án D, Đáp án đúng"),
        ("cau-hoi.json", b"\xff\xfe\x00", "The file could not be read as JSON"),
        ("cau-hoi.json", json.dumps([{"content": "Q"}] * 201).encode(), "Missing column: Đáp án A, Đáp án B, Đáp án C, Đáp án D, Đáp án đúng"),
        ("cau-hoi.xlsx", huge_dimension(), "The sheet is too large: at most 200 questions and 20 columns"),
        ("cau-hoi.xlsx", huge_dimension(row=2, column=16000), "The sheet is too large: at most 200 questions and 20 columns"),
        ("cau-hoi.xlsx", workbook(*[(f"Q{i}", "a", "b", "c", "d", "A", None) for i in range(201)]), "At most 200 questions per file"),
    ],
)
def test_unusable_files_are_refused(bank, h, topic, filename, data, message):
    response = upload(bank, h, topic["id"], data, filename=filename)
    assert response.status_code == 400
    assert response.json()["message"] == message


def test_import_needs_an_existing_topic_and_a_lecturer(bank, h, topic):
    assert upload(bank, h, 999, workbook(*ROWS)).status_code == 404
    assert upload(bank, h, topic["id"], workbook(*ROWS), who=STUDENT).status_code == 403
    too_big = upload(bank, h, topic["id"], b"x" * (question_import.MAX_BYTES + 1), filename="big.csv")
    assert (too_big.status_code, too_big.json()["message"]) == (400, "The file is larger than 1 MB")


def test_columns_are_found_by_name_not_by_position(bank, h, topic):
    book = Workbook()
    sheet = book.active
    sheet.append(["Đáp án đúng", "Câu hỏi", "Đáp án D", "Đáp án C", "Đáp án B", "Đáp án A"])
    sheet.append(["c", "Shuffled columns", "d", "c", "b", "a"])
    buffer = io.BytesIO()
    book.save(buffer)
    body = upload(bank, h, topic["id"], buffer.getvalue(), dry_run=False).json()
    assert body["imported"] == 1
    assert (body["rows"][0]["optionA"], body["rows"][0]["optionD"], body["rows"][0]["correctOption"]) == ("a", "d", "C")


JSON_QUESTIONS = [
    {"Câu hỏi": "Q1: He ___ tea every morning.", "Đáp án A": "drink", "Đáp án B": "drinks", "Đáp án C": "drank", "Đáp án D": "drinking", "Đáp án đúng": "b", "Giải thích": "Chủ ngữ số ít."},
    {"Câu hỏi": "Q2: missing C", "Đáp án A": "a", "Đáp án B": "b", "Đáp án C": "", "Đáp án D": "d", "Đáp án đúng": "A"},
    {"Câu hỏi": "Q3: wrong answer letter", "Đáp án A": "a", "Đáp án B": "b", "Đáp án C": "c", "Đáp án D": "d", "Đáp án đúng": "E"},
    {"Câu hỏi": "she ___ to school   every day.", "Đáp án A": "go", "Đáp án B": "goes", "Đáp án C": "gone", "Đáp án D": "going", "Đáp án đúng": "B"},
    {"Câu hỏi": "1 + 1 = ?", "Đáp án A": 1, "Đáp án B": 2, "Đáp án C": 3, "Đáp án D": 4, "Đáp án đúng": "B"},
]


def test_json_files_are_checked_row_by_row_like_excel(bank, h, topic):
    data = json.dumps(JSON_QUESTIONS, ensure_ascii=False).encode()
    body = upload(bank, h, topic["id"], data, filename="cau-hoi.json").json()
    assert (body["total"], body["valid"], body["imported"], body["dryRun"]) == (5, 2, 0, True)
    assert [(row["row"], row["problems"]) for row in body["rows"]] == [
        (1, []),
        (2, ["option_c_missing"]),
        (3, ["correct_option_invalid"]),
        (4, ["duplicate"]),
        (5, []),
    ]
    assert body["rows"][0]["correctOption"] == "B"
    assert (body["rows"][4]["optionA"], body["rows"][4]["optionD"]) == ("1", "4")

    saved = upload(bank, h, topic["id"], data, filename="cau-hoi.json", dry_run=False).json()
    assert (saved["valid"], saved["imported"]) == (2, 2)
    questions = bank.get(f"/questions?topicId={topic['id']}", headers=h(TEACHER)).json()
    assert len(questions) == 3


def test_json_accepts_english_keys_and_a_utf8_bom(bank, h, topic):
    items = [{"content": "Q?", "optionA": "a", "optionB": "b", "optionC": "c", "optionD": "d", "correctOption": "d"}]
    data = b"\xef\xbb\xbf" + json.dumps(items).encode()
    body = upload(bank, h, topic["id"], data, filename="Cau-Hoi.JSON", dry_run=False).json()
    assert (body["imported"], body["rows"][0]["correctOption"]) == (1, "D")


def test_json_item_without_a_key_is_reported_on_its_row_only(bank, h, topic):
    items = [dict(JSON_QUESTIONS[0]), {k: v for k, v in JSON_QUESTIONS[4].items() if k != "Đáp án D"}]
    body = upload(bank, h, topic["id"], json.dumps(items, ensure_ascii=False).encode(), filename="q.json").json()
    assert [row["problems"] for row in body["rows"]] == [[], ["option_d_missing"]]
