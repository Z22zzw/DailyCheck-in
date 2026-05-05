package com.z22zzw.dailycheckin.ui.ai

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun OnboardingDialog(
    step: OnboardingStep,
    onNext: (String) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var textValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Column {
                Text(step.question)
            }
        },
        text = {
            when (step) {
                is OnboardingStep.TextInput -> {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        placeholder = { Text(step.placeholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is OnboardingStep.Options -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        step.options.forEach { option ->
                            OutlinedButton(
                                onClick = { onNext(option) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(option)
                            }
                        }
                    }
                }

                is OnboardingStep.DatePicker -> {
                    // 直接弹出 DatePickerDialog
                    LaunchedEffect(Unit) {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                onNext("${year}-${month + 1}-${dayOfMonth}")
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    Text("请在弹出的日期选择器中选择...", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (step is OnboardingStep.TextInput) {
                Button(
                    onClick = { onNext(textValue) },
                    enabled = textValue.isNotBlank()
                ) {
                    Text("下一步")
                }
            }
        },
        dismissButton = {
            if (step !is OnboardingStep.DatePicker) {
                TextButton(onClick = onSkip) {
                    Text("跳过")
                }
            }
        }
    )
}
