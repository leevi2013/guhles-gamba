#!/bin/sh
# Lightweight Gradle bootstrap for environments where the standard wrapper JAR is unavailable.
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
VERSION="9.7.1"
BOOTSTRAP="$ROOT/.gradle-bootstrap"
GRADLE_HOME="$BOOTSTRAP/gradle-$VERSION"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$BOOTSTRAP"
  ZIP="$BOOTSTRAP/gradle-$VERSION-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
  echo "Gradle $VERSION nicht gefunden; lade Gradle herunter ..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "$URL"
  else
    echo "Fehler: curl oder wget wird zum Herunterladen von Gradle benötigt." >&2
    exit 1
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$ZIP" -d "$BOOTSTRAP"
  else
    echo "Fehler: unzip wird zum Entpacken von Gradle benötigt." >&2
    exit 1
  fi
  rm -f "$ZIP"
fi

exec "$GRADLE_BIN" "$@"
