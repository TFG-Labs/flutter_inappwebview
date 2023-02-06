package com.pichillilorenzo.flutter_inappwebview.chrome_custom_tabs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.pichillilorenzo.flutter_inappwebview.InAppWebViewFlutterPlugin;
import com.pichillilorenzo.flutter_inappwebview.Util;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import android.util.Log;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class ChromeSafariBrowserManager implements MethodChannel.MethodCallHandler {

  protected static final String LOG_TAG = "ChromeBrowserManager";
  public MethodChannel channel;
  @Nullable
  public InAppWebViewFlutterPlugin plugin;
  public String id;
  public static final Map<String, ChromeSafariBrowserManager> shared = new HashMap<>();

  public ChromeSafariBrowserManager(final InAppWebViewFlutterPlugin plugin) {
    this.id = UUID.randomUUID().toString();
    this.plugin = plugin;
    channel = new MethodChannel(plugin.messenger, "com.pichillilorenzo/flutter_chromesafaribrowser");
    channel.setMethodCallHandler(this);
    shared.put(this.id, this);
  }

  @Override
  public void onMethodCall(final MethodCall call, final MethodChannel.Result result) {
    final String id = (String) call.argument("id");

    switch (call.method) {
      case "open":
        if (plugin != null && plugin.activity != null) {
          String url = (String) call.argument("url");
          HashMap<String, Object> options = (HashMap<String, Object>) call.argument("options");
          HashMap<String, Object> actionButton = (HashMap<String, Object>) call.argument("actionButton");
          List<HashMap<String, Object>> menuItemList = (List<HashMap<String, Object>>) call.argument("menuItemList");
          open(plugin.activity, id, url, options, actionButton, menuItemList, result);
        } else {
          result.success(false);
        }
        break;
      case "isAvailable":
        if (plugin != null && plugin.activity != null) {
          result.success(CustomTabActivityHelper.isAvailable(plugin.activity));
        } else {
          result.success(false);
        }
        break;
      case "close":
        if (plugin != null && plugin.activity != null) {
          if(plugin.activity instanceof Activity){
            Activity activity = (Activity) plugin.activity;
            activity.finish();
            result.success(true);
          }
        }
        break;
      default:
        result.notImplemented();
    }
  }

  public void open(Activity activity, String id, String url, HashMap<String, Object> options,
                   HashMap<String, Object> actionButton,
                   List<HashMap<String, Object>> menuItemList, MethodChannel.Result result) {

    Intent intent = null;
    Bundle extras = new Bundle();
    extras.putString("url", url);
    extras.putBoolean("isData", false);
    extras.putString("id", id);
    extras.putString("managerId", this.id);
    extras.putSerializable("options", options);
    extras.putSerializable("actionButton", (Serializable) actionButton);
    extras.putSerializable("menuItemList", (Serializable) menuItemList);

    Boolean isSingleInstance = (Boolean) Util.getOrDefault(options, "isSingleInstance", false);
    Boolean isTrustedWebActivity = (Boolean) Util.getOrDefault(options, "isTrustedWebActivity", false);
    if (CustomTabActivityHelper.isAvailable(activity)) {
      intent = new Intent(activity, !isSingleInstance ? 
              (!isTrustedWebActivity ? ChromeCustomTabsActivity.class : TrustedWebActivity.class) :
              (!isTrustedWebActivity ? ChromeCustomTabsActivitySingleInstance.class : TrustedWebActivitySingleInstance.class));
      intent.putExtras(extras);
      Boolean noHistory = (Boolean) Util.getOrDefault(options, "noHistory", false);
      if (noHistory) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
      }
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      activity.startActivity(intent);
      result.success(true);
      return;
    }else{
      intent = new Intent(Intent.ACTION_VIEW);
      intent.setData(Uri.parse(url));
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      activity.startActivity(intent);
      result.success(true);
      return;
    }
  }

  public void dispose() {
    channel.setMethodCallHandler(null);
    shared.remove(this.id);
    plugin = null;
  }
}
