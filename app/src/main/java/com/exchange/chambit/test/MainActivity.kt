package com.exchange.chambit.test

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.exchange.chambit.R
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import java.net.URISyntaxException

class MainActivity : AppCompatActivity() {

    private lateinit var socket: Socket


    var userId: String? = null
    var merchantId: String? = null

    private val CHANNEL_ID = "chambit_exchange_channel"
    private val NOTIFICATION_ID = 1

    private val REQUEST_CODE_NOTIFICATION_PERMISSION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        // Create notification channel
        createNotificationChannel()

        // Check and request notification permission
        checkAndRequestNotificationPermission()

        // Retrieve the userId and merchantId from the intent
         userId = intent.getStringExtra("USER_ID")
         merchantId = intent.getStringExtra("MERCHANT_ID")


        // Initialize Socket.IO
        try {
            socket = IO.socket("https://chambitexchange.onrender.com")
            socket.on(Socket.EVENT_CONNECT, onConnect)
            socket.on("notification", onNotification)
            socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
            socket.on(Socket.EVENT_DISCONNECT, onDisconnect)
            socket.connect()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SocketIO", "Error initializing socket: ${e.message}")
        }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Chambit Exchange Notifications"
            val descriptionText = "Channel for Chambit Exchange notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d("NotificationPermission", "Notification permission granted")
            } else {
                Log.d("NotificationPermission", "Notification permission denied")
                Toast.makeText(this, "Notification permission is required to receive notifications", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showNotification(message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon) // Change to your app's notification icon
            .setContentTitle("Chambit Exchange")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }


    private val onConnect = Emitter.Listener {
        runOnUiThread {
            Log.d("SocketIO", "App connected")
            Toast.makeText(this@MainActivity, "App connected", Toast.LENGTH_SHORT).show()

            // Emit register event with userId if it is not null
            if (userId != null) {
                socket.emit("register", userId)
                Log.d("SocketIO", "Register event sent with userId: $userId")
                Toast.makeText(this@MainActivity, "Register event sent with userId: $userId", Toast.LENGTH_SHORT).show()

                // After emitting register event, emit adClick event
                if (merchantId != null) {
                    socket.emit("adClick", userId, merchantId)
                    Log.d("SocketIO", "adClick event sent with userWhoClicked: $userId, adOwnerId: $merchantId")
                    Toast.makeText(this@MainActivity, "adClick event sent with userWhoClicked: $userId, adOwnerId: $merchantId", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("SocketIO", "MerchantId is null")
                    Toast.makeText(this@MainActivity, "MerchantId is null", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("SocketIO", "UserId is null")
                Toast.makeText(this@MainActivity, "UserId is null", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val onNotification = Emitter.Listener { args ->
        Log.d("SocketIO", "onNotification event triggered with args: ${args.joinToString()}")
        if (args.isNotEmpty() && args[0] != null) {
            val notificationMessage = args[0] as? String
            if (!notificationMessage.isNullOrEmpty()) {
                Log.d("SocketIO", "Notification received: $notificationMessage")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, notificationMessage, Toast.LENGTH_SHORT).show()
                    showNotification(notificationMessage) // Show notification

                }
            } else {
                Log.d("SocketIO", "Notification message is empty or null")
            }
        } else {
            Log.d("SocketIO", "No notification message received")
        }
    }

    private val onConnectError = Emitter.Listener { args ->
        Log.e("SocketIO", "Connection error: ${args.joinToString()}")
        runOnUiThread {
            Toast.makeText(this@MainActivity, "Connection error", Toast.LENGTH_SHORT).show()
        }
    }

    private val onDisconnect = Emitter.Listener {
        Log.d("SocketIO", "Disconnected from server")
        runOnUiThread {
            Toast.makeText(this@MainActivity, "Disconnected from server", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.disconnect()
        socket.off(Socket.EVENT_CONNECT, onConnect)
        socket.off("notification", onNotification)
        socket.off(Socket.EVENT_CONNECT_ERROR, onConnectError)
        socket.off(Socket.EVENT_DISCONNECT, onDisconnect)
    }

//
//        // Initialize Socket.IO
//        try {
//            socket = IO.socket("https://chambitexchange.onrender.com")
//            socket.on(Socket.EVENT_CONNECT, onConnect)
//            socket.on("notification", onNotification)
//            socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
//            socket.on(Socket.EVENT_DISCONNECT, onDisconnect)
//            socket.connect()
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Log.e("SocketIO", "Error initializing socket: ${e.message}")
//        }
//    }
//
//    private val onConnect = Emitter.Listener {
//        runOnUiThread {
//            Log.d("SocketIO", "App connected")
//            Toast.makeText(this@MainActivity, "App connected", Toast.LENGTH_SHORT).show()
//
//            // Emit register event with userId if it is not null
//            if (userId != null) {
//                socket.emit("register", userId)
//                Log.d("SocketIO", "Register event sent with userId: $userId")
//                Toast.makeText(this@MainActivity, "Register event sent with userId: $userId", Toast.LENGTH_SHORT).show()
//
//                // After emitting register event, emit adClick event
//                if (merchantId != null) {
//                    socket.emit("adClick", userId, merchantId)
//                    Log.d("SocketIO", "adClick event sent with userWhoClicked: $userId, adOwnerId: $merchantId")
//                    Toast.makeText(this@MainActivity, "adClick event sent with userWhoClicked: $userId, adOwnerId: $merchantId", Toast.LENGTH_SHORT).show()
//                } else {
//                    Log.d("SocketIO", "MerchantId is null")
//                    Toast.makeText(this@MainActivity, "MerchantId is null", Toast.LENGTH_SHORT).show()
//                }
//            } else {
//                Log.d("SocketIO", "UserId is null")
//                Toast.makeText(this@MainActivity, "UserId is null", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private val onNotification = Emitter.Listener { args ->
//        Log.d("SocketIO", "onNotification event triggered")
//        if (args.isNotEmpty()) {
//            val notificationMessage = args[0] as String
//            Log.d("SocketIO", "Notification received: $notificationMessage")
//            runOnUiThread {
//                Toast.makeText(this@MainActivity, notificationMessage, Toast.LENGTH_SHORT).show()
//            }
//        } else {
//            Log.d("SocketIO", "No notification message received")
//        }
//    }
//
//    private val onConnectError = Emitter.Listener { args ->
//        Log.e("SocketIO", "Connection error: ${args.joinToString()}")
//        runOnUiThread {
//            Toast.makeText(this@MainActivity, "Connection error", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private val onDisconnect = Emitter.Listener {
//        Log.d("SocketIO", "Disconnected from server")
//        runOnUiThread {
//            Toast.makeText(this@MainActivity, "Disconnected from server", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        socket.disconnect()
//        socket.off(Socket.EVENT_CONNECT, onConnect)
//        socket.off("notification", onNotification)
//        socket.off(Socket.EVENT_CONNECT_ERROR, onConnectError)
//        socket.off(Socket.EVENT_DISCONNECT, onDisconnect)
//    }

//
//        // Initialize Socket.IO
//        try {
//            socket = IO.socket("https://chambitexchange.onrender.com")
//            socket.on(Socket.EVENT_CONNECT, onConnect)
//            socket.on("notification", onNotification)
//            socket.connect()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    private val onConnect = Emitter.Listener {
//        runOnUiThread {
//            Log.d("SocketIO", "App connected")
//            Toast.makeText(this@MainActivity, "App connected", Toast.LENGTH_SHORT).show()
//
//            // Emit register event with userId if it is not null
//            if (userId != null) {
//                socket.emit("register", userId)
//                Log.d("SocketIO", "Register event sent with userId: $userId")
//                Toast.makeText(this@MainActivity, "Register event sent with userId: $userId", Toast.LENGTH_SHORT).show()
//
//                // After emitting register event, emit adClick event
//                if (merchantId != null) {
//                    socket.emit("adClick", merchantId, userId)
//                    Log.d("SocketIO", "adClick event sent with userWhoClicked: $userId, adOwnerId: $merchantId")
//                    Toast.makeText(this@MainActivity, "adClick event sent with userWhoClicked: $userId, adOwnerId: $merchantId", Toast.LENGTH_SHORT).show()
//                } else {
//                    Log.d("SocketIO", "MerchantId is null")
//                    Toast.makeText(this@MainActivity, "MerchantId is null", Toast.LENGTH_SHORT).show()
//                }
//            } else {
//                Log.d("SocketIO", "UserId is null")
//                Toast.makeText(this@MainActivity, "UserId is null", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private val onNotification = Emitter.Listener { args ->
//        if (args.isNotEmpty()) {
//            val notificationMessage = args[0] as String
//            Log.d("SocketIO", "Notification received: $notificationMessage")
//            runOnUiThread {
//                Toast.makeText(this@MainActivity, notificationMessage, Toast.LENGTH_SHORT).show()
//            }
//        } else {
//            Log.d("SocketIO", "No notification message received")
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        socket.disconnect()
//        socket.off(Socket.EVENT_CONNECT, onConnect)
//        socket.off("notification", onNotification)
//    }
}
