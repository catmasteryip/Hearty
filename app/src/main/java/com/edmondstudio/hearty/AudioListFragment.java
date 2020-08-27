package com.edmondstudio.hearty;


import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.File;
import java.io.IOException;


/**
 * A simple {@link Fragment} subclass.
 */
public class AudioListFragment extends Fragment implements AudioListAdapter.onItemListClick {

    private ConstraintLayout playerSheet;
    private BottomSheetBehavior bottomSheetBehavior;

    private RecyclerView audioList;
    private File[] allFiles;

    private AudioListAdapter audioListAdapter;

    private MediaPlayer mediaPlayer = null;
    private boolean isPlaying = false;

    private File fileToPlay = null;

    //UI Elements
    private ImageButton playBtn;
    private Button denoiseBtn;
    private TextView playerHeader;
    private TextView playerFilename;

    private SeekBar playerSeekbar;
    private Handler seekbarHandler;
    private Runnable updateSeekbar;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_audio_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playerSheet = view.findViewById(R.id.player_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(playerSheet);
        audioList = view.findViewById(R.id.audio_list_view);

        playBtn = view.findViewById(R.id.player_play_btn);

        denoiseBtn = view.findViewById(R.id.denoise_btn);

        playerHeader = view.findViewById(R.id.player_header_title);
        playerFilename = view.findViewById(R.id.player_filename);

        playerSeekbar = view.findViewById(R.id.player_seekbar);

        String path = getActivity().getExternalFilesDir("/").getAbsolutePath();
        File directory = new File(path);
        allFiles = directory.listFiles();

        audioListAdapter = new AudioListAdapter(allFiles, this);

        audioList.setHasFixedSize(true);
        audioList.setLayoutManager(new LinearLayoutManager(getContext()));
        audioList.setAdapter(audioListAdapter);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if(newState == BottomSheetBehavior.STATE_HIDDEN){
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                //We cant do anything here for this app
            }
        });

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPlaying){
                    pauseAudio();
                } else {
                    if(fileToPlay != null){
                        resumeAudio();
                    }
                }
            }
        });

//        denoiseBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                if(isPlaying){
//                    pauseAudio();
//                }
//                Log.i("Info","Denoising");
//                // Use ffmpeg to convert 3gp into wav
//                Log.i("Info","Getting original track at  "+fileToPlay.getAbsolutePath());
//                String original_3gp_path = fileToPlay.getAbsolutePath();
//                String original_wav_path = original_3gp_path.replace("3gp","wav");
//                // sampling rate = 8192 as required by the python transformer, 192k bitrate as limited by native android
//                FFmpeg.execute("-i "+original_3gp_path+" -ar 8192 -b:a 192k "+original_wav_path);
//                // run python denoiser
//                String denoised_wav_path = original_wav_path.replace(".wav","_denoised.wav");
//                Log.i("Info"," " + denoised_wav_path);
////                runpython also includes resultant audio conversion;
//                runpython(original_wav_path);
//            }
//
//            public void runpython(final String wav_path){
//                // new thread is added because tensorflow is too much to run on one thread
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if(! Python.isStarted()){
////                            Log.i("Info", "Starting chaquopy");
//                            Python.start(new AndroidPlatform(getActivity()));
////                            Log.i("Info", "Chaquopy has started");
//                        }else{
////                            Log.i("Info", "Chaquopy is already in");
//                        }
//                        Python py = Python.getInstance();
//                        PyObject main = py.getModule("main");
////                        String original_track_path = fileToPlay.getAbsolutePath();
////                        Denoising takes place
//                        PyObject denoised_pypath = main.callAttr("denoising",wav_path);
//                        String denoised_wav_path = denoised_pypath.toString();
////                        Denoised
//                        Log.i("Info","Denoised wav track is at " + denoised_wav_path);
//
//                        String denoised_mp3_path = denoised_wav_path.replace("wav","mp3");
//                        // Use ffmpeg to convert wav into mp3/3gp
//                        FFmpeg.execute("-i "+ denoised_wav_path+" -codec:a libmp3lame -qscale:a 0 -filter:a loudnorm "+denoised_mp3_path);
//                        Log.i("Info","Denoised mp3 track is at " + denoised_mp3_path);
//                    }
//                }).start();
//            }
//        });

        playerSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                pauseAudio();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                mediaPlayer.seekTo(progress);
                resumeAudio();
            }
        });

    }

    @Override
    public void onClickListener(File file, int position) {
        Log.i("Info","state: "+isPlaying);
        fileToPlay = file;
        Log.i("Info","path of playing track: "+fileToPlay);
        if(isPlaying){
            stopAudio();
            playAudio(fileToPlay);
        } else {
            playAudio(fileToPlay);
        }
    }



    private void pauseAudio() {
        mediaPlayer.pause();
        playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_play_btn, null));
        isPlaying = false;
        seekbarHandler.removeCallbacks(updateSeekbar);
    }

    private void resumeAudio() {
        mediaPlayer.start();
        playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_pause_btn, null));
        isPlaying = true;

        updateRunnable();
        seekbarHandler.postDelayed(updateSeekbar, 0);

    }

    private void stopAudio() {
        //Stop The Audio
        playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_play_btn, null));
        playerHeader.setText("Stopped");
        isPlaying = false;
        mediaPlayer.stop();
        mediaPlayer.reset();
//        mediaPlayer.release();
        seekbarHandler.removeCallbacks(updateSeekbar);
    }

    private void playAudio(File fileToPlay) {

        mediaPlayer = new MediaPlayer();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(fileToPlay.getAbsolutePath());
            Log.i("Info","data source set at: "+fileToPlay.getAbsolutePath());
            // mediaplayer failed to prepare for denoised .wav file
//            https://stackoverflow.com/questions/11540076/android-mediaplayer-error-1-2147483648
            mediaPlayer.prepare();
            Log.i("Info","mediaplayer prepared");
            mediaPlayer.start();
            Log.i("Info","mediaplayer started");
        } catch (IOException e) {
            e.printStackTrace();
        }
        playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_pause_btn, null));
        playerFilename.setText(fileToPlay.getName());
        playerHeader.setText("Playing");
        //Play the audio
        isPlaying = true;
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopAudio();
                playerHeader.setText("Finished");
                // reset mediaplayer?
            }
        });

        playerSeekbar.setMax(mediaPlayer.getDuration());

        seekbarHandler = new Handler();
        updateRunnable();
        seekbarHandler.postDelayed(updateSeekbar, 0);

    }

    private void updateRunnable() {
        updateSeekbar = new Runnable() {
            @Override
            public void run() {
                playerSeekbar.setProgress(mediaPlayer.getCurrentPosition());
                seekbarHandler.postDelayed(this, 500);
            }
        };
    }

    @Override
    public void onStop() {
        super.onStop();
        if(isPlaying) {
            stopAudio();
        }
    }
}
