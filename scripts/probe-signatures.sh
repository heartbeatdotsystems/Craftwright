#!/usr/bin/env bash
# Prints the signatures of the Minecraft client classes the agent touches.
# Used when porting the agent to a new Minecraft version.
set -uo pipefail
OUT="${1:-build/probe}"
mkdir -p "$OUT"
JAR=""
for candidate in $(find "$HOME/.gradle" . -name '*.jar' -path '*minecraft*' 2>/dev/null); do
  if unzip -l "$candidate" 2>/dev/null | grep -q 'net/minecraft/client/Minecraft.class'; then JAR="$candidate"; break; fi
done
[ -n "$JAR" ] || { echo "no Minecraft client jar found"; exit 1; }
echo "Minecraft jar: $JAR"
unzip -l "$JAR" | awk '{print $4}' | grep -E '^net/minecraft/(client/gui/components|client/gui/screens|client/gui/screens/inventory|client/multiplayer|client/player|client|network/protocol/game|network/protocol/common|world/inventory)/[^/$]*\.class$' > "$OUT/classes.txt"
CLASSES="$(cat "$(dirname "$0")/probe-classes.txt" | grep -v '^#' | tr '\n' ' ')"
for c in $CLASSES; do
  echo "##### $c"
  javap -p -cp "$JAR" "$c" 2>&1 || true
done > "$OUT/signatures.txt"
wc -l "$OUT/classes.txt" "$OUT/signatures.txt"
