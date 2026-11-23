# Changelog

All notable changes to Perch are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The section matching each release tag is published as that release's notes.

## [Unreleased]

## [0.1.1] - 2026-11-24

### Added
- Waking a locked tablet (power button, a tap) while music is playing now shows Perch instead of the plain lock screen.
  It only reacts to a wake that already happened; it never wakes the screen by itself, and it leaves an unlocked tablet alone.

## [0.1.0] - 2026-11-24

First release.

### Added
- A now-playing screen for the tablet's own Spotify app: square cover with a glow, title and artist, a wavy
  progress bar you can tap or drag to seek, a play button that morphs between a circle and a rounded square,
  and swipe-on-cover for next and previous. Colours follow the cover.
- An always-on-display style clock and date, stacked in portrait and on one line in landscape, that dims to a
  low backlight and drifts a few pixels a minute against burn-in.
- Plugged in, the clock stays on forever. On battery it stays on for a time you set (1 minute to 1 hour).
- After a pause the player gives way to the clock after 30 seconds to 5 minutes, as you choose.
- A screen saver (Settings > Display > Screen saver) that shows the same screens by itself when the tablet is
  idle on the charger, including over the lock screen.
- Opens itself when a song starts on a sleeping or locked tablet: wakes the screen and shows Perch over the lock
  screen. On by default, never while you are using the tablet, and a switch in settings turns it off. It needs the
  notification permission and the "full screen notifications" switch, which Perch asks for once.
- Works with any app that publishes a media session, not only Spotify.

### Notes
- Perch does not play or decode audio. It reads the media session of the Spotify app on the same tablet and
  needs notification access for that. See [How it works](docs/how-it-works.md).

[Unreleased]: https://github.com/bash-hike/perch/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/bash-hike/perch/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/bash-hike/perch/releases/tag/v0.1.0
