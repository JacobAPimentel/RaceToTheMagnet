package com.example.raceapp;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* ADAPTER FOR RECYCLER. ALSO HAVE ON CLICK LISTENERS FOR THE BUTTONS */
public class RaceAdapter extends RecyclerView.Adapter<RaceAdapter.ViewHolder>
{
    // Member variables.
    private List<Race> races;
    private final Context context;
    private final RaceViewModel raceViewModel; //so delete button has access to it

    //CONSTRUCTOR
    public RaceAdapter(Context context, ArrayList<Race> races, RaceViewModel raceViewModel)
    {
        this.races = races;
        this.context = context;
        this.raceViewModel = raceViewModel;
    }//RaceAdapter

    @NonNull
    @Override
    public RaceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.race_card, parent, false)); //give the ViewHolder the race card layout.
    }//onCreateViewHolder

    @Override
    public void onBindViewHolder(RaceAdapter.ViewHolder viewHolder, int position)
    {
        // Get current race item.
        Race currentRace = races.get(position);

        // Populate the views with data.
        viewHolder.bindTo(currentRace);
    }//OnBindViewHolder

    public void setRaces(List<Race> races) //changes the order list based on LiveData
    {
        this.races = races;
        notifyDataSetChanged();
        //maybe in the future focus on the notifyItem____ methods (one by one, rather than updating all).
        // See here: https://stackoverflow.com/questions/68602157/it-will-always-be-more-efficient-to-use-more-specific-change-events-if-you-can
    }//setOrders

    public List<Race> getRaces()
    {
        return races;
    }

    @Override
    public int getItemCount() {
        return races.size();
    }//getItemCount

    class ViewHolder extends RecyclerView.ViewHolder
    {
        //The views for the data to be changed
        private final TextView viewName;
        private final ImageView imageRace;
        private final TableLayout tableResults;

        ViewHolder(View itemView)
        {
            super(itemView);

            // Initialize the views.
            viewName = itemView.findViewById(R.id.viewRaceName);
            imageRace = itemView.findViewById(R.id.imageResultPhoto);
            tableResults = itemView.findViewById(R.id.tableResults);
        }

        void bindTo(Race currentRace)
        {
            //Grab the Race object and assign it to the card view.
            if(currentRace.getName().isEmpty())
            {
                viewName.setText(context.getString(R.string.race_number,currentRace.getId())); // race #id
            }
            else
            {
                viewName.setText(currentRace.getName());
            }

            //Set the race image
            if (currentRace.getUriString() != null)
            {
                Uri photoUri = Uri.parse(currentRace.getUriString());
                Glide.with(context).load(photoUri).into(imageRace); // quickly change image
                imageRace.setVisibility(View.VISIBLE);
            }
            else
            {
                imageRace.setVisibility(View.GONE); //there is no image, no need to have the view.
            }

            //Get the base time for each racer and put it in the table
            List<Racer> racers = currentRace.getRacers();

            //Sort the racer
            Collections.sort(racers,(racer1, racer2) -> Long.compare(racer1.getBestTime(), racer2.getBestTime()));

            tableResults.removeAllViews(); //clear the table. Do not want to keep creating rows everytime bindTo gets called (which will due to notifyChanged.
                                            // (Would not be needed if you do separate notify events as talked about on notifyChanged comment)

            for(int i = 0; i < racers.size(); i++)
            {
                Racer racer = racers.get(i);
                TextView[] cols = Format.AddRow(context,tableResults);
                cols[0].setText(racer.getName());

                long racerBestTime = racer.getBestTime();
                cols[1].setText(racerBestTime > 1000000000 ? "N\\A" : Format.Time(racer.getBestTime())); //A large number occurs if racer never attempted. This is to make sorting easier.
            }

            //Delete button. In bindTo so we can keep track of current order.
            Button deleteButton = itemView.findViewById(R.id.buttonDeleteRace);
            deleteButton.setOnClickListener(view -> {
                //Creates an alert dialog confirmation box
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                dialogBuilder.setMessage(context.getString(R.string.delete_entry));

                dialogBuilder.setPositiveButton(context.getString(R.string.confirm), (dialogInterface, i) -> raceViewModel.deleteSingle(currentRace));

                dialogBuilder.setNegativeButton(context.getString(R.string.cancel), (dialogInterface, i) -> {
                    //do literally nothing
                });

                dialogBuilder.show(); // show the dialog box
            });
        }//bindTo
    }//ViewHolder
}//OrderAdapter
