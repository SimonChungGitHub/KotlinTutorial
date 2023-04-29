package com.simonchung.kotlin.sqlite

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class Photo (context: Context?) :
    SQLiteOpenHelper(context, db, null, version) {
    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        val sql = "CREATE TABLE IF NOT EXISTS $table (file TEXT PRIMARY KEY, path TEXT);"
        sqLiteDatabase.execSQL(sql)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, i: Int, i1: Int) {
        val sql = "DROP TABLE $table"
        sqLiteDatabase.execSQL(sql)
    }

    companion object {
        private const val version = 1
        private const val db = "database.db"
        const val table = "photo"
    }
}