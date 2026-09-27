package com.alunando.wifidoorbell.core.db

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

object DatabaseFactory {
    private var instance: WifiDoorbellDb? = null
    
    fun create(context: Context): WifiDoorbellDb {
        if (instance == null) {
            val driver = AndroidSqliteDriver(
                schema = WifiDoorbellDb.Schema,
                context = context.applicationContext, // always use app context
                name = "wifidoorbell.db"
            )
            instance = WifiDoorbellDb(driver)
        }
        return instance!!
    }
}
