#!/usr/bin/env python3
"""`make demo`: demo accounts, a question bank and vocabulary (seed_data.json), a class with its invite code and an exercise.

Everything goes through the web proxy and the gateway like a client would, and it is safe to run again:
what exists is reused. The questions are imported with the JSON import of the question bank.
"""
from datetime import datetime, timedelta, timezone
from pathlib import Path
import argparse, json
import apiclient
from apiclient import CFG, PASSWORD, call, login, upload

SEED = json.loads((Path(__file__).parent / 'seed_data.json').read_text())

def account(admin, role):
    email = f"{role.lower()}@{CFG['owner']}.local"
    if not any(u['email'] == email for u in call('GET', '/users', token=admin)):
        call('POST', '/users', {'email': email, 'password': PASSWORD, 'fullName': f'Demo {role}', 'role': role}, admin, 201)
    return email

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--api', default=apiclient.BASE)
    apiclient.configure(parser.parse_args().api, 'sprint1-demo')
    admin = login(CFG['admin_email'], CFG['admin_password'])['accessToken']
    teacher_email, student_email = account(admin, 'TEACHER'), account(admin, 'STUDENT')
    teacher, student = login(teacher_email)['accessToken'], login(student_email)['accessToken']

    topics = {t['name']: t for t in call('GET', '/topics', token=teacher)}
    imported = 0
    for seed in SEED['topics']:
        topic = topics.get(seed['name']) or call('POST', '/topics', {'name': seed['name'], 'description': seed['description']}, teacher, 201)
        topics[seed['name']] = topic
        result = upload(f"/questions/import?topicId={topic['id']}&dryRun=false", 'questions.json', json.dumps(seed['questions'], ensure_ascii=False).encode(), teacher)
        imported += result['imported']
    words = {w['word'] for w in call('GET', '/vocabularies', token=student)}
    for word in SEED['vocabulary']:
        if word['word'] not in words: call('POST', '/vocabularies', word, student, 201)

    wanted = SEED['demo']['class']
    classroom = next((c for c in call('GET', '/classes', token=teacher) if c['name'] == wanted['name']), None) or call('POST', '/classes', wanted, teacher, 201)
    if not any(c['id'] == classroom['id'] for c in call('GET', '/classes', token=student)):
        call('POST', '/classes/join', {'code': classroom['code']}, student)

    spec = SEED['demo']['exercise']
    if not any(e['title'] == spec['title'] for e in call('GET', f"/exercises?classId={classroom['id']}", token=teacher)):
        ids = []
        for name, count in spec['topics'].items():
            ids += [q['id'] for q in call('GET', f"/questions?topicId={topics[name]['id']}", token=teacher)][:count]
        due = (datetime.now(timezone.utc) + timedelta(days=spec['days'])).strftime('%Y-%m-%dT%H:%M:%SZ')
        call('POST', '/exercises', {'classId': classroom['id'], 'title': spec['title'], 'description': spec['description'], 'dueAt': due, 'questionIds': ids}, teacher, 201)

    print(f"Question bank: {len(topics)} topics, {imported} new questions imported from JSON; {len(SEED['vocabulary'])} words in the student's vocabulary")
    print(f"Class: {classroom['name']} - invite code {classroom['code']} ({student_email} has joined)")
    print(f"Exercise: {spec['title']}")
    print(f"ADMIN: {CFG['admin_email']} / {CFG['admin_password']}")
    print(f"TEACHER: {teacher_email} / {PASSWORD}")
    print(f"STUDENT: {student_email} / {PASSWORD}")

if __name__ == '__main__': main()
