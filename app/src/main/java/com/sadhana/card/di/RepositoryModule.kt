package com.sadhana.card.di

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
}
