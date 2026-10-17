package de.belegkasten.core.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.belegkasten.core.database.BelegkastenDb
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDb(@ApplicationContext context: Context): BelegkastenDb =
        Room.databaseBuilder(context, BelegkastenDb::class.java, "belegkasten.db")
            // No destructive migration, also not on downgrade: Room then fails loudly instead of
            // silently wiping the archive (backups must stay readable, Phase 6.7).
            .build()

    @Provides
    fun provideReceiptDao(db: BelegkastenDb) = db.receipts()

    @Provides
    fun provideItemDao(db: BelegkastenDb) = db.items()

    @Provides
    fun provideEmbeddingDao(db: BelegkastenDb) = db.embeddings()

    @Provides
    fun provideDeadlineDao(db: BelegkastenDb) = db.deadlines()
}
