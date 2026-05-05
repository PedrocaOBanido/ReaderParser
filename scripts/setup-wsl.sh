#!/usr/bin/env bash
set -euo pipefail

print_status() { echo ">>> $*"; }

# ============================================================
# WSL environment bootstrap for ReaderParser
# One-shot script: installs JDK 17 + Android SDK in WSL
# ============================================================

export SDK_ROOT="${ANDROID_HOME:-$HOME/android-sdk}"

# -- 1. JDK 17 -------------------------------------------------
if command -v java &>/dev/null && java -version 2>&1 | grep -q 'version "17'; then
    print_status "JDK 17 already installed"
else
    print_status "Installing JDK 17..."
    sudo apt-get update -qq
    sudo apt-get install -y -qq openjdk-17-jdk
fi

JAVA_HOME_DIR=$(readlink -f "$(dirname "$(dirname "$(readlink -f "$(which java)")")")")
print_status "JAVA_HOME: $JAVA_HOME_DIR"

# -- 2. Android CLI --------------------------------------------
if command -v android >/dev/null 2>&1; then
    print_status "Android CLI already installed ($(android --version 2>&1))"
else
    print_status "Installing Android CLI..."
    curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/install.sh | bash
    print_status "Android CLI installed"
fi

# -- 3. Android SDK packages ------------------------------------
mkdir -p "$SDK_ROOT"
export ANDROID_HOME="$SDK_ROOT"

print_status "Installing Android SDK packages (API 36)..."
android sdk install \
    "platforms;android-36" \
    "build-tools;36.0.0" \
    "platform-tools" \
    "emulator" \
    "system-images;android-36;google_apis;x86_64"

print_status "SDK packages installed"

# -- 4. Environment variables (.zshrc) -------------------------
print_status "Setting environment variables..."

SHELL_RC="$HOME/.zshrc"
if [ ! -f "$SHELL_RC" ] || [ "$(basename "$SHELL")" != "zsh" ]; then
    # Fallback to .bashrc for bash users, .profile as last resort
    if [ -f "$HOME/.bashrc" ]; then
        SHELL_RC="$HOME/.bashrc"
    else
        SHELL_RC="$HOME/.profile"
    fi
fi

for VAR in \
    "export ANDROID_HOME=$SDK_ROOT" \
    "export JAVA_HOME=$JAVA_HOME_DIR" \
    'export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"'; do
    if ! grep -qF "$VAR" "$SHELL_RC" 2>/dev/null; then
        echo "$VAR" >> "$SHELL_RC"
    fi
done

print_status "Environment variables written to $SHELL_RC"

# -- 5. Done ---------------------------------------------------
echo ""
echo "=== Setup complete ==="
echo "Run: source $SHELL_RC"
echo "Then: ./gradlew :app:assembleDebug"
echo ""
echo "To verify: ls -la /dev/kvm   # if present -> emulator works"
echo "            adb devices       # check connected devices"
