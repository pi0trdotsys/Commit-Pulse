package com.commitpulse.app.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commitpulse.app.data.summarize
import java.time.LocalDate
import com.commitpulse.app.ui.screens.AccountScreen
import com.commitpulse.app.ui.screens.NotificationsScreen
import com.commitpulse.app.ui.screens.PersonalizationScreen
import com.commitpulse.app.ui.screens.PreviewModesScreen
import com.commitpulse.app.ui.theme.CommitPulseTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsState()
            CommitPulseTheme(materialYou = settings.materialYou) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CommitPulseRoot(viewModel)
                }
            }
        }
    }
}

@Composable
private fun CommitPulseRoot(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    val history by viewModel.history.collectAsState()
    val account by viewModel.account.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastError by viewModel.lastError.collectAsState()
    val lastSync by viewModel.lastSync.collectAsState()
    val summary = remember(history) { summarize(history, LocalDate.now()) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Podgląd", "Personalizacja", "Powiadomienia", "Konto")

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Commit Pulse", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StatItem("dziś", summary.todayCount.toString())
                StatItem("seria", "${summary.streak} dni")
                StatItem("7 dni", summary.last7.toString())
                StatItem("vs poprz. 7", summary.trend.label(settings.trendFormat))
            }
        }

        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { i, label ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            when (tab) {
                0 -> PreviewModesScreen(
                    settings, history, summary, lastSync,
                    onModeChange = { mode -> viewModel.updateSettings { it.copy(mode = mode) } },
                )
                1 -> PersonalizationScreen(settings, history, summary, onUpdate = viewModel::updateSettings)
                2 -> NotificationsScreen(settings, history, summary, onUpdate = viewModel::updateSettings)
                3 -> AccountScreen(
                    account = account,
                    isSyncing = isSyncing,
                    lastError = lastError,
                    onSignIn = viewModel::signIn,
                    onSignOut = viewModel::signOut,
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 11.sp)
    }
}
