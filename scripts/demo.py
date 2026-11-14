#!/usr/bin/env python3
"""`make demo`: demo accounts and a class with its invite code that the demo student has already joined (seed_data.json).

Everything goes through the web proxy and the gateway like a client would, and it is safe to run again:
what exists is reused.
"""
from pathlib import Path
import argparse, json
import apiclient
from apiclient import CFG, PASSWORD, call, login

SEED = json.loads((Path(__file__).parent / 'seed_data.json').read_text())

def account(admin, role):
    email = f"{role.lower()}@{CFG['owner']}.local"
    if not any(u['email'] == email for u in call('GET', '/users', token=admin)):
        call('POST', '/users', {'email': email, 'password': PASSWORD, 'fullName': f'Demo {role}', 'role': role}, admin, 201)
    return email

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--api', default=apiclient.BASE)
    apiclient.configure(parser.parse_args().api, 'sprint1-nam-demo')
    admin = login(CFG['admin_email'], CFG['admin_password'])['accessToken']
    teacher_email, student_email = account(admin, 'TEACHER'), account(admin, 'STUDENT')
    teacher, student = login(teacher_email)['accessToken'], login(student_email)['accessToken']

    wanted = SEED['demo']['class']
    classroom = next((c for c in call('GET', '/classes', token=teacher) if c['name'] == wanted['name']), None) or call('POST', '/classes', wanted, teacher, 201)
    if not any(c['id'] == classroom['id'] for c in call('GET', '/classes', token=student)):
        call('POST', '/classes/join', {'code': classroom['code']}, student)

    print(f"Class: {classroom['name']} - invite code {classroom['code']} ({student_email} has joined)")
    print(f"ADMIN: {CFG['admin_email']} / {CFG['admin_password']}")
    print(f"TEACHER: {teacher_email} / {PASSWORD}")
    print(f"STUDENT: {student_email} / {PASSWORD}")

if __name__ == '__main__': main()
