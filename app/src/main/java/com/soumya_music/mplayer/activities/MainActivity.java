package com.soumya_music.mplayer.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.soumya_music.mplayer.BuildConfig;
import com.soumya_music.mplayer.R;
import com.soumya_music.mplayer.adapters.MediaAdapter;
import com.soumya_music.mplayer.databinding.ActivityMainBinding;
import com.soumya_music.mplayer.models.Song;
import com.soumya_music.mplayer.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    MediaAdapter myAdapter;
    List<Song> mySongs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        grantPermissionForStorage();
        binding.btnRefresh.setOnClickListener(v -> grantPermissionForStorage());
        binding.btnAllow.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                loadSongs();
            } else {
                showRationDialogForPermissions();
            }
        });

    }

    @SuppressLint("Range")
    private void loadSongs() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{"_data", "_display_name", "artist", "_id"};
        String Type_ = "mime_type=?";
        String mp3 = MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3");
        String[] Arguments = new String[]{mp3};
        Cursor cursor = this.getContentResolver().query(uri, projection, Type_, Arguments, (String) null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String url = cursor.getString(cursor.getColumnIndex("_data"));
                String author = cursor.getString(cursor.getColumnIndex("artist"));
                String title = cursor.getString(cursor.getColumnIndex("_display_name"));
                int id = cursor.getInt(cursor.getColumnIndex("_id"));
                this.mySongs.add(new Song(title, author, url, id));
            }
        }
        Log.e("Size :: ",String.valueOf(mySongs.size()));
        if (mySongs.size() > 0) {
            binding.llPermissionDenied.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.llNoSongs.setVisibility(View.GONE);

            myAdapter = new MediaAdapter(mySongs, this, (position, item) -> {
                Intent intent = new Intent(MainActivity.this, SongDetailsActivity.class);
                intent.putExtra(Constants.EXTRA_SONG_DETAILS, item);
                intent.putExtra(Constants.CURRENT_POSITION, position);
                intent.putParcelableArrayListExtra(Constants.SONG_LIST, (ArrayList<? extends Parcelable>) mySongs);
                startActivity(intent);
            });
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
            binding.recyclerView.setHasFixedSize(true);
            binding.recyclerView.setAdapter(myAdapter);
        } else {
            binding.recyclerView.setVisibility(View.GONE);
            binding.llNoSongs.setVisibility(View.VISIBLE);
            binding.llPermissionDenied.setVisibility(View.GONE);
        }
    }

    private void grantPermissionForStorage() {
        Dexter.withContext(this).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        if (multiplePermissionsReport.areAllPermissionsGranted()) loadSongs();
                        else binding.llPermissionDenied.setVisibility(View.VISIBLE);
                        if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied()) {
                            Toast.makeText(MainActivity.this, "Oops! You just denied the permission", Toast.LENGTH_SHORT).show();
                            showRationDialogForPermissions();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        showRationDialogForPermissions();
                    }
                }).onSameThread().check();

    }

    private void showRationDialogForPermissions() {
        (new AlertDialog.Builder((Context) this)).setMessage((CharSequence) "It looks like you have turned off permission required for this feature. " +
                "It can enabled under the Application settings.")
                .setPositiveButton((CharSequence) "GO TO SETTINGS",
                        (android.content.DialogInterface.OnClickListener) (new android.content.DialogInterface.OnClickListener() {
                            public final void onClick(DialogInterface dialogInterface, int number) {
                                try {
                                    Intent settingIntent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                                    Uri uri = Uri.fromParts("package", MainActivity.this.getPackageName(), (String) null);
                                    settingIntent.setData(uri);
                                    MainActivity.this.startActivityForResult(settingIntent, 4);
                                } catch (ActivityNotFoundException e) {
                                    e.printStackTrace();
                                }

                            }
                        })).setNegativeButton((CharSequence) "Cancel", (DialogInterface.OnClickListener) ((dialog, num) -> {
            binding.llPermissionDenied.setVisibility(View.VISIBLE);
            dialog.dismiss();
        })).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == Constants.SETTING_CODE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                binding.llPermissionDenied.setVisibility(View.GONE);
                loadSongs();
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void openRatingPage(String url) {
        Uri ratingPage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, ratingPage);
        if (intent.resolveActivity(this.getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.shareApp) {
            try {
                Intent shareAppIntent = new Intent(Intent.ACTION_SEND);
                shareAppIntent.setType("text/plain");
                String shareMessage = getString(R.string.shareAppLink) + BuildConfig.APPLICATION_ID ;
                shareAppIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                startActivity(Intent.createChooser(shareAppIntent, getString(R.string.titleShareby)));
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.rating) {
            String  ratingUrlAsString = getString(R.string.shareAppLink) + BuildConfig.APPLICATION_ID;
            openRatingPage(ratingUrlAsString);
        }
        return super.onOptionsItemSelected(item);
    }
}