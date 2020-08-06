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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.eriwang.mbspro_updater.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DriveFolderSelectionActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive_folder_selection);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ArrayList<DriveFolder> driveFolders = new ArrayList<>();
        driveFolders.add(new DriveFolder("MobileSheets Pro Folder", "1"));
        driveFolders.add(new DriveFolder("Google Drive Folder", "2"));

        ListView listView = findViewById(R.id.drive_folder_list);
        listView.setAdapter(new CustomAdapter(this, driveFolders));
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
}

class DriveFolder
{
    public String mName;
    public String mId;

    public DriveFolder(String name, String id)
    {
        mName = name;
        mId = id;
    }
}

// TODO: rename
class CustomAdapter extends ArrayAdapter<DriveFolder>
{
    public CustomAdapter(Context context, List<DriveFolder> values)
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