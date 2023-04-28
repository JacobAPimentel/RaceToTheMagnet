package com.example.raceapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.ArrayList;
import java.util.List;

//This class holds all the race information, and is used for database as an entity.
@Entity(tableName="race_history")
@TypeConverters({DataConverter.class}) //convert Racers List into a GSON.
public class Race implements java.io.Serializable
{
    @PrimaryKey(autoGenerate=true)
    private int id;

    private String name = "";
    private String uriString = null;
    private List<Racer> racers = new ArrayList<>();

    public Race()
    {
        racers.add(new Racer("Racer #1")); //add one racer to the list
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getUriString()
    {
        return uriString;
    }

    public List<Racer> getRacers()
    {
        return racers;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public void setUriString(String uriString)
    {
        this.uriString = uriString;
    }

    public void setRacers(List<Racer> racers)
    {
        this.racers = racers;
    }
}//Race
