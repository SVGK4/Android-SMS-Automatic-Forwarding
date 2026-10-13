package com.github.warren_bank.sms_automatic_forwarding.security_model;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.ArrayList;

public final class RuntimePermissions {
  private static final int REQUEST_CODE = 0;

  static String[] getMissingPermissions(Activity activity, String[] permissions_all) {
    ArrayList<String> permissions_req = new ArrayList<>();

    for (String permission_name : permissions_all) {
      try {
        if (activity.checkSelfPermission(permission_name) != PackageManager.PERMISSION_GRANTED) {
          permissions_req.add(permission_name);
        }
      } catch (Exception e) {
        permissions_req.add(permission_name);
      }
    }

    return permissions_req.isEmpty()
      ? null
      : permissions_req.toArray(new String[0]);
  }

  public static boolean isEnabled(Activity activity) {
    if (Build.VERSION.SDK_INT < 23)
      return true;

    ArrayList<String> perms = new ArrayList<>();
    perms.add("android.permission.RECEIVE_SMS");
    perms.add("android.permission.SEND_SMS");
    perms.add("android.permission.READ_SMS");
    perms.add("android.permission.RECEIVE_MMS");
    perms.add("android.permission.RECEIVE_WAP_PUSH");
    perms.add("android.permission.READ_PHONE_STATE");
    if (Build.VERSION.SDK_INT >= 33) {
      perms.add("android.permission.POST_NOTIFICATIONS");
    }
    final String[] permissions_all = perms.toArray(new String[0]);
    final String[] permissions_req = RuntimePermissions.getMissingPermissions(activity, permissions_all);

    if (permissions_req == null)
      return true;

    activity.requestPermissions(permissions_req, REQUEST_CODE);
    return false;
  }

  public static void onRequestPermissionsResult (Activity activity, int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode != REQUEST_CODE)
      return;

    if (grantResults.length == 0)
      return;

    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) return;
    }

    activity.recreate();
  }
}
