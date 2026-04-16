# Contributing to Effective Instruments

Thanks for considering a contribution. This mod is small and opinionated,
so a few notes up front save everyone time.

## Dev setup

1. Clone the repo.
2. `./gradlew build` — this pulls Genshin Instruments and Even More
   Instruments from [Curse Maven](https://www.cursemaven.com/) on first run.
   No local jars required.
3. `./gradlew runClient` launches a dev client with both dependencies
   pre-deobfuscated.

You need JDK 17 (Temurin recommended). The Gradle wrapper jar is
committed, so `./gradlew` works out of the box.

If you bump a dependency version, update the matching
`_file_id` property in `gradle.properties`. Look up file ids at
`https://www.curseforge.com/minecraft/mc-mods/<slug>/files`.

## Scope

- The core feature set (aura presets, instrument mapping, selector UI,
  effect application) is intentionally minimal. Proposals to rework
  core mechanics need a design discussion before a PR.
- New config knobs are welcome when they address a modpack-author pain
  point. Aim for sensible defaults that preserve existing behavior.
- Avoid drive-by refactors. A bug fix should fix the bug, not also
  rename three variables.

## Tests

- Unit tests live under `src/test/java/`. They run on plain JUnit 5
  without a Minecraft runtime — keep it that way.
- Pure functions (e.g. `OverwritePolicy.shouldOverwrite`) can be tested
  directly. For logic that touches `ForgeRegistries` or `MobEffect`,
  mirror the approach in `InstrumentAuraMappingJsonTest` and parse a
  local-record version of the data.
- `./gradlew test` before pushing.

## Commits

- Keep commit messages descriptive — the "why" matters more than the "what".
- No co-author tags unless you actually had a co-author.
- Don't amend published commits.

## Reporting bugs

Open an issue with: Minecraft version, Forge version, mod version,
list of other installed mods, and a log excerpt. Server-side bugs
should include `logs/latest.log` from a dedicated server if possible.

## Security

Please report security issues privately — see [`SECURITY.md`](SECURITY.md).
