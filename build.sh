#!/usr/bin/env bash
# Full GSEA desktop build script
# Downloads JDK bundles for all platforms, then builds all distribution targets.


# Initialize build flags early to avoid 'unbound variable' errors
BUILD_LINUX=false
BUILD_MACOS=false
BUILD_WINDOWS=false
BUILD_FULL=false
CLEAN=false


set -euo pipefail

# Parse arguments
for arg in "$@"; do
    case "$arg" in
        --linux)
            BUILD_LINUX=true
            ;;
        --macos)
            BUILD_MACOS=true
            ;;
        --windows)
            BUILD_WINDOWS=true
            ;;
        --full)
            BUILD_FULL=true
            ;;
        --clean)
            CLEAN=true
            ;;
        *)
            echo "Unknown argument: $arg"
            exit 1
            ;;
    esac
done

# If no platform is specified, default to Linux
if ! $BUILD_LINUX && ! $BUILD_MACOS && ! $BUILD_WINDOWS && ! $BUILD_FULL && ! $CLEAN; then
    BUILD_LINUX=true
fi

# If --full is set, enable all platforms
if $BUILD_FULL; then
    BUILD_LINUX=true
    BUILD_MACOS=true
    BUILD_WINDOWS=true
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ---------------------------------------------------------------------------
# JDK configuration (Adoptium Temurin 21; must match build.gradle options.release)
# ---------------------------------------------------------------------------
JDK_VERSION="21.0.12+8"
JDK_VERSION_SHORT="21"
JDK_CACHE_DIR="${SCRIPT_DIR}/build/jdk-cache"
mkdir -p "$JDK_CACHE_DIR"

# Adoptium download base URL
ADOPTIUM_BASE="https://api.adoptium.net/v3/binary/version/jdk-${JDK_VERSION}"

download_jdk() {
    local name="$1"   # label, e.g. "linux-x64"
    local url="$2"
    local format="${3:-tar.gz}"   # "tar.gz" or "zip"
    local archive="$JDK_CACHE_DIR/${name}.${format}"
    local dest="$JDK_CACHE_DIR/${name}"

    if [ -d "$dest" ]; then
        echo "  [cache] JDK for ${name} already present, skipping download."
        return
    fi

    echo "  Downloading JDK for ${name}..."
    curl -fL --retry 3 --output "$archive" "$url"
    mkdir -p "$dest"
    if [ "$format" = "zip" ]; then
        unzip -q "$archive" -d "$JDK_CACHE_DIR/${name}-tmp"
        # strip the single top-level directory inside the zip
        local inner
        inner="$(ls "$JDK_CACHE_DIR/${name}-tmp")"
        mv "$JDK_CACHE_DIR/${name}-tmp/${inner}" "$dest"
        rmdir "$JDK_CACHE_DIR/${name}-tmp"
    else
        tar -xzf "$archive" -C "$dest" --strip-components=1
    fi
    rm "$archive"
    echo "  Extracted JDK for ${name} -> ${dest}"
}


JDK_LINUX="${JDK_CACHE_DIR}/linux-x64"
JDK_MAC_ARM="${JDK_CACHE_DIR}/mac-aarch64"
JDK_MAC_INTEL="${JDK_CACHE_DIR}/mac-x64"
JDK_WINDOWS="${JDK_CACHE_DIR}/windows-x64"

# Download JDKs for selected platforms
if $BUILD_LINUX; then
    download_jdk "linux-x64" "${ADOPTIUM_BASE}/linux/x64/jdk/hotspot/normal/eclipse?project=jdk"
fi
if $BUILD_MACOS; then
    download_jdk "mac-aarch64" "${ADOPTIUM_BASE}/mac/aarch64/jdk/hotspot/normal/eclipse?project=jdk"
    download_jdk "mac-x64" "${ADOPTIUM_BASE}/mac/x64/jdk/hotspot/normal/eclipse?project=jdk"
fi
if $BUILD_WINDOWS; then
    download_jdk "windows-x64" "${ADOPTIUM_BASE}/windows/x64/jdk/hotspot/normal/eclipse?project=jdk" "zip"
fi


# Only build NoJava zip for Linux builds
if $BUILD_LINUX; then
    echo "==> Creating NoJava zip..."
    ./gradlew createDistZip
fi


# Build Linux outputs if requested
if $BUILD_LINUX; then
    echo "==> Creating Linux+Java zip..."
    ./gradlew createLinuxDistZip -PjdkBundleLinux="$JDK_LINUX"
fi


# Build macOS outputs if requested
if $BUILD_MACOS; then
    echo "==> Creating macOS (ARM)+Java zip..."
    ./gradlew createMacAppWithJavaDistZip -PjdkBundleMac="$JDK_MAC_ARM"

    echo "==> Creating macOS (Intel)+Java zip..."
    ./gradlew createMacAppIntelWithJavaDistZip -PjdkBundleMacIntel="$JDK_MAC_INTEL"
fi


# Build Windows outputs if requested
if $BUILD_WINDOWS; then
    echo "==> Creating Windows NoJava zip..."
    ./gradlew createWinDist

    echo "==> Creating Windows+Java dist..."
    ./gradlew createWinWithJavaDist -PjdkBundleWindows="$JDK_WINDOWS"

    # Windows EXE installer requires makensis — skip if not available
    if command -v makensis &>/dev/null; then
        echo "==> Creating Windows EXE installer (NoJava)..."
        ./gradlew createWinExeDist -PmakensisCommand="$(command -v makensis)" -PjdkBundleWindows="$JDK_WINDOWS"
        echo "==> Creating Windows EXE installer (WithJava)..."
        ./gradlew createWinExeWithJavaDist -PmakensisCommand="$(command -v makensis)" -PjdkBundleWindows="$JDK_WINDOWS"
    else
        echo "  [skip] makensis not found — skipping Windows EXE installer targets."
        echo "         Install NSIS (https://nsis.sourceforge.io/) to enable them."
    fi
fi


# Show skip message only if neither macOS nor Windows builds are requested

# Show skip message only if neither macOS nor Windows nor Linux builds are requested
if ! $BUILD_LINUX && ! $BUILD_MACOS && ! $BUILD_WINDOWS; then
    echo "==> No platform selected for build. Use --linux, --macos, --windows, or --full."
fi

echo ""
echo "Build complete. Artifacts in build/distZip/:"
ls build/distZip/ 2>/dev/null || echo "  (none found)"
