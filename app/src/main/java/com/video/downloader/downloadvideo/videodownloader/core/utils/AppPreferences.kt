package com.video.downloader.downloadvideo.videodownloader.core.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs_d54", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_NO_FIRST_OPEN = "isNoFirstOpenApp"
        private const val KEY_USER_LANGUAGE = "user_language_code"
        private const val KEY_LAST_SEARCHED_URL = "last_entered_searched_url"
    }

    var isNoFirstOpenApp: Boolean
        get() = prefs.getBoolean(KEY_IS_NO_FIRST_OPEN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_NO_FIRST_OPEN, value).apply()

    var userLanguageCode: String
        get() = prefs.getString(KEY_USER_LANGUAGE, null) ?: SystemUtil.getPreLanguage(context)
        set(value) {
            prefs.edit().putString(KEY_USER_LANGUAGE, value).apply()
            SystemUtil.setPreLanguage(context, value)
        }

    var lastSearchedUrl: String
        get() = prefs.getString(KEY_LAST_SEARCHED_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SEARCHED_URL, value).apply()

    fun clearLastSearchedUrl() {
        prefs.edit().remove(KEY_LAST_SEARCHED_URL).apply()
    }
}
