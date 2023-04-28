package com.example.raceapp;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//The repository is used to map the dao methods to executor methods so the threads can be separate.
public class RaceRepository
{
    //member variables
    private final RaceDao raceDao;
    private final LiveData<List<Race>> races;

    RaceRepository(Application application)
    {
        RaceRoomDatabase db = RaceRoomDatabase.getDatabase(application); //get the database
        raceDao = db.raceDao();
        races = raceDao.getAllRaces(); //get all races
    }//RaceRepository

    LiveData<List<Race>> getAllRaces()
    {
        return races;
    } //because races is a live data, it will automatically be updated.

    public void insert(Race race)
    {
        Execute(() -> raceDao.insert(race));
    }//insert

    public void deleteSingle(Race race)
    {
        Execute(() -> raceDao.deleteRace(race));
    }//deleteSingle

    public void deleteAll()
    {
        Execute(raceDao::deleteAll);
    }//deleteAll

    private void Execute(Runnable runnable) //runnable is a functional interface, we can use lambda as a parameter.
    {
        ExecutorService executor = Executors.newSingleThreadExecutor(); //create a separate thread
        executor.execute(runnable);
    }//Execute
}//RaceRepository
