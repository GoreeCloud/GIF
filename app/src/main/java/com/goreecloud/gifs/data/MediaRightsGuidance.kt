package com.goreecloud.gifs.data

/**
 * User-facing informational rights guidance, not a legal determination.
 * Public search metadata may be incomplete, misleading or changed upstream.
 */
object MediaRightsGuidance {
    fun forItem(item: MediaItem): String {
        if (item.id.startsWith("local:")) {
            return "Imported from your device. Confirm you have permission before sharing or reposting."
        }
        val license = item.license.trim().uppercase()
        return when {
            license.contains("CC BY-SA") || license.contains("ATTRIBUTION-SHAREALIKE") ->
                "Attribution and share-alike conditions may apply. Check the original license and creator."
            license.contains("CC BY") || license.contains("ATTRIBUTION") ->
                "Attribution and other conditions may apply. Check the source and license before reuse."
            license.contains("CC0") || license.contains("PUBLIC DOMAIN") ->
                "The source labels this as CC0 or public domain. Check its details and any third-party rights."
            license.isBlank() || license.contains("SEE SOURCE") ||
                license.contains("UNKNOWN") || license.contains("UNSPECIFIED") ->
                "Usage rights are not verified. Check the source before sharing or redistributing."
            else ->
                "Licensing information is source-provided, not independently verified. Confirm permitted uses."
        }
    }
}
