package com.example.flashlearn

import android.app.Application
import com.example.flashlearn.data.remote.RetrofitClient
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FlashLearnApp : Application() {

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
    }
}