package com.video.downloader.downloadvideo.videodownloader.core.ads

import com.cscmobi.libraryads.commons.remote.CSCKonfigModel
import com.cscmobi.libraryads.commons.remote.konfig

object RemoteConfigs : CSCKonfigModel {

    val check_organic_user by konfig("check_organic_user", true)
    val native_question by konfig("native_question", true)
    val banner_collap_home by konfig("banner_collap_home", true)
    val inter_home by konfig("inter_home", true)
    val native_home by konfig("native_home", true)
    val native_download by konfig("native_download", true)
    val inter_play by konfig("inter_play", true)
}
