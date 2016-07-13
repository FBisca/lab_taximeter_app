package com.bisca.taximeter.di.component

import android.content.Context
import com.bisca.taximeter.di.module.AndroidModule
import com.bisca.taximeter.di.module.GoogleModule
import com.google.android.gms.common.api.GoogleApiClient
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
  fun googleApiClient(): GoogleApiClient
}
