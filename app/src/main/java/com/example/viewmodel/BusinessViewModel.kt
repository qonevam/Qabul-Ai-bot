package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BusinessInfo
import com.example.data.BusinessRepository
import com.example.data.ChatMessage
import com.example.data.OrderTicket
import com.example.network.GeminiClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

class BusinessViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BusinessRepository
    
    val businessInfo: StateFlow<BusinessInfo?>
    val chatMessages: StateFlow<List<ChatMessage>>
    val orderTickets: StateFlow<List<OrderTicket>>

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    // Shared Flow to notify the UI when a new order is automatically registered
    private val _newOrderEvent = MutableSharedFlow<OrderTicket>()
    val newOrderEvent: SharedFlow<OrderTicket> = _newOrderEvent.asSharedFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BusinessRepository(database)
        
        businessInfo = repository.businessInfo.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        chatMessages = repository.chatMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        orderTickets = repository.orderTickets.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.initializeWithDefaultIfNeeded()
        }
    }

    fun sendMessage(msgText: String) {
        if (msgText.isBlank()) return

        viewModelScope.launch {
            // 1. Add user message to DB
            repository.addMessage("user", msgText)

            // Get updated message list for history
            val history = chatMessages.value
            val currentBusiness = repository.getBusinessInfoDirect()
            val systemPrompt = currentBusiness?.systemPrompt ?: "Siz yordamchisiz."

            _isTyping.value = true

            // Send to Gemini API
            val rawResponse = GeminiClient.generateResponse(systemPrompt, history)

            // Parse any automated order ticket injection
            val cleanResponse = parseAndExtractOrder(rawResponse)

            // Save assistant response to DB
            repository.addMessage("ai", cleanResponse)

            _isTyping.value = false
        }
    }

    private suspend fun parseAndExtractOrder(rawResponse: String): String {
        val regex = Regex("\\[ORDER_TICKET:(.*?)\\]", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(rawResponse)
        
        if (match != null) {
            val jsonString = match.groupValues[1].trim()
            try {
                val json = JSONObject(jsonString)
                val name = json.optString("name", "Noma'lum Mijoz").trim()
                val phone = json.optString("phone", "Noma'lum").trim()
                val datetime = json.optString("datetime", "Noma'lum").trim()
                val details = json.optString("details", "Mahsulot/Xizmat").trim()

                val order = OrderTicket(
                    customerName = if (name.isEmpty() || name == "null") "Noma'lum Mijoz" else name,
                    customerPhone = if (phone.isEmpty() || phone == "null") "Noma'lum" else phone,
                    dateTime = if (datetime.isEmpty() || datetime == "null") "Noaniq sana" else datetime,
                    orderDetails = if (details.isEmpty() || details == "null") "Tafsilotlar yo'q" else details,
                    status = "Yangi"
                )

                // Insert into Database
                repository.addOrder(order)
                
                // Trigger animation/notification event in UI
                _newOrderEvent.emit(order)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Return the greeting/response without the underlying functional JSON injection block
        return rawResponse.replace(regex, "").trim()
    }

    fun resetChat() {
        viewModelScope.launch {
            val currentBusiness = repository.getBusinessInfoDirect()
            repository.clearChat()
            if (currentBusiness != null) {
                repository.addMessage("ai", currentBusiness.welcomeMessage)
            } else {
                repository.addMessage("ai", "Assalomu alaykum! Sizga qanday yordam bera olaman?")
            }
        }
    }

    fun applyBusinessTemplate(template: BusinessRepository.BusinessTemplate) {
        viewModelScope.launch {
            repository.applyTemplate(template)
        }
    }

    fun updateBusinessDetails(name: String, type: String, welcome: String, prompt: String, productsJson: String) {
        viewModelScope.launch {
            val current = repository.getBusinessInfoDirect() ?: BusinessInfo(name = name, type = type, systemPrompt = prompt, welcomeMessage = welcome, productsJson = productsJson)
            val updated = current.copy(
                name = name,
                type = type,
                welcomeMessage = welcome,
                systemPrompt = prompt,
                productsJson = productsJson
            )
            repository.updateBusiness(updated)
        }
    }

    fun updateOrderStatus(id: Int, status: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(id, status)
        }
    }

    fun deleteOrder(id: Int) {
        viewModelScope.launch {
            repository.deleteOrder(id)
        }
    }

    fun getTemplates(): List<BusinessRepository.BusinessTemplate> = repository.getTemplates()
}
