package com.bisca.taximeter.extensions

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.bisca.taximeter.di.component.ApplicationComponent
import com.bisca.taximeter.view.App
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import java.util.*

fun Application.getComponent(): ApplicationComponent {
  return (this as App).component
}

val Context.defaultLocale: Locale
  get() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return resources.configuration.locales.get(0)
    } else {
      return resources.configuration.locale
    }
  }