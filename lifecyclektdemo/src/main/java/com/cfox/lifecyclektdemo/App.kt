package com.cfox.lifecyclektdemo

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(AppObserver())
    }
}