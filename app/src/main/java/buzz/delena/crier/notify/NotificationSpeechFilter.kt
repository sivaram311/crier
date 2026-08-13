package buzz.delena.crier.notify

object NotificationSpeechFilter {
    fun shouldSpeak(
        packageName: String,
        ownPackage: String,
        allowedPackages: Set<String>,
        title: String?,
        text: String?,
        isOngoing: Boolean,
        isGroupSummary: Boolean,
        isForegroundService: Boolean,
        allowAll: Boolean = false,
    ): Boolean {
        if (packageName == ownPackage) return false
        if (!allowAll && (allowedPackages.isEmpty() || packageName !in allowedPackages)) return false
        if (isOngoing || isGroupSummary || isForegroundService) return false
        val titleOk = !title.isNullOrBlank()
        val textOk = !text.isNullOrBlank()
        return titleOk || textOk
    }

    fun spokenLine(appLabel: String, title: String?, text: String?): String {
        val head = title?.trim().orEmpty()
        val body = text?.trim().orEmpty()
        val combined = when {
            head.isNotEmpty() && body.isNotEmpty() -> "$appLabel: $head. $body"
            head.isNotEmpty() -> "$appLabel: $head"
            body.isNotEmpty() -> "$appLabel: $body"
            else -> appLabel
        }
        return combined.take(220)
    }
}
