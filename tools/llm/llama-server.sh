#!/bin/bash
# llama-server control script for the on-device Gemma 3 vision model (AndroidIDE proot).
# Usage: ./llama-server.sh {start|stop|status}   (honours $LLM_HOME)
set -e

LLM_HOME="${LLM_HOME:-/storage/internal_new/llm}"
BIN="$LLM_HOME/bin/llama-server"
MODEL="$LLM_HOME/models/google_gemma-3-4b-it-Q4_K_M.gguf"
MMPROJ="$LLM_HOME/models/mmproj-google_gemma-3-4b-it-f16.gguf"
HOST=127.0.0.1
PORT=8080
PIDFILE="$LLM_HOME/llama-server.pid"
LOG="$LLM_HOME/llama-server.log"

# Only the llama lib dir. The AndroidIDE proot also exports a "support" dir
# whose libcrypto.so.1.1 shadows the rootfs OpenSSL and breaks loaders.
export LD_LIBRARY_PATH="$LLM_HOME/bin"

start() {
  if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
    echo "llama-server already running (pid $(cat "$PIDFILE"))"
    return 0
  fi
  for f in "$BIN" "$MODEL" "$MMPROJ"; do
    if [ ! -f "$f" ]; then
      echo "missing file: $f (run setup.sh first)" >&2
      exit 1
    fi
  done
  setsid "$BIN" \
    -m "$MODEL" \
    --mmproj "$MMPROJ" \
    --host "$HOST" --port "$PORT" \
    -c 8192 \
    -ctk q8_0 -ctv q8_0 \
    -t 6 \
    --no-webui \
    >> "$LOG" 2>&1 < /dev/null &
  echo $! > "$PIDFILE"
  echo "llama-server starting (pid $!)..."
  echo "log: $LOG"
}

stop() {
  if [ -f "$PIDFILE" ]; then
    kill "$(cat "$PIDFILE")" 2>/dev/null && echo "sent SIGTERM to $(cat "$PIDFILE")"
    rm -f "$PIDFILE"
  else
    echo "no pidfile"
  fi
  pkill -f "$MODEL" 2>/dev/null && echo "killed leftover llama-server" || true
}

status() {
  if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
    echo "RUNNING (pid $(cat "$PIDFILE"))"
  elif pgrep -f "$MODEL" >/dev/null 2>&1; then
    echo "RUNNING (no pidfile, pid $(pgrep -f "$MODEL" | head -1))"
  else
    echo "STOPPED"
  fi
}

case "${1:-}" in
  start) start ;;
  stop) stop ;;
  status) status ;;
  *) echo "usage: $0 {start|stop|status}" >&2; exit 1 ;;
esac
