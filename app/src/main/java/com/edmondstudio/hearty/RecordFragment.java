package com.edmondstudio.hearty;


import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */
public class RecordFragment extends Fragment implements View.OnClickListener {

    private NavController navController;

    private ImageButton listBtn;
    private ImageButton recordBtn;
    private TextView filenameText;
    private EditText recordName;

    private boolean isRecording = false;

    private String recordPermission = Manifest.permission.RECORD_AUDIO;
    private int PERMISSION_CODE = 21;

    private MediaRecorder mediaRecorder;
    private String recordFile;
    private String filePath;

    private Chronometer timer;

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
        filenameText = view.findViewById(R.id.record_filename);
        recordName = view.findViewById(R.id.record_name);

        /* Setting up on click listener
           - Class must implement 'View.OnClickListener' and override 'onClick' method
         */
        listBtn.setOnClickListener(this);
        recordBtn.setOnClickListener(this);


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
                    alertDialog.setPositiveButton("OKAY", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            navController.navigate(R.id.action_recordFragment_to_audioListFragment);
                            isRecording = false;
                        }
                    });
                    alertDialog.setNegativeButton("CANCEL", null);
                    alertDialog.setTitle("Audio Still recording");
                    alertDialog.setMessage("Are you sure, you want to stop the recording?");
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
                        //Stop Recording
                        stopRecording();

                        // Change button image and set Recording state to false
                        recordBtn.setImageDrawable(getResources().getDrawable(R.drawable.record_btn_stopped, null));
                        isRecording = false;
                    } else {
                        //Check permission to record audio
                        if (checkPermissions()) {
                            //Start Recording
                            startRecording();

                            // Change button image and set Recording state to false
                            recordBtn.setImageDrawable(getResources().getDrawable(R.drawable.record_btn_recording, null));
                            isRecording = true;
                        }
                    }
                    break;
                }
        }
    }

    private void stopRecording() {
        //Stop Timer, very obvious
        timer.stop();

        //Change text on page to file saved
        filenameText.setText("Recording Stopped, File Saved : " + recordFile);

        //Stop media recorder and set it to null for further use to record new audio
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;

        // Run denoising
        Log.i("Info","Denoising");
        // Use ffmpeg to convert 3gp into wav
        Log.i("Info","Getting original track at  "+filePath);
        String original_3gp_path = filePath;
        String original_wav_path = original_3gp_path.replace("3gp","wav");
        // sampling rate = 8192 as required by the python transformer, 192k bitrate as limited by native android
        FFmpeg.execute("-i "+original_3gp_path+" -ar 8192 -b:a 192k "+original_wav_path);
        // run python denoiser
        String denoised_wav_path = original_wav_path.replace(".wav","_denoised.wav");
        Log.i("Info"," " + denoised_wav_path);
//        runpython also includes resultant audio conversion;
        runpython(original_wav_path);
    }

    public void runpython(final String wav_path){
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
                Log.i("Info","Denoised wav track is at " + denoised_wav_path);

                String denoised_mp3_path = denoised_wav_path.replace("wav","mp3");
                // Use ffmpeg to convert wav into mp3/3gp
                FFmpeg.execute("-i "+ denoised_wav_path+" -codec:a libmp3lame -qscale:a 0 -filter:a 'volume=15dB' "+denoised_mp3_path);
                Log.i("Info","Denoised mp3 track is at " + denoised_mp3_path);
            }
        }).start();
    }

    private void startRecording() {
        //Start timer from 0
        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();

        //Get app external directory path
        String recordPath = getActivity().getExternalFilesDir("/").getAbsolutePath();

        //Get current date and time
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss", Locale.CANADA);
//        Date now = new Date();

        //initialize filename variable with date and time at the end to ensure the new file wont overwrite previous file
//        recordFile = "Recording_" + formatter.format(now) + ".3gp";


        //initializez filename with text input recordName
        recordFile = recordName.getText().toString() + ".3gp";

        filenameText.setText("Recording, File Name : " + recordFile);
        filePath = recordPath + "/" + recordFile;

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

    @Override
    public void onStop() {
        super.onStop();
        if(isRecording){
            stopRecording();
        }
    }
}
