package com.kouroshgram.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TelegramBlue = Color(0xFF229ED9)
private val TelegramBlueDark = Color(0xFF168AC0)
private val ChatBackground = Color(0xFFEFF7FB)

private data class ChatPreview(
    val id: String,
    val name: String,
    val status: String,
    val preview: String,
    val time: String,
    val unread: Int = 0,
    val bot: Boolean = false,
    val avatarColor: Color
)

private data class ChatMessage(
    val text: String,
    val mine: Boolean,
    val time: String
)

private class MessageStore(context: Context) {
    private val preferences = context.getSharedPreferences("kouroshgram_messages", Context.MODE_PRIVATE)

    fun load(chatId: String): List<ChatMessage> {
        val raw = preferences.getString(chatId, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        ChatMessage(
                            text = item.optString("text"),
                            mine = item.optBoolean("mine"),
                            time = item.optString("time")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(chatId: String, messages: List<ChatMessage>) {
        val array = JSONArray()
        messages.forEach { message ->
            array.put(
                JSONObject()
                    .put("text", message.text)
                    .put("mine", message.mine)
                    .put("time", message.time)
            )
        }
        preferences.edit().putString(chatId, array.toString()).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KouroshGramApp(applicationContext)
        }
    }
}

@Composable
private fun KouroshGramApp(context: Context) {
    var darkMode by rememberSaveable { mutableStateOf(false) }
    val light = lightColorScheme(
        primary = TelegramBlue,
        secondary = TelegramBlueDark,
        background = Color(0xFFF7F9FB),
        surface = Color.White
    )
    val dark = darkColorScheme(
        primary = Color(0xFF52B5E0),
        secondary = Color(0xFF77C7E8),
        background = Color(0xFF101418),
        surface = Color(0xFF182027)
    )

    MaterialTheme(colorScheme = if (darkMode) dark else light) {
        Surface(modifier = Modifier.fillMaxSize()) {
            MessengerRoot(
                context = context,
                darkMode = darkMode,
                onToggleTheme = { darkMode = !darkMode }
            )
        }
    }
}

@Composable
private fun MessengerRoot(
    context: Context,
    darkMode: Boolean,
    onToggleTheme: () -> Unit
) {
    val chats = remember {
        listOf(
            ChatPreview("saved", "Saved Messages", "personal cloud", "Notes and links", "09:18", avatarColor = Color(0xFF5AA9E6)),
            ChatPreview("kourosh", "Kourosh", "online", "See you soon 👋", "09:12", unread = 2, avatarColor = Color(0xFF7C4DFF)),
            ChatPreview("bot", "KouroshGram Bot", "bot", "Send me a message", "08:54", bot = true, avatarColor = Color(0xFF26A69A)),
            ChatPreview("school", "Rahil Group", "12 members", "Sara: Tomorrow at 8", "Yesterday", unread = 5, avatarColor = Color(0xFFFF8A65)),
            ChatPreview("ali", "Ali", "last seen recently", "Thanks!", "Sun", avatarColor = Color(0xFF66BB6A))
        )
    }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = chats.firstOrNull { it.id == selectedId }

    BackHandler(enabled = selected != null) { selectedId = null }

    if (selected == null) {
        ChatsScreen(
            chats = chats,
            darkMode = darkMode,
            onToggleTheme = onToggleTheme,
            onOpenChat = { selectedId = it }
        )
    } else {
        ChatScreen(
            context = context,
            chat = selected,
            darkMode = darkMode,
            onBack = { selectedId = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsScreen(
    chats: List<ChatPreview>,
    darkMode: Boolean,
    onToggleTheme: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, chats) {
        if (query.isBlank()) chats else chats.filter {
            it.name.contains(query, ignoreCase = true) || it.preview.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("KouroshGram", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        IconButton(onClick = { searching = !searching }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Theme"
                            )
                        }
                    }
                )
                if (searching) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        singleLine = true,
                        placeholder = { Text("Search chats") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenChat("kourosh") }) {
                Icon(Icons.Default.Edit, contentDescription = "New message")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            items(filtered, key = { it.id }) { chat ->
                ChatRow(chat = chat, onClick = { onOpenChat(chat.id) })
                HorizontalDivider(modifier = Modifier.padding(start = 84.dp), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun ChatRow(chat: ChatPreview, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(chat.name, chat.avatarColor, 56)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    chat.name,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(chat.time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    chat.preview,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp
                )
                if (chat.unread > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(TelegramBlue)
                            .size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(chat.unread.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(
    context: Context,
    chat: ChatPreview,
    darkMode: Boolean,
    onBack: () -> Unit
) {
    val store = remember { MessageStore(context) }
    val messages = remember(chat.id) {
        mutableStateListOf<ChatMessage>().apply {
            val saved = store.load(chat.id)
            if (saved.isEmpty()) {
                addAll(seedMessages(chat))
            } else {
                addAll(saved)
            }
        }
    }
    var input by rememberSaveable(chat.id) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    fun sendMessage() {
        val text = input.trim()
        if (text.isEmpty()) return
        messages.add(ChatMessage(text, mine = true, time = nowTime()))
        store.save(chat.id, messages)
        input = ""
        if (chat.bot) {
            scope.launch {
                delay(500)
                messages.add(ChatMessage("Got it: $text", mine = false, time = nowTime()))
                store.save(chat.id, messages)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(chat.name, chat.avatarColor, 38)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(chat.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(chat.status, fontSize = 12.sp, color = if (chat.status == "online") TelegramBlue else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            )
        },
        bottomBar = {
            MessageComposer(
                text = input,
                onTextChange = { input = it },
                onSend = ::sendMessage
            )
        }
    ) { padding ->
        val bg = if (darkMode) MaterialTheme.colorScheme.background else ChatBackground
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }
    }
}

@Composable
private fun MessageComposer(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(shadowElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onSend, enabled = text.isNotBlank()) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = TelegramBlue)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.mine) Color(0xFFD9FDD3) else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.mine) 16.dp else 4.dp,
                bottomEnd = if (message.mine) 4.dp else 16.dp
            ),
            tonalElevation = 1.dp,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(message.text, color = Color(0xFF17212B), modifier = Modifier.padding(end = 8.dp))
                Text(message.time, color = Color(0xFF6C7A86), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun Avatar(name: String, color: Color, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "K",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size / 2.5).sp
        )
    }
}

private fun seedMessages(chat: ChatPreview): List<ChatMessage> = when (chat.id) {
    "saved" -> listOf(
        ChatMessage("Welcome to Saved Messages.", false, "09:00"),
        ChatMessage("You can keep notes here locally on this phone.", true, "09:01")
    )
    "bot" -> listOf(ChatMessage("Hi! I'm the KouroshGram demo bot. Send me something.", false, "08:54"))
    "school" -> listOf(
        ChatMessage("Hi everyone 👋", false, "08:20"),
        ChatMessage("Tomorrow's meeting starts at 8.", false, "08:23")
    )
    else -> listOf(
        ChatMessage("Hey! This is the first KouroshGram build.", false, "09:08"),
        ChatMessage("Nice 😄", true, "09:09"),
        ChatMessage("Messages you send here are stored on your phone.", false, "09:10")
    )
}

private fun nowTime(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
