package com.edmondstudio.hearty;


import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.visualizer.amplitude.AudioRecordView;

import java.io.File;
import java.io.IOException;
import java.lang.Thread;
import java.util.Timer;
import java.util.TimerTask;


/**
 * A simple {@link Fragment} subclass.
 */
public class RecordFragment extends Fragment implements View.OnClickListener {

    private NavController navController;

    private ImageButton listBtn;
    private ImageButton recordBtn;
    private TextView updateText;
    private EditText recordName;

    private boolean isRecording = false;
//    private boolean isDenoising = false;

    private String recordPermission = Manifest.permission.RECORD_AUDIO;
    private int PERMISSION_CODE = 21;

    private MediaRecorder mediaRecorder;
    private String recordFile;
    private String filePath;

    private Chronometer timer;
    private Chronometer visualizerTimer;

    private AudioRecordView audioRecordView;

    private RecordBroadCastReceiver broadcastReceiver;

    public RecordFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Intitialize Variables
        navController = Navigation.findNavController(view);
        listBtn = view.findViewById(R.id.record_list_btn);
        recordBtn = view.findViewById(R.id.record_btn);
        timer = view.findViewById(R.id.record_timer);
        updateText = view.findViewById(R.id.record_filename);
        recordName = view.findViewById(R.id.record_name);
        audioRecordView = view.findViewById(R.id.audioRecordView);

        /* Setting up on click listener
           - Class must implement 'View.OnClickListener' and override 'onClick' method
         */
        listBtn.setOnClickListener(this);
        recordBtn.setOnClickListener(this);

        broadcastReceiver = new RecordBroadCastReceiver();


    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(broadcastReceiver,
                new IntentFilter("python_action"));

    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(broadcastReceiver);
    }



    @Override
    public void onClick(View v) {
        /*  Check, which button is pressed and do the task accordingly
         */
        switch (v.getId()) {
            case R.id.record_list_btn:
                /*
                Navigation Controller
                Part of Android Jetpack, used for navigation between both fragments
                 */
                if(isRecording){
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
                    alertDialog.setPositiveButton("是", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            navController.navigate(R.id.action_recordFragment_to_audioListFragment);
                            isRecording = false;
                        }
                    });
                    alertDialog.setNegativeButton("否", null);
                    alertDialog.setTitle("現仍在錄音");
                    alertDialog.setMessage("你確定要中止錄音?");
                    alertDialog.create().show();
                } else {
                    navController.navigate(R.id.action_recordFragment_to_audioListFragment);
                }
                break;

            case R.id.record_btn:
                if (recordName.getText().toString().length()==0){
                    recordBtn.setEnabled(false);
                }else {
                    recordBtn.setEnabled(true);
                    if (isRecording) {
                        isRecording = false;
                        if (audioRecordView != null) { audioRecordView.recreate(); }
                        //Stop Recording
                        stopRecording();

                        // Change button image and set Recording state to false
                        recordBtn.setImageDrawable(getResources().getDrawable(R.drawable.record_btn_stopped, null));

                    } else {
                        //Check permission to record audio
                        if (checkPermissions()) {
                            //Start Recording
                            startRecording();
                            startVisualizing(100);

                            // Change button image and set Recording state to false
                            recordBtn.setImageDrawable(getResources().getDrawable(R.drawable.record_btn_recording, null));
//                            recordBtn.setEnabled(false);
                            isRecording = true;
                        }
                    }
                    break;
                }
        }
    }

    private void startRecording() {
        //Start timer from 0
        timer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long remaining_time = chronometer.getBase()-SystemClock.elapsedRealtime();
                if (remaining_time<0){
                    isRecording = false;
                    if (audioRecordView != null) { audioRecordView.recreate(); }
                    //Stop Recording
                    stopRecording();

                    // Change button image and set Recording state to false
                    recordBtn.setImageDrawable(getResources().getDrawable(R.drawable.record_btn_stopped, null));
                }
            }
        });
        timer.setBase(SystemClock.elapsedRealtime()+15000);
        timer.setCountDown(true);
        timer.start();

        //Get app external directory path
        String appPath = getActivity().getExternalFilesDir("/").getAbsolutePath();

        //initialize filename with text input recordName
        recordFile = recordName.getText().toString() + ".3gp";

        updateText.setText("錄音中, 聲帶在: " + recordFile);
        filePath = appPath + "/" + recordFile;

        //Setup Media Recorder for recording
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(filePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Start Recording
        mediaRecorder.start();
    }

    private boolean checkPermissions() {
        //Check permission
        if (ActivityCompat.checkSelfPermission(getContext(), recordPermission) == PackageManager.PERMISSION_GRANTED) {
            //Permission Granted
            return true;
        } else {
            //Permission not granted, ask for permission
            ActivityCompat.requestPermissions(getActivity(), new String[]{recordPermission}, PERMISSION_CODE);
            return false;
        }
    }

    private void startVisualizing(long interval) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask(){
            @Override
            public void run(){
                if (isRecording){
                    int currentMaxAmplitude = mediaRecorder.getMaxAmplitude();
                    audioRecordView.update(currentMaxAmplitude);
                }
            }
        }, 0, interval);
    }

    private void stopRecording() {
        //Stop Timer, very obvious
        timer.stop();

        //Change text on page to file saved
        updateText.setText("自動除噪音中");

        //Stop media recorder and set it to null for further use to record new audio
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;

        // Run denoising
        Log.i("RecordFragment","Denoising");
        // Use ffmpeg to convert 3gp into wav
        Log.i("RecordFragment","Getting original track at  "+filePath);
        String original_3gp_path = filePath;
        String original_wav_path = original_3gp_path.replace("3gp","wav");
        // sampling rate = 8192 as required by the python transformer, 192k bitrate as limited by native android
        FFmpeg.execute("-i "+original_3gp_path+" -ar 8192 -b:a 192k "+original_wav_path);
        File original_3gp = new File(original_3gp_path);
        original_3gp.delete();
        // run python denoiser
        String denoised_wav_path = original_wav_path.replace(".wav","_denoised.wav");
        Log.i("RecordFragment"," " + denoised_wav_path);
//        runpython also includes resultant audio conversion;
        runpython(original_wav_path, denoised_wav_path);
    }

    public void runpython(final String wav_path, final String denoised_wav_path){
        // new thread is added because tensorflow is too much to run on one thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(! Python.isStarted()){
                    Python.start(new AndroidPlatform(getActivity()));
                }else{
                }
                Python py = Python.getInstance();
                PyObject main = py.getModule("main");
//                  Denoising takes place
                PyObject denoised_pypath = main.callAttr("denoising",wav_path);
                String denoised_wav_path = denoised_pypath.toString();
//                  Denoised
                Log.i("runpython","Denoised wav track is at " + denoised_wav_path);

                String denoised_amplified_path = denoised_wav_path.replace(".wav","_amplified.wav");
                FFmpeg.execute("-i "+denoised_wav_path+" -filter:a 'volume=45dB' "+denoised_amplified_path);

//                delete files
                File denoised_wavFile = new File(denoised_wav_path);
                denoised_wavFile.delete();
                Intent intent = new Intent("python_action");
                intent.putExtra("message","Denoising Finished");
                getActivity().sendBroadcast(intent);
            }
        }).start();

    }

    private class RecordBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent intent) {
            // change the TextView text here
            if (intent.getAction() != null
                    && intent.getAction().equalsIgnoreCase("python_action")) {
                updateText.setText("除噪完成");
            }
        }

    }


    @Override
    public void onStop() {
        super.onStop();
        if(isRecording){
            stopRecording();
        }
    }
}
