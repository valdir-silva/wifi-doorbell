package com.alunando.wifidoorbell.core.db

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

object DatabaseFactory {
    fun create(context: Context): WifiDoorbellDb {
        val driver = AndroidSqliteDriver(
            schema = WifiDoorbellDb.Schema,
            context = context,
            name = "wifidoorbell.db"
        )
        return WifiDoorbellDb(driver)
    }
}
