package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessInfoDao {
    @Query("SELECT * FROM business_info WHERE id = 1 LIMIT 1")
    fun getBusinessInfoFlow(): Flow<BusinessInfo?>

    @Query("SELECT * FROM business_info WHERE id = 1 LIMIT 1")
    suspend fun getBusinessInfo(): BusinessInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBusiness(businessInfo: BusinessInfo)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()
}

@Dao
interface OrderTicketDao {
    @Query("SELECT * FROM order_tickets ORDER BY timestamp DESC")
    fun getAllOrdersFlow(): Flow<List<OrderTicket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderTicket)

    @Query("UPDATE order_tickets SET status = :status WHERE id = :id")
    suspend fun updateOrderStatus(id: Int, status: String)

    @Query("DELETE FROM order_tickets WHERE id = :id")
    suspend fun deleteOrder(id: Int)
}

@Database(entities = [BusinessInfo::class, ChatMessage::class, OrderTicket::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessInfoDao(): BusinessInfoDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun orderTicketDao(): OrderTicketDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qabul_ai_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
