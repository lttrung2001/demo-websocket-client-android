package com.example.demowebsocket.models

data class Message<T>(
    val topic: String,
    val action: String,
    val data: T
)
