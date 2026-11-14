#!/usr/bin/env python3
"""Linux local-only stack; owns only processes recorded with matching /proc start time."""
from pathlib import Path
import argparse, json, os, shutil, signal, socket, subprocess, time, urllib.request
ROOT = Path(__file__).resolve().parents[1]
CFG = json.loads((ROOT / 'scripts/app.json').read_text())
STATE = ROOT / '.runtime'
#: Start order; stop() goes through it backwards. class and learning only need auth, exercise needs both of them.
START_ORDER = ['postgres', 'auth', 'class', 'learning', 'exercise', 'gateway', 'web']

def start_time(pid):
    try:
        return Path(f'/proc/{pid}/stat').read_text().split(') ', 1)[1].split()[19]
    except FileNotFoundError:
        return None

def stop():
    for name in reversed(START_ORDER):
        record = STATE / f'{name}.json'
        if not record.exists(): continue
        info = json.loads(record.read_text())
        if start_time(info['pid']) == info['started']:
            os.killpg(info['pid'], signal.SIGTERM)
            for _ in range(200):
                if start_time(info['pid']) is None: break
                # A dead child can remain a zombie until its parent reaps it.
                try:
                    if Path(f"/proc/{info['pid']}/stat").read_text().split(') ',1)[1].startswith('Z'): break
                except FileNotFoundError: break
                time.sleep(.1)
            else:
                raise RuntimeError(f'{name} did not stop; keeping its ownership record')
        record.unlink()
    print('Stopped only this worktree processes; database retained.')

def launch(name, command, cwd, env):
    with (STATE / f'{name}.log').open('a') as log:
        child = subprocess.Popen(command, cwd=cwd, env=env, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
    (STATE / f'{name}.json').write_text(json.dumps({'pid': child.pid, 'started': start_time(child.pid)}))
    return child

def wait_url(url, child, timeout=120):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if child.poll() is not None: raise RuntimeError(f'Process exited: {url}; see .runtime/*.log')
        try:
            with urllib.request.urlopen(url, timeout=2) as response:
                if response.status == 200: return
        except OSError: pass
        time.sleep(.5)
    raise RuntimeError(f'Timed out: {url}; see .runtime/*.log')

SPRING = {'auth': 'auth-service', 'class': 'class-service', 'exercise': 'exercise-service'}

def jar_of(name):
    return ROOT/f'services/{SPRING[name]}/target/{SPRING[name]}-0.1.0-SNAPSHOT.jar'

def url(name):
    return f"http://127.0.0.1:{CFG['ports'][name]}"

def database_url(name, scheme):
    return f"{scheme}://127.0.0.1:{CFG['ports']['postgres']}/{CFG['databases'][name]}"

def spring_env(name):
    """Environment of one Spring service (see its application.yml): its own database, auth keys, other services."""
    env=dict(SERVER_ADDRESS='127.0.0.1',SERVER_PORT=str(CFG['ports'][name]),
        SPRING_DATASOURCE_URL=database_url(name,'jdbc:postgresql'),SPRING_DATASOURCE_USERNAME=CFG['db_user'],SPRING_DATASOURCE_PASSWORD=CFG['db_password'])
    if name == 'auth':
        env.update(ADMIN_EMAIL=CFG['admin_email'],ADMIN_PASSWORD=CFG['admin_password'])
    else:
        env.update(JWKS_URL=url('auth')+'/.well-known/jwks.json',AUTH_SERVICE_URL=url('auth'))
    if name == 'exercise':
        env.update(CLASS_SERVICE_URL=url('class'),LEARNING_SERVICE_URL=url('learning'))
    return env

def learning_env():
    return dict(DATABASE_URL=f"postgresql+psycopg://{CFG['db_user']}:{CFG['db_password']}@127.0.0.1:{CFG['ports']['postgres']}/{CFG['databases']['learning']}",
        JWKS_URL=url('auth')+'/.well-known/jwks.json',AUTH_SERVICE_URL=url('auth'))

def up():
    STATE.mkdir(exist_ok=True)
    if list(STATE.glob('*.json')): raise RuntimeError('Existing process records; run down before up')
    # Check every port before creating data or launching any child. Never stop another listener.
    sockets=[]
    try:
        for port in CFG['ports'].values():
            s=socket.socket(); sockets.append(s); s.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1); s.bind(('127.0.0.1',port))
    finally:
        for s in sockets: s.close()
    pg=Path(os.environ.get('PG_BIN', '/usr/lib/postgresql/17/bin'))
    java=Path(os.environ['JAVA_HOME'])/'bin/java' if 'JAVA_HOME' in os.environ else shutil.which('java')
    for exe in [pg/'postgres', pg/'initdb', java, shutil.which('uv'), shutil.which('npm')]:
        if not exe or not Path(exe).is_file(): raise RuntimeError(f'Missing runtime: {exe}; set JAVA_HOME and PG_BIN')
    if not all(jar_of(name).exists() for name in SPRING) or not (ROOT/'web/node_modules').exists(): raise RuntimeError('Run make build first')
    env=os.environ.copy()
    env.setdefault("UV_CACHE_DIR", str(STATE/"uv-cache"))
    env.update(PGPASSFILE="/dev/null", PGSERVICEFILE="/dev/null")
    data=STATE/'pgdata'; sock=STATE/'socket'; sock.mkdir(exist_ok=True)
    if not data.exists():
        subprocess.run([str(pg/'initdb'),'-D',str(data),'-U',CFG['db_user'],'--auth=trust','--encoding=UTF8','--locale=C'],check=True,stdout=(STATE/'initdb.log').open('w'))
    # Trust authentication is restricted to loopback on a private demo cluster, never an existing database.
    try:
        child=launch('postgres',[str(pg/'postgres'),'-D',str(data),'-h','127.0.0.1','-p',str(CFG['ports']['postgres']),'-k',str(sock)],ROOT,env)
        for _ in range(100):
            if child.poll() is not None: raise RuntimeError('Postgres exited; see .runtime/postgres.log')
            try:
                with socket.create_connection(('127.0.0.1',CFG['ports']['postgres']),timeout=.2): break
            except OSError: time.sleep(.1)
        # Use the existing psql client; only our isolated cluster is queried.
        psql=shutil.which('psql') or str(pg/'psql')
        dbargs=[psql,'-h','127.0.0.1','-p',str(CFG['ports']['postgres']),'-U',CFG['db_user'],'-d','postgres','-v','ON_ERROR_STOP=1']
        for _ in range(100):
            result=subprocess.run(dbargs+['-tAc','SELECT 1'],capture_output=True,text=True,env=env)
            if result.returncode == 0: break
            if child.poll() is not None: raise RuntimeError('Postgres exited before SQL readiness')
            time.sleep(.1)
        else: raise RuntimeError('Postgres SQL readiness timed out')
        for db in CFG['databases'].values():
            exists=subprocess.run(dbargs+['-tAc',f"SELECT 1 FROM pg_database WHERE datname='{db}'"],capture_output=True,text=True,check=True,env=env)
            if exists.stdout.strip()!='1': subprocess.run(dbargs+['-c',f'CREATE DATABASE {db}'],check=True,env=env)
        def start_spring(name):
            process=launch(name,[str(java),'-Xms64m','-Xmx256m','-jar',str(jar_of(name))],ROOT,{**env,**spring_env(name)})
            wait_url(url(name)+'/actuator/health',process)
        def start_learning():
            service_env={**env,**learning_env()}
            cwd=ROOT/'services/learning-service'
            # Same as the Dockerfile: apply the migrations, then start the API
            with (STATE/'learning-migrate.log').open('a') as log:
                subprocess.run(['uv','run','--locked','alembic','upgrade','head'],cwd=cwd,env=service_env,check=True,stdout=log,stderr=subprocess.STDOUT)
            process=launch('learning',['uv','run','--locked','python','-m','uvicorn','learning_service.main:app','--host','127.0.0.1','--port',str(CFG['ports']['learning'])],cwd,service_env)
            wait_url(url('learning')+'/health',process)
        start_spring('auth')
        start_spring('class')
        start_learning()
        start_spring('exercise')
        env.update(AUTH_SERVICE_URL=url('auth'),JWKS_URL=url('auth')+'/.well-known/jwks.json',CLASS_SERVICE_URL=url('class'),
            EXERCISE_SERVICE_URL=url('exercise'),LEARNING_SERVICE_URL=url('learning'),CORS_ORIGINS=url('web'))
        gateway=launch('gateway',['uv','run','--locked','python','-m','uvicorn','gateway.main:app','--host','127.0.0.1','--port',str(CFG['ports']['gateway'])],ROOT/'services/gateway',env)
        wait_url(url('gateway')+'/api/health',gateway)
        web=launch('web',['npm','run','dev'],ROOT/'web',env)
        wait_url(url('web')+'/api/health',web)
    except BaseException:
        stop(); raise
    print(f"Ready: {url('web')} ({CFG['owner']})")

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__); parser.add_argument('action',choices=['up','down']); args=parser.parse_args()
    up() if args.action=='up' else stop()
