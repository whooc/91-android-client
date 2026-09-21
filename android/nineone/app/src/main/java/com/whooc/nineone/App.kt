package com.whooc.nineone

import android.app.Activity
import android.app.Application
import android.os.Bundle
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.whooc.nineone.data.Api
import com.whooc.nineone.data.Brand
import com.whooc.nineone.data.Http
import com.whooc.nineone.data.LockGate
import com.whooc.nineone.data.Prefs
import com.whooc.nineone.data.Store

/**
 * Everything the app needs before the first frame is wired up here.
 *
 * Order matters: [Prefs] backs the server URL that [Http]'s cookie jar and
 * every request are built against, [Store] deserialises its JSON through
 * [Http.json], and [Brand] / [LockGate] read their settings out of [Prefs].
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

        // "退出后需要重新登录" has to survive a process that is killed outright,
        // because that path never reaches onDestroy. Clearing here covers it; the
        // lifecycle callback below covers an ordinary exit.
        if (Prefs.logoutOnExit) Api.forgetSessionLocally()

        Brand.init(this)
        LockGate.sync()
        registerActivityLifecycleCallbacks(SessionTeardown())
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        // Same client (and therefore the same persistent cookie jar) as the
        // REST calls and the media stack.
        .okHttpClient { Http.client }
        .crossfade(true)
        .build()

    /**
     * Ends the session when the last activity goes away, but only while
     * [Prefs.logoutOnExit] is on.
     *
     * `isChangingConfigurations` is checked because a configuration change also
     * destroys the activity — signing the user out on a rotation would be a bug,
     * not a feature.
     */
    private class SessionTeardown : ActivityLifecycleCallbacks {

        private var started = 0

        override fun onActivityStarted(activity: Activity) {
            started++
        }

        override fun onActivityStopped(activity: Activity) {
            started--
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (activity.isChangingConfigurations) return
            if (started > 0) return
            if (Prefs.logoutOnExit) Api.forgetSessionLocally()
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

        override fun onActivityResumed(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }
}
