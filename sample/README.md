# Polylex Sample App

A minimal Compose app demonstrating end-to-end Polylex integration on Android.

## What it shows

- `Polylex.initialize(...)` in `Application.onCreate` (one line)
- `PolylexContextWrapper.wrap(...)` in `attachBaseContext` (one line)
- Existing `getString(R.string.X)` calls auto-resolve through the Polylex cache — **no other code changes**
- A locale picker that calls `Polylex.setActiveLocale(...)` and triggers `Activity.recreate()`
- A "force refresh" button that bypasses the version short-circuit

## Configure the manifest URL

The sample reads `BuildConfig.POLYLEX_MANIFEST_URL`, which is set from a Gradle property. Default is a placeholder.

When your R2 bucket is set up (see [polylex-action/docs/setup/cloudflare-r2.md](../../polylex-action/docs/setup/cloudflare-r2.md)), build the app with:

```bash
./gradlew :sample:installDebug \
  -Ppolylex.manifestUrl=https://pub-<your-id>.r2.dev/polylex/manifest.json
```

Or hard-code it in `sample/build.gradle.kts` for development.

## Build and run

### Fastest path: local CDN, no R2 / S3 setup needed

The Polylex Action repo ships a local-serve helper that stages the smoke-test output and serves it on `localhost` with the same shape an R2 / S3 bucket would have. Use this to demo the app end-to-end before doing any cloud setup.

**Terminal 1** — generate translations + run the local CDN:

```bash
cd /path/to/polylex-action

PROVIDER=gemini ./scripts/run-demo-locally.sh   # produces _output/bundles/
node scripts/serve-demo-locally.js              # serves http://127.0.0.1:8080
```

That prints something like:

```
✓ Serving Polylex demo CDN at http://127.0.0.1:8080
  manifest:  http://127.0.0.1:8080/manifest.json
From your Android emulator:
  manifestUrl: http://10.0.2.2:8080/manifest.json
```

**Terminal 2** — install the sample app pointed at that manifest:

```bash
cd /path/to/polylex-sdk
./gradlew :sample:installDebug \
  -Ppolylex.manifestUrl=http://10.0.2.2:8080/manifest.json
```

Open the app on the emulator. First cold launch shows English (bundled fallback). Second launch (after Polylex has fetched + cached) shows the device locale. Tap a language → activity recreates → translations swap live.

For a physical Android device on the same Wi-Fi, replace `10.0.2.2` with your host's LAN IP (`ifconfig | grep "inet "` on macOS).

### With a real R2 / S3 bucket

When you've set up R2 (see [the setup guide](../../polylex-action/docs/setup/cloudflare-r2.md)):

```bash
./gradlew :sample:installDebug \
  -Ppolylex.manifestUrl=https://pub-<your-id>.r2.dev/polylex/manifest.json
```

## Notes

- Per the session-immutability rule (ADR-004), the very first launch on a fresh install always shows English (bundled). Background fetch populates the cache; second launch onward shows the translated locale.
- The locale picker uses `Polylex.setActiveLocale(...)` which fetches **before** committing — so a bad manifest never produces a half-translated UI.
- Hit "Refresh translations" in the app to test `Polylex.forceRefresh()` (bypasses the manifest-version short-circuit; useful for iterating on translations during development).
