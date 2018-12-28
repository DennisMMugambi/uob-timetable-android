package com.ak.uobtimetable;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ak.uobtimetable.Utilities.Logging.Logger;

/**
 * Activity which allows the user to configure settings
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Logger.getInstance().debug("SettingsActivity", "onCreate");
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){

        Logger.getInstance().debug("SettingsActivity", "Saving state");

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onResume() {

        super.onResume();

        Logger.getInstance().debug("SettingsActivity", "OnResume");
    }
}
