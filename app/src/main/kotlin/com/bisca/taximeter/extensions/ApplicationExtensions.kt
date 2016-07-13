package com.bisca.taximeter.extensions

import android.app.Application
import com.bisca.taximeter.di.component.ApplicationComponent
import com.bisca.taximeter.view.App

fun Application.getComponent(): ApplicationComponent {
  return (this as App).getComponent()
}
