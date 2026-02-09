package com.example.anonx

import android.app.Application

class AnonXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Establish the connection as soon as the app starts
        SocketManager.establishConnection()
    }
}
