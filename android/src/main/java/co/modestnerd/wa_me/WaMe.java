package co.modestnerd.wa_me;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import android.content.ActivityNotFoundException;

public class WaMe implements FlutterPlugin, MethodCallHandler {
    private Context context;
    private MethodChannel methodChannel;

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        context = binding.getApplicationContext();
        methodChannel = new MethodChannel(binding.getBinaryMessenger(), "wa_me");
        methodChannel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(FlutterPluginBinding binding) {
        context = null;
        methodChannel.setMethodCallHandler(null);
        methodChannel = null;
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "shareFile":
                shareFile(call, result);
                break;
            case "share":
                share(call, result);
                break;
            case "isInstalled":
                isInstalled(call, result);
                break;
            default:
                result.notImplemented();
        }
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("", "Package not found: " + e.getMessage());
            return false;
        }
    }

    private void isInstalled(MethodCall call, Result result) {
        String packageName = call.argument("package");
        if (TextUtils.isEmpty(packageName)) {
            Log.e("", "WaMe: Package name is null or empty");
            result.error("WaMe: Package name cannot be null or empty", null, null);
            return;
        }

        Log.i("", packageName);

        boolean isInstalled = isPackageInstalled(packageName);
        result.success(isInstalled);
    }

    private void share(MethodCall call, Result result) {
        String title = call.argument("title");
        String text = call.argument("text");
        String linkUrl = call.argument("linkUrl");
        String chooserTitle = call.argument("chooserTitle");
        String phone = call.argument("phone");
        String packageName = call.argument("package");

        if (TextUtils.isEmpty(title)) {
            Log.e("", "WaMe: Title is null or empty");
            result.error("WaMe: Title cannot be null or empty", null, null);
            return;
        } else if (TextUtils.isEmpty(phone)) {
            Log.e("", "WaMe: Phone is null or empty");
            result.error("WaMe: Phone cannot be null or empty", null, null);
            return;
        } else if (TextUtils.isEmpty(packageName)) {
            Log.e("", "WaMe: Package name is null or empty");
            result.error("WaMe: Package name cannot be null or empty", null, null);
            return;
        } else if (TextUtils.isEmpty(text) && TextUtils.isEmpty(linkUrl)) {
            Log.e("", "WaMe: Text and linkUrl are null or empty");
            result.error("WaMe: Text and linkUrl cannot be null or empty", null, null);
            return;
        }

        ArrayList<String> extraTextList = new ArrayList<>();

        if (!TextUtils.isEmpty(text)) {
            extraTextList.add(text);
        }
        if (!TextUtils.isEmpty(linkUrl)) {
            extraTextList.add(linkUrl);
        }

        String extraText = TextUtils.join("\n\n", extraTextList);

        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.setPackage(packageName);
        intent.putExtra("jid",phone + "@s.whatsapp.net");
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.putExtra(Intent.EXTRA_TEXT, extraText);

        //Intent chooserIntent = Intent.createChooser(intent, chooserTitle);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
            result.success(true);
        } catch (ActivityNotFoundException ex) {
            Log.e("", "WaMe: No app available to handle the 'send' action");
            result.error("WaMe: No app available to handle the 'send' action", null, null);
        }  catch (Exception ex) {
            Log.e("", "WaMe: Error sharing message");
            result.error("WaMe: Error sharing message", null, null);
        }
    }

    private void shareFile(MethodCall call, Result result) {
        String text = call.argument("text");
        String filePath = call.argument("filePath");
        String phone = call.argument("phone");
        String packageName = call.argument("package");

        // log all the arguments
        Log.i("text", text);
        Log.i("phone", phone);
        Log.i("packageName", packageName);


        if (TextUtils.isEmpty(filePath)) {
            Log.e("", "WaMe: ShareLocalFile Error: filePath is null or empty");
            result.error("WaMe: FilePath cannot be null or empty", null, null);
            return;
        } else if (TextUtils.isEmpty(phone)) {
            Log.e("", "WaMe: Phone is null or empty");
            result.error("WaMe: Phone cannot be null or empty", null, null);
            return;
        } else if (TextUtils.isEmpty(packageName)) {
            Log.e("", "WaMe: Package name is null or empty");
            result.error("WaMe: Package name cannot be null or empty", null, null);
            return;
        } else if (TextUtils.isEmpty(text)) {
            Log.e("", "WaMe: Text is null or empty");
            result.error("WaMe: Text cannot be null or empty", null, null);
            return;
        }

        Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", new File(filePath));

        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_SEND);
        intent.setPackage(packageName);
        intent.putExtra("jid",phone + "@s.whatsapp.net");
        intent.putExtra(Intent.EXTRA_SUBJECT, text);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setType("image/png");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        //Intent chooserIntent = Intent.createChooser(intent, chooserTitle);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
            result.success(true);
        } catch (ActivityNotFoundException ex) {
            Log.e("", "WaMe: No app available to handle the 'send' action");
            result.error("WaMe: No app available to handle the 'send' action", null, null);
        } catch (Exception ex) {
            Log.e("", "WaMe: Error sharing file");
            result.error("WaMe: Error sharing file", null, null);
        }
    }
}
