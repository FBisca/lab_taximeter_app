package com.bisca.taximeter.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AndroidModule(
    val applicationContext: Context
) {

  @Provides
  @Singleton
  fun providesApplicationContext(): Context {
    return applicationContext
  }

}
