package com.commitpulse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commitpulse.app.github.GitHubAccount
import com.commitpulse.app.ui.components.Section

@Composable
fun AccountScreen(
    account: GitHubAccount?,
    isSyncing: Boolean,
    lastError: String?,
    onSignIn: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    var token by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Section(title = "Konto GitHub", hint = "Realne pobieranie aktywności przez GitHub GraphQL API.") {
            Card {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (account != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccountCircle, null, modifier = Modifier.size(40.dp))
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(account.login, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text("Połączono", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                            Text("Wyloguj")
                        }
                    } else {
                        Text(
                            "Wklej Personal Access Token (classic), scope: read:user. Token jest szyfrowany i zapisywany tylko lokalnie na urządzeniu.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        OutlinedTextField(
                            value = token,
                            onValueChange = { token = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("ghp_••••••••••••••••") },
                            visualTransformation = PasswordVisualTransformation(),
                        )
                        Button(
                            onClick = { onSignIn(token) },
                            enabled = !isSyncing && token.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Połącz z GitHub")
                            }
                        }
                        if (lastError != null && lastError.isNotBlank()) {
                            Text(lastError, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Section(
            title = "Źródła aktywności",
            hint = "GitHub GraphQL zwraca łączną liczbę kontrybucji (commity, PR-y, review'y, issue) dla właściciela tokenu — bez podziału per typ w rozbiciu dziennym.",
        ) {
            Card {
                Column {
                    listOf("Commity publiczne" to true, "Commity prywatne" to true, "Pull requesty" to false, "Review'y" to false)
                        .forEach { (label, defaultChecked) ->
                            var checked by remember { mutableStateOf(defaultChecked) }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(label, fontSize = 14.sp)
                                Switch(checked = checked, onCheckedChange = { checked = it })
                            }
                        }
                }
            }
        }
    }
}
