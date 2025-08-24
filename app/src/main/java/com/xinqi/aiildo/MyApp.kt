package com.xinqi.aiildo

import android.app.Application

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化配置
        ConfigInitializer.init(this)
    }
}