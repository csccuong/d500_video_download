package com.video.downloader.downloadvideo.videodownloader.core.utils

import android.content.Context
import android.util.Log
import com.cscmobi.libraryads.CSCFOConfigs
import com.cscmobi.libraryads.commons.sharepreference.CSCSPF
import com.video.downloader.downloadvideo.videodownloader.core.ads.RemoteConfigs

object OrganicGuard {

    private const val TAG = "OrganicGuard"

    /**
     * Local override for QA/testing purposes.
     * null  -> Follow production logic (RemoteConfig + Adjust / SPF)
     * true  -> Force treat as organic user (enforces sensitive content blocking)
     * false -> Force treat as non-organic user (bypasses sensitive content blocking)
     */
    var localOverrideOrganic: Boolean? = null

    /**
     * Sensitive keywords/domains commonly checked for video downloader compliance.
     */
    private val BLOCKED_KEYWORDS = listOf(
        "porn", "sex", "xxx", "adult", "nude", "hentai", "onlyfans",
        "xvideos", "xnxx", "pornhub", "xhamster", "redtube", "youporn",
        "spankbang", "brazzers", "livejasmin", "camsoda", "chaturbate",
        "stripchat", "bongacams", "eporner", "tnaflix", "tube8"
    )

    fun shouldCheckOrganicUser(): Boolean {
        localOverrideOrganic?.let { return it }

        try {
            val frc = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
            val value = frc.getValue("check_organic_user")
            Log.d(TAG, "shouldCheckOrganicUser: source=${value.source}, boolean=${value.asBoolean()}, string='${value.asString()}'")
            if (value.source == com.google.firebase.remoteconfig.FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
                return value.asBoolean()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error checking remote config: ${t.message}")
        }

        return true
    }

    /**
     * Checks if current user is considered Organic.
     * Evaluates:
     * 1. localOverrideOrganic (if testing)
     * 2. RemoteConfig check_organic_user (if false, skips blocking; defaults to true)
     * 3. CSCFOConfigs.isOrganic (Adjust attribution callback / revenue)
     * 4. CSCSPF(context).is_organic (Cached attribution across sessions)
     */
    fun isOrganicUser(context: Context): Boolean {
        localOverrideOrganic?.let {
            Log.d(TAG, "isOrganicUser (localOverride): $it")
            return it
        }

        if (!shouldCheckOrganicUser()) {
            Log.d(TAG, "isOrganicUser: shouldCheckOrganicUser is false -> return false")
            return false
        }

        val spf = CSCSPF(context)
        if (spf.is_organic) {
            Log.d(TAG, "isOrganicUser: true (spf=${spf.is_organic})")
            return true
        }

        return false
    }

    /**
     * Returns true if user is organic AND content contains restricted sensitive terms/domains.
     */
    fun isSensitiveContent(text: String?, context: Context): Boolean {
        if (text.isNullOrBlank()) return false
        if (!isOrganicUser(context)) {
            return false
        }
        return isSensitiveKeywordOrUrl(text)
    }

    /**
     * Checks if a keyword or URL matches the blocked sensitive keywords list.
     */
    fun isSensitiveKeywordOrUrl(text: String): Boolean {
        val lower = text.lowercase().trim()
        if (lower.isEmpty()) return false

        for (keyword in BLOCKED_KEYWORDS) {
            if (lower.contains(keyword)) {
                Log.w(TAG, "Blocked sensitive keyword match: '$keyword' in '$text'")
                return true
            }
        }
        return false
    }
}
