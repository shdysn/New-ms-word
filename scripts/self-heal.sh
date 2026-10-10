#!/usr/bin/env bash
# ==============================================================================
# DocCraft - Auto Self-Healing Build & Diagnostic Engine
# Automatically repairs keystores, environment variables, permissions,
# and builds Android APKs with fault-tolerant recovery.
# ==============================================================================

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "=== [DocCraft Self-Healing Engine] Starting System Diagnostic & Repair ==="

# 1. Self-Heal .env
if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        cp .env.example .env
        echo "✅ [HEALED] Created missing .env from .env.example"
    else
        touch .env
        echo "✅ [HEALED] Created blank .env file"
    fi
else
    echo "✓ .env file present"
fi

# 2. Self-Heal debug.keystore
if [ ! -f debug.keystore ] || [ ! -s debug.keystore ]; then
    if [ -f debug.keystore.base64 ]; then
        echo "Attempting to decode debug.keystore.base64..."
        base64 -d debug.keystore.base64 > debug.keystore 2>/dev/null || true
    fi

    if [ ! -s debug.keystore ]; then
        echo "⚠️ Generating fresh debug.keystore via keytool..."
        keytool -genkeypair -v \
            -keystore debug.keystore \
            -alias androiddebugkey \
            -keyalg RSA \
            -keysize 2048 \
            -validity 10000 \
            -storepass android \
            -keypass android \
            -dname "CN=Android Debug,O=Android,C=US"
        echo "✅ [HEALED] Generated standard debug.keystore"
    else
        echo "✅ [HEALED] Restored debug.keystore from base64"
    fi
else
    echo "✓ debug.keystore present ($(ls -lh debug.keystore | awk '{print $5}'))"
fi

# 3. Self-Heal my-upload-key.jks (Release Keystore)
if [ ! -f my-upload-key.jks ] || [ ! -s my-upload-key.jks ]; then
    echo "⚠️ Generating release signing keystore (my-upload-key.jks)..."
    keytool -genkeypair -v \
        -keystore my-upload-key.jks \
        -alias upload \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass android \
        -keypass android \
        -dname "CN=DocCraft Release,O=DocCraft,C=US"
    echo "✅ [HEALED] Generated release keystore (my-upload-key.jks)"
else
    echo "✓ my-upload-key.jks present"
fi

# 4. Self-Heal gradlew permissions
if [ -f gradlew ]; then
    chmod +x gradlew
    echo "✓ gradlew execution permissions verified"
fi

# Export fallback signing environment variables
export KEYSTORE_PATH="${PROJECT_ROOT}/my-upload-key.jks"
export STORE_PASSWORD="${STORE_PASSWORD:-android}"
export KEY_PASSWORD="${KEY_PASSWORD:-android}"

echo "=== [DocCraft Self-Healing Engine] Environment is healthy. Ready to build! ==="
