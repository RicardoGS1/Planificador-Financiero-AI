package com.virtualworld.easyexpensecontrol

import android.app.Application
import com.virtualworld.easyexpensecontrol.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FinancialApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@FinancialApp)
            modules(appModule)
        }
    }
}