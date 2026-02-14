package co.modestnerd.wa_me

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
        return phone.replace("[^0-9]".toRegex(), "")
    }

    private fun getMimeType(filePath: String): String {
        val file = File(filePath)
        val extension = file.extension
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context?.packageManager?.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("", "Package not found: ${e.message}")
            false
        }
    }

    private fun isInstalled(call: MethodCall, result: Result) {
        val packageName = call.argument<String>("package")
        if (packageName.isNullOrEmpty()) {
            Log.e("", "WaMe: Package name is null or empty")
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
            Log.e("", "WaMe: Phone is null or empty")
            result.error("WaMe: Phone cannot be null or empty", null, null)
            return
        } else if (packageName.isNullOrEmpty()) {
            Log.e("", "WaMe: Package name is null or empty")
            result.error("WaMe: Package name cannot be null or empty", null, null)
            return
        }

        val extraTextList = mutableListOf<String>()
        text?.let { if (it.isNotEmpty()) extraTextList.add(it) }
        linkUrl?.let { if (it.isNotEmpty()) extraTextList.add(it) }
        val extraText = extraTextList.joinToString("\n\n")

        val sanitizedPhone = sanitizePhone(phone)
        
        // Use ACTION_VIEW with wa.me for better reliability with unsaved contacts
        val url = if (extraText.isNotEmpty()) {
            "https://wa.me/$sanitizedPhone?text=${Uri.encode(extraText)}"
        } else {
            "https://wa.me/$sanitizedPhone"
        }
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context?.startActivity(intent)
            result.success(true)
        } catch (ex: ActivityNotFoundException) {
            Log.e("", "WaMe: No app available to handle the 'send' action")
            result.error("WaMe: No app available to handle the 'send' action", null, null)
        } catch (ex: Exception) {
            Log.e("", "WaMe: Error sharing message: ${ex.message}")
            result.error("WaMe: Error sharing message", null, null)
        }
    }

    private fun shareFile(call: MethodCall, result: Result) {
        val text = call.argument<String>("text")
        val filePath = call.argument<String>("filePath")
        val phone = call.argument<String>("phone")
        val packageName = call.argument<String>("package")

        if (filePath.isNullOrEmpty()) {
            Log.e("", "WaMe: ShareLocalFile Error: filePath is null or empty")
            result.error("WaMe: FilePath cannot be null or empty", null, null)
            return
        } else if (phone.isNullOrEmpty()) {
            Log.e("", "WaMe: Phone is null or empty")
            result.error("WaMe: Phone cannot be null or empty", null, null)
            return
        } else if (packageName.isNullOrEmpty()) {
            Log.e("", "WaMe: Package name is null or empty")
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

        // Use ACTION_SEND with setDataAndType to combine the wa.me deep link
        // (for phone number routing) with the MIME type (for file handling).
        // This tells WhatsApp both WHO to send to and WHAT to send.
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage(packageName)
            setDataAndType(Uri.parse("https://api.whatsapp.com/send?phone=$sanitizedPhone"), mimeType)
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
            Log.d("WaMe", "Approach 1 failed: ${ex.message}, trying ACTION_SENDTO...")

            // Fallback: ACTION_SENDTO with smsto: URI
            // WhatsApp registers as an SMS/MMS handler, and smsto: routes
            // directly to a phone number without needing a saved contact.
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
                Log.d("WaMe", "Approach 2 failed: ${ex2.message}, trying plain ACTION_SEND...")

                // Final fallback: plain ACTION_SEND (will show contact picker)
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    setPackage(packageName)
                    putExtra("jid", "$sanitizedPhone@s.whatsapp.net")
                    putExtra(Intent.EXTRA_TEXT, text ?: "")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = mimeType
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
