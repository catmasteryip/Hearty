package com.edmondstudio.hearty;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class PyActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if(! Python.isStarted()){
            Log.i("Info", "Starting chaquopy");
            Python.start(new AndroidPlatform(this));
            Log.i("Info", "Chaquopy started");
        }
        Python py = Python.getInstance();
        PyObject main = py.getModule("main");
        String export_path;
//        export_path = main.callAttr("denoise", sound_path);
    }

}
