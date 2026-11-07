package com.mbox.stream.util

import com.mbox.stream.BuildConfig

object BuildStamp {
    val versionName: String = BuildConfig.VERSION_NAME
    val buildTime: String = BuildConfig.BUILD_TIME
    val gitHash: String = BuildConfig.GIT_HASH

    val fullStamp: String = "v$versionName (Build $buildTime, Git $gitHash)"
}
