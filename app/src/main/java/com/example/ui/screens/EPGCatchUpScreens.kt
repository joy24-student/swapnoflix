package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*

@Composable
fun TVGuideScreen(viewModel: StreamingViewModel) {
    var selectedDayOffset by remember { mutableStateOf(0) } // 0 is Today, -1, -2 are Past Catch Up, 1, 2 are Fututre EPG
    var activeDetailsProgram by remember { mutableStateOf<ProgramEvent?>(null) }
    var activeDetailsChannel by remember { mutableStateOf<LiveChannel?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val daysText = listOf(
        -2 to "Tue 9",
        -1 to "Wed 10",
        0 to "Today (Thu)",
        1 to "Fri 12",
        2 to "Sat 13",
        3 to "Sun 14"
    )

    val channels by viewModel.channelsList.collectAsState()
    
    val filteredChannels = channels.filter { channel ->
        val matchesCategory = selectedCategory == "All" || channel.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = if (searchQuery.isBlank()) {
            true
        } else {
            val nameMatches = channel.name.contains(searchQuery, ignoreCase = true)
            val showMatches = getSchedulesForChannel(channel, selectedDayOffset).any { event ->
                event.title.contains(searchQuery, ignoreCase = true)
            }
            nameMatches || showMatches
        }
        matchesCategory && matchesSearch
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0911))
            .testTag("tvguide_screen_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            // Screen Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("7 Days TV Guide Suite", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Plan ahead or stream past archives on-demand.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFF00FFC4).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("GMT+6", color = Color(0xFF00FFC4), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            // Search Bar for filtering Channels and Show Titles (EPG Grid)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("epg_search_bar"),
                placeholder = {
                    Text(
                        text = "Search channel or show title...",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = Color(0xFF00FFC4)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00FFC4),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = Color(0xFF1E172E),
                    unfocusedContainerColor = Color(0xFF130E1F),
                    cursorColor = Color(0xFF00FFC4)
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 7 Days Grid horizontal carousel (Screen 15.2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                daysText.forEach { (offset, dayLabel) ->
                    val isSel = selectedDayOffset == offset
                    Card(
                        modifier = Modifier
                            .width(100.dp)
                            .clickable { selectedDayOffset = offset },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSel) Color(0xFFE50914) else Color(0xFF1E172E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayLabel,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (offset < 0) "CATCH-UP" else if (offset == 0) "LIVE NOW" else "EPG FUTURE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSel) Color.White else if (offset < 0) Color(0xFF00FFC4) else Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Quick Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val categories = listOf("All", "Sports", "News", "Movies", "Kids", "Entertainment")
                categories.forEach { category ->
                    val isSel = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSel) Color(0xFF00FFC4) else Color(0xFF1E172E)
                            )
                            .border(
                                1.dp,
                                if (isSel) Color(0xFF00FFC4) else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            color = if (isSel) Color.Black else Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // EPG Grid Section Title
            Text(
                text = if (selectedDayOffset < 0) "Retroactive Catch-Up Grid" else "Live Channel EPG Grid",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (filteredChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No channels available in selected category.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredChannels.forEach { channel ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(94.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sticky Left: Channel Identity Card
                            Card(
                                modifier = Modifier
                                    .width(115.dp)
                                    .fillMaxHeight()
                                    .padding(start = 16.dp, end = 4.dp)
                                    .border(
                                        1.5.dp,
                                        channel.logoAccent.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.selectLiveChannel(channel)
                                        viewModel.selectTab(MainTab.LIVE_TV)
                                    }
                                    .testTag("epg_channel_card_${channel.id}"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E172E)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(channel.logoAccent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = channel.name.take(1),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                        Text(
                                            text = channel.category,
                                            fontSize = 8.sp,
                                            color = Color(0xFF00FFC4),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = channel.name,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(Color(0xFF00FFC4), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(1.dp))
                                        Text(
                                            text = "${channel.language}",
                                            fontSize = 8.sp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Right: Horizontal Scrollable List of programs
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val schedules = getSchedulesForChannel(channel, selectedDayOffset)
                                schedules.forEachIndexed { index, event ->
                                    val isLiveNow = selectedDayOffset == 0 && index == 2 // simulate third slot as currently active Live show
                                    
                                    Card(
                                        modifier = Modifier
                                            .width(190.dp)
                                            .fillMaxHeight()
                                            .border(
                                                1.dp,
                                                if (isLiveNow) Color(0xFF00FFC4).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                activeDetailsProgram = event
                                                activeDetailsChannel = channel
                                            }
                                            .testTag("epg_program_card_${event.id}"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isLiveNow) Color(0xFF231E35) else Color(0xFF171223)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(10.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = event.timeSlot,
                                                            color = if (isLiveNow) Color(0xFF00FFC4) else Color.White.copy(alpha = 0.6f),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        if (isLiveNow) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(Color(0xFFE50914), RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = "ON AIR",
                                                                    color = Color.White,
                                                                    fontSize = 7.sp,
                                                                    fontWeight = FontWeight.ExtraBold
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = event.title,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                if (isLiveNow) {
                                                    // Linear progress indicator for simulated active play duration
                                                    LinearProgressIndicator(
                                                        progress = 0.65f,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(3.dp)
                                                            .clip(RoundedCornerShape(2.dp)),
                                                        color = Color(0xFF00FFC4),
                                                        trackColor = Color.White.copy(alpha = 0.1f)
                                                    )
                                                } else {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = if (selectedDayOffset < 0) "CATCH-UP AVAILABLE" else "EPG SCHEDULED",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (selectedDayOffset < 0) Color(0xFF00FFC4) else Color.White.copy(alpha = 0.3f)
                                                        )
                                                        Icon(
                                                            imageVector = if (selectedDayOffset < 0) Icons.Filled.History else Icons.Filled.NotificationsNone,
                                                            contentDescription = null,
                                                            tint = Color.White.copy(alpha = 0.3f),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                // Extra Spacer on right end so scroll content doesn't touch screen edge
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Expanded Program details Dialog box (Screen 16)
        activeDetailsProgram?.let { prog ->
            val channel = activeDetailsChannel ?: StreamingDatabase.channels.first()
            var isReminderSet by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = {
                    activeDetailsProgram = null
                    activeDetailsChannel = null
                },
                containerColor = Color(0xFF1E172E),
                title = { Text("Program Details", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = prog.channelName + " BROADCAST",
                            color = Color(0xFF00FFC4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(prog.title, fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp)
                        Text(prog.timeSlot, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                        // Description (Screen 16.1)
                        Text("Synopsis Description", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text(
                            text = "Enjoy pristine broadcast signals of this curated schedule. Relish unmatched depth, multi-language commentary grids, and Dolby surround settings direct from source antennas.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Cast list (Screen 16.2)
                        Text("Show Anchors & Guests", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text("Sarah Jenkins, David Attenborough, Michael Palin", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))

                        Spacer(modifier = Modifier.height(20.dp))

                        // Set automatic Scheduler trigger (Screen 16.4)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("EPG Event Alarm", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                    Text(
                                        text = if (isReminderSet) "Alarm successfully configured!" else "Auto trigger push alert on air",
                                        fontSize = 10.sp,
                                        color = if (isReminderSet) Color(0xFF00FFC4) else Color.White.copy(alpha = 0.4f)
                                    )
                                }
                                Switch(
                                    checked = isReminderSet,
                                    onCheckedChange = { isReminderSet = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE50914))
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.selectLiveChannel(channel)
                            viewModel.selectTab(MainTab.LIVE_TV)
                            activeDetailsProgram = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                    ) {
                        Text("Start PlaybackNow", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

fun getSchedulesForChannel(channel: LiveChannel, dayOffset: Int): List<ProgramEvent> {
    // 1. Get explicitly defined events from database if any
    val dbEvents = StreamingDatabase.epgSchedules.filter { it.channelName == channel.name && it.dayOffset == dayOffset }
    if (dbEvents.isNotEmpty()) return dbEvents
    
    // 2. Generate automatic rich schedule for this channel to ensure a gorgeous full EPG Grid!
    val list = mutableListOf<ProgramEvent>()
    val category = channel.category
    
    // Create some realistic timeslots & show names based on the channel's category
    val shows = when (category) {
        "Sports" -> listOf(
            "Morning Fitness Boost" to "06:00 AM - 07:30 AM",
            "Classic Match Highlights" to "07:30 AM - 09:00 AM",
            "Live Pre-Game Analysis" to "09:00 AM - 10:30 AM",
            "Championship Live Feed" to "10:30 AM - 01:00 PM",
            "Sports Extra: Round Table" to "01:00 PM - 02:30 PM",
            "Extreme Adventure Sports" to "02:30 PM - 04:30 PM",
            "Daily Wrap-Up & Scores" to "04:30 PM - 06:00 PM",
            "Prime Time Live Match" to "06:00 PM - 09:00 PM",
            "Late Night Football Classics" to "09:00 PM - 11:30 PM"
        )
        "News" -> listOf(
            "Breakfast Global News" to "06:00 AM - 08:00 AM",
            "Financial Market Watch" to "08:00 AM - 10:00 AM",
            "World Report Live" to "10:00 AM - 12:00 PM",
            "Midday Press Briefing" to "12:00 PM - 01:30 PM",
            "Investigative Documentary" to "01:30 PM - 03:00 PM",
            "The Evening Pulse" to "03:00 PM - 05:00 PM",
            "Global News Hour" to "05:00 PM - 07:00 PM",
            "Prime Debate & Discussion" to "07:00 PM - 09:00 PM",
            "Nightly Rundown" to "09:00 PM - 11:30 PM"
        )
        "Movies", "Action", "Cinema" -> listOf(
            "Morning Family Comedy" to "06:00 AM - 08:15 AM",
            "Retro Classics Showcase" to "08:15 AM - 10:30 AM",
            "Action Blockbuster of the Day" to "10:30 AM - 01:00 PM",
            "Romantic Comedy Hour" to "01:00 PM - 03:00 PM",
            "Sci-Fi Odyssey" to "03:00 PM - 05:30 PM",
            "Thriller Matinee" to "05:30 PM - 07:45 PM",
            "Prime Cinema: Midnight Premiere" to "07:45 PM - 10:30 PM",
            "Late Night Horror Special" to "10:30 PM - 11:59 PM"
        )
        "Kids" -> listOf(
            "Rise & Shine Cartoons" to "06:00 AM - 08:00 AM",
            "The Learn & Play Zone" to "08:00 AM - 09:30 AM",
            "Magical Kingdom Adventures" to "09:30 AM - 11:30 AM",
            "Little Explorer Journeys" to "11:30 AM - 01:00 PM",
            "Funny Animal Cartoons" to "01:00 PM - 02:30 PM",
            "Anime Afternoon Special" to "02:30 PM - 05:00 PM",
            "Lego Builders Challenge" to "05:00 PM - 06:30 PM",
            "Bedtime Stories Live" to "06:30 PM - 08:00 PM",
            "Stargazing & Lullabies" to "08:00 PM - 11:30 PM"
        )
        else -> listOf( // "Entertainment", Bengali channels like Zee Bangla, Star Jalsha
            "Morning Melody & Music" to "06:00 AM - 08:00 AM",
            "Daily Cooking Secrets" to "08:00 AM - 09:30 AM",
            "Retro Drama Classics" to "09:30 AM - 11:30 AM",
            "Noon Mega Soap Event" to "11:30 AM - 01:30 PM",
            "Family Game Show Live" to "01:30 PM - 03:30 PM",
            "Women's Special Talk" to "03:30 PM - 05:00 PM",
            "Evening Serial Mega-Episode" to "05:00 PM - 07:00 PM",
            "Super Hit Drama Serial" to "07:00 PM - 09:30 PM",
            "Late Night Comedy Circus" to "09:30 PM - 11:30 PM"
        )
    }

    shows.forEachIndexed { index, (title, slot) ->
        val suffix = if (dayOffset == 0) "" else if (dayOffset < 0) " (Archive)" else " (EPG Forecast)"
        list.add(
            ProgramEvent(
                id = "dyn_${channel.id}_${dayOffset}_$index",
                title = "$title$suffix",
                timeSlot = slot,
                channelName = channel.name,
                dayOffset = dayOffset
            )
        )
    }
    return list
}
