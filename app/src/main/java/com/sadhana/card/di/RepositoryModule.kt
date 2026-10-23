package com.sadhana.card.di

import com.sadhana.card.data.preferences.UserPreferencesRepository
import com.sadhana.card.data.preferences.UserPreferencesRepositoryImpl
import com.sadhana.card.data.repository.SadhanaRepositoryImpl
import com.sadhana.card.domain.repository.SadhanaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSadhanaRepository(
        impl: SadhanaRepositoryImpl
    ): SadhanaRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        impl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: com.sadhana.card.data.auth.AuthRepositoryImpl
    ): com.sadhana.card.data.auth.AuthRepository
}
