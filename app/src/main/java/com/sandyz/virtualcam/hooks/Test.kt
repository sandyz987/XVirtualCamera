package com.sandyz.virtualcam.hooks

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 *@author sandyz987
 *@date 2023/12/6
 *@description
 */

class MyCoroutineScope : CoroutineScope {
    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + CoroutineName("Test") + job + CoroutineExceptionHandler {
                coroutineContext, throwable ->
            println("throwable: $throwable in context $coroutineContext")
        }
}
fun main() = runBlocking {
    val coroutineScope = MyCoroutineScope()
    coroutineScope.launch {
        while (true) {
            delay(500L)
            println("hello")
        }
    }
//    delay(3000L)
    coroutineScope.cancel()
    delay(3000L)

}


fun println(msg: String) {
    kotlin.io.println("[${Thread.currentThread().name}]$msg")
}