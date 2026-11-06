package com.mbox.stream.util

import com.mbox.stream.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BuildStamp {
    val versionName: String = BuildConfig.VERSION_NAME
    val buildTime: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    val gitHash: String = "f16952b"

    val fullStamp: String = "v$versionName (Build $buildTime, Git $gitHash)"
}
