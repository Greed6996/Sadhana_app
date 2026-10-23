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
    entities = [SadhanaEntryEntity::class, com.sadhana.card.data.local.entity.SadhanaEntryConflictEntity::class],
    version = 3,
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

        /**
         * Migration from version 2 to 3:
         * Adds userId column and isSynced flag.
         * Updates primary key to composite (userId, date).
         * Creates sadhana_entry_conflicts table for safe historical claim preservation.
         * Migrates existing rows under userId = 'local'.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sadhana_entries_v3 (
                        userId TEXT NOT NULL DEFAULT 'local',
                        date TEXT NOT NULL,
                        bedtime TEXT,
                        wakeTime TEXT,
                        napMinutes INTEGER,
                        trainJourney INTEGER NOT NULL DEFAULT 0,
                        japaTime TEXT,
                        bedtimeCategory TEXT,
                        wakeUpCategory TEXT,
                        daySleepCategory TEXT,
                        japaCategory TEXT,
                        japaCompleted INTEGER NOT NULL DEFAULT 0,
                        pathanMinutes INTEGER NOT NULL DEFAULT 0,
                        sravanMinutes INTEGER NOT NULL DEFAULT 0,
                        sevaMinutes INTEGER NOT NULL DEFAULT 0,
                        sevaTags TEXT NOT NULL DEFAULT '[]',
                        isSynced INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(userId, date)
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO sadhana_entries_v3 (
                        userId, date, bedtime, wakeTime, napMinutes, trainJourney, japaTime,
                        bedtimeCategory, wakeUpCategory, daySleepCategory, japaCategory, japaCompleted,
                        pathanMinutes, sravanMinutes, sevaMinutes, sevaTags, isSynced, updatedAt
                    )
                    SELECT 'local', date, bedtime, wakeTime, napMinutes, trainJourney, japaTime,
                           bedtimeCategory, wakeUpCategory, daySleepCategory, japaCategory, japaCompleted,
                           pathanMinutes, sravanMinutes, sevaMinutes, sevaTags, 0, updatedAt
                    FROM sadhana_entries
                """.trimIndent())

                db.execSQL("DROP TABLE sadhana_entries")
                db.execSQL("ALTER TABLE sadhana_entries_v3 RENAME TO sadhana_entries")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sadhana_entries_userId_date ON sadhana_entries(userId, date)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sadhana_entry_conflicts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        date TEXT NOT NULL,
                        source TEXT NOT NULL,
                        conflictTimestamp INTEGER NOT NULL,
                        bedtime TEXT,
                        wakeTime TEXT,
                        napMinutes INTEGER,
                        trainJourney INTEGER NOT NULL DEFAULT 0,
                        japaTime TEXT,
                        bedtimeCategory TEXT,
                        wakeUpCategory TEXT,
                        daySleepCategory TEXT,
                        japaCategory TEXT,
                        japaCompleted INTEGER NOT NULL DEFAULT 0,
                        pathanMinutes INTEGER NOT NULL DEFAULT 0,
                        sravanMinutes INTEGER NOT NULL DEFAULT 0,
                        sevaMinutes INTEGER NOT NULL DEFAULT 0,
                        sevaTags TEXT NOT NULL DEFAULT '[]',
                        isSynced INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}

