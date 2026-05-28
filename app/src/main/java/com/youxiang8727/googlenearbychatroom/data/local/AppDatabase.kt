package com.youxiang8727.googlenearbychatroom.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.youxiang8727.googlenearbychatroom.data.local.entity.MessageEntity

@Database(entities = [MessageEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
