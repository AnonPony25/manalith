#!/usr/bin/env bash
set -euo pipefail

FORGE_DIR="$(dirname "$0")/../../forge"

if [ ! -d "$FORGE_DIR" ]; then
  echo "Cloning Forge..."
  git clone https://github.com/Card-Forge/forge.git "$FORGE_DIR"
else
  echo "Updating Forge..."
  (cd "$FORGE_DIR" && git pull)
fi

cd "$FORGE_DIR"

echo "Building Forge engine modules..."
mvn clean install \
    -pl forge-core,forge-game,forge-ai \
    -am \
    -DskipTests \
    -Drevision=LOCAL-SNAPSHOT

echo ""
echo "[OK] Forge engine published to local .m2 — version: LOCAL-SNAPSHOT"
