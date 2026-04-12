package com.virtualworld.easyexpensecontrol

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.virtualworld.easyexpensecontrol.ads.AppOpenAdManager
import com.virtualworld.easyexpensecontrol.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FinancialApp : Application() {

    private lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        appOpenAdManager = AppOpenAdManager(this)
        startKoin {
            androidContext(this@FinancialApp)
            modules(appModule)
        }
    }
}