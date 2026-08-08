#!/bin/bash
# One-time bootstrap of the on-device Gemma 3 vision LLM for OpenCode (AndroidIDE proot).
# Installs llama.cpp binaries + models into $LLM_HOME, then start the server with:
#   LLM_HOME="$LLM_HOME" llama-server.sh start
set -e

LLM_HOME="${LLM_HOME:-/storage/internal_new/llm}"
LLAMA_TAG="b10327"
LLAMA_URL="https://github.com/ggml-org/llama.cpp/releases/download/${LLAMA_TAG}/llama-${LLAMA_TAG}-bin-android-arm64.tar.gz"
MODEL_URL="https://huggingface.co/bartowski/google_gemma-3-4b-it-GGUF/resolve/main/google_gemma-3-4b-it-Q4_K_M.gguf"
MMPROJ_URL="https://huggingface.co/bartowski/google_gemma-3-4b-it-GGUF/resolve/main/mmproj-google_gemma-3-4b-it-f16.gguf"

# 1) Bionic linker visibility. The Android-arm64 llama binaries need the host
#    /system (their interpreter /system/bin/linker64 lives there). proot hides
#    /system, so link it into the AndroidIDE rootfs via the /host-rootfs bind.
if [ ! -e /system/bin/linker64 ]; then
  ROOTFS=$(ps -ef 2>/dev/null | grep -m1 "[p]root -r " | sed -E 's/.*proot -r ([^ ]+).*/\1/')
  if [ -n "$ROOTFS" ] && [ -d "$ROOTFS" ]; then
    ln -sfn /host-rootfs/system "$ROOTFS/system"
    echo "[setup] linked /system -> /host-rootfs/system (rootfs: $ROOTFS)"
  else
    echo "[setup] WARNING: could not locate the proot rootfs." >&2
    echo "[setup] Create the symlink manually: ln -sfn /host-rootfs/system <rootfs>/system" >&2
  fi
else
  echo "[setup] /system symlink already present"
fi

mkdir -p "$LLM_HOME/bin" "$LLM_HOME/models"
cd "$LLM_HOME"

# 2) llama.cpp Android-arm64 build
if [ ! -x "$LLM_HOME/bin/llama-server" ]; then
  echo "[setup] downloading llama.cpp android-arm64 ($LLAMA_TAG)..."
  wget -q -O /tmp/llama.tar.gz "$LLAMA_URL"
  mkdir -p /tmp/llama-x && tar xzf /tmp/llama.tar.gz -C /tmp/llama-x
  cp -a /tmp/llama-x/llama-*/ "$LLM_HOME/bin/"
  rm -rf /tmp/llama-x /tmp/llama.tar.gz
else
  echo "[setup] llama.cpp already installed"
fi

# 3) Model + vision projector (resumable downloads)
for spec in "google_gemma-3-4b-it-Q4_K_M.gguf:$MODEL_URL" "mmproj-google_gemma-3-4b-it-f16.gguf:$MMPROJ_URL"; do
  f="${spec%%:*}"; url="${spec#*:}"
  if [ ! -s "$LLM_HOME/models/$f" ]; then
    echo "[setup] downloading $f ..."
    wget -c -q -O "$LLM_HOME/models/$f" "$url"
  else
    echo "[setup] $f already present"
  fi
done

SELF_DIR="$(cd "$(dirname "$0")" && pwd)"
echo
echo "[setup] done. Everything is in $LLM_HOME"
echo "[setup] next steps:"
echo "  1. LLM_HOME=\"$LLM_HOME\" \"$SELF_DIR/llama-server.sh\" start"
echo "  2. make sure opencode.jsonc has the \"local\" provider + vision agent (see opencode.jsonc.example)"
echo "  3. check: wget -q -O - http://127.0.0.1:8080/health"
