package com.lbz.workmanagersample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        workManager = WorkManager.getInstance(applicationContext)

        val data: Data = Data.Builder()
            .putString(KEY_WORK_CONTENT, "I am the content of notification")
            .putString(KEY_WORK_TITLE, "Work Manager Test")
            .build()

        val constraints: Constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request: OneTimeWorkRequest = OneTimeWorkRequest.Builder(NotificationWorker::class.java)
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        notify_button.setOnClickListener {
            workManager.enqueue(request)
        }

        workManager.getWorkInfoByIdLiveData(request.id)
            .observe(this, Observer { workInfo ->
                if (workInfo != null) {
                    val status: String = workInfo.state.name
                    status_textview.append(status + "\n")

                    if (workInfo.state.isFinished) {
                        status_textview.append(
                            "\n" + workInfo.outputData.getString(
                                NotificationWorker.KEY_OUTPUT_TEXT
                            ) + "\n"
                        )
                    }
                }
            })
    }

    companion object {
        const val KEY_WORK_CONTENT: String = "content_work"
        const val KEY_WORK_TITLE: String = "title_work"
    }
}