package de.belegkasten.feature.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.work.*
import de.belegkasten.core.database.BelegkastenDb
import de.belegkasten.core.database.MIGRATION_1_2
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

class DeadlineWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Belegfristen", NotificationManager.IMPORTANCE_DEFAULT))
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled() ||
            manager.getNotificationChannel(CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) return Result.success()
        val db = Room.databaseBuilder(applicationContext, BelegkastenDb::class.java, "belegkasten.db").addMigrations(MIGRATION_1_2).build()
        return try {
            // One notification per deadline; no merchant/product contents on lockscreen.
            val due = db.deadlines().dueUntil(LocalDate.now().plusDays(21).toString())
            for (deadline in due) {
                val intent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
                val pending = intent?.let { PendingIntent.getActivity(applicationContext, deadline.id.hashCode(), it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) }
                val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Belegfrist prüfen")
                    .setContentText("Eine ${if (deadline.type == "RETURN") "Rückgabe-" else "Garantie-"}Frist liegt bis zum ${deadline.dueDate}.")
                    .setContentIntent(pending).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
                NotificationManagerCompat.from(applicationContext).notify(deadline.id, 1, notification)
                // Do not overwrite a date edited while the notification was sent.
                db.deadlines().markNotified(deadline.id, deadline.dueDate)
            }
            Result.success()
        } catch (_: SecurityException) { Result.success() }
        catch (e: Exception) { if (e is CancellationException) throw e; Result.retry() }
        finally { db.close() }
    }
    companion object {
        private const val CHANNEL = "receipt-deadlines"
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("deadline-check", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DeadlineWorker>(1, TimeUnit.DAYS).build())
        }
    }
}
