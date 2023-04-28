package com.example.raceapp;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

//The dao is used to map sql "methods" from our database to methods we can use in code.
@Dao
public interface RaceDao
{
    @Query("SELECT * from race_history ORDER BY id DESC") //get race history by latest
    LiveData<List<Race>> getAllRaces();

    @Insert(onConflict = OnConflictStrategy.IGNORE) //ignore if conflicts
    void insert(Race race);

    @Query("DELETE FROM race_history") //delete entire table
    void deleteAll();

    @Delete //delete a single race entry
    void deleteRace(Race race);
}
