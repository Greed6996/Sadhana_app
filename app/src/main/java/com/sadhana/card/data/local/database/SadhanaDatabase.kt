package com.sadhana.card.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sadhana.card.data.local.converter.SadhanaTypeConverters
import com.sadhana.card.data.local.dao.SadhanaDao
import com.sadhana.card.data.local.entity.SadhanaEntryEntity

/**
 * Room Database for Sadhana Card.
 */
@Database(
    entities = [SadhanaEntryEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(SadhanaTypeConverters::class)
abstract class SadhanaDatabase : RoomDatabase() {
    abstract val sadhanaDao: SadhanaDao

    companion object {
        const val DATABASE_NAME = "sadhana_card.db"

        /**
         * Migration from version 1 to 2:
         * Adds category-based scoring columns and japaCompleted flag.
         * Existing data is preserved – legacy time fields remain intact
         * and category columns default to NULL (unset).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sadhana_entries ADD COLUMN bedtimeCategory TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE sadhana_entries ADD COLUMN wakeUpCategory TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE sadhana_entries ADD COLUMN daySleepCategory TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE sadhana_entries ADD COLUMN japaCategory TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE sadhana_entries ADD COLUMN japaCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
