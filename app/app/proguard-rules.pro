# Keep model + crypto names; shrink everything else.
-keep class de.belegkasten.core.embed.** { *; }
-keep class de.belegkasten.core.backup.** { *; }
-keep class androidx.room.** { *; }
# BKBF2 JSON is a portable contract, not an obfuscated implementation detail.
-keep class de.belegkasten.core.data.ArchiveSnapshot { *; }
-keep class de.belegkasten.core.database.ReceiptEntity { *; }
-keep class de.belegkasten.core.database.ItemEntity { *; }
-keep class de.belegkasten.core.database.EmbeddingEntity { *; }
-keep class de.belegkasten.core.database.DeadlineEntity { *; }
