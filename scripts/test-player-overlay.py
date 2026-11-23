"""Run the shipped player overlay in WebKitGTK: /usr/bin/python3 scripts/test-player-overlay.py"""
from pathlib import Path
import sys
import gi

gi.require_version('Gtk', '3.0')
gi.require_version('WebKit2', '4.1')
from gi.repository import GLib, Gtk, WebKit2

checks = r"""(() => {
  const errors = [], check = (value, label) => { if (!value) errors.push(label); };
  const commands = [];
  window.chrome = { webview: { postMessage: message => commands.push(message.type) } };
  const key = (key, repeat = false) => document.dispatchEvent(new KeyboardEvent('keydown', {key, repeat, bubbles:true, cancelable:true}));
  const rect = selector => document.querySelector(selector).getBoundingClientRect();
  for (const mode of ['codeine_xo', 'official']) {
    window.playerControls({playerUiMode:mode, controlsVisible:true, title:'Series', episodeText:'S1 E2', isFullscreen:true, durationMs:3600000, showPlaybackTimeOverlay:true});
    const seek = rect('#seek'), fs = rect('#fullscreenButton'), end = rect('#torrentStatsEnds');
    const identity = ['#title','#codeineTitle'].map(s => document.querySelector(s)).find(e => e && e.getBoundingClientRect().width);
    const episode = ['#episode','#codeineEpisode'].map(s => document.querySelector(s)).find(e => e && e.getBoundingClientRect().width);
    for (const [name, el] of [['title',identity],['episode',episode]]) {
      const r = el?.getBoundingClientRect();
      check(r && r.left < innerWidth/4 && r.top > innerHeight/2 && r.bottom <= seek.top, mode+' '+name);
    }
    check(fs.width && fs.left > innerWidth/2 && fs.bottom < innerHeight/2, mode+' fullscreen');
    check(end.width && end.left > innerWidth/2 && Math.abs(end.bottom-seek.top) < 80 && document.querySelector('#torrentStatsEnds').textContent.startsWith('Ends at '), mode+' end clock');
    check(!identity.inert, mode+' title must remain interactive');
    commands.length = 0; key('Escape'); window.playerControls({isFullscreen:false}); key('Escape', true);
    check(commands.join() === 'exitFullscreen', mode+' fullscreen Escape: '+commands);
    commands.length = 0; key('Escape'); check(commands.join() === 'back', mode+' windowed Escape');
    commands.length = 0; window.playerControls({isFullscreen:true}); key('Backspace'); check(commands.join() === 'back', mode+' Backspace');
  }
  document.querySelector('.official-action-row [data-command=subtitles]').click();
  commands.length = 0; key('Escape');
  check(!commands.includes('exitFullscreen') && !commands.includes('back'), 'modal Escape');
  return errors.join('; ');
})()"""
window = Gtk.Window()
window.set_default_size(960, 640)
view = WebKit2.WebView()
window.add(view)
status = 1

def result(view, task):
    global status
    try:
        error = view.evaluate_javascript_finish(task).to_string()
        print(error or 'PASS: both player layouts and Escape/Backspace routing')
        status = int(bool(error))
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
