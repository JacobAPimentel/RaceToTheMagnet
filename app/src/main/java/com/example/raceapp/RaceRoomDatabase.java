package com.example.raceapp;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

//A singleton class that creates our data base.
@Database(entities = {Race.class}, version = 1, exportSchema = false)
public abstract class RaceRoomDatabase extends RoomDatabase //abstract as we do not need to initialize the actual roomDatabase class
{
    public abstract RaceDao raceDao();

    private static RaceRoomDatabase INSTANCE; //make sure only one data base gets created

    static RaceRoomDatabase getDatabase(final Context context)
    {
        if (INSTANCE == null)
        {
            synchronized (RaceRoomDatabase.class) //one thread
            {
                if(INSTANCE == null)
                {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            RaceRoomDatabase.class, "races_database")
                            .fallbackToDestructiveMigration()
                            //.addCallback(sRoomDatabaseCallback) //Do not need callbacks for this app, no need to do anything special for onOpen or onCreate
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}
