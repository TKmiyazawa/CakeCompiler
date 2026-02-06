# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CakeCompiler is a Kotlin Multiplatform (KMP) app targeting Android and iOS using Compose Multiplatform. It recommends cakes by computing a weighted happiness score from 5-dimensional preference vectors (sweetness, sourness, texture, temperature, artistry), prioritizing partner happiness (80/20 default weighting). Key features include serendipity detection (unexpected preference divergences as learning opportunities), user override with full autonomy, a "shake to serendipity" hidden command, and the ButterflyEffect hidden function (contextual memory messages triggered on override).

**Tech Stack:** Kotlin 2.3.0, Compose Multiplatform 1.10.0, Material3, Gradle 8.14.3, version catalog at `gradle/libs.versions.toml`. Android compileSdk 36, minSdk 24. iOS: iosArm64 + iosSimulatorArm64.

## Build Commands

```bash
./gradlew :composeApp:assembleDebug        # Android debug build
./gradlew :composeApp:test                 # Run all common tests
./gradlew :composeApp:testDebugUnitTest    # Run Android unit tests only
./gradlew clean                            # Clean build
```

For iOS, open `iosApp/iosApp.xcodeproj` in Xcode.

## Architecture

### Layered Structure (Clean Architecture)

All shared code lives under `composeApp/src/commonMain/kotlin/com/example/cakecompiler/`:

- **`domain/model/`** — Pure value objects: `PreferenceVector` (5D normalized vector with dot product, distance, blend), `HappinessWeights`, `HappinessScore`, `SerendipityEvent`, `UserChoice` (sealed: AcceptRecommendation | ManualOverride), `ButterflyEffect` (hidden override memory system)
- **`domain/entity/`** — `PartnerProfile` with `PreferenceHistory` and `LearningEntry`. Evolves over time via `applyLearning()` extension
- **`domain/port/`** — `PartnerPreferenceProvider` interface (port for external inference, e.g., Gemini API). Includes `MockPartnerPreferenceProvider` for testing
- **`usecase/`** — Business logic orchestrators:
  - `CalculateHappinessUseCase` — Core formula: H = ω_self × (V_self · V_cake) + ω_partner × (V_partner · V_cake)
  - `ObserveSerendipityAnomalyUseCase` — Detects preference divergence above threshold (0.5 Euclidean distance in 5D)
  - `ApplyUserOverrideUseCase` — Processes user override (always allowed, ethical principle)
  - `LearnNewAspectUseCase` — Updates partner profile from serendipity events with adaptive learning rate
- **`presentation/viewmodel/`** — `CakeSelectionViewModel` using MVI pattern (StateFlow + SharedFlow for effects)
- **`presentation/model/`** — UI state classes (`CakeDisplayState`, `ScreenState`, `CakeUiEvent`, `CakeUiEffect`), animation models (`JiggleAnimation`, `OverrideAnimationState`), `MockCakeData` (demo data)
- **`presentation/effect/`** — `SerendipityShakeEffect` — finds most divergent cake on shake
- **`presentation/ui/`** — Compose UI: `CakeSelectionScreen`, `CakeCompilerTheme`, components (`CakeCard`, `RecommendationBanner`, `OverrideButton`, `ButterflyEffectOverlay`, `PartnerStatement`, `StatusBar`)
- **`platform/`** — `expect/actual` abstractions for `HapticFeedback` and `ShakeDetector` with mock implementations for testing

### Platform-Specific Code (`expect/actual`)

- `Platform.kt` / `Platform.android.kt` / `Platform.ios.kt` — Platform name
- `currentTimeMillis()` — in `domain/model/SerendipityEvent.kt` (expect), with `Time.android.kt` and `Time.ios.kt` (actual)
- `createHapticFeedback()` / `createShakeDetector()` — in `platform/` package

### Key Design Principles

- **User autonomy:** `canOverride()` always returns `true`. The system recommends but never prevents user choice
- **Serendipity as positive:** Divergence from predictions is treated as a learning opportunity, never an error
- **Partner-prioritizing:** Default weights favor partner happiness (0.8) over self (0.2)
- **Immutable domain models:** All domain types are data classes; state updates produce new instances

## Package Structure

All Kotlin code lives under `com.example.cakecompiler`. Comments and UI strings are in Japanese.