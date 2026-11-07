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

if __name__=='__main__': unittest.main()
