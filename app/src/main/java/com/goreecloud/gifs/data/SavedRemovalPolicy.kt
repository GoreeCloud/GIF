package com.goreecloud.gifs.data

/** Protect app-private offline media from accidental one-tap deletion. */
object SavedRemovalPolicy {
    fun requiresConfirmation(savedIds: Set<String>, item: MediaItem): Boolean =
        item.id in savedIds

    fun warning(item: MediaItem): String =
        if (item.id.startsWith("local:")) {
            "This removes the imported file from GoreeCloud GIF's private storage. " +
                "There may be no remote source to restore it. " +
                "Any separate photo-library export is not deleted."
        } else {
            "This removes the offline copy from GoreeCloud GIF's private storage. " +
                "The item can be saved again if its source remains available. " +
                "Any separate photo-library export is not deleted."
        }
}
