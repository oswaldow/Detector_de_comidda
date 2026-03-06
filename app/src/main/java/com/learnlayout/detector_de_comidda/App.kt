package com.learnlayout.detector_de_comidda

import android.app.Application

class App : Application() {

    lateinit var foodDetector: FoodDetector

    override fun onCreate() {
        super.onCreate()
        foodDetector = FoodDetector(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        foodDetector.close()
    }
}