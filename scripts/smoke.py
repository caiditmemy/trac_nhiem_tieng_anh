#!/usr/bin/env python3
"""Real HTTP integration through the web proxy, gateway, Spring/FastAPI services and isolated PostgreSQL. No mocks or credential-file reads.

auth checks, then the end-to-end flow: teacher creates a topic, questions (also imported from JSON), a class and an exercise
assigned from the bank, a student joins by code and sees the exercise; students cannot create questions or exercises.
"""
import argparse, base64, json, time, urllib.request, uuid
from datetime import datetime, timedelta, timezone
import apiclient
from apiclient import CFG, PASSWORD, call, login, upload

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--api',default=apiclient.BASE); parser.add_argument('--auth',default=f"http://127.0.0.1:{CFG['ports']['auth']}")
    args=parser.parse_args(); apiclient.configure(args.api,'sprint1-smoke')
    assert call('GET','/health')['status']=='UP'
    admin_session=login(CFG['admin_email'],CFG['admin_password'])
    assert admin_session['user']['role']=='ADMIN'
    admin=admin_session['accessToken']
    admin_profile=call('GET','/users/me',token=admin)
    assert admin_profile['role']=='ADMIN'
    assert call('PUT','/users/me',{'fullName':admin_profile['fullName']},admin)['id']==admin_profile['id']
    tag=uuid.uuid4().hex[:12]; email=f'student-{tag}@sprint1.local'
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
        call('POST','/users',{'email':f'forbidden-{tag}@sprint1.local','password':PASSWORD,'fullName':'Forbidden','role':'ADMIN'},student,403)
        call('PUT',f'/users/{sid}/role',{'role':'ADMIN'},student,403)
        call('GET','/users',token=student,status=403)
        assert call('PUT','/users/me',{'fullName':'Tên đã lưu'},student)['fullName']=='Tên đã lưu'
        call('PUT','/users/me/password',{'currentPassword':'wrong-password','newPassword':'Changed@123'},student,403)
        call('PUT','/users/me/password',{'currentPassword':PASSWORD,'newPassword':'Changed@123'},student,204)
        call('GET','/users/me',token=student) # own password change preserves current session
        login(email,'Changed@123')
        teacher=call('POST','/users',{'email':f'teacher-{tag}@sprint1.local','password':PASSWORD,'fullName':'Giáo viên kiểm thử','role':'TEACHER'},admin,201)
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
        learning_flow(admin,tag,created)
        print('PASS: web proxy → gateway → Spring → PostgreSQL; RS256/JWKS, register/login, 401/403/409/429 + Retry-After, all three role profiles, API role guards/password, admin CRUD, VIP, lock/session invalidation, role change; E2E topic → questions (+JSON import, invalid file 400) → class → assigned exercise (questions copied) → student joins by code and sees it; student 403 on questions/exercises')
    finally:
        for user_id in reversed(created): call('DELETE',f'/users/{user_id}',token=admin,status=204)

def learning_flow(admin,tag,created):
    """End-to-end: everything through the web proxy and the gateway, with a fresh teacher, students and data."""
    teacher=call('POST','/users',{'email':f'e2e-teacher-{tag}@sprint1.local','password':PASSWORD,'fullName':'Giáo viên E2E','role':'TEACHER'},admin,201)
    created.append(teacher['id']); t=login(teacher['email'])['accessToken']
    student=call('POST','/auth/register',{'email':f'e2e-student-{tag}@sprint1.local','password':PASSWORD,'fullName':'Sinh viên E2E'},status=201)
    created.append(student['user']['id']); s=student['accessToken']
    outsider=call('POST','/auth/register',{'email':f'e2e-outsider-{tag}@sprint1.local','password':PASSWORD,'fullName':'Sinh viên ngoài lớp'},status=201)
    created.append(outsider['user']['id']); o=outsider['accessToken']
    class_id=exercise_id=topic_id=None; question_ids=[]
    try:
        # Paths the gateway does not route
        call('POST','/activities/exercise',{},s,404)
        call('GET','/practice/limit',token=s,status=404)
        # Teacher: topic, 3 questions (answers B, C, A), one more from a JSON file
        call('GET','/topics',token=s)  # students may read topics ...
        call('GET','/questions',token=s,status=403)  # ... but not the bank with its answers
        call('POST','/topics',{'name':f'Student {tag}','description':''},s,403)
        topic_id=call('POST','/topics',{'name':f'E2E {tag}','description':'smoke'},t,201)['id']
        body=lambda n,right:{'topicId':topic_id,'content':f'Question {n} {tag}','optionA':'a','optionB':'b','optionC':'c','optionD':'d','correctOption':right,'explanation':f'Because {right}'}
        call('POST','/questions',body(0,'A'),s,403)
        for n,right in enumerate('BCA',start=1):
            question_ids.append(call('POST','/questions',body(n,right),t,201)['id'])
        row={'Câu hỏi':f'From JSON {tag}','Đáp án A':'a','Đáp án B':'b','Đáp án C':'c','Đáp án D':'d','Đáp án đúng':'d'}
        assert upload(f'/questions/import?topicId={topic_id}&dryRun=true','q.json',json.dumps([row,{**row,'Câu hỏi':'Bad','Đáp án đúng':'E'}]).encode(),t)['valid']==1
        assert upload(f'/questions/import?topicId={topic_id}&dryRun=false','q.json',json.dumps([row]).encode(),t)['imported']==1
        assert upload(f'/questions/import?topicId={topic_id}&dryRun=true','q.json','[{"Câu hỏi": "x"}]'.encode(),t,400)['message'].startswith('Missing column')
        upload(f'/questions/import?topicId={topic_id}&dryRun=true','q.json',json.dumps([row]).encode(),s,403)
        bank=call('GET',f'/questions?topicId={topic_id}',token=t); assert len(bank)==4
        # Teacher creates the class: a 6-character invite code
        classroom=call('POST','/classes',{'name':f'Lớp E2E {tag}','description':'smoke'},t,201); class_id=classroom['id']
        code=classroom['code']; assert len(code)==6 and code.isalnum(), code
        call('POST','/classes',{'name':'Student cannot'},s,403)
        # ... assigns the exercise from the bank (the student is not in the class yet)
        due=(datetime.now(timezone.utc)+timedelta(days=2)).strftime('%Y-%m-%dT%H:%M:%SZ')
        exercise_body={'classId':class_id,'title':'Bài E2E','description':'','dueAt':due,'questionIds':question_ids}
        call('POST','/exercises',exercise_body,s,403)
        call('POST','/exercises',{**exercise_body,'dueAt':'2020-01-01T00:00:00Z'},t,400)
        call('POST','/exercises',{**exercise_body,'questionIds':[]},t,400)
        exercise=call('POST','/exercises',exercise_body,t,201); exercise_id=exercise['id']
        assert exercise['questionCount']==3 and exercise['className']==classroom['name']
        # The questions are copies: editing the bank afterwards does not change the exercise
        call('PUT',f'/questions/{question_ids[0]}',{**body(1,'D'),'content':f'Edited {tag}'},t)
        detail=call('GET',f'/exercises/{exercise_id}',token=t)
        assert [q['correctOption'] for q in detail['questions']]==['B','C','A'] and detail['questions'][0]['content']==f'Question 1 {tag}', detail
        call('GET',f'/exercises/{exercise_id}',token=s,status=403)
        # Student joins by code (any letter case) and sees the assigned exercise
        call('POST','/classes/join',{'code':'ZZZZZZ'},s,404)
        assert call('POST','/classes/join',{'code':code.lower()},s)['id']==class_id
        call('POST','/classes/join',{'code':code},s,409)
        mine=[e for e in call('GET','/exercises',token=s) if e['id']==exercise_id]
        assert len(mine)==1 and mine[0]['questionCount']==3 and mine[0]['studentCount'] is None, mine
        assert [e['title'] for e in call('GET',f'/exercises?classId={class_id}',token=s)]==['Bài E2E']
        assert call('GET','/exercises',token=o)==[]  # not in the class
        call('GET',f'/exercises?classId={class_id}',token=o,status=403)
        call('GET',f'/exercises/{exercise_id}',token=o,status=403)
        listed=[e for e in call('GET','/exercises',token=t) if e['id']==exercise_id]
        assert len(listed)==1 and listed[0]['studentCount']==1, listed
        call('DELETE',f'/exercises/{exercise_id}',token=s,status=403)
    finally:
        if exercise_id: call('DELETE',f'/exercises/{exercise_id}',token=t,status=204)
        if class_id: call('DELETE',f'/classes/{class_id}',token=t,status=204)
        for qid in call('GET',f'/questions?topicId={topic_id}',token=t) if topic_id else []: call('DELETE',f"/questions/{qid['id']}",token=t,status=204)
        if topic_id: call('DELETE',f'/topics/{topic_id}',token=t,status=204)

if __name__=='__main__': main()
