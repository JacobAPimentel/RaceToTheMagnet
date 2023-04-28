package com.example.raceapp;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;

//This class allows us to use use lists of Racers in the room database.
//Thank you: https://developer.android.com/training/data-storage/room/referencing-data
public class DataConverter implements Serializable
{
    @TypeConverter
    public String fromRacers(List<Racer> racers) //convert racers to json string.
    {
        Gson gson = new Gson(); //using gson library to convert it to json
        Type type = new TypeToken<List<Racer>>() {}.getType();
        return gson.toJson(racers, type);
    }//fromRacers

    @TypeConverter
    public List<Racer> toRacers(String racersString) //convert json to string
    {
        Gson gson = new Gson();
        Type type = new TypeToken<List<Racer>>() {}.getType();
        return gson.fromJson(racersString, type);
    }//toRacers
}//DataConverter
