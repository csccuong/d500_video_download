package com.video.downloader.downloadvideo.videodownloader.core.utils

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

/**
 * Safely displays a [DialogFragment] without crashing when [FragmentManager] has already
 * saved its instance state or been destroyed, and prevents duplicate dialogs with the same tag.
 */
fun DialogFragment.showSafely(manager: FragmentManager, tag: String) {
    if (manager.isDestroyed || manager.isStateSaved) return
    if (manager.findFragmentByTag(tag) != null) return
    try {
        show(manager, tag)
    } catch (_: IllegalStateException) {
    }
}
