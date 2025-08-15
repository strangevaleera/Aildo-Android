package com.xinqi.utils.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())