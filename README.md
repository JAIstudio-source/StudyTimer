# StudyTimer (Android)

Android source for the StudyTimer app. This repo (`JAIstudio-source/StudyTimer-android`) is the full Android project backup — see `AGENTS.md` for the release workflow and the difference from the `StudyTimer` website repo.

## Setting up the on-device OpenCode LLM (fresh device)

This repo carries everything needed to rebuild the local vision setup on a new AndroidIDE device in a few minutes:

```sh
# 1. copy the portable config (real one is gitignored — it references a local key path)
cp opencode.jsonc.example opencode.jsonc

# 2. bootstrap: links /system into the proot rootfs, downloads llama.cpp + Gemma 3 4B
#    into $LLM_HOME (default /storage/internal_new/llm, ~3.4 GB total)
./tools/llm/setup.sh

# 3. start the server and verify
./tools/llm/llama-server.sh start
curl -s http://127.0.0.1:8080/health
```

That gives you a fully-local vision model (`local/gemma3-4b`) used by the `vision` subagent, while the default model stays `opencode/big-pickle` (no server needed). The `google/*` models need a Gemini key placed at the path referenced in `opencode.jsonc` — that key is a secret and is not in this repo.

See `AGENTS.md` → "On-device OpenCode LLM setup (portable)" for full details and gotchas (rootfs resets, `LD_LIBRARY_PATH`, server dependency).

## Requirements

- AndroidIDE (proot Ubuntu) with `git` + `wget`.
- ~4 GB free RAM and ~4 GB storage to serve the Gemma 3 4B model.
