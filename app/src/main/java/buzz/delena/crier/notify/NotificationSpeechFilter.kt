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

    fun spokenLine(appLabel: String, title: String?, text: String?, subText: String? = null): String {
        val head = title?.trim().orEmpty()
        val body = text?.trim().orEmpty()
        val extra = subText?.trim().orEmpty()

        val parts = mutableListOf<String>()
        if (head.isNotEmpty()) parts += head
        if (extra.isNotEmpty() && extra != head) parts += "($extra)"
        if (body.isNotEmpty() && body != head) parts += body

        val content = parts.joinToString(". ")
        val combined = if (content.isNotEmpty()) "$appLabel: $content" else appLabel
        return combined.take(4000)
    }
}
