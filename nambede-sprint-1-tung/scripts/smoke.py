#!/usr/bin/env python3
"""Real HTTP integration through the web proxy, gateway, Spring and isolated PostgreSQL. No mocks or credential-file reads."""
from pathlib import Path
import argparse, base64, json, time, urllib.error, urllib.request, uuid
CFG=json.loads((Path(__file__).parent/'branch.json').read_text())
BASE=f"http://127.0.0.1:{CFG['ports']['web']}/api"
PASSWORD='DemoUser@123'

def call(method,path,body=None,token=None,status=200):
    headers={'Content-Type':'application/json','X-Request-ID':'split-smoke'}
    if token: headers['Authorization']='Bearer '+token
    req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,headers=headers,method=method)
    try: res=urllib.request.urlopen(req,timeout=20)
    except urllib.error.HTTPError as error: res=error
    with res:
        raw=res.read(); payload=json.loads(raw) if raw else None
        assert res.status==status, (method,path,res.status,status,payload)
        assert res.headers.get('X-Request-ID')=='split-smoke', (path,'request id')
        if status == 429: assert int(res.headers.get('Retry-After', '0')) > 0
    return payload

def login(email,password=PASSWORD):
    return call('POST','/auth/login',{'email':email,'password':password})

def main():
    global BASE
    parser=argparse.ArgumentParser(); parser.add_argument('--demo',action='store_true')
    parser.add_argument('--api',default=BASE); parser.add_argument('--auth',default=f"http://127.0.0.1:{CFG['ports']['auth']}")
    args=parser.parse_args(); BASE=args.api.rstrip('/')
    assert call('GET','/health')['status']=='UP'
    admin=login(CFG['admin_email'],CFG['admin_password'])['accessToken']
    if args.demo:
        for role in ['TEACHER','STUDENT']:
            email=f"{role.lower()}@{CFG['owner']}.local"
            users=call('GET','/users',token=admin)
            if not any(u['email']==email for u in users):
                call('POST','/users',{'email':email,'password':PASSWORD,'fullName':f'Demo {role}','role':role},admin,201)
            print(f'{role}: {email} / {PASSWORD}')
        print(f"ADMIN: {CFG['admin_email']} / {CFG['admin_password']}")
        return
    tag=uuid.uuid4().hex[:12]; email=f'student-{tag}@split.local'
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
        assert call('GET','/users/me',token=student)['email']==email
        call('GET','/users',token=student,status=403)
        assert call('PUT','/users/me',{'fullName':'Tên đã lưu'},student)['fullName']=='Tên đã lưu'
        call('PUT','/users/me/password',{'currentPassword':'wrong-password','newPassword':'Changed@123'},student,403)
        call('PUT','/users/me/password',{'currentPassword':PASSWORD,'newPassword':'Changed@123'},student,204)
        call('GET','/users/me',token=student) # own password change preserves current session
        login(email,'Changed@123')
        teacher=call('POST','/users',{'email':f'teacher-{tag}@split.local','password':PASSWORD,'fullName':'Giáo viên kiểm thử','role':'TEACHER'},admin,201)
        created.append(teacher['id']); teacher_token=login(teacher['email'])['accessToken']
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
        print('PASS: web proxy → gateway → Spring → PostgreSQL; RS256/JWKS, register/login, 401/403/409/429 + Retry-After, profile/password, admin CRUD, VIP, lock/session invalidation, role change')
    finally:
        for user_id in reversed(created): call('DELETE',f'/users/{user_id}',token=admin,status=204)

if __name__=='__main__': main()
