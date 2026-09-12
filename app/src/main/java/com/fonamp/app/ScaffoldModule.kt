package com.fonamp.app

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Slice A scaffold: proves Hilt + KSP wiring compiles. Real bindings land in Slice I. */
@Module
@InstallIn(SingletonComponent::class)
object ScaffoldModule {

    @Provides
    @Singleton
    fun provideAppTag(): String = "fonamp-slice-a"
}
