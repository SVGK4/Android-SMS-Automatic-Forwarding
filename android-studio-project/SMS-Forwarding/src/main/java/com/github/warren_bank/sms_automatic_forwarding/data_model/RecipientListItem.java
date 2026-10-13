package com.github.warren_bank.sms_automatic_forwarding.data_model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public final class RecipientListItem {
  public String recipient;
  public String sender;
  public String textFilter;

  public RecipientListItem() {
    this.recipient  = "";
    this.sender     = "";
    this.textFilter = "";
  }

  public RecipientListItem(String recipient, String sender) {
    this.recipient  = recipient;
    this.sender     = sender;
    this.textFilter = "";
  }

  public RecipientListItem(String recipient, String sender, String textFilter) {
    this.recipient  = recipient;
    this.sender     = sender;
    this.textFilter = (textFilter == null) ? "" : textFilter;
  }

  @Override
  public String toString() {
    return recipient;
  }

  // helpers

  public static ArrayList<RecipientListItem> fromJson(String json) {
    ArrayList<RecipientListItem> arrayList;
    Gson gson = new Gson();
    arrayList = gson.fromJson(json, new TypeToken<ArrayList<RecipientListItem>>(){}.getType());
    return arrayList;
  }

  public static String toJson(ArrayList<RecipientListItem> arrayList) {
    String json = new Gson().toJson(arrayList);
    return json;
  }

  public static ArrayList<String> match(ArrayList<RecipientListItem> arrayList, String sender) {
    return match(arrayList, sender, null);
  }

  public static ArrayList<String> match(ArrayList<RecipientListItem> arrayList, String sender, String body) {
    ArrayList<String> allRecipients = new ArrayList<String>();
    RecipientListItem item;
    RecipientListItemCriteria criteria;

    for (int i=0; i < arrayList.size(); i++) {
      try {
        item = arrayList.get(i);
        criteria = new RecipientListItemCriteria(item);

        criteria.match(sender, body, allRecipients);
      }
      catch(Exception e) { continue; }
    }

    allRecipients = sanitizeMatches(sender, allRecipients);
    return allRecipients;
  }

  private static ArrayList<String> sanitizeMatches(String sender, ArrayList<String> allRecipients) {
    // remove duplicates from recipients list:
    HashSet<String> uniqueRecipients = new HashSet<String>(allRecipients);
    allRecipients = new ArrayList<String>(uniqueRecipients);

    // remove sender from recipients list:
    allRecipients.remove(sender);

    return allRecipients;
  }

  /*
   * ---------------------------------------------------------------------------
   * Parser that supports more advanced values in 'recipient' and 'sender' fields.
   * Fully backwards compatible with older single-value field values.
   * ---------------------------------------------------------------------------
   */

  private static class RecipientListItemCriteria {
    private String[] recipients;
    private String[] sender_whitelist;
    private String[] sender_blacklist;
    private String textFilter;

    private static String[] splitCSV(String list) {
      return splitCSV(list, /* delimiter */ "[,\\n]+");
    }

    private static String[] splitCSV(String list, String delimiter) {
      return splitCSV(list, delimiter, /* limit */ 0);
    }

    private static String[] splitCSV(String list, String delimiter, int limit) {
      if (list == null) return null;

      String regex = "\\s*(?:" + delimiter + ")\\s*";
      return list.trim().split(regex, limit);
    }

    private static void sanitizeInput(String[] input, boolean allowGlobPattern) {
      if ((input == null) || (input.length == 0)) return;

      if (allowGlobPattern && (input.length == 1) && ("*".equals(input[0]))) return;

      for (int i=0; i < input.length; i++) {
        input[i] = sanitizeInput(input[i]);
      }
    }

    private static String sanitizeInput(String input) {
      if ((input == null) || input.isEmpty()) return "";

      String prefix = (input.charAt(0) == '+')
        ? "+"
        : "";

      String numeric = input.replaceAll("[^0-9]", "");

      return prefix + numeric;
    }

    public RecipientListItemCriteria(RecipientListItem item) {
      this.textFilter = item.textFilter;
      parseRecipient(item.recipient);
      parseSender(item.sender);

      sanitizeInput(this.recipients,       false);
      sanitizeInput(this.sender_whitelist, true);
      sanitizeInput(this.sender_blacklist, false);
    }

    private void parseRecipient(String recipient) {
      this.recipients = splitCSV(recipient);
    }

    private void parseSender(String sender) {
      String[] parts = splitCSV(sender, "[!/#]+|[\\n]{2,}", 2);

      if (parts == null)
        return;
      if (parts.length > 0) {
        String whitelist = parts[0];
        if (!whitelist.isEmpty()) {
          ArrayList<String> wl_list = new ArrayList<String>();
          String[] wl_array = splitCSV(whitelist);
          for (String wl_value : wl_array) {
            if (wl_value.isEmpty())
              continue;
            if (wl_value.equals("*")) {
              wl_list.clear();
              wl_list.add(wl_value);
              break;
            }
            wl_value = wl_value.replace("*", "");
            wl_list.add(wl_value);
          }
          this.sender_whitelist = wl_list.toArray(new String[0]);
        }
      }
      if (parts.length > 1) {
        String blacklist = parts[1];
        if (!blacklist.isEmpty()) {
          this.sender_blacklist = splitCSV(blacklist);
        }
      }
    }

    // mutates 'allRecipients' list
    private void match(String sender, String body, ArrayList<String> allRecipients) {
      if ((this.recipients == null) || (this.sender_whitelist == null)) return;

      // Check text filter if specified:
      if (this.textFilter != null && !this.textFilter.trim().isEmpty()) {
        if (body == null || !body.toLowerCase().contains(this.textFilter.trim().toLowerCase())) {
          return;
        }
      }

      String sanitizedSender = sanitizeInput(sender);

      // is sender in blacklist?
      if (this.sender_blacklist != null) {
        for (String bl_value : this.sender_blacklist) {
          if ((sender != null && sender.endsWith(bl_value)) || (!sanitizedSender.isEmpty() && sanitizedSender.endsWith(bl_value))) {
            return;
          }
        }
      }

      // is sender in whitelist?
      for (String wl_value : this.sender_whitelist) {
        if (wl_value.equals("*") || (sender != null && sender.endsWith(wl_value)) || (!sanitizedSender.isEmpty() && sanitizedSender.endsWith(wl_value))) {
          allRecipients.addAll(
            Arrays.asList(this.recipients)
          );
          return;
        }
      }
    }
  }

}
