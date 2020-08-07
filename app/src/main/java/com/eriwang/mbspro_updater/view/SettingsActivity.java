package com.eriwang.mbspro_updater.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.eriwang.mbspro_updater.R;
import com.eriwang.mbspro_updater.mbspro.MbsProSongFileManager;
import com.eriwang.mbspro_updater.utils.ProdAssert;

public class SettingsActivity extends AppCompatActivity
{
    private static String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            super.onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
    {
        private static final int REQ_CODE_MBSPRO_FOLDER_SELECTED = 1;
        private static final int REQ_CODE_DRIVE_FOLDER_SELECTED = 2;

        private static final String MBSPRO_FOLDER_URI_KEY = "mbspro_folder_uri";
        private static final String DRIVE_FOLDER_PATH_KEY = "drive_folder_path";
        private static final String DRIVE_FOLDER_ID_KEY = "drive_folder_id";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
        {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference mbsProFolderUriPreference = safeFindPreference(MBSPRO_FOLDER_URI_KEY);
            mbsProFolderUriPreference.setOnPreferenceClickListener(preference -> {
                Log.d(TAG, "mbspro clicked");
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                // TODO: best if there's a dialog that pops up first that says where it probably is
                // TODO: is there a cancel button?
                startActivityForResult(intent, REQ_CODE_MBSPRO_FOLDER_SELECTED);
                Log.d(TAG, "activity called");
                return false;
            });
            mbsProFolderUriPreference.setSummary(
                    getDefaultSharedPreferences().getString(MBSPRO_FOLDER_URI_KEY, "None set"));

            Preference driveFolderPathPreference = safeFindPreference(DRIVE_FOLDER_PATH_KEY);
            driveFolderPathPreference.setOnPreferenceClickListener(preference -> {
                Log.d(TAG, "drive folder clicked");
                Intent intent = new Intent(getContext(), DriveFolderSelectionActivity.class);
                startActivityForResult(intent, REQ_CODE_DRIVE_FOLDER_SELECTED);
                return false;
            });
            driveFolderPathPreference.setSummary(
                    getDefaultSharedPreferences().getString(DRIVE_FOLDER_PATH_KEY, "None set"));
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent result)
        {
            switch (requestCode)
            {
            case REQ_CODE_MBSPRO_FOLDER_SELECTED:
                handleMbsProFolderSelected(resultCode, result);
                break;

            case REQ_CODE_DRIVE_FOLDER_SELECTED:
                handleDriveFolderSelected(resultCode, result);
                break;

            default:
                throw new RuntimeException(String.format("Unknown request code %d", requestCode));
            }

            super.onActivityResult(requestCode, resultCode, result);
        }

        private void handleMbsProFolderSelected(int resultCode, Intent result)
        {
            // TODO: actual error handling
            if (resultCode != Activity.RESULT_OK || result == null || result.getData() == null)
            {
                return;
            }

            // Ideally, maybe songFileManager doesn't need to know about Drive at all.
            MbsProSongFileManager mbsProSongFileManager = new MbsProSongFileManager(null, getContext());
            mbsProSongFileManager.setDirectoryUri(result.getData());
            if (mbsProSongFileManager.findMobileSheetsDbFile() == null)
            {
                Toast.makeText(getContext(),
                        "Did not find a mobilesheets.db file in the selected directory, make sure the correct " +
                                "directory was selected.", Toast.LENGTH_SHORT).show();
                return;
            }


            String mbsproFolderUri = result.getData().toString();
            SharedPreferences.Editor editor = getDefaultSharedPreferences().edit();
            editor.putString(MBSPRO_FOLDER_URI_KEY, mbsproFolderUri);
            editor.apply();

            safeFindPreference(MBSPRO_FOLDER_URI_KEY).setSummary(mbsproFolderUri);
        }

        private void handleDriveFolderSelected(int resultCode, Intent result)
        {
            Bundle resultExtras = result.getExtras();
            if (resultCode != Activity.RESULT_OK || resultExtras == null)
            {
                Log.d(TAG, String.format("Not changing prefs: resultCode=%d", resultCode));
                return;
            }

            String driveFolderPath =
                    resultExtras.getString(DriveFolderSelectionActivity.DRIVE_FOLDER_PATH_INTENT_EXTRA);
            String driveFolderId = resultExtras.getString(DriveFolderSelectionActivity.DRIVE_FOLDER_ID_INTENT_EXTRA);
            SharedPreferences.Editor editor = getDefaultSharedPreferences().edit();
            editor.putString(DRIVE_FOLDER_PATH_KEY, driveFolderPath);
            editor.putString(DRIVE_FOLDER_ID_KEY, driveFolderId);
            editor.apply();

            safeFindPreference(DRIVE_FOLDER_PATH_KEY).setSummary(driveFolderPath);
        }

        private Preference safeFindPreference(String key)
        {
            Preference preference = findPreference(key);
            ProdAssert.notNull(preference);
            return preference;
        }

        private SharedPreferences getDefaultSharedPreferences()
        {
            Context context = getContext();
            ProdAssert.notNull(context);
            return PreferenceManager.getDefaultSharedPreferences(context);
        }
    }
}