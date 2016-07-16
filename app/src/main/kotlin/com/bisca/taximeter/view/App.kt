package com.bisca.taximeter.view

import android.app.Application
import com.bisca.taximeter.data.logger.Logger
import com.bisca.taximeter.di.component.ApplicationComponent
import com.bisca.taximeter.di.component.DaggerApplicationComponent
import com.bisca.taximeter.di.module.AndroidModule

class App : Application() {

  lateinit var component: ApplicationComponent

  override fun onCreate() {
    super.onCreate()
    Logger.init()
    initInjectionGraph()
  }

  private fun initInjectionGraph() {
    component = DaggerApplicationComponent.builder()
        .androidModule(AndroidModule(this))
        .build()
  }
}
