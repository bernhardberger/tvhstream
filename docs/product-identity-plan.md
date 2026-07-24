# Product identity migration plan

Status: decision brief, partially decided on 2026-07-23

## Decision record

| Decision | Status |
|---|---|
| Distribution | GitHub-first signed releases, while remaining compatible with a later Google Play path |
| Application ID continuity | A clean break is acceptable; choose `at.leoville.<final-slug>` and re-enter credentials locally |
| Product boundary | Open: household appliance, public client with appliance profile, or public appliance product |
| Product name | Open; no working candidate is approved yet |

Do not start the mechanical namespace, artwork, or product-facing harness rename
until the two open decisions are resolved. Product-neutral stabilization and
release-safety work can proceed first.

## Recommended product boundary

If this fork is intended to become an independently developed public project,
the durable product should be described as an Android TV live-TV client for
TVHeadend. Appliance mode should be a product profile or integration layer, not
the name of every core type and engineering workflow.

That boundary supports both goals:

- The household can keep one-button startup, wake, GUIDE mapping, channel keys,
  and last-channel behavior.
- Generic connection, guide, playback, navigation, and TV UI improvements can
  evolve without TCL or Leoville assumptions.

If the app will remain household-only, `Leoville TV` is already a coherent name
and a public consumer brand is unnecessary.

## Decisions required before migration

| Decision | Why it matters |
|---|---|
| Household-only or public product | Determines name, support expectations, privacy text, settings, and repository presentation |
| Sideload/GitHub or Google Play | Determines AAB, store assets, 64-bit/16 KB policy, accessibility review, signing, and release automation |
| TVHeadend-only or future backends | Determines whether the name and architecture can explicitly reference HTSP/TVHeadend |
| Preserve installed app data | Determines whether `at.leoville.tvhstream` can change without a reinstall and credential re-entry |
| Appliance mode mandatory or optional | Determines manifest entries, onboarding, settings, and product documentation |

## Application ID continuity

The current application ID is `at.leoville.tvhstream`. The source namespace is
still `cz.preclikos.tvhstream`; these are independent Gradle concepts even
though matching them is easier to maintain.

### Preserve the application ID

Choose this when an installed, stably signed appliance must upgrade in place.
The source namespace can still move to a new package, but the public Android
identity remains `at.leoville.tvhstream`. DataStore files and the Android
Keystore alias must retain their current names to preserve settings.

This avoids data loss but leaves the upstream project name in the permanent app
ID. Android application IDs are not user-facing, so that may be acceptable.

### Make a clean application-ID break

Choose this before the first durable signed release if the current install is
disposable. Use `at.leoville.<final-slug>` for both application ID and namespace,
install it as a new app, and re-enter credentials locally. Do not build a secret
export/import bridge merely to avoid one controlled setup step.

This is the recommended choice if no production signing identity or irreplaceable
app-private state exists yet.

## Naming direction

The name should not imply that this is the official TVHeadend app, should not
retain TVHStream's visual identity, and should still make sense if recordings,
search, favorites, or additional appliance behavior arrive later.

### Shortlist

| Name | Positioning | Example package | Notes |
|---|---|---|---|
| **Kanalwerk** | An engineered, dependable channel appliance | `at.leoville.kanalwerk` | Recommended working candidate; distinctive and compatible with an industrial broadcast visual language |
| **Bildfunk** | A compact, slightly retro name for picture broadcasting | `at.leoville.bildfunk` | Distinctive, but the meaning is less obvious outside German-speaking markets |
| **Zappwerk** | Fast remote-first channel switching | `at.leoville.zappwerk` | Friendly and memorable, but more playful and less premium |
| **Livegrid** | Live TV plus guide/grid navigation | `at.leoville.livegrid` | International and descriptive, but generic and harder to protect/search |
| **Livetide** | A continuous flow of live programming | `at.leoville.livetide` | Consumer-oriented and backend-neutral; does not immediately signal television |
| **Leoville TV** | A private household appliance | `at.leoville.tvhstream` or `at.leoville.tv` | Best continuity if this remains a private deployment, weakest separation between publisher and product |

Preliminary GitHub searches found an existing IPTV project called ChannelDeck,
many unrelated SignalNest projects, and an active project called Fernsicht.
Those candidates should be avoided. The shortlist above has not received legal
or store-name clearance.

Before selecting any name, search Austrian and EU trademarks, Google Play,
GitHub, common package registries, domains, and social handles. Verify spelling
and pronunciation with both German- and English-speaking users. A GitHub search
alone is not clearance.

## Recommended visual direction for Kanalwerk

Use a "broadcast instrument" identity rather than another generic television
outline with a play triangle.

- Mark: a compact K or channel-grid symbol built from aligned signal bars and a
  single tuning notch.
- Tone: precise, calm, durable, and legible from across a room.
- Palette: near-black navy, warm off-white, and one high-visibility tuning color
  such as amber or cyan. Validate every focus and text pair for contrast.
- Motion: short focus scale and glow only; no decorative ambient animation
  competing with live content.
- Typography: a highly legible humanist or geometric sans with tabular channel
  numbers and a complete Latin/Central European glyph set.
- UI relationship: the launcher identity may be expressive, but in-app chrome
  should recede behind channels and programs.

The final design should be tested on the actual TV at launcher scale. Fine lines,
small text, gradients, and low-contrast marks that look good on a monitor often
fail at ten feet.

## Required artwork set

| Asset | Requirement |
|---|---|
| Adaptive launcher icon | Foreground, background, and Android monochrome layer; test masks and launcher scaling |
| Legacy launcher icons | Density-specific fallbacks generated from the same source |
| TV banner | 320x180 px, includes the product name, with safe internal padding |
| README mark | Source SVG plus rendered light/dark PNG as needed |
| Repository social preview | 1280x640 px or the current GitHub recommendation |
| Play feature graphic | 1024x500 px if Play distribution is approved |
| TV screenshots | Representative 1080p channel, guide, settings, and player views with no private server data |
| Source artwork | Editable vector source, palette/type notes, and export instructions tracked in an `artwork/` directory |

Do not place TVHeadend credentials, server names, household channels, private
addresses, or signing information in screenshots or source artwork.

## README and public documentation

The replacement README should contain:

1. Product name, one-sentence purpose, and current maturity.
2. Representative, privacy-safe screenshots.
3. Supported platform, remote control, and TVHeadend/HTSP requirements.
4. Features that actually exist, including appliance mode as optional or
   household-specific where appropriate.
5. Build and test instructions using repository tools.
6. Security statement that credentials stay in app-private storage and direct
   HTSP assumes a trusted LAN or protected tunnel.
7. Release/install status without promising Google Play until approved.
8. Clear GPLv3 license and fork attribution.
9. Contribution, issue, and upstream policy.

Suggested attribution wording:

> This project is a GPLv3 fork of
> [Preclikos/tvhstream](https://github.com/Preclikos/tvhstream). It retains the
> upstream project's history and copyright. Generic fixes may be proposed
> upstream; product-specific behavior is developed in this fork.

Keep `LICENSE`. Add a concise attribution/third-party notice that covers the
upstream project, TVHClient-derived material identified by upstream, Media3,
native decoder sources, and other distributed notices. Do not claim the combined
codebase as wholly original.

`privacy-policy.md` must be rewritten at the same time. It currently claims
Firebase use and no credential handling, neither of which describes this fork.

## Namespace and identity inventory

The migration must cover more than a source-directory rename:

| Surface | Current value/example |
|---|---|
| Gradle application ID | `at.leoville.tvhstream` |
| Gradle namespace/source package | `cz.preclikos.tvhstream` |
| Root project | `TVHStream` |
| App label | `Leoville TV` |
| Theme/API symbols | `Theme.TVHStream`, `TVHStreamTheme` |
| HTSP client name | `TVHStream / <version>` |
| Appliance action | `at.leoville.tvhstream.action.APPLIANCE_ENTRY` |
| Accessibility component | Source namespace class name |
| Stores/key alias | `tvh_settings`, `tvh_secure`, `tvh_appliance`, `tvh_secure_aes_key` |
| Fastlane package | Stale `cz.preclikos.tvhstream` |
| Tools/config | `.tvhstream-*`, package assertions and paths |
| AI harness | TVHStream/Leoville/appliance-specific agent and skill copy |
| Public metadata | README, privacy policy, GitHub name/description/topics |
| Artwork | TVHStream banner, TVH icon, adaptive/legacy launcher assets |

## Migration sequence

1. Make the product/distribution/application-ID decisions and record them in a
   durable product specification.
2. Create a feature branch from the fork base; do not perform the mechanical
   rename directly on `master`.
3. Disable stale deployment workflows before changing package identity.
4. Fix current test compilation and HTSP lifecycle blockers so the rename starts
   from a trustworthy baseline.
5. Change application ID only if a clean break is approved. Verify install versus
   upgrade behavior explicitly.
6. Rename the Gradle namespace, Kotlin packages, source directories, tests,
   manifest components, actions, theme symbols, and root project in one
   mechanical commit.
7. Preserve old DataStore and key-alias strings only when upgrade continuity is
   required. Otherwise adopt product-neutral names without compatibility code.
8. Replace launcher/banner assets and product strings in a separate visual
   identity commit.
9. Rewrite README, privacy policy, attribution/notices, native provenance, and
   repository metadata.
10. Update `AGENTS.md`, `.opencode/`, local config filenames, package assertions,
    device tooling, and harness checks to the final product model.
11. Add read-only CI and run unit tests, lint, instrumentation-test compilation,
    APK identity, ABI, 16 KB, and harness checks.
12. Install on a designated test TV and execute the full remote, playback,
    lifecycle, wake, reboot, and rollback matrix.
13. Create stable signing and deployment automation only after all preceding
    gates pass and the user explicitly approves release publication.

## Acceptance criteria

- No production source package, UI string, artwork, workflow, tool, or public
  document unintentionally presents the old TVHStream identity.
- Upstream and third-party attribution remains explicit and accurate.
- The chosen application ID follows the approved upgrade-continuity decision.
- Credentials are either preserved safely in place or intentionally re-entered;
  no secret migration channel is added without a reviewed need.
- Unit tests, lint, Android-test compilation, APK identity, 16 KB checks, and AI
  harness checks pass.
- Progressive and interlaced playback remain unchanged on a designated test TV.
- The launcher icon and banner are readable at ten feet and every operator UI
  screen retains visible, predictable D-pad focus.
