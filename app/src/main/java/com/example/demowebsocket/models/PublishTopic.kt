package com.example.demowebsocket.models

data class PublishTopic<T>(
    val topic: String,
    val message: T?
)
