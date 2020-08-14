
[WorkManager](https://developer.android.com/reference/androidx/work/WorkManager)是[Android Jetpack](https://developer.android.com/jetpack/getting-started)库，可将可延期的执行的后台任务加入任务队列，并且仅在满足其[约束条件](https://developer.android.com/reference/androidx/work/Constraints.Builder)时保证其任务执行。

在这篇博客中，我们需要掌握的是：
- 了解WorkManager及其内部运作方式？
- 什么时候可以使用WorkManager？
- WorkManager的用例
- 为什么要使用WorkManager
- 怎么实现WorkManager

## 了解WorkManager

考虑到Android的省点功能和用户的API级别，WorkManager是可延期的有保证的后台任务的统一解决方案。

即使用户退出应用程序或者设备重新启用，WorkManager也会执行后台的任务

我们已经有了Job Scheduler和Alarm Manager，那么为什么要使用WorkManager？

使用Job Scheduler和Alarm Manager，我们需要找到设备API级别，然后检查哪个API可以处理特定设备级别并相应地使用它。代码变得冗长而复杂。使用WorkManager，我们开发人员不必担心它。WorkManager 向后兼容API级别14，可在有或没有Google Play服务的情况下运行。

在内部，WorkManager使用这些API。



```
graph TB

 co[Device API Level?]
 co --API 23+ -->sub(JobScheduler)
  co --API 14-22 -->sub2(Device has access to Google Play Services and work-gcm?)
  sub2 -- Yes --> sub3(GcmNeworkManager)
  sub2 -- NO --> sub4(Custom AlarmManager and BroadCastReceiver)

```

## 什么时候可以使用？

WorkManager并不依赖于应用程序的寿命，他可以在应用程序处于后台中，前台中，或者是前台进入后台中。

如果可以将某项工作归类为保证执行和可延期执行，则可以使用WorkManager来完成。

那什么是“保证和延期”呢？

- 保证，就是说就算是用户退出应用程序或者设备重启也需要完成的工作

举个栗子，如果你想获取一张位图，提取颜色并以此更新UI，这项工作需要保证吗？没有。再者，你要在一个图片做一些美颜功能，然后压缩再保存，就算你离开了应用程序，这难道就不执行了吗？肯定。这属于保证执行的类别。

- 延期，就是说可以等待一段时间，不需要在指定的时间内完成的操作。

举个栗子，用户请求下载一些东西，当设备有足够的电池电量时，应该延后执行此操作吗？不，这是应用户要求。但是，假设我们要在凌晨2点将应用的日志上传到服务器，可以等吗？答案是的，如果未在指定的时间完成这个操作，用户也不受影响。这项工作属于可取的范畴。

## 用例
- 将应用程序的数据备份到服务器
- 上传log信息
- 压缩和保存图片

在这，我们不希望这项工作立即执行，可以考虑设备的电池用量，网络限制等等，可以稍后执行。无论应用程序是否处于活动状态或者重新启动，都必须执行该操作。执行得到保障。

## 为什么选择WorkManager？

- 处理不同API级别的兼容性问题
- 考虑设备运行状况以执行任务
- 我们可以添加网络约束
- 我们可以安排单次任务，定期任务，任务链（并行和顺序）
- 灵活的重试政策
- 保证工作执行


## 所以呢？

如果您的应用程序需要完成某些任务，我强烈建议您首先看看它是什么样的工作。检查这是尽力而为工作还是有保证的执行工作，请检查是否需要在准确的时间完成或可以延期？将WorkManager用于需要保证执行且只能推迟执行的后台任务。

## WorkManager该如何实现呢？

在此，我们进入了WorkManager的实战阶段，我们分为以下几个部分
- 添加依赖
- WorkManager的类关键类
- OneTimeWorkRequest和PeriodicWorkRequest的使用
- 发送和接受数据
- 链式的任务请求
- 为任务设定一些限制
- 取消任务

#### 添加依赖

在在app / build.gradle文件中，添加

```
dependencies {
    def work_version = "2.3.4"
    implementation "androidx.work:work-runtime-ktx:$work_version"
}
```
目前最新是2.3.4，点击[此处](https://developer.android.com/jetpack/androidx/releases/work)获取最新版本

以上是针对于Kotlin语言，如果你使用的是Java，则添加以下

```
 dependencies {
    def work_version = "2.3.4"
    implementation "androidx.work:work-runtime:$work_version"
}
```

#### 关键类

WorkManager通常包含4个类

1. [Worker](https://developer.android.com/reference/androidx/work/Worker.html) - 定义需要完成的工作
2. [WorkRequest](https://developer.android.com/reference/androidx/work/WorkRequest.html) - 在这里定义将要执行的Worker类
- OneTimeWorkRequest 一次请求
- PeriodicWorkRequest 多次请求
3. [WorkManager](https://developer.android.com/reference/androidx/work/WorkManager.html) 管理Worker请求和队列
4. [WorkInfo](https://developer.android.com/reference/androidx/work/WorkInfo) - 有关Worker的一些信息


#### 开启任务

下面，我们就来简单实现以下WorkManager。我们定义一个*NotificationWorker*类，它继承Worker类，他有一个showNotification()方法，在doWork()中被调用。

在Activity中，我们创建一个按钮和一个文本，单击按钮后，WorkManager将进入排队工作请求，并将任务的一些状态显示在文本中。

- 步骤1：定义Worker

创建一个继承Worker类命名为NotificationWorker，并重写doWork()方法，在里面执行一个showNotification（）方法。


```
class NotificationWorker(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {

    override fun doWork(): Result {
        showNotification("Test Title", "Test Content")
        return Result.success()
    }
}
```

doWork返回值有三个，Result.success()，Result.failure(),Result.retry(),我们可以定义任何一个告诉WorkManager现在任务的执行情况。

接下来，我们用显示通知的方法，完善showNotification方法


```
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
        val notification: Notification = notificationBuilder.build()
        notificationManager.notify(1, notification)
    }
```

- 步骤2：创建WorkRequest

在Activity中，创建一个[OneTimeWorkRequest](https://developer.android.com/reference/androidx/work/OneTimeWorkRequest)


```
val request : OneTimeWorkRequest =
        OneTimeWorkRequest.Builder(NotificationWorker::class.java)
            .build()
```

- 步骤3：使用WorkManager，使工作入队。

```
  workManager = WorkManager.getInstance(applicationContext)
  notify_button.setOnClickListener {
        workManager.enqueue(request)
  }
```

- 步骤4：使用getWorkInfoByIdLiveData()观察任务工作的信息并显示在textview上

```
     workManager.getWorkInfoByIdLiveData(request.id)
            .observe(this, Observer { workInfo ->
                if (workInfo != null) {
                    val status: String = workInfo.state.name
                    status_textview.append(status + "\n")
                }
            })
```

上面我们使用OneTimeWorkRequest是一次性任务，但是假设我们要重复执行一些任务，比如每隔1天，我们发送用户日志到服务器，我们就必须使用PeriodicWorkRequest加入队列，例如

```
val request : PeriodicWorkRequest =
    PeriodicWorkRequest.Builder(NotificationWorker::class.java, 1, TimeUnit.Days)
    .build()
```

#### 发送和接受数据

1. 在Activity发送数据并在Worker类中接收数据，使用setInputData()

```
val data : Data = Data.Builder()
            .putString(KEY_WORK_CONTENT, "I am the content of notification")
            .putString(KEY_WORK_TITLE, "Work Manager Test")
            .build()

val request : OneTimeWorkRequest = OneTimeWorkRequest.Builder(NotificationWorker::class.java)
            .setInputData(data)
            .build()
```

在Worker类中，使用inputData来获取Activity或者Fragment的值


```
val title : String = inputData.getString(MainActivity.KEY_WORK_TITLE).toString()
val content : String = inputData.getString(MainActivity.KEY_WORK_CONTENT).toString()
```
2. 从Worker发送数据，并在Activity使用getWorkInfoByIdLiveData()方法接收数据

首先，Worker


```
val data : Data = Data.Builder()
            .putString(KEY_OUTPUT_TEXT, "Work finished!")
            .build()
return Result.success(data)
```

然后Activity


```
if(workInfo.state.isFinished) {
    status_textview.append("\n" +
    workInfo.outputData
   .getString(NotificationWorker.KEY_OUTPUT_TEXT) + "\n")
}
```

注意，传递的对象是一个键值对，其中值可以是字符串或者数据或者原始类型，但是有大小限制。Constant Value: 10240 (0x00002800) 看[这里](https://developer.android.com/reference/androidx/work/Data#MAX_DATA_BYTES)


#### 任务请求队列

WorkManager使我们可以并行且顺序链接每一个WorkRequests。任务链可以把一个WorkRequest的结果传递给另一个WorkRequest


```
workManager = WorkManager.getInstance(applicationContext)
workManager
    .beginWith(Arrays.asList(workRequest1, workRequest2))
    .then(workRequest3)
    .then(workRequest4)
    .enqueue()
```

在上面伪代码中，WorkManager将以并行运行的workRequest1和workRequest2开始工作。仅当workRequest1和workRequest2都完成时，workRequest3才会启动，然后是workRequest4

为了从workRequest1和workRequest2的并行执行中接收workRequest3中的输入，我们可以使用[InputMerger](https://developer.android.com/reference/androidx/work/InputMerger)。


#### 设定约束条件

我们可以在WorkRequest添加一些约束条件，当满足约束条件的时候，工作任务才会开始执行，比如，我们希望仅在电池电量不低时执行工作，我们可以这么写


```
val constraints : Constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
```

并将其设置为


```
val request : OneTimeWorkRequest = OneTimeWorkRequest.Builder(NotificationWorker::class.java)
            .setInputData(data)
            .setConstraints(constraints)
            .build()
```

也有很多种不同的约束条件

- setRequiredNetworkType 它将特定的网络类型设置成要完成的工作的要求，比如，我们想有网络的情况下执行某些任务，我们就可以使用CONNECTED作为约束条件，网络类型其它选项分别是METERED，NOT_ROAMING，UNMETERED。默认值为NOT_REQUIRED。
- setRequiresBatteryNotLow 如果为true，则设备在电量不低的情况下才可以执行任务
- setRequiresCharging 如果为true，则设备在充电的时候才允许执行任务
- setRequiresDeviceIdle 如果为true，则设备处于空闲状态下才可以执行任务
- setRequiresStorageNotLow 如果为true，则要在存储空间充足时候才可以执行任务

点击[这里](https://developer.android.com/reference/kotlin/androidx/work/Constraints.Builder)，查看更多约束条件



#### 取消工作

我们可以使用id，tag取消任务


```
workManager.cancelWorkById(id)
```

cancelAllWork，cancelAllWorkByTag，cancelUniqueWork和cancelWorkById


## 最终
代码地址: https://github.com/laibinzhi/WorkManagerSample





