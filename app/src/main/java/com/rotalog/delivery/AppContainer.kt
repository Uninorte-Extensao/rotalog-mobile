package com.rotalog.delivery

import android.app.Application
import com.rotalog.delivery.data.DataStoreSettingsRepository
import com.rotalog.delivery.data.DemoRouteRepository

class RotalogApplication : Application() {
    val container by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    val routes = DemoRouteRepository()
    val settings = DataStoreSettingsRepository(application)
}