package org.jameslick.foresight.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * The encrypted local database (spec §2.1, §3.6, §6): Room on top of SQLCipher. The passphrase comes from
 * KeyStoreCrypto and is never written anywhere in the clear. `exportSchema = true` writes the schema JSON
 * to app/schemas/ so migrations are reviewed in version control.
 */
@Database(
    entities = [Event::class, Participant::class, Capture::class, Inference::class, Referral::class],
    version = 1,
    exportSchema = true,
)
abstract class ForesightDatabase : RoomDatabase() {
    abstract fun events(): EventDao
    abstract fun participants(): ParticipantDao
    abstract fun captures(): CaptureDao
    abstract fun inferences(): InferenceDao
    abstract fun referrals(): ReferralDao
    abstract fun export(): ExportDao

    companion object {
        private const val NAME = "foresight.db"
        @Volatile private var instance: ForesightDatabase? = null

        fun get(context: Context): ForesightDatabase = instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

        private fun build(context: Context): ForesightDatabase {
            System.loadLibrary("sqlcipher")
            val passphrase = KeyStoreCrypto.get().databasePassphrase(context)
            return Room.databaseBuilder(context, ForesightDatabase::class.java, NAME)
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .build()
        }
    }
}
