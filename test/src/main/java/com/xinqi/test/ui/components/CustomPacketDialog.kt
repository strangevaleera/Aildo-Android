package com.xinqi.test.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xinqi.utils.bt.spec.odm.SvakomPacket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPacketDialog(
    onDismiss: () -> Unit,
    onConfirm: (SvakomPacket) -> Unit
) {
    var command by remember { mutableStateOf("") }
    var param1 by remember { mutableStateOf("") }
    var param2 by remember { mutableStateOf("") }
    var param3 by remember { mutableStateOf("") }
    var param4 by remember { mutableStateOf("") }
    var param5 by remember { mutableStateOf("") }
    
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "自定义数据包",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 指令选择
                Column {
                    Text(
                        text = "指令:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text("指令编码 (十六进制)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("例如: 00") }
                    )
                    
                    // 常用指令提示
                    Text(
                        text = "常用指令: 00(设备信息), 02(电量状态), 03(震动控制), 05(加热控制), 12(特殊模式), A1(LED控制), A2(寻物), 57(OTA升级)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 参数输入
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = param1,
                        onValueChange = { param1 = it },
                        label = { Text("参数1") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("0x") }
                    )
                    
                    OutlinedTextField(
                        value = param2,
                        onValueChange = { param2 = it },
                        label = { Text("参数2") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("0x") }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = param3,
                        onValueChange = { param3 = it },
                        label = { Text("参数3") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("0x") }
                    )
                    
                    OutlinedTextField(
                        value = param4,
                        onValueChange = { param4 = it },
                        label = { Text("参数4") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("0x") }
                    )
                }
                
                OutlinedTextField(
                    value = param5,
                    onValueChange = { param5 = it },
                    label = { Text("参数5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0x") }
                )
                
                // 错误提示
                if (showError) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val cmd = command.toIntOrNull(16) ?: throw IllegalArgumentException("指令格式错误")
                        val p1 = param1.ifEmpty { "0" }.toIntOrNull(16) ?: 0
                        val p2 = param2.ifEmpty { "0" }.toIntOrNull(16) ?: 0
                        val p3 = param3.ifEmpty { "0" }.toIntOrNull(16) ?: 0
                        val p4 = param4.ifEmpty { "0" }.toIntOrNull(16) ?: 0
                        val p5 = param5.ifEmpty { "0" }.toIntOrNull(16) ?: 0
                        
                        val packet = SvakomPacket(
                            command = cmd,
                            param1 = p1,
                            param2 = p2,
                            param3 = p3,
                            param4 = p4,
                            param5 = p5
                        )
                        
                        onConfirm(packet)
                        
                    } catch (e: Exception) {
                        showError = true
                        errorMessage = "参数格式错误: ${e.message}"
                    }
                }
            ) {
                Text("发送")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
