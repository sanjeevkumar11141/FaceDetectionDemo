package com.example.personademo
import kotlinx.coroutines.*

fun main() {
    /*println("Task 1 started")
    Thread.sleep(1000) // Simulates a blocking task
    println("Task 1 completed")

    println("Task 2 started")
    Thread.sleep(1000) // Simulates a blocking task
    println("Task 2 completed")

    println("Task 3 started")
    Thread.sleep(1000) // Simulates a blocking task
    println("Task 3 completed")*/

    synchronousTask()
   // asynchronousTask()
}



fun asynchronousTask() = runBlocking {
    launch {
        println("Task 1 started")
        delay(2000) // Simulates a non-blocking task
        println("Task 1 completed")
    }

    launch {
        println("Task 2 started")
        delay(4000) // Simulates a non-blocking task
        println("Task 2 completed")
    }

    launch {
        println("Task 3 started")
        delay(6000) // Simulates a non-blocking task
        println("Task 3 completed")
    }
}

fun synchronousTask(){
    val thread1 = Thread {
        for (i in 1..5) {
            println("Thread 1 - $i")
            Thread.sleep(2000)
        }
    }

    val thread2 = Thread {
        for (i in 1..5) {
            println("Thread 2 - $i")
            Thread.sleep(2000)
        }
    }

    thread1.start()
    thread2.start()

   // thread1.join() // Waits for thread1 to finish
   // thread2.join() // Waits for thread2 to finish

    println("Both threads have finished")

    var thread3 = Thread(object : Runnable {
        override fun run() {
            TODO("Not yet implemented")
        }

    })
}