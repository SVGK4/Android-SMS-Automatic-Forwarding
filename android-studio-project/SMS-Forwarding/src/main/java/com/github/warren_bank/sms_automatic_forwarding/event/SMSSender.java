package com.github.warren_bank.sms_automatic_forwarding.event;

import com.github.warren_bank.sms_automatic_forwarding.R;
import com.github.warren_bank.sms_automatic_forwarding.utils.PreferencesMgr;

import android.content.Context;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;
import java.util.ArrayList;

public final class SMSSender {
  private static final String TAG = "SMSSender";

  public static void forward(Context context, ArrayList<String> recipients, String sender_phone_number, String sender_contact_name, String body) {
    SmsManager sms = null;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      sms = context.getSystemService(SmsManager.class);
    }
    if (sms == null) {
      sms = SmsManager.getDefault();
    }
    if (sms == null) {
      Log.e(TAG, "SmsManager is not available on this device.");
      return;
    }

    String sender = context.getString(R.string.sms_sender_heading);
    if ((sender_contact_name != null) && !sender_contact_name.isEmpty()) {
      sender += "\n  " + sender_contact_name;
    }
    sender += "\n  " + sender_phone_number;

    String template = PreferencesMgr.get_template(context);
    String message  = template.equals(context.getString(R.string.pref_template_value_prefix))
      ? (sender + "\n\n" + body)
      : (body   + "\n\n" + sender);

    ArrayList<String> parts = sms.divideMessage(message);

    Log.i(TAG, "Forwarding SMS to " + recipients.size() + " recipient(s)...");

    String recipient;
    for (int i=0; i < recipients.size(); i++) {
      try {
        recipient = recipients.get(i);
        sms.sendMultipartTextMessage(recipient, null, parts, null, null);
        Log.i(TAG, "SMS successfully forwarded to: " + recipient);
      }
      catch(Exception e) {
        Log.e(TAG, "Error forwarding SMS to recipient: " + recipients.get(i), e);
      }
    }
  }
}
