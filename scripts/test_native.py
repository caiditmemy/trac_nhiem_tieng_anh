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
            self.assertEqual(sorted(asked), [5171, 8101, 8111, 8201, 55431])
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
            self.assertEqual(names, ['web', 'gateway', 'class', 'auth', 'postgres'])
            self.assertEqual(list(Path(temp).glob('*.json')), [])

    def test_databases_ports_and_start_order_of_sprint_1_nam(self):
        self.assertEqual(native.CFG['databases'], {'auth': 'auth_db', 'class': 'class_db'})
        self.assertEqual(native.CFG['ports'], {'postgres': 55431, 'auth': 8101, 'class': 8111, 'gateway': 8201, 'web': 5171})
        self.assertEqual(native.START_ORDER, ['postgres', 'auth', 'class', 'gateway', 'web'])

    def test_each_service_gets_its_own_database_and_the_urls_it_needs(self):
        auth, cls = native.spring_env('auth'), native.spring_env('class')
        self.assertTrue(auth['SPRING_DATASOURCE_URL'].endswith(':55431/auth_db'))
        self.assertTrue(cls['SPRING_DATASOURCE_URL'].endswith(':55431/class_db'))
        self.assertEqual((auth['SERVER_PORT'], cls['SERVER_PORT']), ('8101', '8111'))
        self.assertIn('ADMIN_EMAIL', auth); self.assertNotIn('JWKS_URL', auth)
        self.assertEqual(cls['JWKS_URL'], 'http://127.0.0.1:8101/.well-known/jwks.json'); self.assertEqual(cls['AUTH_SERVICE_URL'], 'http://127.0.0.1:8101')
        self.assertEqual(auth['SERVER_ADDRESS'], '127.0.0.1')
        self.assertEqual(cls['SERVER_ADDRESS'], '127.0.0.1')

if __name__=='__main__': unittest.main()
