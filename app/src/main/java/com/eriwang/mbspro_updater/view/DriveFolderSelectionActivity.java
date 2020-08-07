package com.eriwang.mbspro_updater.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ActivityChooserView;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eriwang.mbspro_updater.R;
import com.eriwang.mbspro_updater.drive.DriveUtils;
import com.eriwang.mbspro_updater.drive.DriveWrapper;
import com.eriwang.mbspro_updater.utils.TaskUtils;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// Unfortunately, Android's Storage Access Framework does not allow selecting actual folders from Drive (just allows
// individual file selection). Therefore I'm using a custom activity that allows Drive Folder selection. It's not an
// optimal solution, but should get the job done.
public class DriveFolderSelectionActivity extends AppCompatActivity
{
    public static final String DRIVE_FOLDER_PATH_INTENT_EXTRA = "driveFolderPath";
    public static final String DRIVE_FOLDER_ID_INTENT_EXTRA = "driveFolderId";

    private static final String TAG = "DriveFolderSelection";

    private DriveWrapper mDrive;
    private Executor mExecutor;
    private ArrayList<DriveFolder> mCurrentTreeDriveFolders;
    private ArrayList<DriveFolder> mDriveFolders;
    private DriveFolderViewAdapter mDriveFolderViewAdapter;
    private boolean mCurrentlySwitchingFolder;

    private void initMembers()
    {
        mDrive = new DriveWrapper();
        mDrive.setCredentialFromContextAndInitialize(this);
        mExecutor = Executors.newSingleThreadExecutor();
        mCurrentTreeDriveFolders = new ArrayList<>(Collections.singletonList(DriveFolder.ROOT));
        mDriveFolders = new ArrayList<>();
        mDriveFolderViewAdapter = new DriveFolderViewAdapter(this, mDriveFolders);
        mCurrentlySwitchingFolder = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive_folder_selection);

        initMembers();

        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ListView listView = findViewById(R.id.drive_folders_list_view);
        listView.setAdapter(mDriveFolderViewAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (mCurrentlySwitchingFolder)
            {
                return;
            }

            DriveFolder driveFolder = (DriveFolder) listView.getItemAtPosition(position);
            Log.d(TAG, String.format("Clicked %s:%s", driveFolder.mName, driveFolder.mId));

            DriveFolder targetDriveFolder;
            if (driveFolder == DriveFolder.UP_ONE_LEVEL)
            {
                mCurrentTreeDriveFolders.remove(mCurrentTreeDriveFolders.size() - 1);
                targetDriveFolder = mCurrentTreeDriveFolders.get(mCurrentTreeDriveFolders.size() - 1);
            }
            else
            {
                mCurrentTreeDriveFolders.add(driveFolder);
                targetDriveFolder = driveFolder;
            }

            switchToParentOrChildFolder(targetDriveFolder);
        });

        switchToParentOrChildFolder(DriveFolder.ROOT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.drive_folder_selection_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case android.R.id.home:
            super.onBackPressed();
            return true;

        case R.id.drive_folder_selection_select_folder:
            handleDriveFolderSelected();
            return true;

        default:
            throw new RuntimeException(
                    String.format("Unhandled item id=%d, title=%s", item.getItemId(), item.getTitle()));
        }
    }

    private void switchToParentOrChildFolder(DriveFolder driveFolder)
    {
        mCurrentlySwitchingFolder = true;

        TextView currentDrivePathTextView = findViewById(R.id.current_drive_folder_text_view);
        String drivePathText = getCurrentDrivePathText();
        currentDrivePathTextView.setText(String.format("%s (Loading...)", drivePathText));

        TaskUtils.execute(mExecutor, () -> mDrive.listDirectory(driveFolder.mId))
            .addOnSuccessListener(directoryItems -> {
                Log.d(TAG, String.format("ListDir complete, found %d items", directoryItems.size()));

                currentDrivePathTextView.setText(drivePathText);

                mDriveFolders.clear();
                if (driveFolder != DriveFolder.ROOT)
                {
                    mDriveFolders.add(DriveFolder.UP_ONE_LEVEL);
                }
                for (File item : directoryItems)
                {
                    if (DriveUtils.isFolder(item))
                    {
                        mDriveFolders.add(new DriveFolder(item.getName(), item.getId()));
                    }
                }
                mDriveFolderViewAdapter.notifyDataSetChanged();

                mCurrentlySwitchingFolder = false;
            })
            // TODO: better error handling
            .addOnFailureListener(exception -> Log.e(TAG, "ListDirectory on Drive failed.", exception));
    }

    private void handleDriveFolderSelected()
    {
        DriveFolder driveFolder = mCurrentTreeDriveFolders.get(mCurrentTreeDriveFolders.size() - 1);
        if (driveFolder == DriveFolder.ROOT)
        {
            Toast.makeText(this, "Using root as Drive Folder is currently unsupported.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent()
                .putExtra(DRIVE_FOLDER_PATH_INTENT_EXTRA, getCurrentDrivePathText())
                .putExtra(DRIVE_FOLDER_ID_INTENT_EXTRA, driveFolder.mId);

        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private String getCurrentDrivePathText()
    {
        StringBuilder fullPath = new StringBuilder();
        for (DriveFolder driveFolder : mCurrentTreeDriveFolders)
        {
            fullPath.append('/');
            fullPath.append(driveFolder.mName);
        }
        return fullPath.toString();
    }
}

class DriveFolder
{
    public static DriveFolder ROOT = new DriveFolder("(Root)", "root");
    public static DriveFolder UP_ONE_LEVEL = new DriveFolder("(Go up one level)", null);

    public String mName;
    public String mId;

    public DriveFolder(String name, String id)
    {
        mName = name;
        mId = id;
    }
}

class DriveFolderViewAdapter extends ArrayAdapter<DriveFolder>
{
    public DriveFolderViewAdapter(Context context, List<DriveFolder> values)
    {
        super(context, R.layout.drive_folder_selection_folder, R.id.drive_folder_name, values);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        DriveFolder driveFolder = getItem(position);
        if (convertView == null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.drive_folder_selection_folder, parent,
                    false);
        }

        TextView driveFolderNameView = convertView.findViewById(R.id.drive_folder_name);
        driveFolderNameView.setText(driveFolder.mName);

        return convertView;
    }
}