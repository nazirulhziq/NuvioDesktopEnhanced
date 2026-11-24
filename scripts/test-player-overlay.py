"""Run the shipped player overlay in WebKitGTK: /usr/bin/python3 scripts/test-player-overlay.py"""
from pathlib import Path
import sys
import gi

gi.require_version('Gtk', '3.0')
gi.require_version('WebKit2', '4.1')
from gi.repository import GLib, Gtk, WebKit2

checks = r"""(() => {
  const errors = [], check = (value, label) => { if (!value) errors.push(label); };
  const commands = [], clearances = [];
  window.chrome = { webview: { postMessage: message => {
    if (message.type === 'subtitleChromeClearance') clearances.push(message.value);
    else commands.push(message.type);
  } } };
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
    check(end.width && end.bottom < innerHeight/2 && document.querySelector('#torrentStatsEnds').textContent.startsWith('Ends at '), mode+' header clock');
    const duration = rect('#duration');
    check(duration.width && Math.abs(duration.right-seek.right) <= 1 && Math.abs(duration.bottom-seek.top) <= 16 && document.querySelector('#duration').textContent === '1:00:00', mode+' duration endpoint');
    for (const label of ['Skip Intro', 'Skip Outro']) {
      window.playerControls({skipPromptVisible:true, skipPromptLabel:label});
      check(rect('#skipPrompt').bottom < rect('#title').top, mode+' '+label+' overlaps title');
    }
    window.playerControls({controlsVisible:false, showSeekbarWhileSeeking:true});
    check(clearances.at(-1) === 0, mode+' hidden subtitles not restored');
    window.playerSeekActivity();
    const seekInset = clearances.at(-1);
    check(mode === 'official' ? seekInset === 0 : seekInset > 0, mode+' seek-only lift');
    window.playerControls({controlsVisible:false, showSeekbarWhileSeeking:false});
    check(clearances.at(-1) === 0, mode+' seek-only cleared');
    window.playerControls({controlsVisible:true, showRemainingTime:true});
    const progress = document.querySelector('.progress');
    const expected = Math.ceil(100*(progress.offsetParent.clientHeight-progress.offsetTop+12)/progress.offsetParent.clientHeight);
    check(clearances.at(-1) === expected && expected > seekInset, mode+' full controls clearance');
    check(document.querySelector('#duration').textContent === '1:00:00', mode+' remaining toggle changed duration');
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
window.set_default_size(*(map(int, sys.argv[1:]) if len(sys.argv) == 3 else (960, 640)))
view = WebKit2.WebView()
window.add(view)
status = 1

def result(view, task):
    global status
    try:
        error = view.evaluate_javascript_finish(task).to_string()
        print(error or 'PASS: both themes, title/skip and duration geometry, subtitle clearance and shortcut routing')
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
