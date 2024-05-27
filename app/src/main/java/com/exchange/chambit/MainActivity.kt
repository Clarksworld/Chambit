package com.exchange.chambit

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.exchange.chambit.databinding.ActivityMainBinding
import com.exchange.chambit.test.MainActivity
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {



    private lateinit var binding: ActivityMainBinding // <----



    var filePath: ValueCallback<Array<Uri>>? = null;
    var alertDialog: AlertDialog? = null


    var webViewUrl = "https://app.chambit.exchange"
//    var webViewUrl = "https://chambit.netlify.app"

    val appName = "CHAMBIT"

    lateinit var executor: Executor
    lateinit var biometricPrompt: androidx.biometric.BiometricPrompt
    lateinit var prompt: androidx.biometric.BiometricPrompt.PromptInfo


    private var socket: Socket? = null


    var userId: String? = null
    var merchantId: String? = null

    private val CHANNEL_ID = "chambit_exchange_channel"
    private val NOTIFICATION_ID = 1

    private val REQUEST_CODE_NOTIFICATION_PERMISSION = 1001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)


        binding = ActivityMainBinding.inflate(layoutInflater) // <----
        val view = binding.root // <----
        setContentView(view)


        supportActionBar?.hide();


        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

//        val urlFromIntent = intent.getStringExtra("URL")
//        if (!urlFromIntent.isNullOrEmpty()) {
//            displayWebView(urlFromIntent)
//        } else {
//            displayWebView(webViewUrl)
//        }

        handleIntent(intent)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun displayWebView(mUrl: String){



        binding.webViewPrivacy.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {

                binding.fingerPrintLoginProgress.visibility = View.VISIBLE

                if (url.contains("NOTIFICATION")){

                    val uri: Uri = Uri.parse(url)
                    userId = uri.getQueryParameter("userId")
                    merchantId = uri.getQueryParameter("merchantId")

                    // Create a message with the extracted parameters
                    val message = "UserId: $userId, MerchantId: $merchantId"


                    // Create notification channel
                    createNotificationChannel()

                    // Check and request notification permission
                    checkAndRequestNotificationPermission()


                    // Initialize Socket.IO
                    try {
                        socket = IO.socket("https://chambitexchange.onrender.com")
                        socket?.on(Socket.EVENT_CONNECT, onConnect)
                        socket?.on("notification", onNotification)
                        socket?.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
                        socket?.on(Socket.EVENT_DISCONNECT, onDisconnect)
                        socket?.connect()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("SocketIO", "Error initializing socket: ${e.message}")
                    }


//                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()


                }



                view.loadUrl(url)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)


                binding.fingerPrintLoginProgress.visibility = View.VISIBLE


            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                binding.fingerPrintLoginProgress.visibility = View.INVISIBLE
                binding.refreshLayout.isRefreshing = false

//                Toast.makeText(this@MainActivity, "url $url", Toast.LENGTH_SHORT).show()


            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)

                Toast.makeText(this@MainActivity, "There was an error", Toast.LENGTH_SHORT).show()
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)

//                val directions = SignUpFragmentDirections.
//                actionSignUpFragmentToErrorScreenFragment(errorResponse.toString())
//                findNavController().navigate(directions)

//                Log.d(TAG, "onReceivedHttpError: ${view?.url} $errorResponse")
//
//                Toast.makeText(requireContext(), "There was an error try again", Toast.LENGTH_SHORT).show()

//                findNavController().navigate(R.id.action_signUpFragment_to_errorScreenFragment2)


            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
            ): Boolean {
                if (view == binding.webViewPrivacy && detail?.didCrash() == true){
                    Toast.makeText(this@MainActivity, "Wow", Toast.LENGTH_SHORT).show()
                    return true
                }
                return super.onRenderProcessGone(view, detail)
            }


            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                super.onReceivedSslError(view, handler, error)
            }
        }





        binding.webViewPrivacy.viewTreeObserver.addOnScrollChangedListener {

            binding.refreshLayout.isEnabled = binding.webViewPrivacy.scrollY == 0
        }

//        binding.nestedScrollview.setOnScrollChangeListener { v, _, scrollY, _, _ ->
//            // Enable/disable the SwipeRefreshLayout based on the scroll position
//            binding.refreshLayout.isEnabled = scrollY == 0
//        }

        binding.refreshLayout.setOnRefreshListener {
            refreshPage()
        }

//        binding.refreshLayout.setColorSchemeColors(R.color.trueeal_700, R.color.purple_200, R.color.purple_500)

        binding.webViewPrivacy.loadUrl(mUrl)
        binding.webViewPrivacy.settings.javaScriptEnabled = true
        binding.webViewPrivacy.settings.allowContentAccess = true
        binding.webViewPrivacy.settings.domStorageEnabled = true
        binding.webViewPrivacy.settings.useWideViewPort = true
        binding.webViewPrivacy.webChromeClient = MyWebChromeClient(this)

        binding.webViewPrivacy.setOnKeyListener { _, _, keyEvent ->
            if (keyEvent.keyCode == KeyEvent.KEYCODE_BACK && !binding.webViewPrivacy.canGoBack()) {
                false
            } else if (keyEvent.keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == MotionEvent.ACTION_UP) {
                binding.webViewPrivacy.goBack()
                true
            } else true
        }

//        Toast.makeText(requireContext(), "$mUrl", Toast.LENGTH_SHORT).show()


        onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                if (binding.webViewPrivacy.canGoBack()){
                    binding.webViewPrivacy.goBack()
                }else{

                    createDialog()
                    alertDialog?.show()
                }
            }
        })


    }


    fun createDialog() {


        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Notice")
        alertDialogBuilder.setMessage("You are about leaving $appName, are you sure you want to continue?")

        alertDialogBuilder.setPositiveButton("No") { _: DialogInterface, _: Int ->
            alertDialog?.dismiss()
        }
        alertDialogBuilder.setNegativeButton("Yes") { _: DialogInterface, _: Int ->
            ActivityCompat.finishAffinity(this)
        }
        alertDialog = alertDialogBuilder.create()

    }




    private fun refreshPage(){

        binding.webViewPrivacy.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {

                if (url.contains("NOTIFICATION")){

                    val uri: Uri = Uri.parse(url)
                    userId = uri.getQueryParameter("userId")
                    merchantId = uri.getQueryParameter("merchantId")

                    // Create a message with the extracted parameters
                    val message = "UserId: $userId, MerchantId: $merchantId"


                    // Create notification channel
                    createNotificationChannel()

                    // Check and request notification permission
                    checkAndRequestNotificationPermission()


                    // Initialize Socket.IO
                    try {
                        socket = IO.socket("https://chambitexchange.onrender.com")
                        socket?.on(Socket.EVENT_CONNECT, onConnect)
                        socket?.on("notification", onNotification)
                        socket?.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
                        socket?.on(Socket.EVENT_DISCONNECT, onDisconnect)
                        socket?.connect()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("SocketIO", "Error initializing socket: ${e.message}")
                    }


//                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()


                }

                view.loadUrl(url)

                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                binding.fingerPrintLoginProgress.visibility = View.VISIBLE


            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                binding.fingerPrintLoginProgress.visibility = View.INVISIBLE
                binding.refreshLayout.isRefreshing = false


            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)

            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)



            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                super.onReceivedSslError(view, handler, error)

                handler?.cancel()
            }
        }



        binding.webViewPrivacy.loadUrl(binding.webViewPrivacy.url.toString())
        binding.webViewPrivacy.settings.javaScriptEnabled = true
        binding.webViewPrivacy.settings.allowContentAccess = true
        binding.webViewPrivacy.settings.domStorageEnabled = true
        binding.webViewPrivacy.settings.useWideViewPort = true

    }

    inner class MyWebChromeClient(val myActivity: Activity) : WebChromeClient(){

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            filePath  = filePathCallback

            val contentIntent = Intent(Intent.ACTION_GET_CONTENT)
            contentIntent.type = "*/*"
            contentIntent.addCategory(Intent.CATEGORY_OPENABLE)

            startActivityForResult(contentIntent, 1)
            return true
        }


    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_CANCELED) {
            filePath?.onReceiveValue(null)
            return
        } else if (resultCode == Activity.RESULT_OK) {
            if (filePath == null) return

            filePath!!.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            )
            filePath = null
        }
    }


    fun fingerPrintAuth(){
        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = androidx.biometric.BiometricPrompt(this@MainActivity,
            executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback(){

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)

//                    findNavController().navigate(R.id.action_signUpFragment_self)

                    Toast.makeText(this@MainActivity, "error", Toast.LENGTH_SHORT).show()

                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

//                    val mEmail = runBlocking { userPreference.userId.first().toString() }
//                    val mPassword = runBlocking { userPreference.password.first().toString() }
//
//                    email = runBlocking { userPreference.userId.first().toString() }
//                    password = runBlocking { userPreference.password.first().toString() }
//
//                    loginDetails(mEmail!!, mPassword!!)
                    Toast.makeText(this@MainActivity, "authenticated", Toast.LENGTH_SHORT).show()


                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        prompt = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometrics Authentication")
            .setSubtitle("Login using finger print authentication")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(prompt)


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

        val currentUrl = binding.webViewPrivacy.url // Get the current URL of the WebView

        val intent = Intent(this, com.exchange.chambit.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("URL", currentUrl) // Pass the current URL as an extra

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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val urlFromIntent = it.getStringExtra("URL")
            Log.d("Notification", "URL from intent: $urlFromIntent")
            if (!urlFromIntent.isNullOrEmpty()) {
                displayWebView(urlFromIntent)
            } else {
                displayWebView(webViewUrl)
            }
        }
    }


    private val onConnect = Emitter.Listener {
        runOnUiThread {
            Log.d("SocketIO", "App connected")
//            Toast.makeText(this@MainActivity, "App connected", Toast.LENGTH_SHORT).show()

            // Emit register event with userId if it is not null
            if (userId != null) {
                socket?.emit("register", userId)
                Log.d("SocketIO", "Register event sent with userId: $userId")
//                Toast.makeText(this@MainActivity, "Register event sent with userId: $userId", Toast.LENGTH_SHORT).show()

                // After emitting register event, emit adClick event
                if (merchantId != null) {
                    socket?.emit("adClick", merchantId, userId)
                    Log.d("SocketIO", "adClick event sent with userWhoClicked: $userId, adOwnerId: $merchantId")
//                    Toast.makeText(this@MainActivity, "adClick event sent with userWhoClicked: $userId, adOwnerId: $merchantId", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("SocketIO", "MerchantId is null")
//                    Toast.makeText(this@MainActivity, "MerchantId is null", Toast.LENGTH_SHORT).show()
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
//                    Toast.makeText(this@MainActivity, notificationMessage, Toast.LENGTH_SHORT).show()
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
        socket?.disconnect()
        socket?.off(Socket.EVENT_CONNECT, onConnect)
        socket?.off("notification", onNotification)
        socket?.off(Socket.EVENT_CONNECT_ERROR, onConnectError)
        socket?.off(Socket.EVENT_DISCONNECT, onDisconnect)
    }



}