#!/usr/bin/env bash
set -euo pipefail

# ── 설정 ──────────────────────────────────────────────────────────────────────
PACKAGE="com.jongwook.dshub"
BUILD_TYPE="${1:-debug}"   # 인자 없으면 debug, release도 지원

# ── ADB 기기 확인 ──────────────────────────────────────────────────────────────
echo "📱 ADB 기기 확인 중..."
DEVICE=$(adb devices -l | awk 'NR>1 && /device/{print $1}' | head -1)

if [ -z "$DEVICE" ]; then
  echo "❌ 연결된 ADB 기기가 없습니다. 기기를 연결하고 다시 시도하세요."
  exit 1
fi

echo "✅ 기기 발견: $DEVICE"

# ── 빌드 ──────────────────────────────────────────────────────────────────────
echo ""
echo "🔨 빌드 시작 ($BUILD_TYPE)..."

if [ "$BUILD_TYPE" = "release" ]; then
  ./gradlew assembleRelease
  APK_PATH="app/build/outputs/apk/release/app-release.apk"
else
  ./gradlew assembleDebug
  APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

if [ ! -f "$APK_PATH" ]; then
  echo "❌ APK 파일을 찾을 수 없습니다: $APK_PATH"
  exit 1
fi

APK_SIZE=$(du -sh "$APK_PATH" | cut -f1)
echo "✅ 빌드 완료: $APK_PATH ($APK_SIZE)"

# ── 설치 ──────────────────────────────────────────────────────────────────────
echo ""
echo "📦 설치 중..."
if ! adb -s "$DEVICE" install -r "$APK_PATH" 2>&1; then
  echo "⚠️  서명 충돌 감지 — 기존 앱 삭제 후 재설치..."
  adb -s "$DEVICE" uninstall "$PACKAGE" || true
  adb -s "$DEVICE" install "$APK_PATH"
fi

# ── 앱 실행 ───────────────────────────────────────────────────────────────────
echo ""
echo "🚀 앱 실행 중..."
adb -s "$DEVICE" shell am start -n "$PACKAGE/$PACKAGE.MainActivity"

echo ""
echo "🎉 완료! SM-S918N에 DSHub ($BUILD_TYPE) 설치 및 실행됨."
