package com.lexumi.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Marks the app-lifetime [CoroutineScope] below — needed so a fire-and-forget push (e.g.
 * [com.lexumi.app.data.sync.ContentSyncRepository.pushWordProgress]) survives the calling screen
 * being left/recreated, unlike a ViewModel's own `viewModelScope` (cancelled as soon as that
 * screen is gone, which would silently drop an in-flight push). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppCoroutineScope

@Module
@InstallIn(SingletonComponent::class)
object AppCoroutineScopeModule {
    @Provides
    @Singleton
    @AppCoroutineScope
    fun provideAppCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
