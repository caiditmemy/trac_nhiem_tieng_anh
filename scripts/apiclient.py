"""Small HTTP client shared by smoke.py and demo.py: JSON calls through the web proxy, standard library only."""
from pathlib import Path
import json, urllib.error, urllib.request, uuid
CFG = json.loads((Path(__file__).parent / 'app.json').read_text())
BASE = f"http://127.0.0.1:{CFG['ports']['web']}/api"
PASSWORD = 'DemoUser@123'
REQUEST_ID = 'sprint1-script'

def configure(base, request_id=None):
    global BASE, REQUEST_ID
    BASE = base.rstrip('/')
    if request_id: REQUEST_ID = request_id

def _send(req, path, status):
    try: res = urllib.request.urlopen(req, timeout=30)
    except urllib.error.HTTPError as error: res = error
    with res:
        raw = res.read(); payload = json.loads(raw) if raw else None
        assert res.status == status, (req.get_method(), path, res.status, status, payload)
        assert res.headers.get('X-Request-ID') == REQUEST_ID, (path, 'request id')
        if status == 429: assert int(res.headers.get('Retry-After', '0')) > 0
    return payload

def call(method, path, body=None, token=None, status=200):
    headers = {'Content-Type': 'application/json', 'X-Request-ID': REQUEST_ID}
    if token: headers['Authorization'] = 'Bearer ' + token
    data = json.dumps(body).encode() if body is not None else None
    return _send(urllib.request.Request(BASE + path, data=data, headers=headers, method=method), path, status)

def upload(path, filename, content, token, status=200):
    """multipart/form-data with one `file` field, like the browser sends it."""
    boundary = uuid.uuid4().hex
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\n'
            f'Content-Type: application/octet-stream\r\n\r\n').encode() + content + f'\r\n--{boundary}--\r\n'.encode()
    headers = {'Content-Type': f'multipart/form-data; boundary={boundary}', 'X-Request-ID': REQUEST_ID, 'Authorization': 'Bearer ' + token}
    return _send(urllib.request.Request(BASE + path, data=body, headers=headers, method='POST'), path, status)

def login(email, password=PASSWORD):
    return call('POST', '/auth/login', {'email': email, 'password': password})
