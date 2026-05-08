# Recording the demo GIF

End-to-end: install the sample app on an Android emulator, record the user-facing locale switch, convert MP4 → GIF, drop it into the SDK README.

Total wall-clock: ~15 minutes once everything is installed.

## Pre-requisites

Verify each in one go:

```bash
adb version           # Android Platform Tools — comes with Android Studio
emulator -list-avds   # at least one AVD configured
ffmpeg -version       # for MP4→GIF; install via:  brew install ffmpeg
```

If any are missing:
- `adb` / `emulator` → install Android Studio, accept SDK license, create an AVD via Tools → Device Manager
- `ffmpeg` → `brew install ffmpeg`

## Step 1 — Boot the emulator

```bash
emulator -list-avds
# pick one, e.g., Pixel_8_API_34
emulator -avd Pixel_8_API_34 &
```

Wait until the home screen is up. Verify:

```bash
adb devices
# should list:  emulator-5554  device
```

## Step 2 — Start the local CDN (terminal 1)

In the **polylex-action** repo:

```bash
cd /Users/akhilesh.yadav/Desktop/vernac/polylex-action
export GEMINI_API_KEY="<your-key>"

# Generate translation bundles (real Gemini)
PROVIDER=gemini ./scripts/run-demo-locally.sh

# Serve them at localhost:8080
node scripts/serve-demo-locally.js
```

Leave this terminal running.

## Step 3 — Install the sample app (terminal 2)

In the **polylex-sdk** repo:

```bash
cd /Users/akhilesh.yadav/Desktop/vernac/polylex-sdk

./gradlew :sample:installDebug \
  -Ppolylex.manifestUrl=http://10.0.2.2:8080/manifest.json
```

Launch from the emulator's app drawer. The app icon is "PolylexDemo".

**First cold launch:** strings render in English (the bundled fallback). This is the session-immutability rule (ADR-004) in action — Polylex hasn't fetched yet.

Force-stop and relaunch:

```bash
adb shell am force-stop dev.polylex.sample
adb shell am start -n dev.polylex.sample/.MainActivity
```

**Second launch:** strings render in your device's locale (likely English on a fresh emulator, but the cache is now warm).

## Step 4 — Start screen recording

```bash
# Start recording on the device. Max 180s per file.
adb shell screenrecord --time-limit=60 --bit-rate=8000000 /sdcard/polylex-demo.mp4
```

The shell call blocks until either you `Ctrl+C` or it hits `--time-limit`. Leave this terminal alone — interact on the emulator.

## Step 5 — Perform the demo (~30 seconds)

While `screenrecord` is running, do this on the emulator:

1. App is on the home screen showing English copy.
2. Tap **"हिन्दी (hi)"** in the language picker. Activity recreates. Strings flip to Hindi — Devanagari script, format specifiers preserved (look at `Battery: 84%` and `Hello, Akhilesh`).
3. Tap **"日本語 (ja)"**. Strings flip to Japanese.
4. Tap **"العربية (ar)"**. Strings flip to Arabic. Layout becomes RTL, but format specifiers (the `%1$d`, etc.) stay LTR-wrapped — that's the LRE/PDF bidi handling in action.
5. Tap **"English (en)"** to flip back.
6. Tap **"Refresh translations"** at the bottom — this calls `Polylex.forceRefresh()`. No visible UI change but the action is recorded in logs.

## Step 6 — Pull the recording

Stop the `screenrecord` (Ctrl+C if it didn't auto-stop). Then:

```bash
adb pull /sdcard/polylex-demo.mp4 ./polylex-demo.mp4
adb shell rm /sdcard/polylex-demo.mp4
```

## Step 7 — Convert to GIF

The README needs a GIF (autoplays in GitHub markdown without click-to-play).

```bash
# Single-pass conversion at 12 fps, 720px wide. Adjust if file size is too big.
ffmpeg -i polylex-demo.mp4 \
  -vf "fps=12,scale=720:-1:flags=lanczos,split[s0][s1];[s0]palettegen=max_colors=128[p];[s1][p]paletteuse=dither=bayer:bayer_scale=5" \
  -loop 0 \
  polylex-demo.gif

# Verify size — GitHub renders inline GIFs up to ~10 MB but >5 MB feels slow.
ls -lh polylex-demo.gif
```

If the GIF is over 5 MB, drop the framerate or width:

```bash
# More aggressive: 10 fps, 600px wide, 96 colors
ffmpeg -i polylex-demo.mp4 \
  -vf "fps=10,scale=600:-1:flags=lanczos,split[s0][s1];[s0]palettegen=max_colors=96[p];[s1][p]paletteuse=dither=bayer:bayer_scale=5" \
  -loop 0 \
  polylex-demo.gif
```

## Step 8 — Add it to the README

Move the GIF somewhere persistent in the SDK repo:

```bash
mkdir -p docs/assets
mv polylex-demo.gif docs/assets/
```

In the SDK README, swap the placeholder hero block for:

```markdown
![Polylex Android demo: live language switch with format specifiers and HTML preserved](docs/assets/polylex-demo.gif)
```

Commit and push.

## Troubleshooting

- **`adb: no devices`** → emulator hasn't finished booting; wait. Or `adb start-server`.
- **App crashes on launch** → run `adb logcat | grep -i polylex` while reproducing. The most likely cause is the manifest URL being unreachable from the emulator (verify `curl http://localhost:8080/manifest.json` works from your laptop, then the SDK is at `http://10.0.2.2:8080/manifest.json`).
- **First language tap doesn't change anything** → this is correct behavior on cold installs (session-immutability). Force-stop and relaunch once before recording.
- **Translations look like `[hi] Welcome…`** → you ran the smoke test with `PROVIDER=mock` instead of `gemini`. Re-run with `PROVIDER=gemini` set.
- **Arabic doesn't look RTL** → the AppCompat locale switch needs the activity recreate; that's automatic in `MainActivity.switchToLocale`. If layout still looks wrong, file a bug — that's a real defect.

## What success looks like

The 30-second clip should clearly show:
1. App boots in English with realistic UI.
2. One tap → Hindi (Devanagari).
3. Format specifiers visibly intact in translated strings (e.g., user name interpolation works).
4. RTL flip on Arabic.
5. Smooth, fast, no perceptible loading skeleton.

That's the whole story for a 30-second LinkedIn/Twitter clip and the README hero asset.
