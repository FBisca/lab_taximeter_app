package com.bisca.taximeter.di.module

import android.content.Context
import com.bisca.taximeter.view.ui.LocationManager
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class GoogleModule {

  @Provides
  @Singleton
  fun providesLocationManager(context: Context): LocationManager {
    return LocationManager(context)
  }
}
