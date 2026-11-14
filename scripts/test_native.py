"""Launcher safety: refuse occupied ports before starting services; never signal a reused PID."""
from pathlib import Path
import json, tempfile, unittest
from unittest.mock import patch
import native

class NativeSafetyTest(unittest.TestCase):
    def test_busy_port_does_not_start_or_initialize_anything(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(native, 'STATE', Path(temp)), patch.object(native.socket, 'socket') as socket_factory, patch.object(native, 'launch') as launch:
            socket_factory.return_value.bind.side_effect=OSError('address in use')
            with self.assertRaises(OSError): native.up()
            launch.assert_not_called()
            self.assertFalse((Path(temp)/'pgdata').exists())
            socket_factory.return_value.close.assert_called_once()

    def test_reused_pid_is_never_signalled(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(native, 'STATE', Path(temp)), patch.object(native, 'start_time', return_value='new-owner'), patch.object(native.os, 'killpg') as kill:
            (Path(temp)/'auth.json').write_text(json.dumps({'pid': 12345, 'started': 'old-owner'}))
            native.stop()
            kill.assert_not_called()

    def test_every_port_of_every_service_is_checked_before_anything_starts(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(native, 'STATE', Path(temp)), patch.object(native.socket, 'socket') as socket_factory, patch.object(native, 'launch') as launch, patch.dict(native.os.environ, {'PG_BIN': '/nonexistent'}):
            with self.assertRaises(RuntimeError): native.up()  # no runtime there: stops right after the port check
            asked=[call.args[0][1] for call in socket_factory.return_value.bind.call_args_list]
            self.assertEqual(sorted(asked), [5174, 8104, 8114, 8124, 8134, 8204, 55434])
            launch.assert_not_called()

    def test_stop_goes_through_the_services_in_reverse_start_order(self):
        with tempfile.TemporaryDirectory() as temp:
            alive=set(); stopped=[]
            for index, name in enumerate(native.START_ORDER):
                (Path(temp)/f'{name}.json').write_text(json.dumps({'pid': 1000+index, 'started': 'mine'})); alive.add(1000+index)
            def start_time(pid): return 'mine' if pid in alive else None
            def killpg(pid, sig): alive.discard(pid); stopped.append(pid)
            with patch.object(native, 'STATE', Path(temp)), patch.object(native, 'start_time', start_time), patch.object(native.os, 'killpg', killpg):
                native.stop()
            names=[native.START_ORDER[pid-1000] for pid in stopped]
            self.assertEqual(names, ['web', 'gateway', 'exercise', 'learning', 'class', 'auth', 'postgres'])
            self.assertEqual(list(Path(temp).glob('*.json')), [])

    def test_databases_ports_and_start_order_of_sprint_1(self):
        self.assertEqual(native.CFG['databases'], {'auth': 'auth_db', 'class': 'class_db', 'exercise': 'exercise_db', 'learning': 'learning_db'})
        self.assertEqual({name: native.CFG['ports'][name] for name in ['auth', 'class', 'exercise', 'learning', 'gateway', 'web', 'postgres']},
            {'auth': 8104, 'class': 8114, 'exercise': 8124, 'learning': 8134, 'gateway': 8204, 'web': 5174, 'postgres': 55434})
        order=native.START_ORDER
        self.assertEqual((order[0], order[-2:]), ('postgres', ['gateway', 'web']))
        self.assertLess(order.index('auth'), order.index('class')); self.assertLess(order.index('auth'), order.index('learning'))
        self.assertLess(order.index('class'), order.index('exercise')); self.assertLess(order.index('learning'), order.index('exercise'))

    def test_each_service_gets_its_own_database_and_the_urls_it_needs(self):
        auth, cls, exercise = native.spring_env('auth'), native.spring_env('class'), native.spring_env('exercise')
        self.assertTrue(auth['SPRING_DATASOURCE_URL'].endswith(':55434/auth_db'))
        self.assertTrue(cls['SPRING_DATASOURCE_URL'].endswith(':55434/class_db'))
        self.assertTrue(exercise['SPRING_DATASOURCE_URL'].endswith(':55434/exercise_db'))
        self.assertEqual((auth['SERVER_PORT'], cls['SERVER_PORT'], exercise['SERVER_PORT']), ('8104', '8114', '8124'))
        self.assertIn('ADMIN_EMAIL', auth); self.assertNotIn('JWKS_URL', auth)
        self.assertEqual(cls['JWKS_URL'], 'http://127.0.0.1:8104/.well-known/jwks.json'); self.assertEqual(cls['AUTH_SERVICE_URL'], 'http://127.0.0.1:8104')
        self.assertEqual((exercise['CLASS_SERVICE_URL'], exercise['LEARNING_SERVICE_URL']), ('http://127.0.0.1:8114', 'http://127.0.0.1:8134'))
        learning=native.learning_env()
        self.assertTrue(learning['DATABASE_URL'].endswith(':55434/learning_db'))
        self.assertEqual((learning['JWKS_URL'], learning['EXERCISE_SERVICE_URL']), ('http://127.0.0.1:8104/.well-known/jwks.json', 'http://127.0.0.1:8124'))
        self.assertTrue(all(host in value for value in [exercise['SERVER_ADDRESS']] for host in ['127.0.0.1']))

if __name__=='__main__': unittest.main()
