package co.modestnerd.wa_me

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File

class WaMe : FlutterPlugin, MethodCallHandler {
    private var context: Context? = null
    private var methodChannel: MethodChannel? = null

    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        context = binding.applicationContext
        methodChannel = MethodChannel(binding.binaryMessenger, "wa_me")
        methodChannel?.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        context = null
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "shareFile" -> shareFile(call, result)
            "share" -> share(call, result)
            "isInstalled" -> isInstalled(call, result)
            else -> result.notImplemented()
        }
    }

    private fun sanitizePhone(phone: String?): String {
        if (phone == null) return ""
        var cleaned = phone.replace("[^0-9]".toRegex(), "")
        while (cleaned.startsWith("00")) {
            cleaned = cleaned.substring(2)
        }
        return cleaned
    }

    private fun getMimeType(filePath: String): String {
        val file = File(filePath)
        val extension = file.extension
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            val pm = context?.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm?.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm?.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("WaMe", "Package not found: ${e.message}")
            false
        } catch (e: Exception) {
            Log.d("WaMe", "Error checking package: ${e.message}")
            false
        }
    }

    private fun isInstalled(call: MethodCall, result: Result) {
        val packageName = call.argument<String>("package")
        if (packageName.isNullOrEmpty()) {
            Log.e("WaMe", "WaMe: Package name is null or empty")
            result.error("WaMe: Package name cannot be null or empty", null, null)
            return
        }

        val isInstalled = isPackageInstalled(packageName)
        result.success(isInstalled)
    }

    private fun share(call: MethodCall, result: Result) {
        val text = call.argument<String>("text")
        val linkUrl = call.argument<String>("linkUrl")
        val phone = call.argument<String>("phone")
        val packageName = call.argument<String>("package")

        if (phone.isNullOrEmpty()) {
            Log.e("WaMe", "WaMe: Phone is null or empty")
            result.error("WaMe: Phone cannot be null or empty", null, null)
            return
        } else if (packageName.isNullOrEmpty()) {
            Log.e("WaMe", "WaMe: Package name is null or empty")
            result.error("WaMe: Package name cannot be null or empty", null, null)
            return
        }

        val extraTextList = mutableListOf<String>()
        text?.let { if (it.isNotEmpty()) extraTextList.add(it) }
        linkUrl?.let { if (it.isNotEmpty()) extraTextList.add(it) }
        val extraText = extraTextList.joinToString("\n\n")

        val sanitizedPhone = sanitizePhone(phone)
        val encodedText = if (extraText.isNotEmpty()) Uri.encode(extraText) else ""

        // Multi-tier URI strategy:
        // 1. whatsapp://send - native deep link, works reliably with unsaved numbers across all WhatsApp variants
        // 2. https://api.whatsapp.com/send - universal endpoint supported by all WhatsApp packages
        // 3. https://wa.me - official web link supported by WhatsApp Messenger
        val whatsappUri = if (encodedText.isNotEmpty()) {
            Uri.parse("whatsapp://send?phone=$sanitizedPhone&text=$encodedText")
        } else {
            Uri.parse("whatsapp://send?phone=$sanitizedPhone")
        }

        val apiUri = if (encodedText.isNotEmpty()) {
            Uri.parse("https://api.whatsapp.com/send?phone=$sanitizedPhone&text=$encodedText")
        } else {
            Uri.parse("https://api.whatsapp.com/send?phone=$sanitizedPhone")
        }

        val waMeUri = if (encodedText.isNotEmpty()) {
            Uri.parse("https://wa.me/$sanitizedPhone?text=$encodedText")
        } else {
            Uri.parse("https://wa.me/$sanitizedPhone")
        }

        val urisToTry = listOf(whatsappUri, apiUri, waMeUri)
        var launched = false

        for (uri in urisToTry) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = uri
                    setPackage(packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context?.startActivity(intent)
                launched = true
                break
            } catch (ex: Exception) {
                Log.d("WaMe", "Attempt with $uri for package $packageName failed: ${ex.message}")
            }
        }

        if (!launched) {
            // Fallback without restricting package (opens system app chooser / default handler)
            for (uri in listOf(apiUri, waMeUri)) {
                try {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = uri
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context?.startActivity(fallbackIntent)
                    launched = true
                    break
                } catch (ex: Exception) {
                    Log.d("WaMe", "Unrestricted fallback with $uri failed: ${ex.message}")
                }
            }
        }

        if (launched) {
            result.success(true)
        } else {
            Log.e("WaMe", "WaMe: No app available to handle the 'send' action")
            result.error("WaMe: No app available to handle the 'send' action", null, null)
        }
    }

    private fun shareFile(call: MethodCall, result: Result) {
        val text = call.argument<String>("text")
        val filePath = call.argument<String>("filePath")
        val phone = call.argument<String>("phone")
        val packageName = call.argument<String>("package")

        if (filePath.isNullOrEmpty()) {
            Log.e("WaMe", "WaMe: ShareLocalFile Error: filePath is null or empty")
            result.error("WaMe: FilePath cannot be null or empty", null, null)
            return
        } else if (phone.isNullOrEmpty()) {
            Log.e("WaMe", "WaMe: Phone is null or empty")
            result.error("WaMe: Phone cannot be null or empty", null, null)
            return
        } else if (packageName.isNullOrEmpty()) {
            Log.e("WaMe", "WaMe: Package name is null or empty")
            result.error("WaMe: Package name cannot be null or empty", null, null)
            return
        }

        val ctx = context ?: return
        val file = File(filePath)
        if (!file.exists()) {
            result.error("WaMe: File does not exist at $filePath", null, null)
            return
        }

        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".provider", file)
        val sanitizedPhone = sanitizePhone(phone)
        val mimeType = getMimeType(filePath)

        // Grant URI read permission to WhatsApp explicitly
        ctx.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Standard ACTION_SEND with jid
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage(packageName)
            type = mimeType
            putExtra("jid", "$sanitizedPhone@s.whatsapp.net")
            putExtra(Intent.EXTRA_TEXT, text ?: "")
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            ctx.startActivity(intent)
            result.success(true)
        } catch (ex: Exception) {
            Log.d("WaMe", "Primary shareFile failed: ${ex.message}, trying ACTION_SENDTO...")

            // Fallback: ACTION_SENDTO with smsto: URI
            val sendToIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$sanitizedPhone")
                setPackage(packageName)
                putExtra("jid", "$sanitizedPhone@s.whatsapp.net")
                putExtra("sms_body", text ?: "")
                putExtra(Intent.EXTRA_TEXT, text ?: "")
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                ctx.startActivity(sendToIntent)
                result.success(true)
            } catch (ex2: Exception) {
                Log.d("WaMe", "Fallback 1 failed: ${ex2.message}, trying plain ACTION_SEND...")

                // Final fallback: plain ACTION_SEND without jid restriction
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    setPackage(packageName)
                    type = mimeType
                    putExtra(Intent.EXTRA_TEXT, text ?: "")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newRawUri("", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    ctx.startActivity(fallbackIntent)
                    result.success(true)
                } catch (ex3: ActivityNotFoundException) {
                    Log.e("WaMe", "No app available to handle the 'send' action")
                    result.error("WaMe: No app available to handle the 'send' action", null, null)
                } catch (ex3: Exception) {
                    Log.e("WaMe", "Error sharing file: ${ex3.message}")
                    result.error("WaMe: Error sharing file", null, null)
                }
            }
        }
    }
}
