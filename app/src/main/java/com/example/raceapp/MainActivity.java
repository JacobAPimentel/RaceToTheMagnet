package com.example.raceapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements View.OnClickListener, SensorEventListener
{
    //VIEWS
    private LinearLayout racersView;
    private TextView textViewMagnetStatus;
    private TextView textViewAverageStrength;

    //CODES
    private final int CAMERA_PERM = 123;

    //INTENT LAUNCHER
    private ActivityResultLauncher<Intent> photoResultLauncher;

    //PHOTO
    private Uri photoUri;

    //SENSORS
    private final List<Float> recentStrengths = new ArrayList<>();

    //Edit text listener to disable keyboard when needed
    private boolean canHideKeyboard = true; //racer cells will disable this
    //onDone
    private final TextView.OnEditorActionListener editActionListener = (view, actionId, keyEvent) -> {
        if(actionId == EditorInfo.IME_ACTION_DONE) //Should be last child of the table (add row initialize action buttons)
        {
            view.clearFocus(); // clears the focus
        }
        return true;
    };

    public static boolean LOADED_PREFERENCES = false; //only need to load once
    //onFocusChange
    private final View.OnFocusChangeListener focusListener = (view, hasFocus) -> {
        EditText editTextView = (EditText) view; // current EditText
        if (!hasFocus)
        {
            HideKeyboard(view); // hide the keyboard when focus is gone.

            int id = editTextView.getId();

            if(id == R.id.editMagnetThreshold) // set the threshold
            {
                try
                {
                    Settings.magnetThreshold = Double.parseDouble(editTextView.getText().toString());
                }
                catch (NumberFormatException e)
                {
                    //Log.d("E", getString(R.string.double_failed));
                }

                editTextView.setText(""); //clear the text, hint will display it.
                editTextView.setHint(getString(R.string.magnetic_threshold,Settings.magnetThreshold));
            }
            else if(id == R.id.editRaceName)
            {
                Settings.race.setName(editTextView.getText().toString());
            }
            else //must be racer name.
            {
                int childID = racersView.indexOfChild(editTextView);
                String name = editTextView.getText().toString();

                if(name.isEmpty())
                {
                    Settings.race.getRacers().get(childID) //get corresponding racer
                            .setName(getString(R.string.racer_number,childID + 1)); //Set default name if empty (Racer #1)
                }
                else
                {
                    Settings.race.getRacers().get(childID)
                            .setName(name);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        super.createToolbar(R.string.create_race);

        //GLOBAL VIEWS
        racersView = findViewById(R.id.racersLinearLayout);
        textViewMagnetStatus = findViewById(R.id.textViewMagnetStatus);
        textViewAverageStrength = findViewById(R.id.textViewAverageStrength);

        //SET BUTTON ON CLICK LISTENER EVENT.
        findViewById(R.id.buttonAddRow).setOnClickListener(this); //the activity implements onClick method.
        findViewById(R.id.buttonDeleteRow).setOnClickListener(this);
        findViewById(R.id.buttonStartRace).setOnClickListener(this);

        ImageButton imageButtonRace = findViewById(R.id.imageButtonRace);
        imageButtonRace.setOnClickListener(this); //will grab the onClick method

        //INTENT LAUNCHER FOR IMAGE
        //onActivityResult
        photoResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK)
            {
                //Uri data = result.getData().getData();  //Because we are using EXTRA_OUTPUT, no intents get sent. It automatically stores it in the photoUri.
                imageButtonRace.setImageURI(photoUri);
                Settings.race.setUriString(photoUri.toString());
            }
            else //failed, delete file stored in uri
            {
                getContentResolver().delete(photoUri,null,null);
            }
        });

        //GET THE SENSORS
        Settings.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Settings.magnetic = Settings.sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Settings.light = Settings.sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        //SET ENABLE LIGHT VIEW
        SwitchCompat switchLight = findViewById(R.id.switchLight);
        if (Settings.light == null) //no light sensor
        {
            switchLight.setVisibility(View.GONE); //hide the view
        } else //there's a light sensor, set the light switch code.
        {
            //onCheckedChanged
            switchLight.setOnCheckedChangeListener((compoundButton, isEnabled) -> {
                if (isEnabled)
                {
                    Settings.light = Settings.sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
                }
                else
                {
                    Settings.light = null; //Settings activity treats null as "default".
                }
            });
        }

        //Give edit text the focus change listeners.
        EditText editRaceName = findViewById(R.id.editRaceName);
        editRaceName.setOnFocusChangeListener(focusListener);
        editRaceName.setOnEditorActionListener(editActionListener);

        //Setup MagnetThreshold
        EditText editMagnetThreshold = findViewById(R.id.editMagnetThreshold);
        editMagnetThreshold.setOnFocusChangeListener(focusListener);
        editMagnetThreshold.setHint(getString(R.string.magnetic_threshold, Settings.magnetThreshold)); // change the hint to the current

        //onEditorAction TODO: You can probably just set the editActionListener variable to here?
        editMagnetThreshold.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) //Last racer cell will only have DONE.
            {
                textView.clearFocus(); //clear the focus, which will end up calling the text focus change listener code.
                return true;
            }
            return false;
        });

        //SET UP PREFERENCES (ONLY NEED TO DO ONCE!)
        if(!LOADED_PREFERENCES)
        {
            Settings.preferences = getSharedPreferences("settings", MODE_PRIVATE);
            Settings.pocketAmbientThreshold = Settings.preferences.getInt(Settings.POCKET_KEY, Settings.pocketAmbientThreshold);
            Settings.defaultMagnetThreshold = Settings.preferences.getInt(Settings.MAG_THRESHOLD_KEY, Settings.defaultMagnetThreshold);
            Settings.magnetThreshold = Settings.defaultMagnetThreshold; //change the magnetThreshold based on default
            Settings.delayTime = Settings.preferences.getInt(Settings.DELAY_TIME_KEY, Settings.delayTime);
            Settings.defaultLight = Settings.preferences.getBoolean(Settings.LIGHT_KEY, Settings.defaultLight);

            String soundString = Settings.preferences.getString(Settings.SOUND_KEY, Settings.defaultSound.toString());
            Settings.notifySound = Uri.parse(soundString);
            LOADED_PREFERENCES = true;
        }
        // If you decide to add more settings in the future, consider using preferences.getAll(), which returns a map. The settings itself would have to be map based too though.

        //BASED ON PREFERENCES, UPDATE CERTAIN VIEWS.
        editMagnetThreshold.setHint(getString(R.string.magnetic_threshold, Settings.magnetThreshold));
        switchLight.setChecked(Settings.defaultLight);
        if(!Settings.defaultLight){Settings.light = null;} //surprisingly setChecked is not firing onSetCheck.

        if (Settings.race == null) //This will only occur when user open the app. There is no current race, so create a new one.
        {
            Settings.race = new Race(); //the constructor will create a default race object with one racer in tact.
        }

        //populate the current list of racers
        for (int i = 0; i < Settings.race.getRacers().size(); i++)
        {
            AddRacerRow(Settings.race.getRacers().get(i).getName());
        }

        //In case of orientation change
        String photoUriString = Settings.race.getUriString();
        if (photoUriString != null) //there's a photo uri path
        {
            imageButtonRace.setImageURI(Uri.parse(photoUriString));
        }
    }//onCreate

    public void SetRacerCell(EditText view) //Add a racer row for the table.
    {
        view.setOnFocusChangeListener(focusListener); //Add the edit text focus listener.

        view.setOnEditorActionListener((view1, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) //If user press the next button, go to next cell.
            {
                canHideKeyboard = false; //Disable the auto hide keyboard to prevent unwanted behavior. (HideKeyboard method will check this)
                int currentIndex = racersView.indexOfChild(view1); //get the current child id

                View nextChild = racersView.getChildAt(currentIndex + 1); //Using the current child id, get the next child view.

                view1.clearFocus(); //We do not need the current child focus anymore. (this will also call the focus listener to set the text to the race object)
                if (nextChild != null) //there is a next child, request focus
                {
                    nextChild.requestFocus();
                }
                canHideKeyboard  = true;
                return true;
            }
            else if(actionId == EditorInfo.IME_ACTION_DONE) //Should be last child of the table (add row initialize action buttons)
            {
                view1.clearFocus(); // clears the focus
            }
            return false;
        });
    }//SetRacerCell

    @Override
    protected void onStart()
    {
        super.onStart();

        //SET UP THE RADIO GROUP (radio group can register/unregister, therefore is in onStart).
        RadioGroup modeRadio = findViewById(R.id.radioGroup);
        //onCheckedChanged
        modeRadio.setOnCheckedChangeListener((radioGroup, id) -> {
            RadioButton button = findViewById(id);

            if(button.isChecked()) //make sure it's the one that is checked
            {
                SetStopwatchMode(id == R.id.radioAuto); //set based on if it is automatic or manual
            }
        });

        //VERIFY THE SENSOR
        if(Settings.magnetic != null) //sensor exists
        {
            textViewMagnetStatus.setText(getString(R.string.magnet_status, getString(R.string.unreliable))); // in case sensor is unreliable from the get go.

            if(modeRadio.getCheckedRadioButtonId() == R.id.radioAuto) //radio button is already checked, manually add the registerLister (only do this as sensor gets unregistered when user close app)
            {
                Settings.sensorManager.registerListener(MainActivity.this, Settings.magnetic, SensorManager.SENSOR_DELAY_UI);
            }
            else if(modeRadio.getCheckedRadioButtonId() == -1) //if there is no checked button, this means this the user opened the app. Auto check auto.
            {
                modeRadio.check(R.id.radioAuto); //enable automatic view (this will also register the magnetic sensor inside the radio method!)
            }
        }
        else //sensor does not exist
        {
            textViewMagnetStatus.setText(getString(R.string.magnet_status, "SENSOR NOT DETECTED"));

            if(modeRadio.getCheckedRadioButtonId() == -1) //if there is no checked button, this means this the user opened the app.
            {
                modeRadio.check(R.id.radioManual); //enable manual view
                modeRadio.findViewById(R.id.radioAuto).setEnabled(false); //no sensors, cannot enable automatic mode
            }
        }
    }//onStart

    public void SetStopwatchMode(boolean isAutomatic)
    {
        ConstraintLayout sensorSettingsConstraint = findViewById(R.id.sensorSettingsConstraint); //get the settings constraint

        if(isAutomatic && Settings.magnetic != null) //isAutomatic and sensor exists...
        {
            Settings.sensorManager.registerListener(this, Settings.magnetic, SensorManager.SENSOR_DELAY_UI);
        }
        else if(Settings.magnetic != null) //If it is not automatic yet there is a sensor, unregister the sensor. It is unneeded.
        {
            Settings.sensorManager.unregisterListener(this, Settings.magnetic);
        }

        Settings.isAutomatic = isAutomatic; //So RaceActivity will know the current mode.
        sensorSettingsConstraint.setVisibility(isAutomatic ? View.VISIBLE : View.GONE); //Hide the settings constraint based on mode.
    }//SetStopwatchMode

    @Override
    protected void onStop()
    {
        super.onStop();
        Settings.sensorManager.unregisterListener(this); //unregister listeners as we do not need it to run in the background.
    }//onStop

    public void getRaceImage()
    {
        String[] permissions =
        {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
        }; //All of the permissions we need for race image.

        boolean permissionGranted = true; //default to true
        for(String permission : permissions)
        {
            if(ContextCompat.checkSelfPermission //permission was not granted
                    (this.getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED)
            {
                permissionGranted = false;
                break; //break out of the loop, permission was not granted.
            }
        }

        if (permissionGranted) //permission granted! Open camera app.
        {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            //will save the photo to photos.
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE,getString(R.string.photo_title));
            values.put(MediaStore.Images.Media.DESCRIPTION,getString(R.string.photo_description));
            photoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri); //the photo will be store in the uri location. This will result in their being no intent sent to result!
            photoResultLauncher.launch(cameraIntent);
        }
        else //permission was not granted, request permission.
        {
            ActivityCompat.requestPermissions(this, permissions, CAMERA_PERM);
        }
    }//getRaceImage

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERM) //gets called from getRaceImage.
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                getRaceImage(); //Permission granted now, try again.
            }
            else //not granted, notify user they cannot do custom images.
            {
                Toast.makeText(this, getString(R.string.camera_denied), Toast.LENGTH_LONG).show();
            }
        }
    }//onRequestPermissionsResult

    public void AddRacerRow( String ... preexistingName) //addToList will be false if it's just reloading the tables (orientation change for example)
    {
        //Remember: this is linear layout, not a table.
        EditText newRow = new EditText(this);

        //Set up the properties
        newRow.setLayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,ActionBar.LayoutParams.WRAP_CONTENT));
        newRow.setHint(R.string.racer_name);
        newRow.setGravity(Gravity.CENTER);
        newRow.setSingleLine(); //So they cannot enter a new line character.
        newRow.setImeOptions(EditorInfo.IME_ACTION_DONE); //newest row (which will be last) will have a DONE button.

        String racerName;
        if (preexistingName.length > 0) //this occurs when trying to repopulate a race table
        {
            racerName = preexistingName[0];
        }
        else //adding a new racer
        {
            racerName = getString(R.string.racer_number,racersView.getChildCount() + 1); // + 1 as index start at 0
        }

        newRow.setHint(racerName); //set the placeholder/racer name as hint.

        SetRacerCell(newRow); //Set the event listeners for the new racer.
        racersView.addView(newRow); //add the row to the linear layout.

        //Find the previous child and change button to next.
        if(racersView.getChildCount() > 1) //first row will also be created
        {
            EditText previousET = (EditText) racersView.getChildAt(racersView.getChildCount() - 2);
            previousET.setImeOptions(EditorInfo.IME_ACTION_NEXT); // set the previous child to "Next"
        }

        if(preexistingName.length == 0) //no preexisting name, this is a new racer from "add row button". Add to race object!
        {
            Settings.race.getRacers().add(new Racer(racerName)); //add the new racer to the list.
        }
    }

    //BUTTON METHODS
    @Override
    public void onClick(View view) //every button refer to this onclick method
    {
        int id = view.getId();

        findViewById(R.id.scrollViewConstraint).requestFocus(); //Because scrollview is focusable for edit text, request the focus in case user has keyboard open. This will close the keyboard if user click a button.

        if(id == R.id.buttonAddRow) //add a new racer
        {
            AddRacerRow();
        }
        else if(id == R.id.buttonDeleteRow) //delete a racer row
        {
            int lastChildIndex = racersView.getChildCount() - 1; //Get the last child index from size.

            if(lastChildIndex > 0) //If there is more than one child, set the previous index to Done (as it will be the last now).
            {
                EditText et = (EditText) racersView.getChildAt(lastChildIndex - 1); //second to last child
                et.setImeOptions(EditorInfo.IME_ACTION_DONE); // set the 2nd to last child to "Done"
            }

            if (lastChildIndex != 0) //make sure you don't destroy the only row, as you need at least one racer.
            {
                Settings.race.getRacers().remove(lastChildIndex);
                racersView.removeViewAt(lastChildIndex);
            }
        }
        else if(id == R.id.imageButtonRace) //custom image
        {
            getRaceImage();
        }
        else if(id == R.id.buttonStartRace)
        {
            super.StartIntent(RaceActivity.class); //call the BaseActivity start intent
        }
    }//onClick

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {//Only magnetic sensor will be in this activity, no need to check which is which.
        float mag = Settings.GetMagneticStrength(sensorEvent.values); //Get the magnetic strength from the values.

        recentStrengths.add(mag); //Add the mag to the recent strengths

        if (recentStrengths.size() >= 10) //If recentStrengths size is 10, grab the largest mag to represent the "average"
        {
            float max = recentStrengths.get(0);
            for(int i = 1; i < recentStrengths.size(); i++) //i is 1 as max is set to index 0 strength.
            {
                max = (recentStrengths.get(i) > max) ? recentStrengths.get(i) : max; //compare and set the new max if larger.
            }

            textViewAverageStrength.setText(getString(R.string.average_strength,max)); //Display the "average"
            recentStrengths.clear(); //Reset the list.
        }
    }//onSensorChanged

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    { //As we are working with magnets, the accuracy may get inaccurate. Therefore, tell the user of their magnets' accuracy.
        textViewMagnetStatus.setText(Settings.GetAccuracyString(this, accuracy));
    }//onAccuracyChange

    public void HideKeyboard(View view) //called from edittext focus change listener.
    {
        if (canHideKeyboard) //Make sure the keyboard can be hidden.
        {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    } // HideKeyboard
}//MainActivity