# Permissions

Perch asks for as little as Android allows.

| Permission | Why | Prompt |
| --- | --- | --- |
| **Notification access** (special access) | The only way for an app to read another app's media session, to show what is playing and to send play, pause, next, previous and seek. Perch does not read notification content. | You switch it on in Settings |
| `POST_NOTIFICATIONS` | Post the one silent notification that carries "open Perch when music starts" | Runtime prompt, asked once after media access is on |
| `USE_FULL_SCREEN_INTENT` | Lets that notification wake the screen and show Perch over the lock screen | A Settings switch. Android leaves it off by default for apps that are not calling or alarm apps (a sideloaded install usually has it on), so Perch sends you to it once if needed |
| `BIND_NOTIFICATION_LISTENER_SERVICE`, `BIND_DREAM_SERVICE` | Declared on Perch's own services so only the system can bind them | Never shown to you |

Without those two permissions Perch still works: you just open it yourself or use the screen
saver. "Open Perch automatically" is on by default and can be switched off in settings; the settings sheet shows
which permission is missing and has a button for it.

**Not used:** `INTERNET` (the full-size cover is read from Spotify's own content provider on the device),
location, storage, contacts, camera, microphone, `QUERY_ALL_PACKAGES`, overlay, accessibility.

Check any build yourself: `aapt2 dump permissions app-release.apk`.
