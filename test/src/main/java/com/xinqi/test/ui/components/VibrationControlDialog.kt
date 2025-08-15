package com.xinqi.test.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xinqi.utils.bt.spec.odm.BluetoothProtocol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrationControlDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit
) {
    var selectedMotorId by remember { mutableStateOf(BluetoothProtocol.Motor.MOTOR_ALL) }
    var selectedMode by remember { mutableStateOf(BluetoothProtocol.VibrationMode.MODE_STOP) }
    var intensity by remember { mutableStateOf(50) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "震动控制",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 马达选择
                Column {
                    Text(
                        text = "选择马达:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column {
                        listOf(
                            BluetoothProtocol.Motor.MOTOR_ALL to "全部马达",
                            BluetoothProtocol.Motor.MOTOR_1 to "马达1",
                            BluetoothProtocol.Motor.MOTOR_2 to "马达2",
                            BluetoothProtocol.Motor.MOTOR_3 to "马达3"
                        ).forEach { (id, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedMotorId == id,
                                    onClick = { selectedMotorId = id }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = name)
                            }
                        }
                    }
                }
                
                // 震动模式选择
                Column {
                    Text(
                        text = "震动模式:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column {
                        listOf(
                            BluetoothProtocol.VibrationMode.MODE_STOP to "停止",
                            BluetoothProtocol.VibrationMode.MODE_CONSTANT to "持续震动",
                            BluetoothProtocol.VibrationMode.MODE_PULSE to "脉冲震动",
                            BluetoothProtocol.VibrationMode.MODE_ESCALATING to "递增震动",
                            BluetoothProtocol.VibrationMode.MODE_WAVE to "波浪震动"
                        ).forEach { (mode, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedMode == mode,
                                    onClick = { selectedMode = mode }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = name)
                            }
                        }
                    }
                }
                
                // 强度控制
                Column {
                    Text(
                        text = "强度: $intensity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Slider(
                        value = intensity.toFloat(),
                        onValueChange = { intensity = it.toInt() },
                        valueRange = 0f..100f,
                        steps = 100
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "0")
                        Text(text = "100")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedMotorId, selectedMode, intensity)
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
