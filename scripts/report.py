#!/usr/bin/env python3
"""`make report`: collects test results and coverage that the tools already wrote and packages them into reports/.

JaCoCo (class, exercise, auth service), pytest + pytest-cov (gateway, learning) and Vitest (web). Writes reports/README.md with the numbers and reports/sprint1-reports.tar.gz with everything.
"""
from pathlib import Path
from xml.etree import ElementTree
import shutil, tarfile
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'reports'
SPRING = ['auth-service', 'class-service', 'exercise-service']
PYTHON = ['gateway', 'learning-service']

def junit(files):
    """(tests, failures + errors, skipped) summed over JUnit XML files (surefire, pytest, vitest)."""
    tests = failed = skipped = 0
    for file in files:
        root = ElementTree.parse(file).getroot()
        suites = [root] if root.tag == 'testsuite' else root.iter('testsuite')
        for suite in suites:
            tests += int(suite.get('tests', 0)); failed += int(suite.get('failures', 0)) + int(suite.get('errors', 0)); skipped += int(suite.get('skipped', 0))
    return tests, failed, skipped

def jacoco_lines(xml):
    for counter in ElementTree.parse(xml).getroot().findall('counter'):
        if counter.get('type') == 'LINE':
            covered, missed = int(counter.get('covered')), int(counter.get('missed'))
            return covered, covered + missed
    return 0, 0

def cobertura_lines(xml):
    root = ElementTree.parse(xml).getroot()
    return int(root.get('lines-covered', 0)), int(root.get('lines-valid', 0))

def percent(covered, total):
    return f'{100 * covered / total:.1f} %' if total else 'n/a'

def main():
    OUT.mkdir(exist_ok=True)
    for old in OUT.iterdir():  # screenshots/ is kept: it is taken from the running app, not generated here
        if old.name != 'screenshots': shutil.rmtree(old) if old.is_dir() else old.unlink()
    rows, notes = [], []
    for service in SPRING:
        base = ROOT / 'services' / service / 'target'
        files = sorted((base / 'surefire-reports').glob('TEST-*.xml'))
        if not files: notes.append(f'{service}: no test results, run `make test-java` first'); continue
        shutil.copytree(base / 'surefire-reports', OUT / 'junit' / service, ignore=shutil.ignore_patterns('*.txt', '*-output.txt'))
        coverage = 'n/a'
        if (base / 'site/jacoco/jacoco.xml').exists():
            coverage = percent(*jacoco_lines(base / 'site/jacoco/jacoco.xml'))
            shutil.copytree(base / 'site/jacoco', OUT / 'coverage' / service)
        rows.append((service, 'JUnit 5 + Mockito / MockMvc', *junit(files), coverage + ' (JaCoCo, lines)'))
    for service in PYTHON:
        base = ROOT / 'services' / service
        if not (base / 'report.xml').exists(): notes.append(f'{service}: no pytest report, run `make report`'); continue
        (OUT / 'junit' / service).mkdir(parents=True); shutil.copy(base / 'report.xml', OUT / 'junit' / service / 'pytest.xml')
        coverage = 'n/a'
        if (base / 'coverage.xml').exists():
            coverage = percent(*cobertura_lines(base / 'coverage.xml'))
            (OUT / 'coverage' / service).mkdir(parents=True); shutil.copy(base / 'coverage.xml', OUT / 'coverage' / service / 'coverage.xml')
            if (base / 'htmlcov').exists(): shutil.copytree(base / 'htmlcov', OUT / 'coverage' / service / 'html')
        rows.append((service, 'pytest', *junit([base / 'report.xml']), coverage + ' (pytest-cov, lines)'))
    web = ROOT / 'web/vitest-report.xml'
    if web.exists():
        (OUT / 'junit/web').mkdir(parents=True); shutil.copy(web, OUT / 'junit/web/vitest.xml')
        rows.append(('web', 'Vitest + Testing Library', *junit([web]), 'n/a'))
    else: notes.append('web: no Vitest report, run `make report`')

    lines = ['# Báo cáo kiểm thử Sprint 1 — Tùng (Tuần 6 + Tuần 7)', '', 'Sinh bởi `make report` (`scripts/report.py`). Không sửa tay.', '',
             '| Thành phần | Công cụ | Số test | Lỗi | Bỏ qua | Độ phủ |', '|---|---|---:|---:|---:|---|']
    lines += [f'| {name} | {tool} | {tests} | {failed} | {skipped} | {coverage} |' for name, tool, tests, failed, skipped, coverage in rows]
    lines += ['', f'Tổng: {sum(r[2] for r in rows)} test, {sum(r[3] for r in rows)} lỗi.', '']
    lines += ['## Thư mục', '', '- `junit/<thành phần>/`: kết quả từng test (XML).', '- `coverage/<thành phần>/`: báo cáo JaCoCo (`index.html`) và pytest-cov (`html/`, `coverage.xml`).', '- `sprint1-reports.tar.gz`: gói tất cả các thư mục trên.', '']
    if notes: lines += ['## Chưa có', ''] + [f'- {note}' for note in notes] + ['']
    (OUT / 'README.md').write_text('\n'.join(lines), encoding='utf-8')
    with tarfile.open(OUT / 'sprint1-reports.tar.gz', 'w:gz') as archive:
        for name in ('README.md', 'junit', 'coverage'):
            if (OUT / name).exists(): archive.add(OUT / name, arcname=f'reports/{name}')
    print('\n'.join(lines))

if __name__ == '__main__': main()
