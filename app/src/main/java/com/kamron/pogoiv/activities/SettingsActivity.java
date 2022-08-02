package com.kamron.pogoiv.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import android.widget.Toast;

import com.kamron.pogoiv.BuildConfig;
import com.kamron.pogoiv.GoIVSettings;
import com.kamron.pogoiv.R;
import com.kamron.pogoiv.updater.AppUpdate;
import com.kamron.pogoiv.updater.AppUpdateUtil;

import timber.log.Timber;

public class SettingsActivity extends AppCompatActivity {

    private final BroadcastReceiver showUpdateDialog = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!(BuildConfig.DISTRIBUTION_GITHUB && BuildConfig.INTERNET_AVAILABLE)) {
                return;
            }
            AppUpdate update = intent.getParcelableExtra("update");
            if (update.getStatus() == AppUpdate.UPDATE_AVAILABLE) {
                AppUpdateUtil.getInstance()
                        .getAppUpdateDialog(SettingsActivity.this, update)
                        .show();
            } else if (update.getStatus() == AppUpdate.UP_TO_DATE) {
                Toast.makeText(SettingsActivity.this, getResources().getString(R.string.up_to_date), Toast.LENGTH_SHORT)
                        .show();
            } else {
                Toast.makeText(SettingsActivity.this, getResources().getString(R.string.update_check_failed),
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(getResources().getString(R.string.settings_page_title));
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(showUpdateDialog, new IntentFilter(MainActivity.ACTION_SHOW_UPDATE_DIALOG));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(showUpdateDialog);
        super.onPause();
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName(GoIVSettings.PREFS_GO_IV_SETTINGS);
            addPreferencesFromResource(R.xml.settings);

            //Initialize the button which opens the credits activity
            Preference creditsButton = findPreference(getString(R.string.view_credits_button));
            if (creditsButton != null) {
                creditsButton.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(getActivity(), CreditsActivity.class);
                    startActivity(intent);
                    return true;
                });
            }

            if (!BuildConfig.DISTRIBUTION_GITHUB || !BuildConfig.INTERNET_AVAILABLE) {
                // Internal auto-update is available only for online build distributed via GitHub
                effectivelyRemovePreference(GoIVSettings.AUTO_UPDATE_ENABLED);
            }

            Preference checkForUpdatePreference = findPreference("checkForUpdate");
            if (checkForUpdatePreference != null) {
                checkForUpdatePreference.setOnPreferenceClickListener(preference -> {
                    AppUpdateUtil.getInstance().checkForUpdate(getActivity(), true);
                    return true;
                });
            }

            if (!BuildConfig.INTERNET_AVAILABLE) {
                // Hide crash report related settings
                effectivelyRemovePreference(GoIVSettings.SEND_CRASH_REPORTS);
            }

            //If strings support use_default_pokemonsname_as_ocrstring, display pref and set default ON
            if (getResources().getBoolean(R.bool.use_default_pokemonsname_as_ocrstring)) {
                SwitchPreference useDefaultPokemonNamePreference = findPreference(GoIVSettings.SHOW_TRANSLATED_POKEMON_NAME);
                if (useDefaultPokemonNamePreference != null) {
                    useDefaultPokemonNamePreference.setEnabled(true);
                    useDefaultPokemonNamePreference.setDefaultValue(true);
                }
            } else {
                effectivelyRemovePreference(GoIVSettings.SHOW_TRANSLATED_POKEMON_NAME);
            }

            SeekBarPreference seekBarPreference = findPreference(GoIVSettings.AUTO_APPRAISAL_SCAN_DELAY);
            if (seekBarPreference != null) {
                seekBarPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    final String key = preference.getKey();
                    if (key.equals(GoIVSettings.AUTO_APPRAISAL_SCAN_DELAY)) {
                        final SeekBarPreference sbp = (SeekBarPreference) preference;
                        final int increment = sbp.getSeekBarIncrement();
                        float value = (int) newValue;
                        final int rounded = Math.round(value / increment);
                        final int finalValue = rounded * increment;
                        if (finalValue == value) {
                            return true;
                        } else {
                            sbp.setValue(finalValue);
                        }
                        return false;
                    }
                    return true;
                });
            }
        }

        private void effectivelyRemovePreference(@NonNull String preferenceKey) {
            Preference unwantedPreference = getPreferenceManager().findPreference(preferenceKey);

            if (unwantedPreference == null) {
                Timber.e("Can't find a Preference with key: %1$s", preferenceKey);
                return;
            }

            PreferenceScreen preferenceScreen = getPreferenceScreen();
            if (preferenceScreen.removePreference(unwantedPreference)) {
                return; // Hurray!
            }

            for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
                Preference p = preferenceScreen.getPreference(i);
                if (p instanceof PreferenceCategory) {
                    if (((PreferenceCategory) p).removePreference(unwantedPreference)) {
                        return; // Hurray!
                    }
                }
            }

            Timber.e("Can't remove unwanted Preference: %1$s", unwantedPreference.toString());
        }
    }
}
