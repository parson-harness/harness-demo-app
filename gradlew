#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRADLE_VERSION="${GRADLE_VERSION:-8.7}"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$SCRIPT_DIR/.gradle}"
DIST_DIR="$GRADLE_USER_HOME/wrapper/dists/gradle-$GRADLE_VERSION-bin"
DIST_ZIP="$GRADLE_USER_HOME/wrapper/gradle-$GRADLE_VERSION-bin.zip"
GRADLE_HOME="$DIST_DIR/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

download_gradle_dist() {
  local target_zip="$1"
  rm -f "$target_zip"
  echo "Downloading Gradle $GRADLE_VERSION..."
  curl -fsSL --retry 3 --retry-delay 2 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$target_zip"
}

extract_gradle_dist() {
  local zip_file="$1"
  local dest_dir="$2"
  local tmp_dir="$3"

  rm -rf "$tmp_dir"
  mkdir -p "$tmp_dir"
  mkdir -p "$dest_dir"
  if ! unzip -q "$zip_file" -d "$tmp_dir"; then
    rm -f "$zip_file"
    rm -rf "$tmp_dir"
    return 1
  fi
  mv "$tmp_dir/gradle-$GRADLE_VERSION" "$dest_dir/"
  rm -rf "$tmp_dir"
}

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$GRADLE_USER_HOME/wrapper/dists" "$GRADLE_USER_HOME/wrapper/tmp"
  if [ ! -f "$DIST_ZIP" ]; then
    download_gradle_dist "$DIST_ZIP"
  fi
  if [ ! -d "$GRADLE_HOME" ]; then
    TMP_DIR="$GRADLE_USER_HOME/wrapper/tmp/gradle-$GRADLE_VERSION"
    if ! extract_gradle_dist "$DIST_ZIP" "$DIST_DIR" "$TMP_DIR"; then
      echo "Cached Gradle distribution looks corrupt; re-downloading."
      download_gradle_dist "$DIST_ZIP"
      extract_gradle_dist "$DIST_ZIP" "$DIST_DIR" "$TMP_DIR"
    fi
  fi
fi

exec "$GRADLE_BIN" "$@"
