"""Questions from an Excel (.xlsx), CSV or JSON file laid out like the template: one question per row.

JSON is an array of objects whose keys are the template's column names (the English names `content`, `optionA`..`optionD`,
`correctOption`, `explanation` work too). The columns are found by name, so their order does not matter.

Each row is checked on its own and reported with a list of problem codes (empty = can be imported),
so the lecturer sees exactly which rows to fix. The web app turns the codes into Vietnamese text.
"""

import csv
import io
import json
from dataclasses import dataclass, field
from typing import Any

from openpyxl import Workbook, load_workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.worksheet.datavalidation import DataValidation

COLUMNS = ["Câu hỏi", "Đáp án A", "Đáp án B", "Đáp án C", "Đáp án D", "Đáp án đúng", "Giải thích"]
#: Column names accepted in the header row / JSON keys, by field. The first one is the template's.
ALIASES = {
    "content": ("Câu hỏi", "content", "question"),
    "option_a": ("Đáp án A", "optionA", "option_a"),
    "option_b": ("Đáp án B", "optionB", "option_b"),
    "option_c": ("Đáp án C", "optionC", "option_c"),
    "option_d": ("Đáp án D", "optionD", "option_d"),
    "correct_option": ("Đáp án đúng", "correctOption", "correct_option"),
    "explanation": ("Giải thích", "explanation"),
}
#: Every column but the explanation must exist in the file
REQUIRED = tuple(field for field in ALIASES if field != "explanation")
EXAMPLES = [
    ["She ___ to school every day.", "go", "goes", "gone", "going", "B", "Chủ ngữ số ít ở thì hiện tại đơn: động từ thêm -es."],
    ["They ___ football yesterday.", "play", "plays", "played", "playing", "C", "Có yesterday nên dùng thì quá khứ đơn."],
]
XLSX_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
MAX_BYTES = 1_000_000
MAX_QUESTIONS = 200
#: The header plus at most MAX_QUESTIONS + 1 data rows (one more than allowed, so "too many" is still reported)
MAX_XLSX_ROWS = MAX_QUESTIONS + 2
MAX_XLSX_COLUMNS = 20
#: Same limits as QuestionIn
MAX_LENGTH = {"content": 5000, "option": 1000, "explanation": 5000}


class ImportFileError(Exception):
    """The file itself cannot be used (type, size, unreadable, no rows, too many rows)."""


@dataclass
class ImportRow:
    row: int
    content: str
    option_a: str
    option_b: str
    option_c: str
    option_d: str
    correct_option: str | None
    explanation: str | None
    problems: list[str] = field(default_factory=list)


def template() -> bytes:
    """The workbook lecturers fill in: column names, two examples, A to D picker for the correct answer."""
    workbook = Workbook()
    sheet = workbook.active
    sheet.title = "Câu hỏi"
    sheet.append(COLUMNS)
    for example in EXAMPLES:
        sheet.append(example)
    for cell in sheet[1]:
        cell.font = Font(bold=True, color="FFFFFF")
        cell.fill = PatternFill("solid", fgColor="0066CA")
        cell.alignment = Alignment(vertical="center")
    for letter, width in zip("ABCDEFG", (48, 18, 18, 18, 18, 14, 48)):
        sheet.column_dimensions[letter].width = width
    sheet.freeze_panes = "A2"
    picker = DataValidation(type="list", formula1='"A,B,C,D"', allow_blank=False)
    picker.add(f"F2:F{MAX_QUESTIONS + 1}")
    sheet.add_data_validation(picker)
    buffer = io.BytesIO()
    workbook.save(buffer)
    return buffer.getvalue()


def read_rows(filename: str, data: bytes) -> list[tuple[int, dict[str, str]]]:
    """(row number, cells by field) for every non-empty row.

    Excel and CSV: row 1 holds the column names, so the first question is row 2, like in Excel. JSON: the position in the array.
    """
    name = filename.lower()
    if name.endswith(".json"):
        numbered = _json_rows(data)
    elif name.endswith(".xlsx") or name.endswith(".csv"):
        table = _xlsx_rows(data) if name.endswith(".xlsx") else _csv_rows(data)
        numbered = _table_rows(table)
    else:
        raise ImportFileError("Only .xlsx, .csv and .json files are supported")
    numbered = [(number, cells) for number, cells in numbered if any(cells.values())]
    if not numbered:
        raise ImportFileError("The file has no questions")
    if len(numbered) > MAX_QUESTIONS:
        raise ImportFileError(f"At most {MAX_QUESTIONS} questions per file")
    return numbered


def check(rows: list[tuple[int, dict[str, str]]]) -> list[ImportRow]:
    """Turns the cells into questions and lists what is wrong with each one."""
    checked = []
    for number, cells in rows:
        content, option_a, option_b, option_c, option_d, correct, explanation = (cells.get(field, "") for field in ALIASES)
        problems = []
        if not content:
            problems.append("content_missing")
        for letter, text in zip("abcd", (option_a, option_b, option_c, option_d)):
            if not text:
                problems.append(f"option_{letter}_missing")
        correct = correct.upper()
        if correct not in ("A", "B", "C", "D"):
            problems.append("correct_option_invalid")
        too_long = (
            len(content) > MAX_LENGTH["content"]
            or any(len(text) > MAX_LENGTH["option"] for text in (option_a, option_b, option_c, option_d))
            or len(explanation) > MAX_LENGTH["explanation"]
        )
        if too_long:
            problems.append("too_long")
        checked.append(
            ImportRow(
                row=number,
                content=content,
                option_a=option_a,
                option_b=option_b,
                option_c=option_c,
                option_d=option_d,
                correct_option=correct if correct in ("A", "B", "C", "D") else None,
                explanation=explanation or None,
                problems=problems,
            )
        )
    return checked


def same_question_key(content: str) -> str:
    """Spaces and letter case do not make a question different."""
    return " ".join(content.split()).lower()


def _text(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, float) and value.is_integer():
        value = int(value)  # Excel stores 1990 as 1990.0
    return str(value).strip()


def _xlsx_rows(data: bytes) -> list[list[str]]:
    try:
        workbook = load_workbook(io.BytesIO(data), read_only=True, data_only=True)
    except Exception as exc:  # not a zip, not a workbook, damaged file...
        raise ImportFileError("The file could not be read as an Excel workbook") from exc
    try:
        sheet = workbook.worksheets[0]
        # A tiny file can declare a huge dimension; refuse it instead of materialising that many cells
        if (sheet.max_row or 0) > MAX_XLSX_ROWS or (sheet.max_column or 0) > MAX_XLSX_COLUMNS:
            raise ImportFileError(f"The sheet is too large: at most {MAX_QUESTIONS} questions and {MAX_XLSX_COLUMNS} columns")
        rows = sheet.iter_rows(max_row=MAX_XLSX_ROWS, max_col=MAX_XLSX_COLUMNS, values_only=True)
        return [[_text(value) for value in row] for row in rows]
    finally:
        workbook.close()


def _csv_rows(data: bytes) -> list[list[str]]:
    try:
        text = data.decode("utf-8-sig")  # Excel adds a BOM when saving "CSV UTF-8"
    except UnicodeDecodeError as exc:
        raise ImportFileError("CSV files must be saved as UTF-8") from exc
    return [[cell.strip() for cell in row] for row in csv.reader(io.StringIO(text))]


def _field_of(column: Any) -> str | None:
    key = _text(column).casefold()
    for field, names in ALIASES.items():
        if key in (name.casefold() for name in names):
            return field
    return None


def _require_columns(fields: set[str | None]) -> None:
    missing = [ALIASES[field][0] for field in REQUIRED if field not in fields]
    if missing:
        raise ImportFileError(f"Missing column: {', '.join(missing)}")


def _table_rows(table: list[list[str]]) -> list[tuple[int, dict[str, str]]]:
    """Excel / CSV: the header row says which column holds what."""
    if not table:
        raise ImportFileError("The file has no questions")
    fields = [_field_of(column) for column in table[0]]
    _require_columns(set(fields))
    return [
        (index, {field: cell for field, cell in zip(fields, cells) if field})
        for index, cells in enumerate(table, start=1)
        if index > 1
    ]


def _json_rows(data: bytes) -> list[tuple[int, dict[str, str]]]:
    try:
        items = json.loads(data.decode("utf-8-sig"))
    except (UnicodeDecodeError, ValueError) as exc:
        raise ImportFileError("The file could not be read as JSON") from exc
    if not isinstance(items, list):
        raise ImportFileError("The JSON file must contain an array of questions")
    if not all(isinstance(item, dict) for item in items):
        raise ImportFileError("Every JSON item must be an object with the question's fields")
    present = {_field_of(key) for item in items for key in item}
    if items:
        _require_columns(present)
    return [
        (number, {field: _text(value) for key, value in item.items() if (field := _field_of(key))})
        for number, item in enumerate(items, start=1)
    ]
