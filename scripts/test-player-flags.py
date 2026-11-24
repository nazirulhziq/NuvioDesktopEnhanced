"""Run with /usr/bin/python3 (WebKitGTK), or pass a Chromium executable."""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

scripts = Path(__file__).resolve().parent
resources = scripts.parent / 'composeApp/src/desktopMain/resources/player-ui'
with tempfile.TemporaryDirectory() as tmp:
    page = Path(tmp) / 'player-ui'
    shutil.copytree(resources, page)
    html = page / 'controls.html'
    fixture = (scripts / 'test-player-flags.html').read_text(encoding='utf-8')
    html.write_text(html.read_text(encoding='utf-8').replace('</body>', fixture + '</body>'), encoding='utf-8')
    if len(sys.argv) > 1:
        result = subprocess.run([sys.argv[1], '--headless', '--no-sandbox', '--disable-gpu', '--allow-file-access-from-files',
                                 '--virtual-time-budget=7000', '--dump-dom', html.as_uri()], capture_output=True, encoding='utf-8', errors='replace', timeout=30, check=True)
        title = re.search(r'<title>(.*?)</title>', result.stdout)
        status = title.group(1) if title else 'FAIL: fixture did not finish'
    else:
        import gi
        gi.require_version('Gtk', '3.0')
        gi.require_version('WebKit2', '4.1')
        from gi.repository import GLib, Gtk, WebKit2
        window = Gtk.Window()
        view = WebKit2.WebView()
        view.get_settings().set_allow_file_access_from_file_urls(True)
        window.add(view)
        view.connect('notify::title', lambda v, _: Gtk.main_quit() if (v.get_title() or '').startswith(('PASS:', 'FAIL:')) else None)
        view.load_uri(html.as_uri())
        window.show_all()
        GLib.timeout_add_seconds(15, Gtk.main_quit)
        Gtk.main()
        status = view.get_title() or 'FAIL: fixture did not finish'
    print(status)
    sys.exit(0 if status == 'PASS: player flags' else 1)
