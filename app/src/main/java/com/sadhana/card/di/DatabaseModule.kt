package com.sadhana.card.di

import android.content.Context
import androidx.room.Room
import com.sadhana.card.data.local.dao.SadhanaDao
import com.sadhana.card.data.local.database.SadhanaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSadhanaDatabase(
        @ApplicationContext context: Context
    ): SadhanaDatabase {
        return Room.databaseBuilder(
            context,
            SadhanaDatabase::class.java,
            SadhanaDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideSadhanaDao(
        database: SadhanaDatabase
    ): SadhanaDao {
        return database.sadhanaDao
    }
}
