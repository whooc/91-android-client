package com.whooc.nineone

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.whooc.nineone.data.Http
import com.whooc.nineone.data.Prefs
import com.whooc.nineone.data.Store

/**
 * Everything the app needs before the first frame is wired up here.
 *
 * Order matters: [Prefs] backs the server URL that [Http]'s cookie jar and
 * every request are built against, and [Store] deserialises its JSON through
 * [Http.json].
 *
 * Implementing [ImageLoaderFactory] is not optional: every thumbnail and poster
 * comes from `/p/thumb/...`, which is behind the same session cookie as the
 * API. Coil's default OkHttp client would send no cookie and get a 401, so the
 * grid would render as empty boxes.
 */
class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Http.init(this)
        Store.init(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        // Same client (and therefore the same persistent cookie jar) as the
        // REST calls and the media stack.
        .okHttpClient { Http.client }
        .crossfade(true)
        .build()
}
