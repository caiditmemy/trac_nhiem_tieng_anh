#!/usr/bin/env python3
"""Real HTTP integration through the web proxy, gateway, Spring services and isolated PostgreSQL. No mocks or credential-file reads.

auth checks, then the class flow: a teacher creates a class and gets its 6-character invite code, a student joins by code
(any letter case) and the teacher sees the student in the class.
"""
import argparse, base64, json, time, urllib.request, uuid
import apiclient
from apiclient import CFG, PASSWORD, call, login

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--api',default=apiclient.BASE); parser.add_argument('--auth',default=f"http://127.0.0.1:{CFG['ports']['auth']}")
    args=parser.parse_args(); apiclient.configure(args.api,'sprint1-nam-smoke')
    assert call('GET','/health')['status']=='UP'
    admin_session=login(CFG['admin_email'],CFG['admin_password'])
    assert admin_session['user']['role']=='ADMIN'
    admin=admin_session['accessToken']
    admin_profile=call('GET','/users/me',token=admin)
    assert admin_profile['role']=='ADMIN'
    assert call('PUT','/users/me',{'fullName':admin_profile['fullName']},admin)['id']==admin_profile['id']
    tag=uuid.uuid4().hex[:12]; email=f'student-{tag}@sprint1-nam.local'
    user=call('POST','/auth/register',{'email':email,'password':PASSWORD,'fullName':'Sinh viên kiểm thử'},status=201)
    assert user['user']['role']=='STUDENT'
    student=user['accessToken']; sid=user['user']['id']; created=[sid]
    try:
        header=json.loads(base64.urlsafe_b64decode(student.split('.')[0]+'=='))
        assert header['alg']=='RS256' and header['kid']
        with urllib.request.urlopen(args.auth+"/.well-known/jwks.json") as response: keys=json.load(response)['keys']
        assert any(k['kid']==header['kid'] for k in keys) and all('d' not in k for k in keys)
        call('POST','/auth/register',{'email':email,'password':PASSWORD,'fullName':'Trùng'},status=409)
        call('POST','/auth/login',{'email':email,'password':'wrong-password'},status=401)
        student=login(email)['accessToken']
        call('GET','/users/me',status=401); call('GET','/users/me',token='bad.token.value',status=401)
        student_profile=call('GET','/users/me',token=student)
        assert student_profile['email']==email and student_profile['role']=='STUDENT'
        call('POST','/users',{'email':f'forbidden-{tag}@sprint1-nam.local','password':PASSWORD,'fullName':'Forbidden','role':'ADMIN'},student,403)
        call('PUT',f'/users/{sid}/role',{'role':'ADMIN'},student,403)
        call('GET','/users',token=student,status=403)
        assert call('PUT','/users/me',{'fullName':'Tên đã lưu'},student)['fullName']=='Tên đã lưu'
        call('PUT','/users/me/password',{'currentPassword':'wrong-password','newPassword':'Changed@123'},student,403)
        call('PUT','/users/me/password',{'currentPassword':PASSWORD,'newPassword':'Changed@123'},student,204)
        call('GET','/users/me',token=student) # own password change preserves current session
        login(email,'Changed@123')
        teacher=call('POST','/users',{'email':f'teacher-{tag}@sprint1-nam.local','password':PASSWORD,'fullName':'Giáo viên kiểm thử','role':'TEACHER'},admin,201)
        created.append(teacher['id']); teacher_token=login(teacher['email'])['accessToken']
        teacher_profile=call('GET','/users/me',token=teacher_token)
        assert teacher_profile['role']=='TEACHER' and teacher_profile['id']==teacher['id']
        call('PUT',f'/users/{sid}/role',{'role':'ADMIN'},teacher_token,403)
        call('DELETE',f'/users/{sid}',token=teacher_token,status=403)
        call('GET','/users',token=teacher_token,status=403)
        assert call('PUT','/users/me',{'fullName':'Giáo viên đã lưu'},teacher_token)['fullName']=='Giáo viên đã lưu'
        call('PUT',f'/users/{sid}/account-type',{'accountType':'VIP'},admin)
        assert call('GET','/users/me',token=student)['accountType']=='VIP'
        call('PUT',f'/users/{sid}/status',{'status':'LOCKED'},admin)
        deadline=time.monotonic()+22
        while True:
            try: call('GET','/users/me',token=student,status=401); break
            except AssertionError:
                if time.monotonic()>deadline: raise
                time.sleep(.5)
        call('POST','/auth/login',{'email':email,'password':'Changed@123'},status=403)
        call('PUT',f'/users/{sid}/status',{'status':'ACTIVE'},admin)
        login(email,'Changed@123')
        call('PUT',f"/users/{teacher['id']}/role",{'role':'STUDENT'},admin)
        deadline=time.monotonic()+22
        while True:
            try: call('GET','/users/me',token=teacher_token,status=401); break
            except AssertionError:
                if time.monotonic()>deadline: raise
                time.sleep(.5)
        for _ in range(5):
            call('POST','/auth/login',{'email':email,'password':'wrong-password'},status=401)
        call('POST','/auth/login',{'email':email,'password':'Changed@123'},status=429)
        class_flow(admin,tag,created)
        print('PASS: web proxy → gateway → Spring → PostgreSQL; RS256/JWKS, register/login, 401/403/409/429 + Retry-After, all three role profiles, API role guards/password, admin CRUD, VIP, lock/session invalidation, role change; class flow: teacher creates class → 6-character code → student joins by code → teacher sees the student, student cannot create a class, wrong code rejected')
    finally:
        for user_id in reversed(created): call('DELETE',f'/users/{user_id}',token=admin,status=204)

def class_flow(admin,tag,created):
    """Everything through the web proxy and the gateway, with a fresh teacher and students."""
    teacher=call('POST','/users',{'email':f'class-teacher-{tag}@sprint1-nam.local','password':PASSWORD,'fullName':'Giáo viên lớp','role':'TEACHER'},admin,201)
    created.append(teacher['id']); t=login(teacher['email'])['accessToken']
    student=call('POST','/auth/register',{'email':f'class-student-{tag}@sprint1-nam.local','password':PASSWORD,'fullName':'Sinh viên lớp'},status=201)
    created.append(student['user']['id']); s=student['accessToken']; student_id=student['user']['id']
    outsider=call('POST','/auth/register',{'email':f'class-outsider-{tag}@sprint1-nam.local','password':PASSWORD,'fullName':'Sinh viên ngoài lớp'},status=201)
    created.append(outsider['user']['id']); o=outsider['accessToken']
    class_id=None
    try:
        # Only a teacher creates a class; it gets a 6-character invite code
        call('POST','/classes',{'name':'Student cannot'},s,403)
        classroom=call('POST','/classes',{'name':f'Lớp kiểm thử {tag}','description':'smoke'},t,201); class_id=classroom['id']
        code=classroom['code']; assert len(code)==6 and code.isalnum(), code
        assert classroom['studentCount']==0
        # Wrong codes are rejected
        call('POST','/classes/join',{'code':'ZZZZZZ'},s,404)
        call('POST','/classes/join',{'code':' '},s,400)
        call('POST','/classes/join',{'code':code},t,403)  # a teacher does not join classes
        # Student joins by code in lower case; a second join is a conflict
        assert call('POST','/classes/join',{'code':code.lower()},s)['id']==class_id
        call('POST','/classes/join',{'code':code},s,409)
        assert [c['id'] for c in call('GET','/classes',token=s)]==[class_id]
        assert call('GET','/classes',token=o)==[]
        # The teacher sees the student in the class; an outsider does not see the class
        members=call('GET',f'/classes/{class_id}/students',token=t)
        assert [m['studentId'] for m in members]==[student_id] and members[0]['email']==student['user']['email'], members
        assert call('GET',f'/classes/{class_id}',token=t)['studentCount']==1
        call('GET',f'/classes/{class_id}/students',token=s,status=403)
        call('GET',f'/classes/{class_id}',token=o,status=403)
        # Leaving takes the student out again
        call('POST',f'/classes/{class_id}/leave',token=s,status=204)
        assert call('GET',f'/classes/{class_id}/students',token=t)==[]
    finally:
        if class_id: call('DELETE',f'/classes/{class_id}',token=t,status=204)

if __name__=='__main__': main()
