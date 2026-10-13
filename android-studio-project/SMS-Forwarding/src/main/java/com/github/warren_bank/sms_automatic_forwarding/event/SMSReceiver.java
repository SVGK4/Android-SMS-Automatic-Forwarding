package com.github.warren_bank.sms_automatic_forwarding.event;

import com.github.warren_bank.sms_automatic_forwarding.R;
import com.github.warren_bank.sms_automatic_forwarding.data_model.Contacts;
import com.github.warren_bank.sms_automatic_forwarding.data_model.Preferences;
import com.github.warren_bank.sms_automatic_forwarding.data_model.RecipientListItem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import java.util.ArrayList;

public class SMSReceiver extends BroadcastReceiver {
  private static final String TAG               = "SMSReceiver";
  private static final String SMS_RECEIVED      = "android.provider.Telephony.SMS_RECEIVED";
  private static final String WAP_PUSH_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.i(TAG, "SMSReceiver onReceive triggered: " + (intent != null ? intent.getAction() : "null"));

    if (!Preferences.isEnabled(context)) {
      Log.i(TAG, "SMS received but forwarding service is disabled in settings.");
      return;
    }

    if (intent == null)
      return;

    final String action = intent.getAction();
    final Bundle extras = intent.getExtras();

    if (extras == null) {
      Log.w(TAG, "SMS received but intent extras are null.");
      return;
    }

    final ArrayList<RecipientListItem> listItems = Preferences.getRecipientListItems(context);

    if (listItems.isEmpty()) {
      Log.i(TAG, "SMS received but recipient forwarding list is empty.");
      return;
    }

    if (SMS_RECEIVED.equals(action) || WAP_PUSH_RECEIVED.equals(action)) {
      final SmsMessage[] messages = get_SmsMessages(intent, extras);

      if (messages.length == 0) {
        Log.w(TAG, "SMS received but no SMS messages could be extracted from intent.");
        return;
      }

      String sender_phone_number   = null;
      ArrayList<String> recipients = null;
      String body                  = null;
      String sender_contact_name   = null;

      for (SmsMessage message : messages) {
        if (message == null)
          continue;

        try {
          if (message.getOriginatingAddress() != null) {
            sender_phone_number = message.getOriginatingAddress().trim();
            body                = message.getMessageBody();

            if (body != null) {
              body = body.trim();

              if (!body.isEmpty()) {
                recipients = RecipientListItem.match(listItems, sender_phone_number, body);

                if (!recipients.isEmpty()) {
                  Log.i(TAG, "SMS received from: " + sender_phone_number + "\nmessage: " + body);

                  sender_contact_name = Contacts.getContactName(context, sender_phone_number);

                  final PendingResult pendingResult = goAsync();
                  final String finalSenderPhone = sender_phone_number;
                  final String finalContactName = sender_contact_name;
                  final String finalBody = body;
                  final ArrayList<String> finalRecipients = recipients;

                  new Thread(new Runnable() {
                    @Override
                    public void run() {
                      try {
                        SMSSender.forward(context, finalRecipients, finalSenderPhone, finalContactName, finalBody);
                      } catch (Exception e) {
                        Log.e(TAG, "Error forwarding SMS in background thread", e);
                      } finally {
                        pendingResult.finish();
                      }
                    }
                  }).start();
                } else {
                  Log.i(TAG, "SMS received from " + sender_phone_number + " but did not match sender or text filter criteria.");
                }
              }
            }
          }
        }
        catch (Exception e) {
          Log.e(TAG, "Error processing incoming SMS message", e);
        }
      }
    }
  }

  private final static SmsMessage[] get_SmsMessages(Intent intent, Bundle extras) {
    if (Build.VERSION.SDK_INT >= 19 && intent != null) {
      try {
        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages != null && messages.length > 0) {
          return messages;
        }
      } catch (Exception e) {
        Log.e(TAG, "Error parsing SMS via Telephony API", e);
      }
    }
    if (extras == null) return new SmsMessage[0];
    final Object[] pdus = (Object[]) extras.get("pdus");
    if (pdus == null) return new SmsMessage[0];
    final String format = extras.getString("format", "3gpp");
    final SmsMessage[] messages = new SmsMessage[pdus.length];

    for (int i = 0; i < pdus.length; i++) {
      try {
        messages[i] = (Build.VERSION.SDK_INT >= 23)
          ? SmsMessage.createFromPdu((byte[]) pdus[i], format)
          : SmsMessage.createFromPdu((byte[]) pdus[i]);
      }
      catch (Exception e) {}
    }
    return messages;
  }
}
