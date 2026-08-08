# Project Conventions

## Two GitHub repos (different purposes — never mix them up)

1. **`JAIstudio-source/StudyTimer`** — the *website / download page*. Contains `index.html`, `style.css`, `script.js`, `assets/`, plus the release artifacts that power the in-app update-checker: `StudyTimer-release.apk`, `version.json`, `RELEASE_NOTES.md`. Added locally as the `website` remote. Do **not** push Android source code here.
2. **`JAIstudio-source/StudyTimer-android`** — the *whole Android project backup*. Contains the full source tree (`app/`, `gradle/`, `build.gradle.kts`, etc.). This is `origin` and the place for any code/commit work. Do **not** confuse it with the website repo.

## Release workflow (run both steps)
- **Build & commit source:** `./gradlew assembleRelease` (signing uses `app/keystore.p12` + passwords from env or `local.properties`). Copy the fresh APK from `app/build/outputs/apk/release/app-release.apk` to the project root as `StudyTimer-release.apk` (overwrite the old one), update `RELEASE_NOTES.md` to the new version, and commit/push the whole change to `origin` (`StudyTimer-android`).
- **Publish to the website repo:** bump `version.json` (`versionCode`, `versionName`, `releaseNotes` — keep `url` and `apkUrl` unchanged), replace `StudyTimer-release.apk`, and update `RELEASE_NOTES.md` in `JAIstudio-source/StudyTimer`, then push (e.g. via `git push website <branch>:main`).

## On-device OpenCode LLM setup (portable)

This device runs OpenCode with a fully-local vision model: llama.cpp serving Gemma 3 4B (`gemma3-4b`, text+image) at `127.0.0.1:8080`, wired in as the `local` provider and used by the `vision` subagent. Everything needed to rebuild this on a new AndroidIDE device is in the repo:

- **`tools/llm/setup.sh`** — one-time bootstrap: links host `/system` into the proot rootfs (bionic linker needed by the Android-arm64 llama binaries), downloads llama.cpp + the model + mmproj into `$LLM_HOME` (default `/storage/internal_new/llm`).
- **`tools/llm/llama-server.sh`** — `{start|stop|status}`; honours `LLM_HOME`, binds `127.0.0.1:8080`, `-c 8192`, `q8_0` KV, 6 threads.
- **`opencode.jsonc.example`** — portable config template (copy to `opencode.jsonc`; the real `opencode.jsonc` is gitignored because it references the local `gemini.key` path).
- **`.opencode/agent/vision.md`** — tracked vision agent, `model: local/gemma3-4b`.

Rebuild steps on a fresh device: copy `opencode.jsonc.example` → `opencode.jsonc`, put the Gemini key at the path it references, run `tools/llm/setup.sh`, then `tools/llm/llama-server.sh start`. Verify with `curl -s http://127.0.0.1:8080/health`.

## Gotchas
- `version.json`'s `apkUrl` must keep pointing at `https://raw.githubusercontent.com/JAIstudio-source/StudyTimer/main/StudyTimer-release.apk` — the website repo is the download host.
- Never force-push unless explicitly asked.
- The `local` provider only works while llama-server is running; the default model is still `opencode/big-pickle` (no server needed).
- AndroidIDE rootfs resets (e.g. app update) remove the `/system` symlink and the `$LLM_HOME` files if inside the rootfs — keep `$LLM_HOME` in app-internal storage (`/storage/internal_new/`) and re-run `setup.sh` after a reset.
