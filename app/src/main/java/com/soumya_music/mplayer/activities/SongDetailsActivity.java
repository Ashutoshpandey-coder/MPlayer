package com.soumya_music.mplayer.activities;

import static java.lang.Thread.sleep;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.soumya_music.mplayer.R;
import com.soumya_music.mplayer.adapters.MediaAdapter;
import com.soumya_music.mplayer.databinding.ActivitySongDetailsBinding;
import com.soumya_music.mplayer.models.Song;
import com.soumya_music.mplayer.notification.NotificationUtils;
import com.soumya_music.mplayer.notification.OnNotificationMethods;
import com.soumya_music.mplayer.utils.Constants;

import java.util.List;
import java.util.Objects;

public class SongDetailsActivity extends AppCompatActivity implements View.OnClickListener , OnNotificationMethods {
    ActivitySongDetailsBinding binding;
    Song mSongDetails;
    List<Song> mSongList;
    int mPosition;
    MediaPlayer mMediaPlayer;
    AudioManager mAudioManager;
    Thread mUpdateSeek;
    boolean repeatAll = true,repeatOne,shuffle,isMediaPlayerReleasedDueToFocusLoss,isMediaInterruptedByUser;
    Toast mToast;
    AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener;
    BroadcastReceiver mReceiver;
    SharedPreferences mSharedPreferences;
    BroadcastReceiver headReceiver;
    IntentFilter bluetoothIntentFilter;
    public static final String ACTION_PAUSE = "pause-song";
    public static final String ACTION_PREV = "prev-song";
    public static final String ACTION_NEXT = "next-song";
    public static final String ACTION_CANCEL = "cancel-notification";
    public static final String CURRENT_POSITION = "currentPosition";
    public static final String SAVE_LAST_STATE = "last-state";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySongDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialization();
        setUpActionBar();
        getDataFromIntent();
        if (mSongDetails != null) {
            releaseMediaPlayer();
            prepareMediaPlayerAndStartPlaying();
        }
        binding.ivPlay.setOnClickListener(this);
        binding.ivNext.setOnClickListener(this);
        binding.ivPrev.setOnClickListener(this);
        binding.ivSequence.setOnClickListener(this);
        binding.ivBackButton.setOnClickListener(v->onBackPressed());
        binding.ivList.setOnClickListener(v->showSongListDialog(this));
        binding.ivShare.setOnClickListener (this);

        binding.sbSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b) binding.sbSeekBar.setProgress(i);
                if(mMediaPlayer != null) binding.tvCurrentTime.setText(createTimeLabel((long) mMediaPlayer.getCurrentPosition()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.seekTo(seekBar.getProgress());
                    binding.tvCurrentTime.setText(createTimeLabel((long)mMediaPlayer.getCurrentPosition()));
                }
            }
        });
        updateSeekBar();

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = getIntent().getExtras().getString("action");
                Log.e("Action ::",action);
                switch (action) {
                    case ACTION_PAUSE:
                        onPlayPauseButton();
                        break;
                    case ACTION_NEXT:
                        onNextButton();
                        break;
                    case ACTION_PREV:
                        onPreviousButton();
                        break;
                    case ACTION_CANCEL:
                        onCancelButton();
                        break;
                }
            }
        };
        registerReceiver(mReceiver, new IntentFilter("Track-Track"));
    }
    private void getDataFromIntent(){
        if(getIntent().hasExtra(Constants.EXTRA_SONG_DETAILS)
                && getIntent().hasExtra(Constants.CURRENT_POSITION)
                && getIntent().hasExtra(Constants.SONG_LIST)) {
            mSongDetails = getIntent().getParcelableExtra(Constants.EXTRA_SONG_DETAILS);
            mPosition = getIntent().getIntExtra(Constants.CURRENT_POSITION, 1);
            mSongList = getIntent().getParcelableArrayListExtra(Constants.SONG_LIST);

        }
    }
    private void setUpActionBar() {
        Objects.requireNonNull(getSupportActionBar()).hide();
    }
    private void initialization(){
        headReceiver = new HeadConnected();
        bluetoothIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(headReceiver, bluetoothIntentFilter);

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mSharedPreferences = getSharedPreferences(SAVE_LAST_STATE, MODE_PRIVATE);
        mToast = new Toast(this);


        mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) pauseAndShowPlayIcon();
                else if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) mMediaPlayer.setVolume(20f,20f);
                else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    if (isMediaInterruptedByUser) {
                        pauseAndShowPlayIcon();
                        mAudioManager.abandonAudioFocus(this);
                        isMediaInterruptedByUser = false;
                    } else
                        playAndShowPauseIcon();
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS){
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putInt(CURRENT_POSITION,mMediaPlayer.getCurrentPosition());
                    editor.apply();
                    binding.ivPlay.setImageResource(R.drawable.play);
                   /* NotificationUtils.createNotification(
                            SongDetailsActivity.this,
                            mSongDetails.getTitle(),
                            R.drawable.play
                    );*/
                    releaseMediaPlayer();
                    isMediaPlayerReleasedDueToFocusLoss = true;
                    mAudioManager.abandonAudioFocus(this);
                }
            }
        };



    }

    private void pauseAndShowPlayIcon() {
        mMediaPlayer.pause();
        binding.ivPlay.setImageResource(R.drawable.play);
     /*   NotificationUtils.createNotification(
                SongDetailsActivity.this,
        mSongDetails.getTitle(),
                R.drawable.play
        );*/
    }
    private void playAndShowPauseIcon(){
        mMediaPlayer.start();
        binding.ivPlay.setImageResource(R.drawable.pause);
   /*     NotificationUtils.createNotification(
                SongDetailsActivity.this,
        mSongDetails.getTitle(),
                R.drawable.pause
        );*/
    }
    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
    private boolean isAudioFocusGranted(){
        int result = mAudioManager.requestAudioFocus(
                mAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        );
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }
    @SuppressLint("SetTextI18n")
    void showSongListDialog(Context context) {
        Dialog mDialog = new Dialog(context);
        mDialog.setContentView(R.layout.songs_dialog);
        mDialog.findViewById(R.id.tv_close).setOnClickListener(v->mDialog.dismiss());
        mDialog.show();
        RecyclerView dialogList = mDialog.findViewById(R.id.recycler_view_dialog);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mDialog.getWindow().setGravity(Gravity.BOTTOM);
        MediaAdapter adapter = new MediaAdapter(mSongList, context, (position, item) -> {
            releaseMediaPlayer();
            mPosition = position;
            mSongDetails = mSongList.get(position);
            prepareMediaPlayerAndStartPlaying();
        });
        dialogList.setHasFixedSize(true);
        dialogList.setLayoutManager(new LinearLayoutManager(context));
        dialogList.setAdapter(adapter);

        TextView title = mDialog.findViewById(R.id.title_playlist_dialog);
        title.setText("Playlist " + "(" + mSongList.size() + " songs" + ")");

    }
    private void prepareMediaPlayerAndStartPlaying() {
        mMediaPlayer = MediaPlayer.create(this, Uri.parse(mSongDetails.getSongUrl()));
        if (isMediaPlayerReleasedDueToFocusLoss) {
            mMediaPlayer.seekTo(mSharedPreferences.getInt(CURRENT_POSITION, 0));
            isMediaPlayerReleasedDueToFocusLoss = false;
        }
        if (isAudioFocusGranted()) mMediaPlayer.start();
        binding.tvSongName.setSelected(true);
        binding.tvSongName.setText(mSongDetails.getTitle());
        binding.tvArtistName.setText(mSongDetails.getAuthor());
        binding.ivPlay.setImageResource(R.drawable.pause);
        binding.sbSeekBar.setMax(mMediaPlayer.getDuration());
        binding.tvTotalTime.setText(createTimeLabel(Long.parseLong(String.valueOf(mMediaPlayer.getDuration()))));
//        NotificationUtils.createNotification(this, mSongDetails.getTitle(), R.drawable.pause);
        updateSeekBar();
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if(repeatOne){
                    releaseMediaPlayer();;
                    prepareMediaPlayerAndStartPlaying();
                }else if(shuffle){
                    releaseMediaPlayer();
                    int min = 1;
                    mPosition = Integer.parseInt(String.valueOf((Math.random() * (mSongList.size() - min + 1)))) + min;
                    mSongDetails = mSongList.get(mPosition);
                    prepareMediaPlayerAndStartPlaying();
                }else playNextSong();
            }
        });
    }
    void playNextSong() {
        releaseMediaPlayer();
        if(mPosition != mSongList.size()-1) mPosition = mPosition + 1; else mPosition = 0;
        mSongDetails = mSongList.get(mPosition);
        prepareMediaPlayerAndStartPlaying();
    }

    private void repeatAllSongs() {
        if (mMediaPlayer != null)
            mMediaPlayer.setOnCompletionListener(mediaPlayer -> playNextSong());
    }

    private void repeatCurrentSong() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mSongDetails = mSongList.get(mPosition);
            mMediaPlayer.setOnCompletionListener(mediaPlayer -> prepareMediaPlayerAndStartPlaying());
        }
    }

    private void shuffleSongs() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.setOnCompletionListener(mediaPlayer -> {
                int min = 1;
                mPosition = Integer.parseInt(String.valueOf((Math.random() * (mSongList.size() - min + 1)))) + min;
                mSongDetails = mSongList.get(mPosition);
                prepareMediaPlayerAndStartPlaying() ;
            });
        }
    }

    private void showToast(String message) {
        if (mToast != null) {
            mToast.cancel();
        }
        assert mToast != null;
        mToast.setText(message);
        mToast.setDuration(Toast.LENGTH_SHORT);
        mToast.show();
    }

    void onClickPlayPause() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                isMediaInterruptedByUser = true;
                pauseAndShowPlayIcon();
            } else {
                if (isAudioFocusGranted()) {
                    playAndShowPauseIcon();
                }
            }
        } else {
            prepareMediaPlayerAndStartPlaying();
        }
    }


    private void updateSeekBar() {
        mUpdateSeek = new Thread(() -> {
            int currentPosition = 0;
            if(mMediaPlayer != null){
                try {
                    while (currentPosition < mMediaPlayer.getDuration()) {
                        currentPosition = mMediaPlayer.getCurrentPosition();
                        binding.sbSeekBar.setProgress(currentPosition);
                        sleep(600);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        mUpdateSeek.start();
    }

    private String createTimeLabel(Long milliSeconds) {
        String timerString = "";
        String secondsString;
        String minuteString;
        int hour = (int) (milliSeconds / (1000 * 60 * 60));
        int min = (int) ((milliSeconds % (1000 * 60 * 60)) / (1000 * 60));
        int seconds = (int) (milliSeconds % (1000 * 60 * 60) % (1000 * 60) / 1000);
        if (hour > 0) {
            timerString = hour + ":";
        }
         if (seconds < 10) {
             secondsString = "0" + seconds;
        } else {
             secondsString = "" + seconds;
        }
         if (min < 10) {
             minuteString = "0" + min;
        } else {
             minuteString = "" + min;
        }
         timerString = timerString + minuteString + ":" + secondsString;
        return timerString;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.iv_play) onClickPlayPause();
        else if(view.getId() == R.id.iv_next) playNextSong();
        else if(view.getId() == R.id.iv_prev) playPreviousSong();
        else if(view.getId() == R.id.iv_sequence){
            if(repeatAll){
                binding.ivSequence.setImageResource(R.drawable.shuffle);
                showToast("Shuffle on");
                shuffle = true;
                repeatAll = false;
                shuffleSongs();
            }else if(shuffle){
                binding.ivSequence.setImageResource(R.drawable.repeat_current_song);
                showToast("Repeat current song");
                repeatOne = true;
                shuffle = false;
                repeatCurrentSong();
            }else if(repeatOne){
                binding.ivSequence.setImageResource(R.drawable.ic_repeat_all);
                showToast("Repeat all songs");
                repeatOne = false;
                repeatAll = true;
                repeatAllSongs();
            }
        }else if(view.getId() == R.id.iv_share){
            mSongDetails = mSongList.get(mPosition);
            Uri uri = Uri.parse(mSongDetails.getSongUrl());
            Intent audio = new Intent(Intent.ACTION_SEND);
            audio.setType("audio/*");
            audio.putExtra(
                    Intent.EXTRA_STREAM,
                    uri
            );
            startActivity(Intent.createChooser(audio, "Favourite song"));
        }

    }
    private void playPreviousSong() {
        releaseMediaPlayer();
         if (mPosition != 0) {
             mPosition = mPosition - 1;
        } else {
             mPosition = mSongList.size() - 1;
        }
        mSongDetails = mSongList.get(mPosition);
        prepareMediaPlayerAndStartPlaying();
    }

    @Override
    public void onPlayPauseButton() {
        onClickPlayPause();
    }

    @Override
    public void onNextButton() {
        playNextSong();
    }

    @Override
    public void onPreviousButton() {
        playPreviousSong();
    }

    @Override
    public void onCancelButton() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            binding.ivPlay.setImageResource(R.drawable.play);
        }
        NotificationUtils.clearAllNotification(this);
    }

    private class HeadConnected extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(mMediaPlayer != null && mMediaPlayer.isPlaying())pauseAndShowPlayIcon();
        }
    }

    @Override
    protected void onDestroy() {
        releaseMediaPlayer();
        mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
        unregisterReceiver(mReceiver);
        NotificationUtils.clearAllNotification(this);
        unregisterReceiver(headReceiver);
        super.onDestroy();
    }
}