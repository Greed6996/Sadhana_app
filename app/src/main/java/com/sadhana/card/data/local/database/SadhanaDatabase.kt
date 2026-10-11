package com.sadhana.card.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sadhana.card.data.local.converter.SadhanaTypeConverters
import com.sadhana.card.data.local.dao.SadhanaDao
import com.sadhana.card.data.local.entity.SadhanaEntryEntity

/**
 * Room Database for Sadhana Card.
 */
@Database(
    entities = [SadhanaEntryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(SadhanaTypeConverters::class)
abstract class SadhanaDatabase : RoomDatabase() {
    abstract val sadhanaDao: SadhanaDao

    companion object {
        const val DATABASE_NAME = "sadhana_card.db"
    }
}
