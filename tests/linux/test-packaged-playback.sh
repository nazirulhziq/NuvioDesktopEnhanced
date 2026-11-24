#!/usr/bin/env bash
# Usage: JAVA_HOME=/path/to/jdk21 bash test-packaged-playback.sh EXTRACTED_PREFIX VIDEO [RESOURCES_OVERRIDE]
# Run from a graphical X11 session. Uses only packaged jars/JNI, a fresh HOME,
# private nested X server and offline namespace; never installs the package.
set -euo pipefail
prefix=$(realpath -e "${1:?pass extracted package prefix}")
video=$(realpath -e "${2:?pass a local video > 15 seconds}")
java=$(realpath -e "${JAVA_HOME:?set JAVA_HOME}")
work=$(mktemp -d); server=
trap 'for dump in "$work"/home/hs_err_pid*.log; do [[ ! -f $dump ]] || grep -A15 "^Native frames:" "$dump"; done; if [[ -n $server ]]; then kill "$server" 2>/dev/null || :; wait "$server" 2>/dev/null || :; fi; rm -rf -- "$work"' EXIT
mkdir -m700 "$work/home"; mkdir "$work/classes"
"$java/bin/javac" -cp "$prefix/lib/app/*" -d "$work/classes" "$(dirname "$0")/PlaybackSmoke.java"
# Xephyr selects a free display atomically and reports it on fd 3.
touch "$work/home/auth"; cookie=$(mcookie); xauth -f "$work/home/auth" add :0 . "$cookie"
Xephyr -displayfd 3 -extension MIT-SHM -screen 800x600 -auth "$work/home/auth" -nolisten tcp 3>"$work/display" >"$work/xlog" 2>&1 & server=$!
for ((i=0;i<100;i++)); do [[ -s $work/display ]] && break; kill -0 "$server"; sleep .1; done
read -r n < "$work/display"; xauth -f "$work/home/auth" add ":$n" . "$cookie"
extra=(); if [[ -n ${3:-} ]]; then extra=(--ro-bind "$(realpath -e "$3")" /resources); fi
resources=/package/lib/app/resources; if ((${#extra[@]})); then resources=/resources; fi
timeout 45 bwrap --die-with-parent --unshare-pid --unshare-net --unshare-ipc --unshare-uts --hostname "$(hostname)" \
  --ro-bind /usr /usr --symlink usr/lib /lib --symlink usr/lib64 /lib64 --symlink usr/bin /bin --ro-bind /etc /etc \
  --ro-bind "$java" /jdk --ro-bind "$prefix" /package --ro-bind "$work/classes" /classes --ro-bind "$video" /fixture.mp4 \
  --dev /dev --proc /proc --tmpfs /tmp --ro-bind "/tmp/.X11-unix/X$n" "/tmp/.X11-unix/X$n" --bind "$work/home" /home/test \
  --chdir /home/test --clearenv --setenv HOME /home/test --setenv DISPLAY ":$n" --setenv XAUTHORITY /home/test/auth \
  --setenv NUVIO_BRIDGE_DEBUG 1 --setenv NUVIO_MPV_HWDEC no --setenv LANG C.UTF-8 --setenv PATH /usr/bin:/bin "${extra[@]}" -- /jdk/bin/java -Duser.home=/home/test \
  -XX:-CreateCoredumpOnCrash -Dcompose.application.resources.dir="$resources" -cp '/classes:/package/lib/app/*' PlaybackSmoke /fixture.mp4
