package com.cfox.workmanagerdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.cfox.workmanagerdemo.worker.InputDataWorker;
import com.cfox.workmanagerdemo.worker.OutDataWorker;
import com.cfox.workmanagerdemo.worker.Periodic1Worker;
import com.cfox.workmanagerdemo.worker.RetryWorker;
import com.cfox.workmanagerdemo.worker.SimpleWorker;
import com.cfox.workmanagerdemo.worker.Tag2Worker;
import com.cfox.workmanagerdemo.worker.TagWorker;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void runSimpleWorker(View view) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED) // 约束运行工作所需的网络类型。例如 Wi-Fi (UNMETERED)。
                .setRequiresStorageNotLow(true) // 如果设置为 true，那么当用户设备上的存储空间不足时，工作不会运行。
                .setRequiresBatteryNotLow(true) // 如果设置为 true，那么当设备处于“电量不足模式”时，工作不会运行。
                .build();
        WorkRequest workRequest = new OneTimeWorkRequest.Builder(SimpleWorker.class)
//                .setConstraints(constraints) // 添加约束
                .setInitialDelay(1, TimeUnit.MINUTES) // 加入队列 1 分钟 后执行
                .build();
        Operation op = WorkManager.getInstance(this).enqueue(workRequest);
        EsLog.d("runSimpleWorker: " + op.getResult().toString());
    }

    public void runPeriodicWorker1(View view) {
        // 注意：可以定义的最短重复间隔是 15 分钟（与 JobScheduler API 相同）。
        PeriodicWorkRequest request = new PeriodicWorkRequest
                .Builder(Periodic1Worker.class, 20, TimeUnit.MINUTES)
                .build();
        Operation op = WorkManager.getInstance(this).enqueue(request);
        EsLog.d("runPerwiodicWorker1: " + op.getResult().toString());
    }

    public void runRetryWorker(View view) {
        WorkRequest request = new OneTimeWorkRequest.Builder(RetryWorker.class)
                /**
                 * 退避延迟时间指定了首次尝试后重试工作前的最短等待时间。此值不能超过 10 秒（或 MIN_BACKOFF_MILLIS）
                 *  LINEAR，每次尝试重试时，重试间隔都会增加约 10 秒
                 *  注意：退避延迟时间不精确，在两次重试之间可能会有几秒钟的差异，但绝不会低于配置中指定的初始退避延迟时间。
                 */
                .setBackoffCriteria(BackoffPolicy.LINEAR, OneTimeWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(this).enqueue(request);
    }

    public void runTagWorker(View view) {
        // 给worker 添加 tag  ,并通过tag 取消worker 队列中未执行的 worker
        WorkRequest request = new OneTimeWorkRequest.Builder(TagWorker.class)
                .addTag("tag-work")
                .build();
        WorkRequest request2 = new OneTimeWorkRequest.Builder(Tag2Worker.class)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .addTag("tag-work")
                .build();
        WorkManager.getInstance(this).enqueue(Arrays.asList(request, request2));
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                //在 ENQUEUED 状态下 随时都可以取消工作，取消后工作将进入 CANCELLED 状态。
                /**
                 * // by id
                 * workManager.cancelWorkById(syncWorker.id);
                 *
                 * // by name
                 * workManager.cancelUniqueWork("sync");
                 *
                 * // by tag
                 * workManager.cancelAllWorkByTag("syncTag");
                 *
                 * 注意：cancelAllWorkByTag(String) 会取消具有给定标记的所有工作。
                 */
                EsLog.e("start cancel by tag");
                WorkManager.getInstance(MainActivity.this).cancelAllWorkByTag("tag-work");
                EsLog.e("end cancel by tag");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void runInputDataWorker(View view) {

        Data input = new Data.Builder().putString("name", "hello world").build();

        WorkRequest request = new OneTimeWorkRequest.Builder(InputDataWorker.class)
                .setInputData(input)
                .build();

        WorkManager.getInstance(this).enqueue(request);

    }

    public void runMultiSameWorker(View view) {
        /**
         * 下面是模拟 同一个worker 被调用两次，会被执行两次
         */
        Data input1 = new Data.Builder().putString("name", "hello world").build();
        OneTimeWorkRequest request1 = new OneTimeWorkRequest.Builder(InputDataWorker.class)
                .setInputData(input1)
                .build();

//        WorkManager.getInstance(this).enqueue(request1);
        WorkManager.getInstance(this).enqueueUniqueWork("one-worker", ExistingWorkPolicy.KEEP, request1);

        Data input2 = new Data.Builder().putString("name", "hello world ------2222").build();
        OneTimeWorkRequest request2 = new OneTimeWorkRequest.Builder(InputDataWorker.class)
                .setInputData(input2)
                .build();

//        WorkManager.getInstance(this).enqueue(request2);
        WorkManager.getInstance(this).enqueueUniqueWork("one-worker", ExistingWorkPolicy.KEEP, request2);
        /**
         * 唯一工作是一个很实用的概念，可确保同一时刻只有一个具有特定名称的工作实例。与 ID 不同的是，唯一名称是人类可读的，由开发者指定，
         * 而不是由 WorkManager 自动生成。与标记不同，唯一名称仅与一个工作实例相关联。
         *
         * 唯一工作既可用于一次性工作，也可用于定期工作。您可以通过调用以下方法之一创建唯一工作序列，具体取决于您是调度重复工作还是一次性工作。
         *
         * WorkManager.enqueueUniqueWork()（用于一次性工作）
         * WorkManager.enqueueUniquePeriodicWork()（用于定期工作）
         * 这两种方法都接受 3 个参数：
         *
         * uniqueWorkName - 用于唯一标识工作请求的 String。
         * existingWorkPolicy - 此 enum 可告知 WorkManager：如果已有使用该名称且尚未完成的唯一工作链，应执行什么操作。如需了解详情，请参阅冲突解决政策。
         * work - 要调度的 WorkRequest。
         * 借助唯一工作，我们可以解决前面提到的重复调度问题。
         *
         *
         * 冲突解决政策
         *
         * 调度唯一工作时，您必须告知 WorkManager 在发生冲突时要执行的操作。您可以通过在将工作加入队列时传递一个枚举来实现此目的。
         * 对于一次性工作，您需要提供一个 ExistingWorkPolicy，它支持用于处理冲突的 4 个选项。
         *
         * REPLACE：用新工作替换现有工作。此选项将取消现有工作。
         * KEEP：保留现有工作，并忽略新工作。
         * APPEND：将新工作附加到现有工作的末尾。此政策将导致您的新工作链接到现有工作，在现有工作完成后运行。
         * 现有工作将成为新工作的先决条件。如果现有工作变为 CANCELLED 或 FAILED 状态，新工作也会变为 CANCELLED 或 FAILED。如果您希望无论现有工作的状态如何都运行新工作，请改用 APPEND_OR_REPLACE。
         *
         * APPEND_OR_REPLACE 函数类似于 APPEND，不过它并不依赖于先决条件工作状态。即使现有工作变为 CANCELLED 或 FAILED 状态，新工作仍会运行。
         */
    }

    public void runOutDataWorker(View view) {
        Data input = new Data.Builder().putString("name", "out Data").build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(OutDataWorker.class)
                .setInputData(input)
                .build();

        WorkManager workManager = WorkManager.getInstance(this);
        workManager.enqueue(request);
        workManager.getWorkInfoByIdLiveData(request.getId()).observe(this, workInfo -> {
            EsLog.d("work out put data : State name:" + workInfo.getState().name());
            if (workInfo.getState() == WorkInfo.State.RUNNING) {
                EsLog.d("progress :::" + workInfo.getProgress().getString("progress"));
            } else if (workInfo.getState() == WorkInfo.State.SUCCEEDED){
                EsLog.d("result Data:" + workInfo.getOutputData().getString("result"));
            }


        });

    }

    @Override
    protected void onResume() {
        super.onResume();

    }
}