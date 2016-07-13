package com.bisca.taximeter.di.component

import com.bisca.taximeter.view.ui.activity.MetricsActivity
import com.bisca.taximeter.view.ui.service.MetricsService
import dagger.Component


@Component(
    dependencies = arrayOf(ApplicationComponent::class)
)
interface MetricsComponent {
  fun inject(metricsService: MetricsService)
  fun inject(metricsActivity: MetricsActivity)
}
