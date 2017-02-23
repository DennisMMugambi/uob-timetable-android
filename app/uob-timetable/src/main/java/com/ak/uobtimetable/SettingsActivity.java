package com.ak.uobtimetable;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.ak.uobtimetable.Fragments.PreferencesFragment;
import com.ak.uobtimetable.Utilities.AndroidUtilities;
import com.ak.uobtimetable.Utilities.Logger;
import com.ak.uobtimetable.Utilities.SettingsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity which allows the user to configure settings. Also contains a hidden application log
 * viewer.
 */
public class SettingsActivity extends AppCompatActivity {

    private TextView tvLog;
    private Button btDeveloperMode;
    private Button btClearSettings;
    private Button btCopyLog;
    private Button btRestart;

    private int devBtnClickCount = 0;
    private boolean showingDebug = false;
    private List<Button> toggleButtons;

    public enum Args {
        debug
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Get UI references
        tvLog = (TextView)findViewById(R.id.tvLog);
        btDeveloperMode = (Button)findViewById(R.id.btDevMode);
        btClearSettings = (Button)findViewById(R.id.btClearSettings);
        btCopyLog = (Button)findViewById(R.id.btCopyLog);
        btRestart = (Button)findViewById(R.id.btRestart);

        // Set initial values
        tvLog.setMovementMethod(new ScrollingMovementMethod());

        toggleButtons = new ArrayList<>();
        toggleButtons.add(btClearSettings);
        toggleButtons.add(btCopyLog);
        toggleButtons.add(btRestart);

        for (Button hiddenButton : toggleButtons)
            hiddenButton.setVisibility(View.GONE);

        tvLog.setVisibility(View.INVISIBLE);
        btDeveloperMode.setText(" ");
        btClearSettings.setVisibility(View.GONE);

        if (AndroidUtilities.isTabletLayout(this) == false)
            tvLog.setTextSize(12);

        // Dev mode button
        btDeveloperMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                devBtnClickCount++;

                if (devBtnClickCount == 7)
                    showDebugOptions();
            }
        });

        btClearSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SettingsManager.getInstance(SettingsActivity.this).clear();
                updateLog();
            }
        });

        btCopyLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", Logger.getInstance().toString());
                clipboard.setPrimaryClip(clip);

                Toast toast = Toast.makeText(SettingsActivity.this, "Copied log!", Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        btRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });

        // Restore state
        if (savedInstanceState != null && savedInstanceState.getBoolean(Args.debug.name(), false) == true)
            showDebugOptions();
    }

    private void showDebugOptions(){

        showingDebug = true;
        updateLog();
        tvLog.setVisibility(View.VISIBLE);

        for (Button hiddenButton : toggleButtons)
            hiddenButton.setVisibility(View.VISIBLE);

        btDeveloperMode.setVisibility(View.GONE);
    }

    private void updateLog(){

        tvLog.setText(AndroidUtilities.fromHtml(Logger.getInstance().toHtml()));
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        Logger.getInstance().debug("SettingsActivity", "Saving state");
        savedInstanceState.putBoolean(Args.debug.name(), showingDebug);
    }
}
