package com.pibal.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale
import com.pibal.tracker.logic.WindResult
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PibalTrackerScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val orientation by viewModel.orientation.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val windResults by viewModel.windResults.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    var isNightMode by remember { mutableStateOf(false) }
    
    val backgroundColor = if (isNightMode) Color.Black else MaterialTheme.colorScheme.background
    val textColor = if (isNightMode) Color.Red else MaterialTheme.colorScheme.onBackground
    val secondaryColor = if (isNightMode) Color(0xFF880000) else MaterialTheme.colorScheme.secondary

    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Status and Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTracking) "TRACKING" else "IDLE",
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Night Mode", color = textColor, fontSize = 12.sp)
                    Switch(
                        checked = isNightMode,
                        onCheckedChange = { isNightMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Red,
                            checkedTrackColor = Color(0x88440000)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Large Angle Display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", orientation.azimuth)}°",
                    fontSize = 80.sp,
                    color = textColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "AZIMUTH",
                    fontSize = 16.sp,
                    color = secondaryColor
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "${String.format(Locale.US, "%.1f", orientation.elevation)}°",
                    fontSize = 60.sp,
                    color = textColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ELEVATION",
                    fontSize = 16.sp,
                    color = secondaryColor
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Timer and Progress
            val progress = (timerSeconds % 30) / 30f
            val remaining = 30 - (timerSeconds % 30)
            
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(120.dp),
                    color = if (remaining <= 5) Color.Yellow else textColor,
                    strokeWidth = 8.dp,
                    trackColor = secondaryColor.copy(alpha = 0.2f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$remaining",
                        fontSize = 32.sp,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SEC",
                        fontSize = 12.sp,
                        color = secondaryColor
                    )
                }
            }
            
            Text(
                text = "Total: ${timerSeconds / 60}:${String.format(Locale.US, "%02d", timerSeconds % 60)}",
                color = textColor,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Control Buttons
            Button(
                onClick = { if (isTracking) viewModel.stopTracking() else viewModel.startTracking() },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) Color.Gray else Color(0xFF006400)
                )
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isTracking) "STOP OBSERVATION" else "START OBSERVATION",
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Results List
            Text(
                text = "LAST RESULTS",
                color = secondaryColor,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            HorizontalDivider(color = secondaryColor, modifier = Modifier.padding(vertical = 4.dp))
            
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(windResults.reversed()) { result ->
                    WindResultItem(result, textColor, secondaryColor)
                }
            }
        }
    }
}

@Composable
fun WindResultItem(result: WindResult, textColor: Color, secondaryColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Alt: ${result.heightMeters.toInt()}m", color = textColor, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Spd: ${String.format(Locale.US, "%.1f", result.windSpeed)}m/s", color = textColor)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Dir: ${result.windDirection.toInt()}°", color = textColor)
            }
        }
    }
}
