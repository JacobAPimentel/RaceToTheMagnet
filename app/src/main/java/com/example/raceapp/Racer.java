package com.example.raceapp;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Racer implements java.io.Serializable
{
    private String name;
    private int bestAttempt = 0; //index value of the best attempt to make it easier to retrieve.
    private final List<Long> attempts = new ArrayList<>();

    public Racer(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<Long> getAttempts()
    {
        return attempts;
    }

    public void addAttempt(long time) //add attempt, and calculate best attempt
    {
        attempts.add(time);

        int curI = attempts.size() - 1;

        if (curI != 0) //First attempt will be the best attempt automatically
        {
            if(attempts.get(curI) < attempts.get(bestAttempt)) //see if the new attempt was fastest.
            {
                bestAttempt = curI;
            }
        }
    }

    public long getBestTime()
    {
        if (attempts.size() > 0) // in case user did not have any attempt...
            return attempts.get(bestAttempt);
        else
            return 1000000001; //a really large number
    }

    public int numAttempts()
    {
        return attempts.size();
    }

    @NonNull
    @Override
    public String toString() //so ArrayAdapter can use Racer.
    {
        return name;
    }


}
