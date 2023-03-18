package com.example.demowebsocket

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.demowebsocket.databinding.ActivityMainBinding
import com.example.demowebsocket.models.Message
import com.google.gson.Gson
import okhttp3.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var ws: WebSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            val message = binding.edittext.text.toString()
            val publish = Gson().toJson(Message("global", "publish", message))
            Log.i("SEND", ws.send(publish).toString())
        }

        val client = OkHttpClient()
        val request: Request = Request.Builder()
            .url("ws://192.168.39.113:8080/websocket")
            .build()

        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("INFO", "Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Message received
                runOnUiThread {
                    val newTextView = TextView(this@MainActivity)
                    newTextView.textSize = 18F
                    newTextView.text = text
                    binding.root.addView(newTextView)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                // Connection closing
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                // Connection closed
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Connection failed
                Log.i("INFO", "onFailure")
                t.printStackTrace()
            }
        })
    }
}