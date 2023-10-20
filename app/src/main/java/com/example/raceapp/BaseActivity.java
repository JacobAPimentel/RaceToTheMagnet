package com.example.raceapp;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

//Holds navigation and toolbar so all activities have it.
public class BaseActivity extends AppCompatActivity
{
    protected void createToolbar(int titleID)
    {
        //Find the views
        Toolbar toolbar = findViewById(R.id.toolbar);

        //setToolbars
        toolbar.setTitle(titleID);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        setSupportActionBar(toolbar);
    }//createToolbar

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu); //put the toolbar xml into the menu.
        return true;
    }//onCreateOptionsMenu

    public void StartIntent(Class<?> activityClass) // use by  toolbar menu items
    {
        if (!activityClass.getName().equals(this.getClass().getName())) //If on the page already, do nothing.
        {
            Intent intent = new Intent(this, activityClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // no need to multiple new activities
            this.startActivity(intent);
            overridePendingTransition(0, 0); // Disables the default slide animation
        }
    }//StartIntent

    @Override //default on option select
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Class<?> activityClass; //Use unbound wildcard to prevent raw use
        int id = item.getItemId();

        if(id == R.id.action_home)
            activityClass = MainActivity.class;
        else if(id == R.id.action_history)
            activityClass = HistoryActivity.class;
        else if(id == R.id.action_settings)
            activityClass = SettingsActivity.class;
        else if(id == R.id.action_race)
            activityClass = RaceActivity.class;
        else
        {
            return false;
        }

        StartIntent(activityClass);
        return true;
    }//onOptionsItemSelected
}//BaseActivity