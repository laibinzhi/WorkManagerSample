package com.lbz.workmanagersample

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * @author: laibinzhi
 * @date: 2020-08-14 10:33
 * @github: https://github.com/laibinzhi
 * @blog: https://www.laibinzhi.top/
 */
class NotificationWorker(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {

    override fun doWork(): Result {
        val title: String = inputData.getString(MainActivity.KEY_WORK_TITLE).toString()
        val content: String = inputData.getString(MainActivity.KEY_WORK_CONTENT).toString()
        showNotification(title, content)

        val data: Data = Data.Builder()
            .putString(KEY_OUTPUT_TEXT, "Work finished!")
            .build()

        return Result.success(data)
    }

    private fun showNotification(title: String, content: String) {
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel: NotificationChannel =
                NotificationChannel("a1", "Work Manager", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(applicationContext, "a1")
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)

        val notification: Notification = notificationBuilder.build()

        notificationManager.notify(1, notification)
    }

    companion object {
        const val KEY_OUTPUT_TEXT: String = "output_text"
    }

}