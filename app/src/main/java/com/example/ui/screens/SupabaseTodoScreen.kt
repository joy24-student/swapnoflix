package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDestination
import com.example.data.StreamingViewModel
import com.example.data.TodoItem
import com.example.supabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseTodoScreen(viewModel: StreamingViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var items by remember { mutableStateOf<List<TodoItem>>(listOf()) }
    var isLoading by remember { mutableStateOf(false) }
    var todoText by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }

    fun loadTodos() {
        coroutineScope.launch {
            isLoading = true
            try {
                val fetched = withContext(Dispatchers.IO) {
                    supabase.postgrest.from("todos")
                        .select()
                        .decodeList<TodoItem>()
                }
                items = fetched
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to load: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    fun addTodo() {
        if (todoText.isBlank()) return
        coroutineScope.launch {
            isAdding = true
            try {
                val newItem = TodoItem(name = todoText.trim())
                withContext(Dispatchers.IO) {
                    supabase.postgrest.from("todos")
                        .insert(newItem)
                }
                todoText = ""
                Toast.makeText(context, "Added successfully!", Toast.LENGTH_SHORT).show()
                loadTodos()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to add: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isAdding = false
            }
        }
    }

    fun deleteTodo(todoId: Int) {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabase.postgrest.from("todos").delete {
                        filter {
                            eq("id", todoId)
                        }
                    }
                }
                Toast.makeText(context, "Deleted successfully!", Toast.LENGTH_SHORT).show()
                loadTodos()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to delete: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTodos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cloud Sync Tasks",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppDestination.WELCOME) },
                        modifier = Modifier.testTag("supabase_back_button")
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { loadTodos() },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF13111A)
                )
            )
        },
        containerColor = Color(0xFF0F0E15)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // New Todo input row
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1C1A24)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = todoText,
                        onValueChange = { todoText = it },
                        placeholder = { Text("What needs to be done?", color = Color.White.copy(alpha = 0.5f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFFE50914),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("todo_input_field"),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { addTodo() },
                        enabled = !isAdding && todoText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE50914),
                            disabledContainerColor = Color(0xFFE50914).copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("todo_add_button")
                    ) {
                        if (isAdding) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Add", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Todos title / list
            Text(
                "Your Tasks",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (isLoading && items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFE50914))
                }
            } else if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Task,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No tasks found",
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Add a new task above to save it to Cloud!",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id ?: 0 }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF16141D), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFFE50914), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            IconButton(
                                onClick = { item.id?.let { deleteTodo(it) } },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
