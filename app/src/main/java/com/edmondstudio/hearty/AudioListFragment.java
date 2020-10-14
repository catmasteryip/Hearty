package com.edmondstudio.hearty;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class AudioListFragment extends Fragment implements AudioListAdapter.onItemListClick {

    private ConstraintLayout playerSheet;
    private BottomSheetBehavior bottomSheetBehavior;

    private RecyclerView audioList;
    private File[] fileArray;
    private ArrayList<File> fileArrayList;

    private AudioListAdapter audioListAdapter;

    private MediaPlayer mediaPlayer = null;
    private boolean isPlaying = false;

    private File fileToPlay = null;

    //UI Elements
    private ImageButton playBtn;
    private TextView playerFilename;

    private SeekBar playerSeekbar;
    private Handler seekbarHandler;
    private Runnable updateSeekbar;
    private File directory;

    //    waveView initialization
    private PhotoView waveView;

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
        playerFilename = view.findViewById(R.id.player_filename);

        playerSeekbar = view.findViewById(R.id.player_seekbar);
        waveView = view.findViewById(R.id.waveView);

        String path = getActivity().getExternalFilesDir("/").getAbsolutePath();
        directory = new File(path);
        fileArray = directory.listFiles();
        fileArray = directory.listFiles();

        fileArrayList = new ArrayList<File>();
        if (fileArray != null) {
            for (int i = 0; i < fileArray.length; i++) {
                fileArrayList.add(fileArray[i]);
            }
        }

        audioListAdapter = new AudioListAdapter(fileArrayList, this);

        audioList.setHasFixedSize(true);
        audioList.setLayoutManager(new LinearLayoutManager(getContext()));
        audioList.setAdapter(audioListAdapter);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
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
                if (isPlaying) {
                    pauseAudio();
                } else {
                    if (fileToPlay != null) {
                        resumeAudio();
                    }
                }
            }
        });


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
        Log.i("Info", "state: " + isPlaying);
        fileToPlay = file;
        Log.i("Info", "path of playing track: " + fileToPlay);
        if (isPlaying) {
            stopAudio();
            playAudio(fileToPlay);
        } else {
            playAudio(fileToPlay);
        }
        String fileToPlay_path = fileToPlay.getAbsolutePath();
        showWaveForm(fileToPlay_path);
    }

    private void showWaveForm(String fileToPlay_path) {
//        call python to return matplotlib waveform graph in byte object
//        display byte object in PhotoView through bitmapfactory
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(getActivity()));
        } else {
            Python py = Python.getInstance();
            PyObject exportWave = py.getModule("exportWave");
            byte[] frameData = exportWave.callAttr("showWave", fileToPlay_path).toJava(byte[].class);
            Bitmap bitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.length);
            waveView.setImageBitmap(bitmap);
        }

    }


    private void pauseAudio() {
        mediaPlayer.pause();
        playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_play_btn, null));
        isPlaying = false;
        seekbarHandler.removeCallbacks(updateSeekbar);
//        mVisualizer.setEnabled(false);
    }

    private void resumeAudio() {
        mediaPlayer.start();
        playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_pause_btn, null));
        isPlaying = true;
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopAudio();
                //reset mediaplayer
                resetAudio(fileToPlay);
            }
        });
        updateRunnable();
        seekbarHandler.postDelayed(updateSeekbar, 0);
//        mVisualizer.setEnabled(true);
    }

    private void stopAudio() {
        //Stop The Audio
        playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_play_btn, null));
        isPlaying = false;
//        mVisualizer.release();
        mediaPlayer.stop();
        mediaPlayer.reset();
        mediaPlayer.release();

        seekbarHandler.removeCallbacks(updateSeekbar);
    }

    private void playAudio(final File fileToPlay) {

        mediaPlayer = new MediaPlayer();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        try {
//            mediaPlayer.reset();
            mediaPlayer.setDataSource(fileToPlay.getAbsolutePath());
//            Log.i("Info","data source set at: "+fileToPlay.getAbsolutePath());
            // mediaplayer failed to prepare for denoised .wav file, so .wav files would not be played on android
//            https://stackoverflow.com/questions/11540076/android-mediaplayer-error-1-2147483648
            mediaPlayer.prepare();
//            Log.i("Info","mediaplayer prepared");
            mediaPlayer.start();
//            Log.i("Info","mediaplayer started");
        } catch (IOException e) {
            e.printStackTrace();
        }
//        setupVisualizerFxAndUI();

//        mVisualizer.setEnabled(true);
        playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_pause_btn, null));
        playerFilename.setText(fileToPlay.getName());
        //Play the audio
        isPlaying = true;
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopAudio();
                //reset mediaplayer
                resetAudio(fileToPlay);
            }
        });

        playerSeekbar.setMax(mediaPlayer.getDuration());

        seekbarHandler = new Handler();
        updateRunnable();
        seekbarHandler.postDelayed(updateSeekbar, 0);

    }

    private void resetAudio(File fileToPlay) {
        playerSeekbar.setProgress(0);
        mediaPlayer = new MediaPlayer();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        try {
//            mediaPlayer.reset();
            mediaPlayer.setDataSource(fileToPlay.getAbsolutePath());
//            Log.i("Info","data source set at: "+fileToPlay.getAbsolutePath());
            // mediaplayer failed to prepare for denoised .wav file, so .wav files would not be played on android
//            https://stackoverflow.com/questions/11540076/android-mediaplayer-error-1-2147483648
            mediaPlayer.prepare();
//            Log.i("Info","mediaplayer prepared");
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        if (isPlaying) {
            stopAudio();
        }
    }
}
