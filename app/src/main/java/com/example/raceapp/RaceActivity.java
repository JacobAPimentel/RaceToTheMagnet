package com.example.raceapp;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RaceActivity extends BaseActivity implements View.OnClickListener, SensorEventListener
{
    //GLOBAL VIEWS
    private Spinner spinnerRacers;
    private TextView timer;
    private Button buttonTimer;
    private TableLayout tableAttempts;
    private NotificationManager notifyManager;
    private TextView magAccuracyStatus;
    PowerManager.WakeLock wakeLock;

    //LIGHT SENSOR / TIMER OPTIONS
    private boolean timerOn = false;
    private boolean inPocket = false;
    private long lastAmbientChange; //global changes
    private boolean determiningDarkness = false; //uses this for ambient darkness thresholds

    //SERVICES
    private Vibrator vibrator;

    //CURRENT RACER STATS
    private Race race;
    private Racer currentRacer;

    //NOTIFICATIONS
    private String PRIMARY_CHANNEL_KEY = ""; //the current channel being used
    private Uri currentSound = Settings.notifySound; // use to verify if user changed sound.
    private final int NOTIFY_ID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race);
        super.createToolbar(R.string.race);

        //global views
        spinnerRacers = findViewById(R.id.spinnerRacers);
        timer = findViewById(R.id.viewTimer);
        tableAttempts =findViewById(R.id.tableAttempts);
        buttonTimer = findViewById(R.id.buttonTimer);
        magAccuracyStatus = findViewById(R.id.magnetAccuracyView);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        race = Settings.race;

        //Spinner
        ArrayAdapter<Racer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, race.getRacers());
        spinnerRacers.setAdapter(adapter);
        spinnerRacers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id)
            {
                currentRacer = (Racer) spinnerRacers.getSelectedItem(); //change the object to current racer.

                tableAttempts.removeAllViews(); // clear the table

                //Set the racers attempt table.
                List<Long> attempts = currentRacer.getAttempts();
                for(int i = 0; i < attempts.size(); i++)
                {
                    AddAttemptRow(attempts.get(i), i + 1);
                }
            }//onItemSelected

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            { }
        });
        spinnerRacers.setSelection(0); //select the first racer

        //Button listener connection
        findViewById(R.id.buttonTimer).setOnClickListener(this); //will grab the onClick method
        findViewById(R.id.buttonResults).setOnClickListener(this); //will grab the onClick method

        CreateNotificationChannel(false); //Create / Retrieve notification channel.
    }//onCreate

    @Override
    protected void onStart()
    {
        super.onStart();

        //hide status view based on stopwatch mode
        if(Settings.isAutomatic)
        {
            magAccuracyStatus.setVisibility(View.VISIBLE);
        }
        else
        {
            magAccuracyStatus.setVisibility(View.INVISIBLE);
        }

        if(Settings.notifySound != currentSound) //it was updated, update channel to change sound.
        {
            currentSound = Settings.notifySound; //update to new sound
            CreateNotificationChannel(true);
        }

        if(Settings.light != null && Settings.isAutomatic) //If light sensor is not null and is automatic, then registered sensor.
        {
            Settings.sensorManager.registerListener(RaceActivity.this, Settings.light, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if(Settings.magnetic != null && Settings.isAutomatic) //If magnetic sensor is not null, then registered sensor.
        {
            Settings.sensorManager.registerListener(RaceActivity.this, Settings.magnetic, SensorManager.SENSOR_DELAY_FASTEST);
        }

        //Does not halt CPU when screen is on
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "raceapp:Timer");
        wakeLock.acquire(120*60*1000L /*120 minutes*/); //props to you if you spent two hours on the screen

        TempNotify(); //runs the secret notification channel so the audio will always be loaded. TODO: I probably don't need this anymore. This could actually been a prior CPU issue, which is now fixed with wake lock? You should test in the future.
    }//onStart

    @Override
    protected void onDestroy() //Will be called for Show Result
    {
        GarbageCollect();
        super.onDestroy();
    }

    public void GarbageCollect() //occurs when switching activities / on destroy
    {
        Settings.sensorManager.unregisterListener(this); //disable all sensors. on start will re-enable it again
        notifyManager.cancelAll(); //end all notifications

        if(wakeLock.isHeld())
        {
            wakeLock.release();
        }
    }

    public void StartRace(boolean ... delay) //so I do not need to specify false
    {
        if(timerOn) //Button debounce.
        {
            return;
        }

        timerOn = true;
        DisableRotation(true); //prevent changing orientation due to background thread. Do not want to create a new activity while that occurs
        buttonTimer.setText(getString(R.string.stop_timer)); //change the timer to stop_timer

        if(Settings.light != null)
        {
            Settings.sensorManager.unregisterListener(this, Settings.light); // no need for light sensor
        }

        /* Old method did have magnetic accuracy. Keeping this for records.
        if(Settings.magnetic != null && Settings.isAutomatic)
        {
            Settings.sensorManager.registerListener(RaceActivity.this, Settings.magnetic, SensorManager.SENSOR_DELAY_UI); //register event
        }
        */

        ExecutorService executor = Executors.newSingleThreadExecutor(); //create a separate thread to run in background
        executor.execute(() -> {
            if (delay.length > 0) //that means there's a delayTime argument, delay! (Only occurs if user does automatic + button timer.
            {
                runOnUiThread(() -> Toast.makeText(RaceActivity.this, getString(R.string.put_phone_in_pocket), Toast.LENGTH_LONG).show());

                Sleep(Settings.delayTime * 1000L); //Wait a bit until starting.
            }

            NotifyStart(); //Send a notification that the race started.

            long start = System.currentTimeMillis(); //start time
            long currentTime = 0; // current time

            while(timerOn)
            {
                currentTime = System.currentTimeMillis() - start; //calculate the currentTime. Outer scope will also use it when timer is over
                final long time = currentTime; //this will be used for the ui thread, it need to be final

                runOnUiThread(() -> timer.setText(Format.Time(time))); //change the timer

                Sleep(10); //give the program a little breathing room, do not want to overdo it by not having a sleep.
                                //TODO..maybe | Perhaps add a "timer increment" option? The more it sleep, the better the performance could be.
            }

            //timer is over, magnetic sensor turned it false OR user clicked stop.
            final long time = currentTime; //used for runOnUiThread
            currentRacer.addAttempt(currentTime); //inside this method will also calculate the best time.
            NotifyFinish();

            runOnUiThread(() -> {
                AddAttemptRow(time, currentRacer.numAttempts());
                buttonTimer.setText(getString(R.string.start_timer));
            }); //add the new attempt to the table

            if(Settings.light != null && Settings.isAutomatic) //register the light sensor again as race is over.
            {
                Settings.sensorManager.registerListener(RaceActivity.this, Settings.light, SensorManager.SENSOR_DELAY_NORMAL);
            }
            DisableRotation(false); //give back control
        });
    }//StartRace

    public void DisableRotation(boolean disable) //do not want to change orientation when timer is on.
    {
        //Thank you: https://stackoverflow.com/questions/2366706/how-to-lock-orientation-during-runtime/10488012#10488012
        if(disable)
        {
            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
            {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
            else
            {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED); //give back control
        }

    }

    public void AddAttemptRow(Long time, int attemptIndex)
    {
        TextView[] cols = Format.AddRow(this, tableAttempts);

        cols[0].setText(getString(R.string.attempt_number,attemptIndex));
        cols[1].setText(Format.Time(time));
    }//AddAttemptRow

    public void Sleep(long milli) //I was getting sick of doing multiple try catch.
    {
        try
        {
            Thread.sleep(milli); //wait a millisecond to prevent overload
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }//Sleep

    public void CreateNotificationChannel(boolean newChannel)
    {
        notifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            List<NotificationChannel> channels = notifyManager.getNotificationChannels();

            if(channels.size() > 0) //channel already exist
            {
                for(NotificationChannel channel : channels)
                {
                    if(!channel.getId().contains("_s")) //make sure it's not the sound channel
                    {
                        PRIMARY_CHANNEL_KEY = channel.getId(); //get the current channel key.

                        if((channel.getSound().equals(currentSound) && !newChannel)) //verify if we should create a new channel.
                        {
                            return;
                        }

                        break; //break out of the loop, it's not the same. Create new channel
                    }
                }
            }

            //If user make it to here, then we are creating a new channel

            //delete all channels
            for(NotificationChannel channel : channels)
            {
                notifyManager.deleteNotificationChannel(channel.getId());
            }

            PRIMARY_CHANNEL_KEY = Long.toString(System.currentTimeMillis()); //new primary channel, will be the time.

            NotificationChannel notificationChannel = new NotificationChannel
                    (PRIMARY_CHANNEL_KEY, getString(R.string.primary_channel),NotificationManager.IMPORTANCE_HIGH);

            notificationChannel.enableLights(true);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription(getString(R.string.channel_description));

            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            notificationChannel.setSound(Settings.notifySound, attributes); //use the new sound

            notifyManager.createNotificationChannel(notificationChannel);

            //super secret notification whose only job is to load the sound file.
            NotificationChannel secretChannel = new NotificationChannel
                    (PRIMARY_CHANNEL_KEY + "_s", getString(R.string.sound_channel),NotificationManager.IMPORTANCE_MIN); //So it won't display in the top bar.

            secretChannel.enableLights(false);
            secretChannel.setSound(Settings.notifySound, attributes);
            notifyManager.createNotificationChannel(secretChannel);
        }
    }//CreateNotificationChannel

    public PendingIntent CreatePendingIntent(int requestCode)
    {
        Intent notificationIntent = new Intent(this, RaceActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //We do not want to create a new activity, just want to reopen the one that is already there.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            return PendingIntent.getActivity(this, requestCode,
                    notificationIntent,PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        }
        else
        {
            return null;
        }

    }//CreatePendingIntent

    public void TempNotify() //This right here is my solution of having notification audio always being loaded.
    {
        PendingIntent notificationPendingIntent = CreatePendingIntent(NOTIFY_ID);

        if (notificationPendingIntent == null) //older version
        {
            return;
        }

        NotificationCompat.Builder notifyBuilder = new NotificationCompat
                .Builder(this, PRIMARY_CHANNEL_KEY + "_s")
                .setSmallIcon(R.drawable.ic_baseline_directions_run_24)
                .setTimeoutAfter(99999999) //long time, but it makes it swipeable.
                .setAutoCancel(false).setContentIntent(notificationPendingIntent);

        try
        {
            notifyManager.notify(22, notifyBuilder.build()); //on a separate channel. The audio will still be loaded
        }
        catch (java.lang.SecurityException e) //This error can occur if the audio file is lost.
        {
            //Log.d("E",getString(R.string.notify_error));

            //Reset the sound notification
            Settings.preferences.edit()
                    .putString(Settings.SOUND_KEY,Settings.defaultSound.toString())
                    .apply();
            Settings.notifySound = Settings.defaultSound;
            CreateNotificationChannel(true); //make a new channel with default sound
            TempNotify(); //run again
        }
    }

    public void NotifyFinish()
    {
        PendingIntent notificationPendingIntent = CreatePendingIntent(NOTIFY_ID);

        if (notificationPendingIntent == null) //older version
        {
            VibratePhone();
            return;
        }

        NotificationCompat.Builder notifyBuilder = new NotificationCompat
                .Builder(this, PRIMARY_CHANNEL_KEY)
                .setContentTitle(getString(R.string.race_ended))
                .setContentText(getString(R.string.race_ended_notify_desc))
                .setSmallIcon(R.drawable.ic_baseline_directions_run_24)
                .setOngoing(false)
                .setTimeoutAfter(3000)
                .setAutoCancel(true).setContentIntent(notificationPendingIntent);

        notifyManager.notify(NOTIFY_ID, notifyBuilder.build());
    }//NotifyFinish

    public void NotifyStart()
    {
        PendingIntent notificationPendingIntent = CreatePendingIntent(NOTIFY_ID);

        if (notificationPendingIntent == null) //older version
        {
            VibratePhone();
            return;
        }

        NotificationCompat.Builder notifyBuilder = new NotificationCompat
                .Builder(this, PRIMARY_CHANNEL_KEY)
                .setContentTitle(getString(R.string.race_started))
                .setContentText(getString(R.string.started_notify_desc))
                .setSmallIcon(R.drawable.ic_baseline_directions_run_24)
                .setOngoing(true) //don't want it to be able to swipe away.
                .setAutoCancel(false).setContentIntent(notificationPendingIntent);

        notifyManager.notify(NOTIFY_ID, notifyBuilder.build());
    }//NotifyStart

    //BUTTON METHODS
    @Override
    public void onClick(View view) //every button refer to this onclick method
    {
        int id = view.getId();

        if(id == R.id.buttonTimer)
        {
            if(Settings.isAutomatic) //User clicked start time in automatic mode. Need time to put phone in pocket.
            {
                if(!timerOn)
                {
                    inPocket = true; //The phone should be in the user pocket in this scenario, so set it to true.
                    StartRace(true); //start race with delay. This will disable the light sensor
                }
                else //timer is on
                {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                    dialogBuilder.setMessage(getString(R.string.stop_timer_automatic));

                    dialogBuilder.setPositiveButton(getString(R.string.confirm), (dialogInterface, i) -> timerOn = false);
                    dialogBuilder.setNegativeButton(getString(R.string.no), (dialogInterface, i) -> { });

                    dialogBuilder.show(); // show the dialog box
                }
            }
            else //classic mode
            {
                if(!timerOn) //timer not on, start it.
                {
                    StartRace();
                }
                else // timer is on, stop it.
                {
                    timerOn = false;
                }
            }
        }
        else if(id == R.id.buttonResults)
        {
            if(timerOn || determiningDarkness) //Should not do anything if timer is on or phone in pocket
            {
                return;
            }

            //alert
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage(getString(R.string.show_results_description));

            dialogBuilder.setPositiveButton(getString(R.string.confirm), (dialogInterface, i) -> {
                if(timerOn || determiningDarkness){return;}//in case phone is in pocket when dialog open up.

                Settings.sensorManager.unregisterListener(this); //unregister everything

                RaceViewModel raceViewModel = new ViewModelProvider(RaceActivity.this).get(RaceViewModel.class);

                raceViewModel.insert(race); //insert the race object into the database
                Settings.race = new Race(); //Create a new race.

                Intent intent = new Intent(RaceActivity.this, HistoryActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // starts a new history activity and clear roots. Practically resets the app.
                startActivity(intent); //go to history
            });

            dialogBuilder.setNegativeButton(getString(R.string.no), (dialogInterface, i) -> { });

            dialogBuilder.show(); // show the dialog box
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        if(sensorEvent.sensor == Settings.magnetic)
        {
            float mag = Settings.GetMagneticStrength(sensorEvent.values);

            if(mag > Settings.magnetThreshold) //unregister event and turn off timer. Everything else will be in the TIMER code!
            {
                //unregister listeners
                //Settings.sensorManager.unregisterListener(this, Settings.magnetic); // Old method did have have accuracy, so it did not need to be on 24/7.
                timerOn = false; //This will end the while loop in timer.
            }
        }
        else if(sensorEvent.sensor == Settings.light)
        {
            float ambient = sensorEvent.values[0];

            if(inPocket) //phone was in pocket... check if phone is out of pocket
            {
                if(ambient <= Settings.pocketAmbientThreshold) //still in pocket
                {
                    return;
                }
                else //out of pocket!
                {
                    inPocket = false;
                }
            }

            long ambientChangeTime = System.currentTimeMillis(); //local ambient time
            if (ambient <= Settings.pocketAmbientThreshold)
            {
                if(!determiningDarkness) //Is negative 1 - update the darkness time.
                {
                    determiningDarkness = true;
                    lastAmbientChange = ambientChangeTime;

                    ExecutorService executor = Executors.newSingleThreadExecutor(); //create a separate thread
                    executor.execute(() -> {
                        Sleep(Settings.delayTime * 1000L); //wait the global delay time setting

                        if (lastAmbientChange == ambientChangeTime) // after 3 seconds, it is the same dark time... start!
                        {
                            inPocket = true; //so when it register light again, it knows it's "in pocket"
                            StartRace();
                        }
                        determiningDarkness = false; //No more waiting.
                    });
                }
                //else, do nothing. We do not want to update the time if the darkness is under the acceptable threshold
            }
            else //bright, not in pocket.
            {
                lastAmbientChange = ambientChangeTime;
            }
        }
    }//onSensorChanged

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        if(sensor == Settings.magnetic)
        {
            magAccuracyStatus.setText(Settings.GetAccuracyString(this,accuracy));
        }
    }

    @Override //default on option select
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(!timerOn && !determiningDarkness) //disable moving through activities if timer is on.
        {
            GarbageCollect(); //want to kill all notifications and wakelock
            super.onOptionsItemSelected(item); //do the default afterwards
        }

        return true;
    }//onOptionsItemSelected

    @Override
    public void onBackPressed() {
        if(!timerOn && !determiningDarkness) //prevent going back if timer is on. This is in case if user did not turn off their screen while racing.
        {
            super.onBackPressed();
        }
    }

    //NOT USED METHODS.
    public void VibratePhone() //old method of notifying user, keeping it for record.
    {
        int vibrateLength = 2000;
        if(Build.VERSION.SDK_INT >= 26) // use newer vibrate method
            vibrator.vibrate(VibrationEffect.createOneShot(vibrateLength,255)); // max amplitude
        else
            vibrator.vibrate(vibrateLength);
    }
}