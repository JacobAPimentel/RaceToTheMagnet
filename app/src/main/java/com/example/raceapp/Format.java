package com.example.raceapp;

import android.content.Context;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Locale;

//This class includes formatting code that multiple activities may call
public class Format
{
    public static TextView[] AddRow(Context context, TableLayout parent) //Add row to the parent
    {
        //create attempt row
        TableRow row = new TableRow(context);
        TextView col1 = new TextView(context);
        TextView col2 = new TextView(context);
        col1.setTextSize(20);
        col2.setTextSize(20);

        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        col1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        col2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        col2.setGravity(Gravity.END);

        row.addView(col1);
        row.addView(col2);

        if(parent.getChildCount() % 2 == 1) //banded row
        {
            row.setBackgroundColor(context.getResources().getColor(R.color.gray_light));
        }
        else
        {
            row.setBackgroundColor(context.getResources().getColor(R.color.white));
        }

        parent.addView(row,new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

        return new TextView[] {col1, col2}; //returns an array to send both col1 and col2 references.
    }//AddRow

    public static String Time(long time) //format millisecond time to a decimal.
    {
        return String.format(Locale.US,"%.2f",(float) time / 1000);
    }//Time
}//Format
