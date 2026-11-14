package com.example.lab9map

import android.app.Application
import android.util.Log
import com.yandex.mapkit.MapKitFactory

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        // setApiKey можно вызвать только один раз за жизнь процесса,
        // поэтому здесь, а не в Activity (Activity пересоздаётся при повороте)
        if (BuildConfig.MAPKIT_API_KEY.isNotBlank()) {
            MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
        } else {
            Log.e("App", "MAPKIT_API_KEY не задан в local.properties")
        }
    }
}
