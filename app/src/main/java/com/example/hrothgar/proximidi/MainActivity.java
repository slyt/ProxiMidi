/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.hrothgar.proximidi;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.support.v4.app.NotificationCompatSideChannelService;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    SensorManager msensorManager;
    Sensor proximitySensor;
    SensorEventListener proximitySensorListener;

    float proximityMaxRange;
    float sensorValue;
    int midiValue;

    private MidiManager mMidiManager;
    private int mChannel; // ranges from 0 to 15
    private int[] mPrograms = new int[MidiConstants.MAX_CHANNELS]; // ranges from 0 to 127
    private byte[] mByteBuffer = new byte[3];

    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.send_button);
        button.setOnTouchListener(handleTouch);


        // Initialize MIDI
        Context context = this;
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            // Open MIDI input port
            MidiManager m = (MidiManager)context.getSystemService(Context.MIDI_SERVICE);
            MidiDeviceInfo[] infos = m.getDevices();
            MidiInputPort inputPort = device.openInputPort(index);
        }
        else{ finish(); }


        //initialize textView text to sensor amount
        final TextView textViewDistance = findViewById(R.id.textViewDistance);
        final TextView textViewMidi = findViewById(R.id.textViewMidiVal)


        // Create Sensor Object and get Data from Sensor
        msensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        proximitySensor = msensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (proximitySensor == null) {
            finish(); // Close app
        }
        proximityMaxRange = proximitySensor.getMaximumRange();

        // Create Listener
        SensorEventListener proximitySensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if(sensorEvent.values[0] < proximitySensor.getMaximumRange()) {
                    // Detected something nearby
                    getWindow().getDecorView().setBackgroundColor(Color.RED);

                    // Update text view with value from proximity sensor
                    sensorValue = sensorEvent.values[0];
                    textViewDistance.setText(String.valueOf(sensorValue) + " cm");

                    int sensorValue100 = Math.round(sensorValue) * 100;
                    midiValue = map(Math.round(sensorValue), 0, Math.round(proximitySensor.getMaximumRange()), 0, 127);
                    textViewMidi.setText("Midi Value: " + String.valueOf(midiValue));
                } else {
                    // Nothing is nearby
                    getWindow().getDecorView().setBackgroundColor(Color.GREEN);
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
        msensorManager.registerListener(proximitySensorListener, proximitySensor, 2 * 1000 * 1000);
    } // END onCreate()


    /** Used to map distance range to MIDI range (0-127) **/
    int map(int x, int in_min, int in_max, int out_min, int out_max)
    {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    private void setupMidi() {
        mMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);
        if (mMidiManager == null) {
            // Toast shows temporary unobtrusive message
            Toast.makeText(this, "MidiManager is null!", Toast.LENGTH_LONG).show();
            return;
        }
    }

    private void noteOff(int channel, int pitch, int velocity) {
        midiCommand(MidiConstants.STATUS_NOTE_OFF + channel, pitch, velocity);
    }

    private void noteOn(int channel, int pitch, int velocity) {
        midiCommand(MidiConstants.STATUS_NOTE_ON + channel, pitch, velocity);
    }

    private void midiCommand(int status, int data1, int data2) {
        mByteBuffer[0] = (byte) status;
        mByteBuffer[1] = (byte) data1;
        mByteBuffer[2] = (byte) data2;
        long now = System.nanoTime();
        midiSend(mByteBuffer, 3, now);
    }

    private void midiCommand(int status, int data1) {
        mByteBuffer[0] = (byte) status;
        mByteBuffer[1] = (byte) data1;
        long now = System.nanoTime();
        midiSend(mByteBuffer, 2, now);
    }

    private View.OnTouchListener handleTouch = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            int x = (int) event.getX();
            int y = (int) event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i("TAG", "touched down");
                    // TODO: Send Midi ON here using midiValue
                    getWindow().getDecorView().setBackgroundColor(Color.BLUE);

                    break;
                case MotionEvent.ACTION_MOVE: // TODO: This is essentially after-touch!
                    Log.i("TAG", "moving: (" + x + ", " + y + ")");
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i("TAG", "touched up");
                    // TODO: Send Midi OFF here using midiValue (is midiValue necesssary for off?)
                    getWindow().getDecorView().setBackgroundColor(Color.MAGENTA);
                    break;
            }

            return true;
        }
    };

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        // Register it, specifying the polling interval in
        // microseconds
        msensorManager.registerListener(proximitySensorListener,
                        proximitySensor, 2 * 1000 * 1000);
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first

        msensorManager.unregisterListener(proximitySensorListener);
    }







}
