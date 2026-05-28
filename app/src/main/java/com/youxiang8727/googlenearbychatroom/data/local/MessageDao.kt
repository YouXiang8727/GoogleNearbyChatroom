package com.youxiang8727.googlenearbychatroom.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.youxiang8727.googlenearbychatroom.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatroomId = :chatroomId ORDER BY timestamp ASC")
    fun getMessagesByChatroom(chatroomId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("DELETE FROM messages WHERE chatroomId = :chatroomId")
    suspend fun deleteMessagesByChatroom(chatroomId: String)

    @Query("SELECT * FROM messages WHERE type = 'VIDEO' AND timestamp < :expiryTime AND mediaUri IS NOT NULL")
    suspend fun getExpiredVideos(expiryTime: Long): List<MessageEntity>
}
