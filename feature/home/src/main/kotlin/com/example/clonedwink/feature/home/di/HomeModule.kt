package com.example.clonedwink.feature.home.di

import com.example.clonedwink.feature.home.data.repository.DefaultHomeRepository
import com.example.clonedwink.feature.home.data.repository.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Android: the `feature:home` half of what used to be one combined RepositoryModule in `app` —
// see `feature:landing`'s `LandingModule.kt` for the full explanation of why this lives in its
// own feature module and how Hilt still discovers it from `app`.
@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {

    @Binds
    abstract fun bindHomeRepository(impl: DefaultHomeRepository): HomeRepository
}
