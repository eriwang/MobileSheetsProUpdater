package com.eriwang.mbspro_updater.view;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
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

import com.eriwang.mbspro_updater.R;
import com.eriwang.mbspro_updater.drive.DriveWrapper;
import com.eriwang.mbspro_updater.utils.TaskUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// Unfortunately, Android's Storage Access Framework does not allow selecting actual folders from Drive (just allows
// individual file selection). Therefore I'm using a custom activity that allows Drive Folder selection. It's not an
// optimal solution, but should get the job done.
public class DriveFolderSelectionActivity extends AppCompatActivity
{
    private static final String TAG = "DriveFolderSelection";

    private DriveWrapper mDrive;
    private Executor mExecutor;
    private DriveFolder mCurrentDriveFolder;
    private TextView mCurrentDriveFolderTextView;

    private ArrayList<DriveFolder> mDriveFolders;
    private DriveFolderViewAdapter mDriveFolderViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive_folder_selection);

        mDrive = new DriveWrapper();
        mDrive.setCredentialFromContextAndInitialize(this);
        mExecutor = Executors.newSingleThreadExecutor();
        mCurrentDriveFolder = DriveFolder.ROOT;
        mCurrentDriveFolderTextView = findViewById(R.id.current_drive_folder_text_view);
        mDriveFolders = new ArrayList<>();
        mDriveFolderViewAdapter = new DriveFolderViewAdapter(this, mDriveFolders);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setCurrentDriveFolder(mCurrentDriveFolder);

        TaskUtils.execute(mExecutor, () -> mDrive.listDirectory("root"))
            .addOnSuccessListener(directoryItems -> {
                Log.d(TAG, String.format("ListDir complete, found %d items", directoryItems.size()));
                mDriveFolders.add(DriveFolder.UP_ONE_LEVEL);
                mDriveFolderViewAdapter.notifyDataSetChanged();
            });

        ListView listView = findViewById(R.id.drive_folders_list_view);

        listView.setAdapter(mDriveFolderViewAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            DriveFolder driveFolder = (DriveFolder) listView.getItemAtPosition(position);
            Log.d(TAG, String.format("Clicked %s:%s", driveFolder.mName, driveFolder.mId));
        });
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
        if (item.getItemId() == android.R.id.home)
        {
            super.onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setCurrentDriveFolder(DriveFolder driveFolder)
    {
        mCurrentDriveFolder = driveFolder;
        mCurrentDriveFolderTextView.setText((driveFolder == null) ? "/ (Root)" : driveFolder.mName);
    }

    private void populateDriveFolderViewWithFolderContents(DriveFolder driveFolder)
    {

    }
}

// TODO: I want to display the full path of the Drive Folder
class DriveFolder
{
    public static DriveFolder ROOT = new DriveFolder("(Root)", null);
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