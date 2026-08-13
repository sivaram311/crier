package buzz.delena.crier

/**
 * Single source for the name+version surface required by CONSCIOUS rule #24
 * (must be visible in the UI About screen, mirrored here for anything else
 * that wants it without pulling in BuildConfig directly).
 */
object BuildInfo {
    const val APP_NAME = "Crier"
    const val VERSION_NAME = BuildConfig.VERSION_NAME
    const val VERSION_CODE = BuildConfig.VERSION_CODE

    val displayVersion: String
        get() = "$APP_NAME v$VERSION_NAME ($VERSION_CODE)"
}
