package com.goreecloud.gifs.data

/**
 * Accept only one explicitly shared, readable image URI from Android ACTION_SEND.
 * Other action types, formats, local file paths and remote URLs are not importable.
 */
object IncomingMediaPolicy {
    const val ACTION_SEND = "android.intent.action.SEND"

    fun accepts(
        action: String?,
        mime: String?,
        scheme: String?,
        authority: String?,
        clipItemCount: Int = 0,
        uriSourcesAgree: Boolean = true
    ): Boolean =
        action == ACTION_SEND &&
            mime?.trim()?.lowercase() in setOf("image/gif", "image/png") &&
            scheme == "content" && !authority.isNullOrBlank() &&
            clipItemCount in 0..1 && uriSourcesAgree
}
