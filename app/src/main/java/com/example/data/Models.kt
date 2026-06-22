package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_info")
data class BusinessInfo(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val type: String,
    val systemPrompt: String,
    val welcomeMessage: String,
    val productsJson: String
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user" or "ai" or "system"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "order_tickets")
data class OrderTicket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val customerPhone: String,
    val dateTime: String,
    val orderDetails: String,
    val status: String = "Yangi", // "Yangi", "Bajarildi", "Bekor qilindi"
    val timestamp: Long = System.currentTimeMillis()
)
