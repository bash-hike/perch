package app.perch

/** Release builds have no demo mode; these constants let R8 delete every use. */
object DemoHooks {
    const val allowOwnSession = false
    val deviceName: String? = null
    val sourceName: String? = null
}
