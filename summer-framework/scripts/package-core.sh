#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="${1:-0.1.0}"
CORE_DIR="$ROOT_DIR/core"
TARGET_DIR="$CORE_DIR/target"
BUILD_DIR="$TARGET_DIR/manual-build"
CLASSES_DIR="$BUILD_DIR/classes"
JAR_PATH="$TARGET_DIR/summer-framework-core-$VERSION.jar"

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR"

JAVA_FILES=$(find "$CORE_DIR/src/main/java" -name "*.java")
if [ -z "$JAVA_FILES" ]; then
  echo "No Java source files found under $CORE_DIR/src/main/java"
  exit 1
fi

javac -d "$CLASSES_DIR" $JAVA_FILES
jar --create --file "$JAR_PATH" -C "$CLASSES_DIR" .

echo "Packaged: $JAR_PATH"
