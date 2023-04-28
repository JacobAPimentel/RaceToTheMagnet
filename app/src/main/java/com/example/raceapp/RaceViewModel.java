package com.example.raceapp;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

//This class is where we call all our methods from our code. It "abstracts" the repository for us.
public class RaceViewModel extends AndroidViewModel
{
    private final RaceRepository repos;
    private final LiveData<List<Race>> allRaces;

    public RaceViewModel(Application application)
    {
        super(application);
        repos = new RaceRepository(application);
        allRaces = repos.getAllRaces();
    }//RaceViewModel

    LiveData<List<Race>> getAllRaces()
    {
        return allRaces;
    }//getAllRaces

    public void deleteAll()
    {
        repos.deleteAll();
    }//deleteAll

    public void deleteSingle(Race race)
    {
        repos.deleteSingle(race);
    }//deleteSingle

    public void insert(Race race)
    {
        repos.insert(race);
    }//insert
}//RaceViewModel.class
