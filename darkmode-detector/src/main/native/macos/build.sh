#!/bin/bash
# Compiles NucleusDarkModeBridge.m into per-architecture dylibs (arm64 + x86_64).
# The outputs are placed in the JAR resources so they ship with the library.
#
# Prerequisites: Xcode command-line tools (clang).
# Usage: ./build.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="$SCRIPT_DIR/NucleusDarkModeBridge.m"
RESOURCE_DIR="$SCRIPT_DIR/../../resources/nucleus/native"
OUT_DIR_ARM64="$RESOURCE_DIR/darwin-aarch64"
OUT_DIR_X64="$RESOURCE_DIR/darwin-x64"

# Detect JAVA_HOME for JNI headers
if [ -z "${JAVA_HOME:-}" ]; then
    JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null || true)
fi
if [ -z "${JAVA_HOME:-}" ]; then
    echo "ERROR: JAVA_HOME not set and /usr/libexec/java_home failed." >&2
    exit 1
fi

JNI_INCLUDE="$JAVA_HOME/include"
JNI_INCLUDE_DARWIN="$JAVA_HOME/include/darwin"

if [ ! -d "$JNI_INCLUDE" ]; then
    echo "ERROR: JNI headers not found at $JNI_INCLUDE" >&2
    exit 1
fi

mkdir -p "$OUT_DIR_ARM64" "$OUT_DIR_X64"

# Compile for arm64
clang -arch arm64 \
    -dynamiclib \
    -o "$OUT_DIR_ARM64/libnucleus_darkmode.dylib" \
    -I"$JNI_INCLUDE" -I"$JNI_INCLUDE_DARWIN" \
    -framework Cocoa \
    -mmacosx-version-min=10.13 \
    -fobjc-arc \
    "$SRC"

# Compile for x86_64
clang -arch x86_64 \
    -dynamiclib \
    -o "$OUT_DIR_X64/libnucleus_darkmode.dylib" \
    -I"$JNI_INCLUDE" -I"$JNI_INCLUDE_DARWIN" \
    -framework Cocoa \
    -mmacosx-version-min=10.13 \
    -fobjc-arc \
    "$SRC"

echo "Built per-architecture dylibs:"
file "$OUT_DIR_ARM64/libnucleus_darkmode.dylib"
file "$OUT_DIR_X64/libnucleus_darkmode.dylib"
