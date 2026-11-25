"""Run with /usr/bin/python3 scripts/test-player-hidden-progress.py (WebKitGTK)."""
from pathlib import Path
import json
import sys

import gi

gi.require_version('Gtk', '3.0')
gi.require_version('WebKit2', '4.1')
from gi.repository import GLib, Gtk, WebKit2

checks = r"""(() => {
  const errors = [], counts = {}, commands = [];
  window.chrome = {webview: {postMessage: ({type}) => commands.push(type)}};
  const check = (ok, label) => { if (!ok) errors.push(label); };
  const el = id => document.getElementById(id);
  const update = (position, paused = false) => window.playerUpdate({position, duration: 120,
    bufferedPosition: position + 20, paused, volumeLevel: 80});
  for (const mode of ['official', 'codeine_xo']) {
    window.playerControls({playerUiMode: mode, controlsVisible: false, showSeekbarWhileSeeking: true,
      showRemainingTime: true, showPlaybackTimeOverlay: true});
    update(1); update(1.25);
    const observer = new MutationObserver(() => {});
    observer.observe(document.documentElement, {subtree:true, attributes:true, childList:true, characterData:true});
    for (let i = 1; i <= 60; i++) update(1.25 + i / 4);
    counts[mode] = observer.takeRecords().length;
    observer.disconnect();
    check(counts[mode] === 0, mode + ' mutated the DOM on hidden playback ticks');
    check(!el('torrentStatsOverlay').hidden && el('torrentStatsClock').textContent, 'clock preference');
    window.playerSeekActivity();
    const root = el('playerRoot');
    check(root.classList.contains('codeine-seeking-seekbar-only') === (mode === 'codeine_xo'), mode + ' seek reveal');
    if (mode === 'codeine_xo') {
      check(el('seekPosition').textContent === '00:16', 'stale seek-only reveal');
      update(17); check(el('seekPosition').textContent === '00:17', 'seek-only not advancing');
    }
    root.dispatchEvent(new PointerEvent('pointermove', {bubbles:true, clientX:10, clientY:10}));
    check(!root.classList.contains('chrome-hidden'), mode + ' pointer reveal');
    check(el('seekPosition').textContent === (mode === 'official' ? '00:16' : '00:17'), mode + ' stale timeline');
    check(parseFloat(el('seek').style.getPropertyValue('--buffered')) > 30, mode + ' stale buffer');
    update(18); check(el('seekPosition').textContent === '00:18', mode + ' visible timeline');
    window.playerControls({controlsVisible: false});
    update(18, true); check(el('toggle').getAttribute('aria-label') === 'Play', mode + ' pause transition');
    window.playerControls({controlsVisible: false, showSeekbarWhileSeeking: false});
    update(18); update(19);
    commands.length = 0;
    root.dispatchEvent(new KeyboardEvent('keydown', {code:'ArrowRight', key:'ArrowRight', bubbles:true, cancelable:true}));
    check(commands.filter(type => type === 'keyboardSeekForward').length === 1, mode + ' keyboard routing');
    check(el('seekPosition').textContent === '00:22', mode + ' seek from stale position');
    check(!root.classList.contains('codeine-seeking-seekbar-only'), mode + ' ignored seekbar preference');
  }
  return JSON.stringify({counts, errors});
})()"""

window = Gtk.Window()
window.set_default_size(960, 640)
view = WebKit2.WebView()
window.add(view)
status = 1


def result(view, task):
    global status
    try:
        report = json.loads(view.evaluate_javascript_finish(task).to_string())
        print('Mutation records for 60 hidden playback ticks:', report['counts'])
        print('; '.join(report['errors']) or 'PASS: hidden playback and input reveals')
        status = int(bool(report['errors']))
    finally:
        Gtk.main_quit()


def loaded(view, event):
    if event == WebKit2.LoadEvent.FINISHED:
        view.evaluate_javascript(checks, -1, None, None, None, result)


view.connect('load-changed', loaded)
view.load_uri((Path(__file__).resolve().parents[1] / 'composeApp/src/desktopMain/resources/player-ui/controls.html').as_uri())
window.show_all()
GLib.timeout_add_seconds(20, Gtk.main_quit)
Gtk.main()
sys.exit(status)
