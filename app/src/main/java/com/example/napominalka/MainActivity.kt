package com.example.napominalka

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Android 12: нужно вручную разрешить точные будильники
        if (Build.VERSION.SDK_INT in 31..32) {
            val am = getSystemService(AlarmManager::class.java)
            if (!am.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }

        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                App()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val ctx = LocalContext.current
    var reminders by remember { mutableStateOf(ReminderStore.load(ctx)) }
    var showDialog by remember { mutableStateOf(false) }

    // перечитываем список при возврате в приложение (уведомление могло пометить как done)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) reminders = ReminderStore.load(ctx)
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    fun persist(list: List<Reminder>) {
        reminders = list.sortedBy { it.time }
        ReminderStore.save(ctx, reminders)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Напоминалка") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { pad ->
        if (reminders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Пусто. Жми +", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reminders, key = { it.id }) { r ->
                    ReminderCard(r) {
                        ReminderScheduler.cancel(ctx, r)
                        persist(reminders - r)
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddDialog(
            onDismiss = { showDialog = false },
            onSave = { text, time ->
                if (time <= System.currentTimeMillis()) {
                    Toast.makeText(ctx, "Это время уже прошло", Toast.LENGTH_SHORT).show()
                } else {
                    val r = Reminder(System.currentTimeMillis(), text, time)
                    ReminderScheduler.schedule(ctx, r)
                    persist(reminders + r)
                    showDialog = false
                }
            }
        )
    }
}

@Composable
fun ReminderCard(r: Reminder, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("EEE, d MMM · HH:mm", Locale.getDefault()) }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    r.text,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (r.done) TextDecoration.LineThrough else null
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    (if (r.done) "Сработало · " else "") + fmt.format(r.time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
            }
        }
    }
}

@Composable
fun AddDialog(onDismiss: () -> Unit, onSave: (String, Long) -> Unit) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    val cal = remember {
        Calendar.getInstance().apply {
            add(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    var tick by remember { mutableIntStateOf(0) } // чтобы перерисовать после пикера
    val fmt = remember { SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое напоминание") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("О чём напомнить") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                key(tick) {
                    Text(fmt.format(cal.time), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedButton(onClick = {
                        DatePickerDialog(
                            ctx,
                            { _, y, m, d -> cal.set(y, m, d); tick++ },
                            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) { Text("Дата") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = {
                        TimePickerDialog(
                            ctx,
                            { _, h, min -> cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min); tick++ },
                            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
                        ).show()
                    }) { Text("Время") }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onSave(text.trim(), cal.timeInMillis) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
