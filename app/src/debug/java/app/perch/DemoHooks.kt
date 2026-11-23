package app.perch

/** Debug builds can show a fake session (see [DemoSession]) with a neutral device name, for screenshots. */
object DemoHooks {
    const val allowOwnSession = true
    val deviceName: String? = "Living room tablet"
    val sourceName: String? = "Spotify"
}
