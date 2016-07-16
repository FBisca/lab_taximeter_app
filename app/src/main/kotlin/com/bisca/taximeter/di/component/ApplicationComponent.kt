package com.bisca.taximeter.di.component

import android.content.Context
import com.bisca.taximeter.di.module.AndroidModule
import com.bisca.taximeter.di.module.GoogleModule
import com.bisca.taximeter.view.ui.location.LocationManager
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = arrayOf(
        AndroidModule::class,
        GoogleModule::class
    )
)
interface ApplicationComponent {
  fun applicationContext(): Context
  fun locationManager(): LocationManager
}
