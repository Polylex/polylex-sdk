# Polylex

**Merge a PR. Every language ships.**

Polylex is a localization runtime for mobile apps. It fetches translations from a CDN on app launch, overrides native string resolution, and lets you ship copy changes in every supported locale without an app release.

- Auto-translate string deltas on every PR merge (via the [Polylex GitHub Action](https://github.com/polylex/polylex-action) — coming soon)
- Native Android + iOS SDK, zero call-site changes — your existing `getString(R.string.welcome_title)` / `NSLocalizedString("welcome_title")` just work
- Session-consistent, offline-safe, graceful fallback to bundled strings
- Apache 2.0 licensed. Bring your own S3 + CDN

## Status

**Pre-alpha.** Active development. Android SDK landing first, iOS (via KMM) next.

Follow this repo or [sign up for updates](https://polylex.dev) to hear when v0.1 ships to Maven Central.

## Quickstart (Android — preview)

```kotlin
// 1. Initialize in your Application class
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Polylex.initialize(
            config = PolylexConfig(
                cdnBaseUrl = "https://cdn.example.com/polylex",
                enableLogging = BuildConfig.DEBUG
            ),
            context = applicationContext
        )
    }
}

// 2. Wrap Activity context in your BaseActivity
class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(PolylexContextWrapper.wrap(newBase))
    }
}
```

That's the full integration. Every `getString(R.string.welcome_title)` across your app now resolves against Polylex's in-memory translation cache first, falling back to your bundled `strings.xml` on any miss.

## How it works

```
  Developer edits strings.xml ─────► Merge PR
                                         │
                                         ▼
                         Polylex GitHub Action
                         ─ extract delta (new/changed keys only)
                         ─ translate via Google / DeepL / OpenAI
                         ─ upload to your S3 + CDN (timestamped)
                                         │
                                         ▼
                      App launches ─────► Polylex SDK
                      ─ fetch messages_<locale>.json from CDN
                      ─ 3-tier cache: memory → disk → network
                      ─ override getString() natively
                      ─ fall back to bundled strings on any failure
                                         │
                                         ▼
                      User sees updated copy on next session
```

## Design principles

1. **Zero call-site migration.** If your code already calls `getString()` / `NSLocalizedString()`, you change nothing. Polylex hooks the platform's native string resolution.
2. **Session immutability.** Translations rendered in the current session never change mid-flight. Background fetches update the disk cache for the next cold start.
3. **Graceful degradation.** If the CDN is down, the cache is corrupt, or the network is offline, the app falls back to bundled strings. Polylex never blocks your UI or shows blank text.
4. **Bring Your Own Cloud.** Your translations live in your S3 bucket, on your CDN, under your keys. Polylex has no access to your content. Walk away anytime — the JSON is yours.

## Roadmap

See [ROADMAP.md](ROADMAP.md).

## License

[Apache License 2.0](LICENSE). The SDK and GitHub Action will remain free forever under this license.
