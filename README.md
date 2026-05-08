# Polylex

**Merge a PR. Every language ships.**

Polylex is a localization runtime for mobile apps. Your CI writes a manifest + per-locale JSON files to your CDN on every release. The SDK reads the manifest on app launch, fetches the relevant translation bundle, and overrides native string resolution — so you can ship copy changes without an app release.

> **Live demo:** point any Polylex SDK build at [`https://pub-6b024abd7a6c48aab35899fc3644645a.r2.dev/polylex/manifest.json`](https://pub-6b024abd7a6c48aab35899fc3644645a.r2.dev/polylex/manifest.json) — the bundles behind it are produced by the [Polylex GitHub Action](https://github.com/Polylex/polylex-action) running against this repo's [`sample/`](sample/) app on every relevant change. The same flow you'd run for your own app.

<!-- TODO(v0.2): replace with docs/assets/polylex-demo.gif once recorded -->

- Auto-translate string deltas on every PR merge via the [Polylex GitHub Action](https://github.com/Polylex/polylex-action)
- Native Android + iOS SDK, zero call-site changes — your existing `getString(R.string.welcome_title)` / `NSLocalizedString("welcome_title")` just work
- Version-aware fetching — the SDK only downloads a new bundle when the manifest version changes
- Session-consistent, offline-safe, graceful fallback to bundled strings
- Apache 2.0 licensed. Bring your own S3 + CDN

## Status

**v0.1 pre-release.** Android SDK is functional and tested; sample Compose app at [`sample/`](sample/) builds clean and is wired to fetch from a real Cloudflare R2 CDN. iOS (via KMM) follows in v0.3. Maven Central publishing is in flight — for now, build from source.

## The manifest contract

Polylex is agnostic to your cloud. You point the SDK at one URL — `manifest.json` — and the manifest tells the SDK where everything else lives. Example bucket layout:

```
s3://your-bucket/polylex/
├── manifest.json                          ← short TTL (e.g., 60s)
└── 2026-04-28T10-30-00Z/                  ← long TTL (immutable, version-stamped)
    └── translations/
        ├── messages_en.json
        ├── messages_hi.json
        ├── messages_ja.json
        └── ...
```

```json
// manifest.json
{
  "schema_version": 1,
  "version": "2026-04-28T10-30-00Z",
  "translations_base_url": "https://cdn.example.com/polylex/2026-04-28T10-30-00Z/translations",
  "locales": ["en", "hi", "ja", "ko", "fr", "de"],
  "generated_at": "2026-04-28T10:30:15Z",
  "source_commit": "a1b2c3d"
}
```

```json
// messages_hi.json
{
  "welcome_title": "स्वागत है",
  "cta_done": "हो गया"
}
```

The SDK fetches the manifest first, compares `version` to what it has cached, and skips the bundle download entirely when they match. This means a short-TTL manifest (~60s) and a long-TTL (days/weeks) bundle cache — fast, cheap, CDN-friendly.

Don't want timestamped versioning? Write a manifest with a fixed `version` (e.g., `"static"`) and a fixed `translations_base_url`. The SDK doesn't care — the contract is all that matters.

## Quickstart (Android)

```kotlin
// 1. Initialize in your Application class
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Polylex.initialize(
            config = PolylexConfig(
                manifestUrl = "https://cdn.example.com/polylex/manifest.json",
                enableLogging = BuildConfig.DEBUG,
            ),
            context = applicationContext,
        )
    }
}

// 2. Wrap Activity context in your BaseActivity
class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(PolylexContextWrapper.wrap(newBase))
    }
}

// 3. From a coroutine (e.g., in your splash screen ViewModel)
viewModelScope.launch {
    Polylex.refresh()  // fetch manifest + bundle, commit to in-memory cache
}
```

That's the full integration. Every `getString(R.string.welcome_title)` across your app now resolves against Polylex's in-memory translation cache first, falling back to your bundled `strings.xml` on any miss.

Want to try it locally without setting up a CDN? See [`sample/RECORDING.md`](sample/RECORDING.md) — runs the sample app on an emulator pointed at the local-serve helper from polylex-action.

## How it works

```
  Developer edits strings.xml ─────► Merge PR
                                         │
                                         ▼
                         Polylex GitHub Action
                         ─ extract delta (new/changed keys only)
                         ─ translate via Gemini (default) or pluggable provider
                         ─ upload versioned bundle to your CDN
                         ─ rewrite manifest.json with new version
                                         │
                                         ▼
                      App launches ─────► Polylex SDK
                      ─ GET manifest.json (short TTL, always fresh)
                      ─ Compare manifest.version to cached version
                          match? → use disk cache, done
                          diff?  → GET messages_<locale>.json, persist
                      ─ Commit to in-memory map
                      ─ Override getString() natively
                      ─ Fall back to bundled strings on any failure
                                         │
                                         ▼
                      User sees updated copy on next session
```

## Design principles

1. **Zero call-site migration.** If your code already calls `getString()` / `NSLocalizedString()`, you change nothing. Polylex hooks the platform's native string resolution.
2. **Version-aware fetching.** The manifest is the source of truth for which release is live. The SDK skips bundle downloads when your cached version matches, so 99% of app launches do a single tiny manifest GET and return immediately.
3. **Session immutability.** Translations rendered in the current session never change mid-flight. Background fetches update the disk cache for the next cold start.
4. **Graceful degradation.** If the CDN is down, the manifest is corrupt, or the network is offline, the app falls back to cached disk bundles → bundled strings. Polylex never blocks your UI or shows blank text.
5. **Bring Your Own Cloud.** Your translations live in your bucket, on your CDN, under your keys. Polylex has no access to your content. Walk away anytime — the JSON is yours.

## Roadmap

See [ROADMAP.md](ROADMAP.md).

## License

[Apache License 2.0](LICENSE). The SDK and GitHub Action will remain free forever under this license.
