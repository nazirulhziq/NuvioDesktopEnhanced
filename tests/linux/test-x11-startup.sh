#!/usr/bin/env bash
# Offline smoke test for a packaged Linux desktop launcher. Requires a parent X
# display, Xephyr, bubblewrap and X11 utilities. No real user profile is mounted.
# Usage: tests/linux/test-x11-startup.sh '/opt/.../bin/Nuvio' [--disconnect]
# --disconnect checks for a JVM fatal-error report after stopping only Xephyr.
set -euo pipefail
launcher=$(realpath -e "${1:?pass installed Nuvio launcher path}")
prefix=$(dirname "$(dirname "$launcher")")
work=$(mktemp -d); server=; app=
cleanup() {
  if [[ -n $app ]]; then kill "$app" 2>/dev/null || :; wait "$app" 2>/dev/null || :; fi
  if [[ -n $server ]]; then kill "$server" 2>/dev/null || :; wait "$server" 2>/dev/null || :; fi
  rm -rf -- "$work"
}
trap cleanup EXIT
for ((n=150; n<500; n++)); do
  [[ -e /tmp/.X11-unix/X$n || -e /tmp/.X$n-lock ]] || break
done
((n<500))
display=:$n
mkdir -m 700 "$work/home"
touch "$work/home/auth"
xauth -f "$work/home/auth" add "$display" . "$(mcookie)"
Xephyr "$display" -screen 960x720 -auth "$work/home/auth" -nolisten tcp >"$work/xserver" 2>&1 & server=$!
window_tree() { DISPLAY=$display XAUTHORITY="$work/home/auth" xwininfo -root -tree 2>/dev/null; }
for ((i=0; i<50; i++)); do kill -0 "$server"; window_tree >/dev/null && break; sleep .2; done
window_tree >/dev/null
bwrap --die-with-parent --unshare-pid --unshare-net --unshare-ipc --unshare-uts --hostname "$(hostname)" \
  --ro-bind /usr /usr --symlink usr/lib /lib --symlink usr/lib64 /lib64 --symlink usr/bin /bin \
  --ro-bind /etc /etc --ro-bind "$prefix" /package --dev /dev --proc /proc --tmpfs /tmp \
  --ro-bind "/tmp/.X11-unix/X$n" "/tmp/.X11-unix/X$n" --bind "$work/home" /home/test --chdir /home/test \
  --clearenv --setenv HOME /home/test --setenv DISPLAY "$display" --setenv XAUTHORITY /home/test/auth \
  --setenv PATH /usr/bin:/bin --setenv LANG C.UTF-8 --setenv JAVA_TOOL_OPTIONS '-Duser.home=/home/test -XX:-CreateCoredumpOnCrash' \
  -- "/package/bin/$(basename "$launcher")" >"$work/app" 2>&1 & app=$!
for ((i=0; i<120; i++)); do
  kill -0 "$app" || { printf 'FAIL: launcher exited before opening a window\n'; exit 1; }
  tree=$(window_tree)
  if [[ $tree =~ \"[Nn]uvio[^\"]*\" ]]; then
    sleep 5; kill -0 "$app"; tree=$(window_tree); [[ $tree =~ \"[Nn]uvio[^\"]*\" ]]
    printf 'PASS: Nuvio window remained present for five seconds in an empty offline profile\n'
    if [[ ${2:-} == --disconnect ]]; then
      # Stop only our nested server. Losing its display can terminate an X11
      # client normally; only report a matching JVM fatal-error fingerprint.
      kill "$server"; wait "$server" 2>/dev/null || :; server=
      for ((j=0; j<100; j++)); do kill -0 "$app" 2>/dev/null || break; sleep .1; done
      if kill -0 "$app" 2>/dev/null; then printf 'Client still alive after ten seconds\n'; else
        rc=0; wait "$app" || rc=$?; app=; printf 'Client exited after display loss: status %s\n' "$rc"
      fi
      for dump in "$work/home"/hs_err_pid*.log; do
        [[ -f $dump ]] || continue
        if grep -q '^#  SIGSEGV' "$dump" && grep -Eq '^C  \[libX11[^]]*\] +_XIOError' "$dump"; then
          printf 'FAIL: reproduced JVM SIGSEGV with libX11 _XIOError\n'; exit 1
        fi
        printf 'FAIL: different JVM fatal-error report after display loss\n'; exit 1
      done
      printf 'No JVM fatal-error report observed after display loss\n'
    fi
    exit 0
  fi
  sleep .5
done
printf 'FAIL: no Nuvio window in 60 seconds\n'; exit 1
