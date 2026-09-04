package com.kheyr.sms.data

import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The startup recovery path deletes the user's entire SMS store, so it must fire ONLY when the file
 * genuinely cannot be decrypted. Everything else - a forgotten migration above all - has to keep
 * failing loudly instead of quietly wiping the device.
 */
@RunWith(RobolectricTestRunner::class)
class AppDatabaseRecoveryTest {
    @Test fun recognisesAnUndecryptableStore() {
        assertTrue(AppDatabase.isUndecryptableStore(SQLiteException("file is not a database")))
        assertTrue(AppDatabase.isUndecryptableStore(SQLiteException("file is encrypted or is not a database")))
        assertTrue(AppDatabase.isUndecryptableStore(SQLiteDatabaseCorruptException("database disk image is malformed")))
    }

    @Test fun findsTheCauseThroughAWrapper() {
        val wrapped = IllegalStateException("could not open", SQLiteException("file is not a database"))
        assertTrue(AppDatabase.isUndecryptableStore(wrapped))
    }

    @Test fun neverRecreatesForAMissingMigration() {
        // Room's own wording when addMigrations() is missing an entry. Deleting here would turn a
        // developer mistake into silent, irreversible data loss for every user on upgrade.
        val missingMigration = IllegalStateException(
            "A migration from 3 to 4 was required but not found. Please provide the necessary Migration path",
        )
        assertFalse(AppDatabase.isUndecryptableStore(missingMigration))
    }

    @Test fun neverRecreatesForAnIntegrityCheckOrATransientFailure() {
        assertFalse(
            AppDatabase.isUndecryptableStore(
                IllegalStateException("Room cannot verify the data integrity. Looks like you've changed schema"),
            ),
        )
        assertFalse(AppDatabase.isUndecryptableStore(SQLiteFullException("database or disk is full")))
        assertFalse(AppDatabase.isUndecryptableStore(SQLiteException("database is locked")))
    }
}
