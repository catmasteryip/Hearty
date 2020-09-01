package com.edmondstudio.hearty;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
//import java.util.ArrayList;
import java.util.ArrayList;

public class AudioListAdapter extends RecyclerView.Adapter<AudioListAdapter.AudioViewHolder> {


    private File[] allFiles;
    private TimeAgo timeAgo;
    private ArrayList<String> fileList;
    private ArrayList<String> timeList;
    private onItemListClick onItemListClick;

    //constructor
    public AudioListAdapter(File[] allFiles, onItemListClick onItemListClick) {
        this.allFiles = allFiles;
//        this.fileList = file2List(allFiles);
//        this.timeList = lastModified2List(allFiles);
        this.onItemListClick = onItemListClick;
    }

//    public ArrayList<String> lastModified2List(File[] allFiles){
//        ArrayList<String> timeList = new ArrayList<String>();
//        if (allFiles.length>0){
//            for (int i = 0; i < allFiles.length; i++) {
//                String timeString = timeAgo.getTimeAgo(allFiles[i].lastModified());
//                timeList.add(timeString);
//                return timeList;
//            }
//        }
//        return null;
//    }
//
//    public ArrayList<String> file2List(File[] allFiles){
//        ArrayList<String> fileList = new ArrayList<String>();
//        if (allFiles.length>0){
//            for (int i = 0; i < allFiles.length; i++) {
//                String fileName = allFiles[i].getName();
//                fileList.add(fileName);
//                return fileList;
//            }
//        }
//        return null;
//    }

    @NonNull
    @Override
    public AudioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_list_item, parent, false);
        timeAgo = new TimeAgo();
//        instantiate arraylist based on allFiles
//        fileListInstantiate();
        return new AudioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AudioViewHolder holder, int position) {
        holder.list_title.setText(allFiles[position].getName());
        holder.list_date.setText(timeAgo.getTimeAgo(allFiles[position].lastModified()));
    }

    @Override
    public int getItemCount() {
        return allFiles.length;
    }

    public class AudioViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView list_image;
        private TextView list_title;
        private TextView list_date;
        private ImageView bin_image;

        public AudioViewHolder(@NonNull View itemView) {
            super(itemView);

            list_image = itemView.findViewById(R.id.list_image_view);
            list_title = itemView.findViewById(R.id.list_title);
            list_date = itemView.findViewById(R.id.list_date);
            bin_image = itemView.findViewById(R.id.bin_view);

            list_image.setOnClickListener(this);
            //delete track
            bin_image.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    File fdelete = new File(allFiles[position].toString());
//                    allFiles.
                    if (fdelete.delete()){
                        Log.d("after deletion", String.valueOf(allFiles.length));
//                        notifyItemRangeRemoved(position,1);
                        notifyItemRemoved(position);
//                        notifyDataSetChanged();
                    };
                }
            });

        }

        @Override
        public void onClick(View v) {
            onItemListClick.onClickListener(allFiles[getAdapterPosition()], getAdapterPosition());
        }
    }

    public interface onItemListClick {
        void onClickListener(File file, int position);
    }

}
