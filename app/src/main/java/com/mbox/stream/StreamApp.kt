package com.mbox.stream

import android.app.Application
import android.util.Log
import com.mbox.source.youtube.YouTubeDownloader
import com.mbox.stream.util.BuildStamp
import dagger.hilt.android.HiltAndroidApp
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization
import java.util.Locale

@HiltAndroidApp
class StreamApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("MBoxStream", "M-Box Stream ${BuildStamp.fullStamp}")
        try {
            NewPipe.init(
                YouTubeDownloader.create(),
                Localization.fromLocale(Locale.getDefault())
            )
        } catch (_: Exception) {}
    }
}
