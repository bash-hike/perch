# How it works

```
phone (Spotify) --Connect--> Spotify app on the tablet --plays audio
                                      | MediaSession
                              Perch  (display + controls + clock)
```

## Why Perch does not decode Spotify

Receivers such as Rusty, librespot and Raspotify implement Spotify Connect themselves and decrypt the audio on
the device. In 2025–26 Spotify began refusing those clients the audio decryption key
(`audio key error 0 1`; see [librespot#1649](https://github.com/librespot-org/librespot/issues/1649)), and
has said these devices are being phased out. Every track then fails to load and is skipped.

Perch sidesteps that entirely. The tablet's **own Spotify app** is the player. It appears in the device list of
Spotify on your phone under the tablet's name ("This tablet" is only what the tablet's own Spotify calls
itself). Perch reads that app's **media session** — title, artist, cover, position, play state — and sends
transport commands (play, pause, next, previous, seek) back to it. Nothing Spotify-specific is decoded, and any
app with a media session works the same way.

The cost is that the Spotify app has to be alive on the tablet, and that Android requires **notification
access** before an app may list another app's media sessions. Perch reads no notifications; it holds the access
only for the media sessions.

## Cover art

Spotify's session carries a 240 px thumbnail and a `content://` URI to the 640 px cover, and re-sends the
metadata about ten times per song (some with the thumbnail, some without). Perch loads the 640 px cover from
Spotify's content provider and never lets a smaller picture replace one it already holds for the same song.

## The screen: player, clock, power

`Policy.kt` holds the rules as pure functions (unit-tested):

- **Player** while a song plays and for the "after a pause" delay; otherwise the **clock**.
- The player always holds the screen on. The clock holds it on forever when plugged in, and for the chosen
  time on battery counted from the last touch; after that the system's own screen timeout takes over.
- The clock dims to a low window brightness until touched.

## Why a screen saver, and why Perch cannot launch itself

Since Android 10 an app cannot start an activity from the background. In testing, Perch's launch from its
bound notification-listener service was blocked (`Background activity launch blocked ... BAL_BLOCK`). The
documented exemptions are a visible window, the user-granted "display over other apps" permission, a
full-screen-intent notification, and being the default home app
([Android: background activity starts](https://developer.android.com/guide/components/activities/background-starts)).

- The **screen saver** (`PerchDream`) is the platform's own mechanism for "show this on the idle, docked
  tablet". Samsung and stock Android start it when the screen would turn off while charging, including over
  the lock screen. It needs no permission. Android does not run screen savers on battery.
- **Opening automatically** covers the battery case and the screen-off case: when a song starts while the tablet
  is asleep or on the lock screen, Perch posts one silent notification carrying a *full-screen intent*, which makes
  Android wake the screen and show Perch over the lock screen. The same notification is posted when the screen comes on
  by itself (power button, a tap) while music plays and the device is locked, so the lock screen is replaced by Perch. It needs the notification permission and the
  user-granted "full screen notifications" switch. Since Android 14 that switch is enabled by default only
  for calling and alarm apps ([AOSP](https://source.android.com/docs/core/permissions/fsi-limits)), which is why Perch asks you to switch it on once.

## Source map

| File | Role |
| --- | --- |
| `MediaWatcher.kt` | Follows the active media session; picks Spotify, or whatever is playing |
| `PerchListener.kt` | The notification-listener service Android requires for session access |
| `PerchDream.kt` | The screen saver; hosts the same Compose UI |
| `Policy.kt` | The display rules, as testable pure functions |
| `PerchApp.kt` | Process-wide state; the open-on-play launch |
| `ui/` | Compose: player, clock, wavy progress, settings sheet, glyphs, cover-derived colours |
