package com.github.h4de5ing.datetime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.h4de5ing.datetime.ui.theme.DateTimeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DateTimeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    var openDateDialog by remember { mutableStateOf(false) }
    var openTimeDialog by remember { mutableStateOf(false) }
    var autoDatetime by remember { mutableStateOf(isDateTimeAuto(context)) }
    var autoTimeZone by remember { mutableStateOf(isTimeZoneAuto(context)) }
    var is24Hour by remember { mutableStateOf(is24HourFormat(context)) }
    var time by remember { mutableStateOf("${Date().time.date2HumanString()},${getTimeZoneId()}") }
    val state = rememberTimePickerState()
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            while (true) {
                delay(TimeUnit.SECONDS.toMillis(1))
                time = "${Date().time.date2HumanString()},${getTimeZoneId()}"
            }
        }
    }
    if (openDateDialog) {
        val datePickerState =
            rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        val confirmEnabled =
            remember { derivedStateOf { datePickerState.selectedDateMillis != null } }
        DatePickerDialog(
            onDismissRequest = { openDateDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDateDialog = false
                        datePickerState.selectedDateMillis?.let {
                            scope.launch(Dispatchers.IO) { setDateTime(context, it) }
                        }
                    },
                    enabled = confirmEnabled.value
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    openDateDialog = false
                }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    if (openTimeDialog) {
        val showingPicker = remember { mutableStateOf(true) }
        TimePickerDialog(
            title = if (showingPicker.value) "Select Time " else "Enter Time",
            onCancel = { openTimeDialog = false },
            onConfirm = {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, state.hour)
                cal.set(Calendar.MINUTE, state.minute)
                cal.isLenient = false
                openTimeDialog = false
                scope.launch(Dispatchers.IO) { setTime(context, state.hour, state.minute) }
            },
            toggle = {
                if (configuration.screenHeightDp > 400) {
                    IconButton(onClick = { showingPicker.value = !showingPicker.value }) {
                        val icon =
                            if (showingPicker.value) Icons.Outlined.Keyboard else Icons.Outlined.Schedule
                        Icon(
                            icon,
                            contentDescription = if (showingPicker.value) "Switch to Text Input" else "Switch to Touch Input"
                        )
                    }
                }
            }
        ) {
            if (showingPicker.value && configuration.screenHeightDp > 400) {
                TimePicker(state = state)
            } else {
                TimeInput(state = state)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = time,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 1.sp
            )
        )
        CheckboxString(
            checked = autoDatetime,
            onCheckedChange = {
                autoDatetime = it
                setDateTimeAuto(context, autoDatetime)
            },
            text = "自动确认的日期和时间"
        )
        CheckboxString(
            checked = autoTimeZone,
            onCheckedChange = {
                autoTimeZone = it
                setTimeZoneAuto(context, autoTimeZone)
            },
            text = "自动确认时区"
        )
        CheckboxString(
            checked = is24Hour,
            onCheckedChange = {
                is24Hour = it
                set24HourFormat(context, is24Hour)
            },
            text = "使用24小时格式"
        )
        Button(onClick = { openDateDialog = true }) { Text(text = "设置日期") }
        Button(onClick = { openTimeDialog = true }) { Text(text = "设置时间") }
        Button(onClick = {
            //先分组，在进行设置
            TimeZone.getAvailableIDs().forEach {
                println("时区:${it}")
            }
        }) { Text(text = "设置时区") }
        Button(onClick = {
            scope.launch(Dispatchers.IO) {
                val url = "https://www.baidu.com"
                val build = OkHttpClient
                    .Builder().addInterceptor(TimeSynchronizationInterceptor())
                    .callTimeout(1, TimeUnit.SECONDS)
                    .build()
                build.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        println("请求失败")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        println("请求成功 = ${response.code}")
                    }
                })
            }
        }) { Text(text = "网络时间") }
    }
}

@Composable
fun CheckboxString(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?, text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = text)
    }
}