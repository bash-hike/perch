# Setup

## Requirements

- An Android **15 or newer** tablet (built and tested on a Galaxy Tab A9, Android 16). A phone works too, but
  the layouts are designed for a tablet on a shelf or dock.
- The **Spotify** app, signed in, on that tablet (Spotify Premium is needed for Connect playback from a phone).
- Perch's notification access (below).

## First run

1. Install Perch ([README](../README.md#install)) and open it. The clock appears.
2. Tap **Allow media access**. Android opens the notification-access screen; switch **Perch** on. Perch reads no
   notifications — Android simply ties media-session access to this permission.
3. In Spotify on your **phone**, open the device list and pick the tablet (it is listed under the name set in the
   tablet's Settings > About tablet > Device name).
4. Settings > Display > **Screen saver** > pick **Perch**, with "while charging". Dock or charge the tablet and leave
   it; at the screen timeout the screen saver starts by itself.

The settings sheet (sliders button, top right; on the clock it appears after a touch) sets how long the clock stays
on on battery and how long a pause lasts before the clock takes over. The gear next to Perch in the Screen saver
list opens the app.

## Opening automatically when music starts

**Open Perch automatically** is on by default. Right after media access is on, Perch asks once for the notification
permission and then opens the "full screen notifications" switch for you to turn on (sideloaded installs already
have it on). After that, a song starting while the tablet sleeps or sits on the lock screen wakes the screen and
shows Perch over it. It does nothing while you are using the tablet. If a permission is still missing, the settings
sheet says which and has a button for it; the switch there turns the feature off.

## Keep Spotify alive

Spotify lists the tablet in Connect only while its app is running, and Perch can only show what an app that is
running is playing. Perch does not start or hold Spotify for you; you set that up once on the tablet:

- **Spotify:** Settings > Apps > Spotify > Battery > **Unrestricted** (on Samsung also add it under Settings > Battery >
  Background usage limits > **Never sleeping apps**).
- **Perch:** the same for Perch, so Android does not stop its media-access service.
- Open Spotify once after a restart, and avoid closing everything from the recents screen: that can stop Spotify, and the
  tablet then drops out of your phone's device list until Spotify runs again.

If the tablet still vanishes from the device list, it is Spotify's side that is not running; open it on the tablet.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| The clock shows but never the player | Notification access is off. Tap **Allow media access** on the clock, or Settings > Notifications > Device & app notifications. |
| The tablet is missing from Spotify's device list | Open Spotify on the tablet once; exclude it from battery optimisation. |
| The screen saver never starts | Settings > Display > Screen saver must be on, set to Perch, "while charging", and the tablet actually charging. Android does not start screen savers on battery. |
| "Show on the lock screen" does nothing | Both the notification permission and "full screen notifications" must be allowed for Perch. The settings sheet warns when the second is off. |
