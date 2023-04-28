package com.example.raceapp;

import android.app.AlertDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

//Shows the database to the user.
public class HistoryActivity extends BaseActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        super.createToolbar(R.string.history);

        RaceViewModel raceViewModel = new ViewModelProvider(this).get(RaceViewModel.class); //get ViewModel to call database methods

        //FIND CARD
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // layout manager determines when to reuse views

        //MEMBERS
        ArrayList<Race> races = new ArrayList<>(); // array the adapter will use. (Will be updated on observe)
        RaceAdapter adapter = new RaceAdapter(this, races, raceViewModel); // create the adapter
        recyclerView.setAdapter(adapter); //put the adapter in the recycler view

        raceViewModel.getAllRaces().observe(this, adapter::setRaces); //upon observing the live data, call the setRaces method.

        //Delete All button
        findViewById(R.id.buttonDeleteAll).setOnClickListener(view -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage(getString(R.string.delete_all_history));

            //overwrite
            dialogBuilder.setPositiveButton(getString(R.string.confirm), (dialogInterface, id) -> raceViewModel.deleteAll());

            //do not overwrite
            dialogBuilder.setNegativeButton(getString(R.string.cancel), (dialogInterface, id) -> { });

            dialogBuilder.show(); // show the dialog box
        });

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0,ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {


            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target)
            {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction)
            {
                List<Race> races = adapter.getRaces();
                Race race = races.get(viewHolder.getAdapterPosition());
                raceViewModel.deleteSingle(race);
            }
        });

        helper.attachToRecyclerView(recyclerView);
    }//onCreate
}//HistoryActivity
