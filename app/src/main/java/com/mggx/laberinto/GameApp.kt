package com.mggx.laberinto

import android.app.Application
import com.mggx.laberinto.core.SaveData

class GameApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Carga (o crea) el perfil apenas arranca, para que el lobby ya lo tenga.
        SaveData.get(this)
    }
}
