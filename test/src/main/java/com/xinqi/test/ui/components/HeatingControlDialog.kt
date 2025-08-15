package com.xinqi.test.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xinqi.utils.bt.spec.odm.BluetoothProtocol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatingControlDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(BluetoothProtocol.Heating.HEATING_OFF) }
    var temperature by remember { mutableStateOf(45) }
    var selectedHeaterId by remember { mutableStateOf(0) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "加热控制",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 加热状态选择
                Column {
                    Text(
                        text = "加热状态:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedStatus == BluetoothProtocol.Heating.HEATING_OFF,
                                onClick = { selectedStatus = BluetoothProtocol.Heating.HEATING_OFF }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "关闭")
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedStatus == BluetoothProtocol.Heating.HEATING_ON,
                                onClick = { selectedStatus = BluetoothProtocol.Heating.HEATING_ON }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "开启")
                        }
                    }
                }
                
                // 温度控制
                if (selectedStatus == BluetoothProtocol.Heating.HEATING_ON) {
                    Column {
                        Text(
                            text = "温度: ${temperature}°C",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Slider(
                            value = temperature.toFloat(),
                            onValueChange = { temperature = it.toInt() },
                            valueRange = BluetoothProtocol.Heating.TEMP_MIN.toFloat()..BluetoothProtocol.Heating.TEMP_MAX.toFloat(),
                            steps = BluetoothProtocol.Heating.TEMP_MAX - BluetoothProtocol.Heating.TEMP_MIN
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${BluetoothProtocol.Heating.TEMP_MIN}°C")
                            Text(text = "${BluetoothProtocol.Heating.TEMP_MAX}°C")
                        }
                    }
                }
                
                // 加热器选择
                Column {
                    Text(
                        text = "加热器编号:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = selectedHeaterId.toString(),
                        onValueChange = { 
                            selectedHeaterId = it.toIntOrNull() ?: 0 
                        },
                        label = { Text("加热器编号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedStatus, temperature, selectedHeaterId)
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
