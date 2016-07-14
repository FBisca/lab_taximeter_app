package com.bisca.taximeter.di.module

import com.bisca.taximeter.data.repository.RideRepository
import com.bisca.taximeter.data.repository.RideRepositoryImpl
import com.bisca.taximeter.di.scope.ContextScope
import dagger.Module
import dagger.Provides

@Module
class RepositoryModule {

  @Provides
  @ContextScope
  fun providesRideRepository(): RideRepository {
    return RideRepositoryImpl()
  }
}
