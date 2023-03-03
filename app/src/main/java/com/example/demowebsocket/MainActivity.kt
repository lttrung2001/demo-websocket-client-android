package com.example.demowebsocket

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.demowebsocket.databinding.ActivityMainBinding
import com.example.demowebsocket.models.PublishTopic
import com.google.gson.Gson
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.nio.ByteBuffer
import java.nio.ByteOrder


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var ws: WebSocket

    private lateinit var audioTrack: AudioTrack
    private lateinit var audioRecord: AudioRecord

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            val message = binding.edittext.text.toString()
            val publish = Gson().toJson(PublishTopic("/global", message))
            Log.i("PUBLISH", publish)
            Log.i("SEND", ws.send(publish).toString())
        }

        val client = OkHttpClient()
        val request: Request = Request.Builder()
            .url("ws://192.168.43.203:8080/websocket")
            .build()

        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Connection opened
                Log.i("INFO", "Connected")
                initAudioTrack()
                initAudioRecord()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Message received
                Log.i("MESSAGE", text)
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

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Audio packet received
                // Pass the audio packet to the media server for processing
                // Convert bytes to PCM data
                val pcmData = ShortArray(bytes.size)
                ByteBuffer.wrap(bytes.toByteArray()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    .get(pcmData)

                // Write PCM data to AudioTrack
                audioTrack.write(pcmData, 0, pcmData.size)
            }
        })
    }

    private fun initAudioTrack() {
        val sampleRate = 48000
        val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize: Int =
            AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize,
            AudioTrack.MODE_STREAM
        )

        // Set AudioTrack parameters
        audioTrack.setPlaybackRate(sampleRate)
        audioTrack.setStereoVolume(1.0f, 1.0f)
        audioTrack.play()
    }

    private fun initAudioRecord() {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_8BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        val buffer = ByteArray(bufferSize)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        audioRecord.startRecording()

        Thread {
            while(true) {
                val bytesRead = audioRecord.read(buffer, 0, bufferSize)
                if (bytesRead != AudioRecord.ERROR_INVALID_OPERATION) {
                    // process the bytes in the buffer array
                    val audioBytes: ByteArray = buffer.copyOf(bytesRead)
                    // send the audioBytes to server or use it for further processing
                    ws.send(audioBytes.toByteString())
                }
            }
        }.start()
    }
}