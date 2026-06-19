---
name: android-skill-router
description: Android skill router. Use when an Android task may need android-cli (docs, emulator, device, layout, screen, run, sdk, skills) or one of the official Android specialty skills (edge-to-edge, adaptive, navigation-3, testing-setup, perfetto-trace-analysis, perfetto-sql, agp-9-upgrade, r8-analyzer, play-billing-library-version-upgrade, migrate-xml-views-to-jetpack-compose, camera1-to-camerax, styles, jetpack-compose-m3, appfunctions, engage-sdk-integration, verified-email, display-glasses-with-jetpack-compose-glimmer). Routes the task to the right skill by scope; does not execute code itself.
---

# Android Skill Router

This is a routing skill. It does not run Android commands. It decides which single Android skill to load, or which small ordered set of skills to load when a task genuinely spans multiple distinct scopes.

## When to use

Invoke when the current task touches Android and you need to decide which official skill to load. The most common case is the orchestrator triaging an Android prompt before dispatch.

## Routing rules

Default to one skill: pick the most specific matching rule. Only when the task clearly spans multiple distinct scopes should you return multiple skills, listed in priority order.

| Task scope | Load skill |
| --- | --- |
| Android CLI, adb, emulator, AVD, device interaction, APK install/run, SDK install, Android docs lookup, Android project creation/description, layout dump, screen capture, screen resolve, Android skills management | `android-cli` |
| System bars, insets, edge-to-edge, IME insets, navigation bar/status bar legibility, fixes for content obscured by system bars | `edge-to-edge` |
| Adaptive layouts, large screens, tablets, foldables, laptops, desktop, TV, Auto, XR, window-size classes, MediaQuery, navigation rail vs nav bar | `adaptive` |
| Navigation 3, Navigation 3 scenes (dialog, bottom sheet, list-detail, two-pane, supporting pane), multiple backstacks, deep links, returning results from flows, Hilt/ViewModel/Navigation 3 integration | `navigation-3` |
| Testing strategy, test libraries, JUnit, Espresso, Compose UI tests, screenshot tests, end-to-end tests, harnesses, test infrastructure setup | `testing-setup` |
| Latency / memory / jank investigation with a Perfetto trace file (open-ended or specific) | `perfetto-trace-analysis` |
| Slice / thread / memory extraction from a Perfetto trace via `trace_processor` and Perfetto SQL | `perfetto-sql` |
| AGP 9 upgrade / migration (not KMP) | `agp-9-upgrade` |
| R8 / ProGuard keep rules, shrinking, broad rules, library-consumer keep rules | `r8-analyzer` |
| Play Billing Library (PBL) version upgrade or migration | `play-billing-library-version-upgrade` |
| Migrating legacy Android XML Views to Jetpack Compose | `migrate-xml-views-to-jetpack-compose` |
| Migrating Camera1 or raw Camera2 APIs to CameraX | `camera1-to-camerax` |
| Jetpack Compose Styles API integration, styleable components, Modifier.styleable | `styles` |
| Wear OS Compose Material3, AppScaffold/ScreenScaffold, TransformingLazyColumn, Horologist migration | `jetpack-compose-m3` |
| Android XR Glimmer for display glasses | `display-glasses-with-jetpack-compose-glimmer` |
| AppFunctions: identifying user workflows, generating Kotlin for system Shortcuts/voice | `appfunctions` |
| Play Engage SDK integration, publishing, entity mapping, debug | `engage-sdk-integration` |
| Verified email retrieval via Credential Manager, OTP-less email verification | `verified-email` |

## Behavior

- Pick the most specific match. Generic Android code editing does not require a skill.
- Do not load more than one CLI-style skill per task.
- Do not pre-load all specialty skills; load only what the current task needs.
- After loading, defer to the loaded skill's instructions for execution details.
