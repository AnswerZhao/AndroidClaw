#!/bin/bash
set -euo pipefail

LM_STUDIO_HOST="${LM_STUDIO_HOST:-localhost:1234}"

echo "=== Real Daemon E2E Tests ==="
echo "Requires LM Studio running at http://$LM_STUDIO_HOST"
echo ""

# Verify LM Studio is reachable
if ! curl -sf "http://$LM_STUDIO_HOST/v1/models" > /dev/null 2>&1; then
    echo "ERROR: LM Studio not reachable at $LM_STUDIO_HOST"
    echo "Start LM Studio and load a Qwen model first."
    echo "Override with: LM_STUDIO_HOST=<ip>:<port> $0"
    exit 1
fi
echo "LM Studio: OK"

# Verify emulator/device is connected
if ! adb devices | grep -q "device$"; then
    echo "ERROR: No Android device/emulator connected"
    echo "Start an emulator: \$ANDROID_HOME/emulator/emulator -avd ZeroClaw_Test"
    exit 1
fi
echo "Device: OK"

# Install latest debug APK
echo "Installing debug APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Run real-daemon flows
echo "Running real-daemon Maestro flows..."
maestro test maestro/flows/real-daemon/

echo ""
echo "=== All real-daemon tests passed ==="
