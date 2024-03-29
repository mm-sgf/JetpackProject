package com.cfox.workmanagerdemo.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.InitializationExceptionHandler;
import androidx.work.InputMergerFactory;
import androidx.work.RunnableScheduler;
import androidx.work.Worker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;

import com.cfox.workmanagerdemo.EsLog;

import java.util.concurrent.Executor;

/**
 * 简单的worker
 *
 * 如果执行中被中断一段时间后会恢复重新执行doWorker 方法
 */
public class SimpleWorker extends Worker {
    public SimpleWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        EsLog.d("doWork: run work method start ===>");

        for (int i = 0; i < 10; i ++) {
            EsLog.d("doWork: running index : " + i);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        EsLog.d("doWork: run work method end ===>");

        return Result.success();
    }
}

