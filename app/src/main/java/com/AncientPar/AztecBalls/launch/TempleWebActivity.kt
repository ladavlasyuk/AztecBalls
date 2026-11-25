package com.AncientPar.AztecBalls.launch

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.AncientPar.AztecBalls.R

class TempleWebActivity : ComponentActivity() {

    companion object {
        const val START_ADDRESS = "temple_start_address"
        private const val SELECT_DOCUMENTS = 4829
        private const val ALLOW_DOWNLOAD = 4830
    }

    private lateinit var webPane: WebView
    private lateinit var loadingIndicator: ProgressBar
    private var uploadResult: ValueCallback<Array<Uri>>? = null
    private var awaitingFirstPage = true
    private data class PendingDownload(val href: String, val agent: String, val disposition: String, val mime: String)
    private var pendingFile: PendingDownload? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

        val initialUrl = intent.getStringExtra(START_ADDRESS) ?: run {
            finish()
            return
        }

        createBrowserLayout()
        attachBrowserHandlers()
        displayAddress(initialUrl)
        handleBackNavigation()
    }

    private fun createBrowserLayout() {
        val windowColor = ContextCompat.getColor(this, R.color.temple_window_color)
        val pageColor = ContextCompat.getColor(this, R.color.temple_page_color)
        val indicatorColor = ContextCompat.getColor(this, R.color.temple_progress_color)

        val container = RelativeLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(windowColor)
        }

        webPane = WebView(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            id = View.generateViewId()
            setBackgroundColor(pageColor)
        }

        loadingIndicator = ProgressBar(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
            indeterminateTintList = ColorStateList.valueOf(indicatorColor)
            visibility = View.VISIBLE
        }

        container.addView(webPane)
        container.addView(loadingIndicator)
        setContentView(container)

        hideSystemBars()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun attachBrowserHandlers() {
        configureWebPane(webPane)
        webPane.setDownloadListener { href, agent, disposition, mime, _ ->
            requestDownload(href, agent, disposition, mime)
        }

        webPane.webViewClient = navigationClient()
        webPane.webChromeClient = documentClient()
    }

    private fun navigationClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (awaitingFirstPage) {
                    loadingIndicator.visibility = View.VISIBLE
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (awaitingFirstPage) {
                    awaitingFirstPage = false
                    loadingIndicator.visibility = View.GONE
                }
                CookieManager.getInstance().flush()
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                CookieManager.getInstance().flush()
                super.doUpdateVisitedHistory(view, url, isReload)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme ?: return false

                if (scheme in listOf("http", "https")) {
                    return false
                }

                return try {
                    val externalIntent = if (scheme == "intent") {
                        Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                    } else {
                        Intent(Intent.ACTION_VIEW, uri)
                    }

                    openExternalTarget(view?.context ?: return true, externalIntent)
                    true
                } catch (_: Exception) {
                    true
                }
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                if (!isFinishing && !isDestroyed) {
                    recreate()
                }
                return true
            }
        }
    }

    private fun documentClient(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                uploadResult?.onReceiveValue(null)
                uploadResult = callback

                val acceptedFormats = params?.acceptTypes ?: arrayOf("*/*")
                val mimeType = acceptedFormats.firstOrNull()?.takeIf { it.isNotEmpty() } ?: "*/*"

                val documentPicker = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = mimeType
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }

                return try {
                    @Suppress("DEPRECATION")
                    startActivityForResult(documentPicker, SELECT_DOCUMENTS)
                    true
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(this@TempleWebActivity, "File manager not found", Toast.LENGTH_SHORT).show()
                    callback?.onReceiveValue(null)
                    uploadResult = null
                    false
                }
            }
        }
    }

    private fun displayAddress(href: String) {
        webPane.loadUrl(href)
    }

    private fun handleBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webPane.canGoBack()) {
                    webPane.goBack()
                }
            }
        })
    }

    private fun requestDownload(href: String, agent: String, disposition: String, mime: String) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingFile = PendingDownload(href, agent, disposition, mime)
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), ALLOW_DOWNLOAD)
            return
        }
        enqueueDownload(href, agent, disposition, mime)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ALLOW_DOWNLOAD) {
            val pending = pendingFile
            pendingFile = null
            if (pending != null && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                enqueueDownload(pending.href, pending.agent, pending.disposition, pending.mime)
            } else {
                Toast.makeText(this, "Storage permission is needed to save this file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enqueueDownload(
        downloadHref: String,
        agent: String,
        disposition: String,
        mimeType: String
    ) {
        try {
            val request = DownloadManager.Request(Uri.parse(downloadHref))
            request.setMimeType(mimeType)

            val cookies = CookieManager.getInstance().getCookie(downloadHref)
            if (!cookies.isNullOrEmpty()) {
                request.addRequestHeader("Cookie", cookies)
            }
            request.addRequestHeader("User-Agent", agent)

            val fileName = URLUtil.guessFileName(downloadHref, disposition, mimeType)
            request.setTitle(fileName)
            request.setDescription("Downloading file...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "Download started: $fileName", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                android.util.Log.e("TempleWeb", "Download request failed", error)
            }
            Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExternalTarget(context: Context, intent: Intent): Boolean {
        return try {
            if (context !is ComponentActivity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SELECT_DOCUMENTS) {
            completeSelection(resultCode, data)
        }
    }

    private fun completeSelection(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            val selectedFiles = mutableListOf<Uri>()

            data?.data?.let { uri -> selectedFiles.add(uri) }

            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uri -> selectedFiles.add(uri) }
                }
            }

            uploadResult?.onReceiveValue(selectedFiles.toTypedArray())
        } else {
            uploadResult?.onReceiveValue(null)
        }

        uploadResult = null
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }
}
