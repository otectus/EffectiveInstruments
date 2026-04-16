# Security Policy

## Supported versions

Only the latest released version of Effective Instruments receives
security fixes. When a fix ships, older versions are not backported.

## Reporting a vulnerability

If you believe you've found a security issue — for example, a way to
crash a dedicated server via a malformed packet, bypass the
trust-boundary checks on `InstrumentOpenC2SPacket`, or cause arbitrary
effect application — please do **not** open a public GitHub issue.

Instead, email the maintainer directly (see the author field on the
project's CurseForge or Modrinth page), or use GitHub's private
vulnerability reporting feature on this repository.

Please include:

- The mod version, Minecraft version, and Forge version you tested against.
- A minimal reproduction (config file, packet capture, or steps).
- Impact — what does the issue let an attacker do?

You'll get an acknowledgement within 7 days. Fixes are published as
patch releases with a security note in the changelog.

## Known defensive surface

For reference, the following defensive measures are already in place
and any bypass of them counts as a security issue:

- Server-authoritative instrument-open state (client packets cannot
  set the authoritative flag).
- 5-tick rate limit on `SelectAuraC2SPacket` and
  `InstrumentOpenC2SPacket` (independent cooldowns).
- Server-side validation that the selected aura exists, is enabled,
  and is in the allowed list for the current instrument.
- Aura JSON files capped at 64 KB and IDs constrained to `[a-z0-9_]+`.
- Effect amplifier clamping to 0–4.
- Schema version rejection for files newer than the running mod.
