#!/usr/bin/env node
/**
 * Automated Release & Version Sync Engine for StudyTimer
 * Keeps website, APK binary, version.json, and release notes in 100% sync.
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const websiteRoot = path.resolve(__dirname, '..');
const appRoot = path.resolve(websiteRoot, '..', 'StudyTimer-app');

console.log('🚀 Running StudyTimer Release Sync Engine...');

// 1. Read build.gradle.kts for latest versionCode and versionName
const gradlePath = path.join(appRoot, 'app', 'build.gradle.kts');
if (!fs.existsSync(gradlePath)) {
    console.error('❌ Could not find build.gradle.kts at', gradlePath);
    process.exit(1);
}

const gradleContent = fs.readFileSync(gradlePath, 'utf8');
const codeMatch = gradleContent.match(/versionCode\s*=\s*(\d+)/);
const nameMatch = gradleContent.match(/versionName\s*=\s*"([^"]+)"/);

const versionCode = codeMatch ? parseInt(codeMatch[1], 10) : 29;
const versionName = nameMatch ? nameMatch[1] : '2.9.7';

console.log(`📌 Detected Version from App: v${versionName} (build ${versionCode})`);

// 2. Copy binary APK
const srcApk = path.join(appRoot, 'StudyTimer-release.apk');
const destApk = path.join(websiteRoot, 'StudyTimer-release.apk');

if (fs.existsSync(srcApk)) {
    fs.copyFileSync(srcApk, destApk);
    const sizeMb = (fs.statSync(destApk).size / (1024 * 1024)).toFixed(2);
    console.log(`✅ Copied StudyTimer-release.apk (${sizeMb} MB) to website repo`);
} else {
    console.warn('⚠️ Warning: srcApk not found at', srcApk);
}

// 3. Read Release Notes
const srcReleaseNotes = path.join(appRoot, 'RELEASE_NOTES.md');
let releaseNotesText = `What's New in Version ${versionName} 🚀\n• New Leaderboard Updates: Privacy controls & live status toggles\n• New Exam Countdown System on home screen\n• Bug Fixes for planner goals, time calculations, and data sync`;

if (fs.existsSync(srcReleaseNotes)) {
    const notesContent = fs.readFileSync(srcReleaseNotes, 'utf8');
    fs.writeFileSync(path.join(websiteRoot, 'RELEASE_NOTES.md'), notesContent, 'utf8');
    console.log('✅ Synchronized RELEASE_NOTES.md');
}

// 4. Update version.json
const versionJsonPath = path.join(websiteRoot, 'version.json');
const versionJsonObj = {
    apkUrl: "https://raw.githubusercontent.com/JAIstudio-source/StudyTimer/main/StudyTimer-release.apk",
    url: "https://get-studytimer.vercel.app/?download=true",
    versionCode: versionCode,
    versionName: versionName,
    releaseNotes: releaseNotesText
};
fs.writeFileSync(versionJsonPath, JSON.stringify(versionJsonObj, null, 4) + '\n', 'utf8');
console.log('✅ Updated version.json');

// 5. Update package.json
const pkgPath = path.join(websiteRoot, 'package.json');
if (fs.existsSync(pkgPath)) {
    const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'));
    pkg.version = versionName;
    fs.writeFileSync(pkgPath, JSON.stringify(pkg, null, 2) + '\n', 'utf8');
    console.log('✅ Updated package.json version to', versionName);
}

// 6. Update Service Worker Cache Name
const swPath = path.join(websiteRoot, 'sw.js');
if (fs.existsSync(swPath)) {
    let sw = fs.readFileSync(swPath, 'utf8');
    const newCacheName = `studytimer-web-v${versionCode}`;
    sw = sw.replace(/var CACHE_NAME = 'studytimer-web-v\d+';/, `var CACHE_NAME = '${newCacheName}';`);
    fs.writeFileSync(swPath, sw, 'utf8');
    console.log(`✅ Updated Service Worker cache name to ${newCacheName}`);
}

console.log('🎉 Sync complete! Website repository is 100% updated with the latest release.');
