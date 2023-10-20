package com.example.raceapp;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SwitchCompat;

import java.io.IOException;
import java.util.Locale;

public class SettingsActivity extends BaseActivity implements MediaController.MediaPlayerControl
{
    //GLOBAL VIEWS
    private MediaPlayer mediaPlayer;
    private MediaController controller;

    private boolean stayActive = true; //for media controller.

    private TextView delayProgressView, magProgressView, pocketProgressView;

    //Intent Launcher
    private ActivityResultLauncher<Intent> soundResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        super.createToolbar(R.string.settings);

        //Views
        View controllerLocation = findViewById(R.id.controllerView);

        //SEEKBAR LISTENER//////////////////////////////////////////////////////////////////////////
        SeekBar.OnSeekBarChangeListener onSeekBar = new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                int id = seekBar.getId();

                if(id == R.id.seekbarMagnetic)
                {
                    Settings.defaultMagnetThreshold = (progress * 5) + 60;
                    magProgressView.setText(String.format(Locale.US, "%d",Settings.defaultMagnetThreshold));
                }
                else if(id == R.id.seekbarDelayTime)
                {
                    Settings.delayTime = (progress) + 1;
                    delayProgressView.setText(String.format(Locale.US, "%d",Settings.delayTime));
                }
                else if(id == R.id.seekbarPocket)
                {
                    Settings.pocketAmbientThreshold = (progress);
                    pocketProgressView.setText(String.format(Locale.US, "%d",Settings.pocketAmbientThreshold));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            { }
        };

        delayProgressView = findViewById(R.id.delayTimeView);
        magProgressView = findViewById(R.id.currentDefaultThresholdView);
        pocketProgressView = findViewById(R.id.pocketProgressView);

        SeekBar magBar = findViewById(R.id.seekbarMagnetic);
        SeekBar delayBar = findViewById(R.id.seekbarDelayTime);
        SeekBar pocketBar = findViewById(R.id.seekbarPocket);

        //set the listeners for the seekbars
        magBar.setOnSeekBarChangeListener(onSeekBar);
        delayBar.setOnSeekBarChangeListener(onSeekBar);
        pocketBar.setOnSeekBarChangeListener(onSeekBar);

        //Set to defaults. Should call onSeekBar, which would change the text too.
        delayBar.setProgress(Settings.delayTime - 1);
        magBar.setProgress((Settings.defaultMagnetThreshold - 60) / 5);
        pocketBar.setProgress(Settings.pocketAmbientThreshold);

        //DEFAULT LIGHT TOGGLE//////////////////////////////////////////////////////////////////////
        SwitchCompat switchDefaultLight = findViewById(R.id.defaultLightSwitch);
        switchDefaultLight.setOnCheckedChangeListener((compoundButton, bool) -> Settings.defaultLight = bool);
        switchDefaultLight.setChecked(Settings.defaultLight);

        //SET UP MEDIA CONTROLLER//////////////////////////////////////////////////////////////////
        controller = new MediaController(this)
        { //anonymous class to prevent MediaController from hiding
            @Override
            public void hide() { // We only want to hide the media controller when it's NOT ACTIVE
                if(!stayActive) //prevent a memory leak error. If it's not active, then we can hide it.
                {
                    super.hide();
                    stayActive = true; //auto set it to true.
                }
            } //do not hide!

            public boolean dispatchKeyEvent(KeyEvent event) // Overwrite the "go back" button for media controller, as we want go back to send to previous activity instead.
            {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                    ((Activity) getContext()).finish();

                return super.dispatchKeyEvent(event);
            }
        };
        controller.setMediaPlayer(this);

        mediaPlayer = MediaPlayer.create(this, Settings.notifySound);
        if (mediaPlayer == null) //this may occur if user uninstall app
        {
            Settings.notifySound = Settings.defaultSound;
            mediaPlayer = MediaPlayer.create(this, R.raw.notify);
        }

        mediaPlayer.setOnPreparedListener(mediaPlayer -> {
            controller.setAnchorView(controllerLocation); //invisible frame that helps guide the location
            controller.show();
        });

        //CUSTOM NOTIFICATION SOUND
        findViewById(R.id.buttonSound).setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("audio/*");
            soundResultLauncher.launch(intent);
        });

        findViewById(R.id.buttonResetSound).setOnClickListener(view -> {
            Settings.notifySound = Settings.defaultSound;
            mediaPlayer = MediaPlayer.create(this, R.raw.notify);
        });

        soundResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK)
            {
                Uri soundURI = null;
                if(result.getData() != null)
                    soundURI = result.getData().getData();

                if(soundURI == null)
                    return;

                try
                {
                    Settings.notifySound = soundURI;

                    mediaPlayer.reset();
                    mediaPlayer.setDataSource(SettingsActivity.this,soundURI);
                    mediaPlayer.prepare();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }//onCreate

    @Override
    protected void onPause() // Activity is not visible anymore NOTE: (Previously was onStop, but caused some potential error as that only occurs once the activity gets destroyed
    {
        super.onPause();

        stayActive = false; // hide the media controller, as it doesn't need to be active anymore
        controller.hide();

        //Clear and save the preferences.
        Settings.preferences.edit().clear() // clear
            .putInt(Settings.MAG_THRESHOLD_KEY,Settings.defaultMagnetThreshold)
            .putInt(Settings.DELAY_TIME_KEY,Settings.delayTime)
            .putInt(Settings.POCKET_KEY,Settings.pocketAmbientThreshold)
            .putBoolean(Settings.LIGHT_KEY,Settings.defaultLight)
            .putString(Settings.SOUND_KEY,Settings.notifySound.toString())
            .apply();
    }//onStop

    @Override
    protected void onStart()
    {
        controller.show(); // reshow the controller
        super.onStart();
    }

    //MEDIA
    @Override
    public void start()
    {
        mediaPlayer.start();
    }

    @Override
    public void pause()
    {
        mediaPlayer.pause();
    }

    @Override
    public int getDuration()
    {
        return mediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition()
    {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos)
    {
        mediaPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying()
    {
        return mediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage()
    {
        return 0;
    }

    @Override
    public boolean canPause()
    {
        return true;
    }

    @Override
    public boolean canSeekBackward()
    {
        return true;
    }

    @Override
    public boolean canSeekForward()
    {
        return true;
    }

    @Override
    public int getAudioSessionId()
    {
        return 0;
    }
}
