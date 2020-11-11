package com.edmondstudio.hearty;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

import static androidx.core.content.ContextCompat.startActivity;

public class AudioListAdapter extends RecyclerView.Adapter<AudioListAdapter.AudioViewHolder> {

    private TimeAgo timeAgo;
    private ArrayList<File> fileArrayList;
    private onItemListClick onItemListClick;
    private Context context;

    //constructor
    public AudioListAdapter(ArrayList<File> fileArrayList, onItemListClick onItemListClick) {
        this.fileArrayList = fileArrayList;
        this.onItemListClick = onItemListClick;
    }

    @NonNull
    @Override
    public AudioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_list_item, parent, false);
        timeAgo = new TimeAgo();
        context = parent.getContext();
        return new AudioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AudioViewHolder holder, int position) {
        holder.list_title.setText(fileArrayList.get(position).getName());
        holder.list_date.setText(timeAgo.getTimeAgo(fileArrayList.get(position).lastModified()));
    }

    @Override
    public int getItemCount() {
        return fileArrayList.size();
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
                    final int position = getAdapterPosition();
                    final File fdelete = fileArrayList.get(position);

//                    Log.d("adapter pos", String.valueOf(position));
//                    Log.d("fileArrayList", "before removal"+String.valueOf(getItemCount()));
//                    Log.d("fileArrayList", "after removal"+String.valueOf(getItemCount()));
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                    alertDialog.setPositiveButton("是", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (fdelete.delete()){
//                        Log.i("File", "File deleted");
                                fileArrayList.remove(position);
                            }
                        }
                    });
                    alertDialog.setNegativeButton("否", null);
                    alertDialog.setMessage("你確定要刪除錄音?");
                    alertDialog.create().show();


                    AudioListAdapter.super.notifyDataSetChanged();
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View v) {
//                    Toast.makeText(v.getContext(), "Position is " + getAdapterPosition(), Toast.LENGTH_SHORT).show();
                    File file = fileArrayList.get(getAdapterPosition());
                    Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.setType("audio/*");
                    sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    context.startActivity(Intent.createChooser(sendIntent, "Share Sound File"));
                    return false;
                }
            });

        }

        @Override
        public void onClick(View v) {
            onItemListClick.onClickListener(fileArrayList.get(getAdapterPosition()), getAdapterPosition());
        }
    }

    public interface onItemListClick {
        void onClickListener(File file, int position);
    }

}
