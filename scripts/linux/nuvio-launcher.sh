#!/bin/sh
# Launcher used inside the Flatpak bundle
# Cap glibc per-thread malloc arenas: the 64 MB arena pattern is the bulk of native RSS.
: "${MALLOC_ARENA_MAX:=2}"
export MALLOC_ARENA_MAX
exec /app/opt/Nuvio/bin/Nuvio "$@"
