package com.mbox.stream

import android.app.Application
import com.mbox.source.youtube.YouTubeDownloader
import dagger.hilt.android.HiltAndroidApp
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization
import java.util.Locale

@HiltAndroidApp
class StreamApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            NewPipe.init(
                YouTubeDownloader.create(),
                Localization.fromLocale(Locale.getDefault())
            )
        } catch (_: Exception) {}
    }
}
