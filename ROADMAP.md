# Polylex Roadmap

This roadmap reflects the current plan. Items may shift as real users report what they actually need.

## v0.1 — Android SDK MVP (in progress)

- `Polylex.initialize(config, context)` public API
- `PolylexContextWrapper` + `PolylexResources` for zero-migration `getString()` override
- 3-tier cache: in-memory → on-disk JSON → CDN fetch
- Locale normalization (Android `zh-CN`/`in`/`nb-NO` → Polylex canonical `zh-cn`/`id`/`no`)
- Exponential-backoff retry (3 attempts, 1s → 5s) with graceful fallback to bundled strings
- Session-consistent fallback rule: in-memory cache only flips on cold start or explicit commit
- Maven Central publishing as `dev.polylex:polylex-android:0.1.0`

## v0.2 — iOS SDK via KMM

- Port core logic to Kotlin Multiplatform `commonMain`
- iOS `Bundle.localizedString(forKey:value:table:)` method swizzling (shipped *inside* the library, not as customer homework)
- Swift Package Manager distribution
- Parity with Android feature set

## v0.3 — GitHub Action

- `polylex/extract` — parses `strings.xml` + iOS `.strings` files, emits a delta JSON of new/changed keys
- `polylex/translate` — Google Cloud Translation v3 by default, DeepL + OpenAI as opt-in providers
- `polylex/deploy` — uploads to your S3 bucket with timestamped versioning
- Placeholder masking (`%1$s`, `%@`, `{var}`, HTML tags) so MT services don't corrupt format specifiers
- RTL locale handling with bidi control characters around placeholders

## v0.4 — Demo & Docs

- Reference app (Jetpack Compose + SwiftUI) demonstrating end-to-end integration
- Documentation site at docs.polylex.dev
- Migration guide from Lokalise / Phrase / Crowdin
- Comparison benchmarks

## Future (directional, not committed)

- **Polylex Cloud** — optional hosted CDN, manifest server, and analytics for teams who don't want to run their own S3. Paid tier. The OSS SDK and Action remain free forever.
- A/B testing on copy across locales (runtime experiments)
- Translation coverage dashboard (which keys are missing in which locales)
- Human review workflow (Slack approvals, Linear ticket auto-creation)
- Flutter and React Native SDKs
- Plural and gender support (ICU MessageFormat)

---

_The SDK and GitHub Action will always be Apache 2.0. A paid hosted product may exist alongside them — never in place of them._
