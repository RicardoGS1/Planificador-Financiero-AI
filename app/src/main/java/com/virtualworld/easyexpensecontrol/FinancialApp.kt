package com.virtualworld.easyexpensecontrol

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.virtualworld.easyexpensecontrol.ads.AppOpenAdManager
import com.virtualworld.easyexpensecontrol.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FinancialApp : Application() {

    private var appOpenAdManager: AppOpenAdManager? = null

    override fun onCreate() {
        super.onCreate()

        MobileAds.initialize(this) { initializationStatus ->
            Log.d("FinancialApp", "AdMob SDK initialized: $initializationStatus")
            appOpenAdManager = AppOpenAdManager(this)
        }

        startKoin {
            androidContext(this@FinancialApp)
            modules(appModule)
        }
    }
}