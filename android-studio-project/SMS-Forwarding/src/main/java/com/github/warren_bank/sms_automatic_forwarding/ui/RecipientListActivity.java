package com.github.warren_bank.sms_automatic_forwarding.ui;

import com.github.warren_bank.sms_automatic_forwarding.R;
import com.github.warren_bank.sms_automatic_forwarding.data_model.Preferences;
import com.github.warren_bank.sms_automatic_forwarding.data_model.RecipientListItem;
import com.github.warren_bank.sms_automatic_forwarding.security_model.OptionalRuntimePermissions;
import com.github.warren_bank.sms_automatic_forwarding.security_model.RuntimePermissions;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import java.util.ArrayList;

public class RecipientListActivity extends Activity {
  private CheckBox inputEnable;
  private ListView listView;
  private View emptyView;

  private ArrayList<RecipientListItem>    listItems;
  private RecipientListAdapter            listAdapter;

  // ---------------------------------------------------------------------------------------------
  // Custom List Adapter:
  // ---------------------------------------------------------------------------------------------

  private class RecipientListAdapter extends ArrayAdapter<RecipientListItem> {
    public RecipientListAdapter(Context context, ArrayList<RecipientListItem> items) {
      super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_recipient, parent, false);
      }
      RecipientListItem item = getItem(position);
      if (item != null) {
        TextView textRecipient = (TextView) convertView.findViewById(R.id.text_recipient);
        TextView textSenderFilter = (TextView) convertView.findViewById(R.id.text_sender_filter);
        TextView textBodyFilter = (TextView) convertView.findViewById(R.id.text_body_filter);

        textRecipient.setText(item.recipient);

        String senderFilter = (item.sender == null || item.sender.trim().isEmpty()) ? "*" : item.sender.trim();
        textSenderFilter.setText(getString(R.string.sender_filter_prefix, senderFilter));

        if (item.textFilter != null && !item.textFilter.trim().isEmpty()) {
          textBodyFilter.setVisibility(View.VISIBLE);
          textBodyFilter.setText(getString(R.string.text_filter_prefix, item.textFilter.trim()));
        } else {
          textBodyFilter.setVisibility(View.GONE);
        }
      }
      return convertView;
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Lifecycle Events:
  // ---------------------------------------------------------------------------------------------

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_recipient_list);

    View mainLayout = findViewById(R.id.main_layout);
    if (mainLayout != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      mainLayout.setFitsSystemWindows(true);
    }

    inputEnable = (CheckBox) findViewById(R.id.input_enable);
    listView    = (ListView) findViewById(R.id.listview);
    emptyView   = findViewById(R.id.empty_view);

    if (emptyView != null) {
      listView.setEmptyView(emptyView);
    }

    listItems   = Preferences.getRecipientListItems(RecipientListActivity.this);
    listAdapter = new RecipientListAdapter(RecipientListActivity.this, listItems);
    listView.setAdapter(listAdapter);

    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        showEditDialog(position);
      }
    });

    updateEnableToggle();
  }

  @Override
  protected void onResume() {
    super.onResume();
    updateEnableToggle();
  }

  private void updateEnableToggle() {
    if (RuntimePermissions.isEnabled(RecipientListActivity.this)) {
      OptionalRuntimePermissions.requestPermissions(RecipientListActivity.this);
      requestBatteryOptimizationExemption();

      boolean isEnabled = Preferences.isEnabled(RecipientListActivity.this);

      inputEnable.setOnCheckedChangeListener(null);
      inputEnable.setChecked(isEnabled);
      inputEnable.setEnabled(true);

      Log.i("RecipientListActivity", "Automatic SMS Forwarding state: " + (isEnabled ? "ENABLED" : "DISABLED"));

      inputEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          Preferences.setEnabled(RecipientListActivity.this, isChecked);
          Log.i("RecipientListActivity", "Automatic SMS Forwarding toggled to: " + (isChecked ? "ENABLED" : "DISABLED"));
        }
      });
    }
    else {
      inputEnable.setOnCheckedChangeListener(null);
      inputEnable.setChecked(false);
      inputEnable.setEnabled(false);
    }
  }

  private void requestBatteryOptimizationExemption() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      try {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
          Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
          intent.setData(Uri.parse("package:" + getPackageName()));
          startActivity(intent);
        }
      } catch (Exception e) {
        Log.e("RecipientListActivity", "Error requesting battery exemption", e);
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    RuntimePermissions.onRequestPermissionsResult(RecipientListActivity.this, requestCode, permissions, grantResults);
  }

  // ---------------------------------------------------------------------------------------------
  // ActionBar:
  // ---------------------------------------------------------------------------------------------

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    if (getActionBar() != null) {
      getActionBar().setDisplayShowHomeEnabled(false);
    }
    getMenuInflater().inflate(R.menu.activity_recipient_list, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem menuItem) {
    int itemId = menuItem.getItemId();
    if (itemId == R.id.menu_add) {
      showEditDialog(-1);
      return true;
    } else if (itemId == R.id.menu_settings) {
      startActivity(SettingsActivity.getStartIntent(RecipientListActivity.this));
      return true;
    } else {
      return super.onOptionsItemSelected(menuItem);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Add/Edit Dialog:
  // ---------------------------------------------------------------------------------------------

  private void showEditDialog(final int position) {
    final boolean isAdd = (position < 0);

    final RecipientListItem listItem = (isAdd)
      ? new RecipientListItem()
      : listItems.get(position)
    ;

    final Dialog dialog = new Dialog(RecipientListActivity.this, R.style.dialog_theme);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(R.layout.dialog_recipient_listitem);

    if (dialog.getWindow() != null) {
      dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        dialog.getWindow().getAttributes().layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
      }
    }

    final TextView dialogTitle      = (TextView) dialog.findViewById(R.id.dialog_title);
    final EditText inputRecipient   = (EditText) dialog.findViewById(R.id.input_recipient);
    final EditText inputSender      = (EditText) dialog.findViewById(R.id.input_sender);
    final EditText inputTextFilter  = (EditText) dialog.findViewById(R.id.input_text_filter);

    final Button buttonDelete = (Button) dialog.findViewById(R.id.button_delete);
    final Button buttonSave   = (Button) dialog.findViewById(R.id.button_save);

    if (dialogTitle != null) {
      dialogTitle.setText(isAdd ? R.string.dialog_title_add : R.string.dialog_title_edit);
    }

    inputRecipient.setText(listItem.recipient);
    inputSender.setText(
      listItem.sender.isEmpty() ? "*" : listItem.sender
    );
    inputTextFilter.setText(
      listItem.textFilter == null ? "" : listItem.textFilter
    );

    if (isAdd) {
      buttonDelete.setText(R.string.label_button_cancel);
    }

    buttonDelete.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (!isAdd) {
          listItems.remove(position);
          listAdapter.notifyDataSetChanged();
          Preferences.setRecipientListItems(RecipientListActivity.this, listItems);
        }
        dialog.dismiss();
      }
    });

    buttonSave.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        final String new_recipient = inputRecipient.getText().toString().trim();

        String new_sender = inputSender.getText().toString().trim();
        if (new_sender.equals("")) {
          new_sender = "*";
        }

        final String new_text_filter = inputTextFilter.getText().toString().trim();

        final boolean same_recipient   = new_recipient.equals(listItem.recipient);
        final boolean same_sender      = new_sender.equals(listItem.sender);
        final boolean same_text_filter = new_text_filter.equals(listItem.textFilter == null ? "" : listItem.textFilter);

        if (new_recipient.equals("")) {
          Toast.makeText(RecipientListActivity.this, getResources().getString(R.string.error_missing_required_value), Toast.LENGTH_SHORT).show();
          return;
        }

        if (same_recipient && same_sender && same_text_filter && !isAdd) {
          // no change
          dialog.dismiss();
          return;
        }

        listItem.recipient  = new_recipient;
        listItem.sender     = new_sender;
        listItem.textFilter = new_text_filter;

        if (isAdd) {
          if (!listItems.add(listItem)) {
            Toast.makeText(RecipientListActivity.this, getResources().getString(R.string.error_add_listitem), Toast.LENGTH_SHORT).show();
            return;
          }          
        }

        listAdapter.notifyDataSetChanged();
        dialog.dismiss();

        Preferences.setRecipientListItems(RecipientListActivity.this, listItems);
      }
    });

    dialog.show();
  }
}
