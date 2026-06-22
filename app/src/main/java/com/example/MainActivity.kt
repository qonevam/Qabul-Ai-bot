package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BusinessInfo
import com.example.data.ChatMessage
import com.example.data.OrderTicket
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BusinessViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel: BusinessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Supports full edge-to-edge drawing, safe areas, and Material 3
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: BusinessViewModel,
    modifier: Modifier = Modifier
) {
    val businessInfo by viewModel.businessInfo.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val orderTickets by viewModel.orderTickets.collectAsStateWithLifecycle()
    val isTyping by viewModel.isTyping.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Customer Chat Simulator, 1 = Business Dashboard
    var showNewOrderAlert by remember { mutableStateOf<OrderTicket?>(null) }

    // Sound/feedback or notification modal whenever a new ticket is auto-parsed
    LaunchedEffect(Unit) {
        viewModel.newOrderEvent.collect { order ->
            showNewOrderAlert = order
        }
    }

    // Success Order Alert Dialog Modal
    if (showNewOrderAlert != null) {
        NewOrderDialog(
            order = showNewOrderAlert!!,
            onDismiss = { showNewOrderAlert = null },
            onViewOrders = {
                showNewOrderAlert = null
                activeTab = 1 // Switch to business dashboard
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Bar
            HeaderBar(
                businessInfo = businessInfo,
                activeTab = activeTab,
                onTabChanged = { activeTab = it }
            )

            // Animated Tab switcher content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (activeTab == 0) {
                    ChatSimulatorView(
                        businessInfo = businessInfo,
                        chatMessages = chatMessages,
                        isTyping = isTyping,
                        onSendMessage = { viewModel.sendMessage(it) },
                        onResetChat = { viewModel.resetChat() }
                    )
                } else {
                    BusinessDashboardView(
                        viewModel = viewModel,
                        businessInfo = businessInfo,
                        orderTickets = orderTickets
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderBar(
    businessInfo: BusinessInfo?,
    activeTab: Int,
    onTabChanged: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            color = Color(0xFF0F9B6E), // Emerald Active
                            shape = CircleShape
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = businessInfo?.name ?: "Qabul AI",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = businessInfo?.type ?: "Virtual Administrator",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Small Status Badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "BOT ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab bar switcher (Role selection)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp)
            ) {
                // Tab 1: Mijoz rejimi
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { onTabChanged(0) }
                        .padding(vertical = 10.dp)
                        .testTag("role_tab_customer"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (activeTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mijoz Rejimi",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (activeTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Tab 2: Biznes Rejimi
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { onTabChanged(1) }
                        .padding(vertical = 10.dp)
                        .testTag("role_tab_business"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (activeTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Biznes Dashboard",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (activeTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatSimulatorView(
    businessInfo: BusinessInfo?,
    chatMessages: List<ChatMessage>,
    isTyping: Boolean,
    onSendMessage: (String) -> Unit,
    onResetChat: () -> Unit
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputTexValue by remember { mutableStateOf("") }

    // Scroll to bottom when list changes size
    LaunchedEffect(chatMessages.size, isTyping) {
        if (chatMessages.isNotEmpty()) {
            lazyListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Quick suggestion simulation suggestions based on the active business type
    val suggestionChips = remember(businessInfo) {
        when (businessInfo?.type) {
            "Kafé/Restoran" -> listOf(
                "Ertaga soat 18:00 ga 4 kishilik joy band qilmoqchiman",
                "Assalomu alaykum, menyuda To'y palovi bormi va narxi necha pul?",
                "Menga 2ta somsa va bitta kola buyurtma bera olamanmi? Telefonim +998901234567"
            )
            "Go'zallik saloni" -> listOf(
                "Shanba kuni soat 15:00 ga makiyaj xizmatiga yozmoqchiman",
                "Manikyur gel-lak bilan qancha bo'ladi? Ismim Madina.",
                "Ertaga bo'sh soatlar bormi? Telefonim: +998935552211"
            )
            "Avtoservis" -> listOf(
                "Jentra mashinasini to'liq yuvish va moy almashtirishga yozmoqchiman",
                "Kompyuter diagnostikasi narxi qancha? Otabekman, tel +998991112233",
                "Bugun usta qabuliga navbat bormi?"
            )
            "Kiyim do'koni" -> listOf(
                "Oversize futbolka oq rangidan M o'lchamini buyurtma bermoqchiman",
                "Klassik jinsi narxini bilsam bo'ladimi?",
                "Toshkent shahar, Chilonzorga kuryer bilan yetkazish bormi? Tel +998903332211"
            )
            else -> listOf(
                "Ertaga soat 10:00 ga ro'yxatdan o'tmoqchiman",
                "Narxlar va xizmatlar ro'yxatni ko'rsating",
                "Siz bilan qanday bog'lansa bo'ladi?"
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        
        // Chat Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (chatMessages.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_receptionist_avatar),
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .shadow(2.dp, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Kommunikatsiya ochiq",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Biznesingizning AI yordamchisini sinab ko'rish uchun quyida xabar yo'llang yoki taklif etilgan savol ustiga bosing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatMessages) { message ->
                        ChatBubble(message = message)
                    }

                    if (isTyping) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }

            // Floating Custom Reset Button
            FloatingActionButton(
                onClick = onResetChat,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .testTag("reset_chat_button"),
                elevation = FloatingActionButtonDefaults.elevation(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Chatni Yangilash",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Suggestions Container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 100.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text(
                        text = "Mijoz nomidan tezkor savol berish:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                items(suggestionChips) { text ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                inputTexValue = ""
                                onSendMessage(text)
                            },
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputTexValue,
                    onValueChange = { inputTexValue = it },
                    placeholder = {
                        Text(
                            text = "Xabaringizni yozing...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = {
                        if (inputTexValue.isNotBlank()) {
                            onSendMessage(inputTexValue)
                            inputTexValue = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_message_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Yuborish",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    }
    val textColor = if (isUser) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val timeString = remember(message.timestamp) {
        val date = Date(message.timestamp)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        format.format(date)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            if (!isUser) {
                Image(
                    painter = painterResource(id = R.drawable.img_receptionist_avatar),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 8.dp, top = 4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .shadow(1.dp, CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Surface(
                color = bubbleColor,
                shape = bubbleShape,
                tonalElevation = if (isUser) 0.dp else 2.dp,
                modifier = Modifier.shadow(1.dp, bubbleShape)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = message.message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor,
                            lineHeight = 20.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = if (isUser) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.End
                        ),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_receptionist_avatar),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(32.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Yozmoqda...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

@Composable
fun BusinessDashboardView(
    viewModel: BusinessViewModel,
    businessInfo: BusinessInfo?,
    orderTickets: List<OrderTicket>
) {
    var subTabSelection by remember { mutableStateOf(0) } // 0 = Orders list, 1 = Bot Configurations

    Column(modifier = Modifier.fillMaxSize()) {
        
        // Secondary Sub-tab Navigation row
        TabRow(
            selectedTabIndex = subTabSelection,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.shadow(1.dp)
        ) {
            Tab(
                selected = subTabSelection == 0,
                onClick = { subTabSelection = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buyurtma va Navbatlar (${orderTickets.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
            Tab(
                selected = subTabSelection == 1,
                onClick = { subTabSelection = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bot Sozlamalari", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (subTabSelection == 0) {
                OrdersListView(
                    orderTickets = orderTickets,
                    onStatusUpdate = { id, status -> viewModel.updateOrderStatus(id, status) },
                    onDelete = { viewModel.deleteOrder(it) }
                )
            } else {
                BotSettingsView(
                    viewModel = viewModel,
                    businessInfo = businessInfo
                )
            }
        }
    }
}

@Composable
fun OrdersListView(
    orderTickets: List<OrderTicket>,
    onStatusUpdate: (Int, String) -> Unit,
    onDelete: (Int) -> Unit
) {
    if (orderTickets.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Buyurtmalar hozircha yo'q",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Mijoz rejimi simulyatoriga o'tib, buyurtma berishni boshlang. AI yordamchi tafsilotlarni tahlil qilib, shu yerda avtomatik buyurtma yaratadi!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Short statistics dashboard
            val total = orderTickets.size
            val pending = orderTickets.count { it.status == "Yangi" }
            val completed = orderTickets.count { it.status == "Bajarildi" }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(title = "Barcha Qabullar", count = total, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                StatCard(title = "Yangi (Kutilmoqda)", count = pending, color = Color(0xFFE65100), modifier = Modifier.weight(1f))
                StatCard(title = "Bajarildi", count = completed, color = Color(0xFF1B5E20), modifier = Modifier.weight(1f))
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(orderTickets, key = { it.id }) { order ->
                    OrderTicketCard(
                        order = order,
                        onStatusUpdate = onStatusUpdate,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
fun OrderTicketCard(
    order: OrderTicket,
    onStatusUpdate: (Int, String) -> Unit,
    onDelete: (Int) -> Unit
) {
    val context = LocalContext.current
    val statusColor = when (order.status) {
        "Yangi" -> Color(0xFFFF9800)
        "Bajarildi" -> Color(0xFF4CAF50)
        else -> Color(0xFFE53935)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Qabul №${1000 + order.id}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = order.status,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details list
            DetailRow(icon = Icons.Default.Person, label = "Mijoz", value = order.customerName)
            DetailRow(icon = Icons.Default.Call, label = "Aloqa", value = order.customerPhone)
            DetailRow(icon = Icons.Default.Clear, label = "Tashrif/Sana", value = order.dateTime, iconTint = MaterialTheme.colorScheme.primary)
            DetailRow(icon = Icons.Default.Info, label = "Buyurtma tafsilotlari", value = order.orderDetails, isMultiline = true)

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onDelete(order.id) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "O'chirish", modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (order.status == "Yangi") {
                    TextButton(
                        onClick = { onStatusUpdate(order.id, "Bekor qilingan") },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Bekor qilish", style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onStatusUpdate(order.id, "Bajarildi") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B5E20)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Bajarildi", style = MaterialTheme.typography.labelMedium.copy(color = Color.White))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    isMultiline: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = if (isMultiline) Alignment.Top else Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(1f),
            maxLines = if (isMultiline) 4 else 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.shadow(1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = color.copy(alpha = 0.8f)
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = color
                )
            )
        }
    }
}

@Composable
fun BotSettingsView(
    viewModel: BusinessViewModel,
    businessInfo: BusinessInfo?
) {
    val templates = remember { viewModel.getTemplates() }

    var businessName by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("") }
    var welcomeMsg by remember { mutableStateOf("") }
    var sysPrompt by remember { mutableStateOf("") }
    var rawProductsList by remember { mutableStateOf<List<ProductItem>>(emptyList()) }

    // Synchronize inputs when DB updates
    LaunchedEffect(businessInfo) {
        if (businessInfo != null) {
            businessName = businessInfo.name
            businessType = businessInfo.type
            welcomeMsg = businessInfo.welcomeMessage
            sysPrompt = businessInfo.systemPrompt
            rawProductsList = parseProducts(businessInfo.productsJson)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // 1. Image Banner Header
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .shadow(1.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_business_hero),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Dark Overlay Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "AI Receptionist Sozlamalari",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Tegishli shablon tanlang va botingiz parametrlarini ishga tushiring.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }
        }

        // 2. Select Template Row
        item {
            Column {
                Text(
                    text = "Tezkor Shablonni Qo'llash:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEach { temp ->
                        val isSelected = businessName == temp.name
                        val outlineColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        val backColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.applyBusinessTemplate(temp)
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.5.dp,
                                color = outlineColor
                            ),
                            color = backColor
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = temp.type.split("/")[0],
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Inputs Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Bot Umumiy Tizimi:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Biznes nomi") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = businessType,
                    onValueChange = { businessType = it },
                    label = { Text("Kategoriya / Biznes turi") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = welcomeMsg,
                    onValueChange = { welcomeMsg = it },
                    label = { Text("Xush kelibsiz (Birinchi salomlashish matni)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = sysPrompt,
                    onValueChange = { sysPrompt = it },
                    label = { Text("AI Tizim Yo'riqnomasi (System Instructions)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 4,
                    maxLines = 12
                )
            }
        }

        // 4. Products / Services Manager
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Mahsulotlar va Xizmatlar Katalogi:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                
                rawProductsList.forEachIndexed { index, product ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = product.price,
                                    onValueChange = { newPrice ->
                                        rawProductsList = rawProductsList.toMutableList().apply {
                                            this[index] = product.copy(price = newPrice)
                                        }
                                    },
                                    label = { Text("Narxi") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                OutlinedTextField(
                                    value = product.desc,
                                    onValueChange = { newDesc ->
                                        rawProductsList = rawProductsList.toMutableList().apply {
                                            this[index] = product.copy(desc = newDesc)
                                        }
                                    },
                                    label = { Text("Tavsif") },
                                    modifier = Modifier.weight(2f),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Build Save Button
        item {
            Button(
                onClick = {
                    val productsJson = serializeProducts(rawProductsList)
                    viewModel.updateBusinessDetails(
                        name = businessName,
                        type = businessType,
                        welcome = welcomeMsg,
                        prompt = sysPrompt,
                        productsJson = productsJson
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_settings_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sozlamalarni Saqlash va Sinab Ko'rish", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// Dialog to notify when the LLM receptionist registers a new booking ticket cleanly
@Composable
fun NewOrderDialog(
    order: OrderTicket,
    onDismiss: () -> Unit,
    onViewOrders: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glow Pulse Ring Icon
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Yangi Murojaat Qabul qilindi!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "AI Receptionist suhbat chog'ida mijoz buyurtmasini aniqladi va ma'lumotlarni muvaffaqiyatli saqladi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Customer details preview
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NewOrderTextRow(label = "Mijoz ismi", value = order.customerName)
                    NewOrderTextRow(label = "Telefon raqam", value = order.customerPhone)
                    NewOrderTextRow(label = "Sana va Vaqt", value = order.dateTime)
                    NewOrderTextRow(label = "Buyurtma", value = order.orderDetails)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onViewOrders,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Buyurtmani ko'rish", color = Color.White)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Yopish", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun NewOrderTextRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

// Helpers for Product items JSON Serialization
fun parseProducts(jsonStr: String?): List<ProductItem> {
    if (jsonStr.isNullOrEmpty()) return emptyList()
    val list = mutableListOf<ProductItem>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ProductItem(
                    name = obj.optString("name", "Noma'lum mahsulot"),
                    price = obj.optString("price", "Kelisuv asosida"),
                    desc = obj.optString("desc", "")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

fun serializeProducts(list: List<ProductItem>): String {
    val array = JSONArray()
    try {
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("name", item.name)
            obj.put("price", item.price)
            obj.put("desc", item.desc)
            array.put(obj)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return array.toString()
}

data class ProductItem(
    val name: String,
    val price: String,
    val desc: String
)
