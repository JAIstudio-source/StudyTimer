# Project Conventions

## Two GitHub repos (different purposes — never mix them up)

1. **`JAIstudio-source/StudyTimer`** — the *website / download page*. Contains `index.html`, `style.css`, `script.js`, `assets/`, plus the release artifacts that power the in-app update-checker: `StudyTimer-release.apk`, `version.json`, `RELEASE_NOTES.md`. Added locally as the `website` remote. Do **not** push Android source code here.
2. **`JAIstudio-source/StudyTimer-android`** — the *whole Android project backup*. Contains the full source tree (`app/`, `gradle/`, `build.gradle.kts`, etc.). This is `origin` and the place for any code/commit work. Do **not** confuse it with the website repo.

## Release workflow (run both steps)
- **Build & commit source:** `./gradlew assembleRelease` (signing uses `app/keystore.p12` + passwords from env or `local.properties`). Copy the fresh APK from `app/build/outputs/apk/release/app-release.apk` to the project root as `StudyTimer-release.apk` (overwrite the old one), update `RELEASE_NOTES.md` to the new version, and commit/push the whole change to `origin` (`StudyTimer-android`).
- **Publish to the website repo:** bump `version.json` (`versionCode`, `versionName`, `releaseNotes` — keep `url` and `apkUrl` unchanged), replace `StudyTimer-release.apk`, and update `RELEASE_NOTES.md` in `JAIstudio-source/StudyTimer`, then push (e.g. via `git push website <branch>:main`).

## Gotchas
- `version.json`'s `apkUrl` must keep pointing at `https://raw.githubusercontent.com/JAIstudio-source/StudyTimer/main/StudyTimer-release.apk` — the website repo is the download host.
- Never force-push unless explicitly asked.
