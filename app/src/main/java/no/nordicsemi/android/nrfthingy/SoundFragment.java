/*
 * Copyright (c) 2010 - 2017, Nordic Semiconductor ASA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form, except as embedded into a Nordic
 *    Semiconductor ASA integrated circuit in a product or a software update for
 *    such product, must reproduce the above copyright notice, this list of
 *    conditions and the following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. Neither the name of Nordic Semiconductor ASA nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * 4. This software, with or without modification, must only be used with a
 *    Nordic Semiconductor ASA integrated circuit.
 *
 * 5. Any software provided in binary form under this license must not be reverse
 *    engineered, decompiled, modified and/or disassembled.
 *
 * THIS SOFTWARE IS PROVIDED BY NORDIC SEMICONDUCTOR ASA "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY, NONINFRINGEMENT, AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NORDIC SEMICONDUCTOR ASA OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.nrfthingy;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.Toolbar;

import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;

import java.util.ArrayList;
import java.util.Arrays;

import no.nordicsemi.android.nrfthingy.ClusterHead.ClhAdvertise;
import no.nordicsemi.android.nrfthingy.ClusterHead.ClhAdvertisedData;
import no.nordicsemi.android.nrfthingy.ClusterHead.ClhConst;
import no.nordicsemi.android.nrfthingy.ClusterHead.ClhProcessData;
import no.nordicsemi.android.nrfthingy.ClusterHead.ClhRoutingData;
import no.nordicsemi.android.nrfthingy.ClusterHead.ClhScan;
import no.nordicsemi.android.nrfthingy.ClusterHead.ClusterHead;
import no.nordicsemi.android.nrfthingy.common.MessageDialogFragment;
import no.nordicsemi.android.nrfthingy.common.PermissionRationaleDialogFragment;
import no.nordicsemi.android.nrfthingy.common.Utils;
import no.nordicsemi.android.nrfthingy.database.DatabaseHelper;
import no.nordicsemi.android.nrfthingy.sound.FrequencyModeFragment;
import no.nordicsemi.android.nrfthingy.sound.PcmModeFragment;
import no.nordicsemi.android.nrfthingy.sound.SampleModeFragment;
import no.nordicsemi.android.nrfthingy.sound.ThingyMicrophoneService;
import no.nordicsemi.android.nrfthingy.thingy.Thingy;
import no.nordicsemi.android.nrfthingy.widgets.VoiceVisualizer;
import no.nordicsemi.android.thingylib.ThingyListener;
import no.nordicsemi.android.thingylib.ThingyListenerHelper;
import no.nordicsemi.android.thingylib.ThingySdkManager;
import no.nordicsemi.android.thingylib.utils.ThingyUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import no.nordicsemi.android.nrfthingy.common.ScannerFragmentListener;
import no.nordicsemi.android.nrfthingy.common.Utils;
import no.nordicsemi.android.nrfthingy.database.DatabaseHelper;
import no.nordicsemi.android.nrfthingy.thingy.Thingy;
import no.nordicsemi.android.thingylib.ThingyListener;
import no.nordicsemi.android.thingylib.ThingyListenerHelper;
import no.nordicsemi.android.thingylib.ThingySdkManager;
import no.nordicsemi.android.thingylib.utils.ThingyUtils;


import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sinh;

public class SoundFragment extends Fragment implements PermissionRationaleDialogFragment.PermissionDialogListener {


    private static final String AUDIO_PLAYING_STATE = "AUDIO_PLAYING_STATE";
    private static final String AUDIO_RECORDING_STATE = "AUDIO_RECORDING_STATE";
    private static final float ALPHA_MAX = 0.60f;
    private static final float ALPHA_MIN = 0.0f;
    private static final int DURATION = 800;

    private ImageView mMicrophone;
    private ImageView mMicrophoneOverlay;
    private ImageView mThingyOverlay;
    private ImageView mThingy;
    private VoiceVisualizer mVoiceVisualizer;

    private BluetoothDevice mDevice;
    private FragmentAdapter mFragmentAdapter;
    private ThingySdkManager mThingySdkManager;
    private boolean mStartRecordingAudio = false;
    private boolean mStartPlayingAudio = false;

    private boolean soundDetected = false;
    private int soundCounter = 0;
    private final int WINDOW_RANGE = 1;
    private int Fs = 8000;
    private int F0 = 2500; // 1200
    private int F1 = 1200;
    private double BW0 = 0.33;
    private int gain0 = 40;

    private double A0 = pow(10, gain0/40);
    private double w0 = 2*PI*F0/Fs;
    private double c0 = cos(w0);
    private double s0 = sin(w0);
    private double alpha0 = s0 * sinh((0.6931/2) * BW0 * w0/s0);

    private double a0 =   1 + alpha0/A0;
    private double b0 =   (1 + alpha0*A0) / a0;
    private double b1 =  (-2*c0) / a0;
    private double b2 =   (1 - alpha0*A0) / a0;
    private double a1 =  (-2*c0) / a0;
    private double a2 =   (1 - alpha0/A0) / a0;

    private double A1 = pow(10, gain0/40);
    private double w1 = 2*PI*F1/Fs;
    private double c1 = cos(w1);
    private double s1 = sin(w1);
    private double alpha1 = s1 * sinh((0.6931/2) * BW0 * w1/s1);

    private double a00 =   1 + alpha1/A1;
    private double b00 =   (1 + alpha1*A1) / a00;
    private double b10 =  (-2*c1) / a00;
    private double b20 =   (1 - alpha1*A1) / a00;
    private double a10 =  (-2*c1) / a00;
    private double a20 =   (1 - alpha0/A0) / a00;

    private double xmem1 = 0, xmem2 = 0, ymem1 = 0, ymem2 = 0;

    private DatabaseHelper mDatabaseHelper;
    private int mCurrentDelay;
    private int mCurrentIntensity;
    private int mCurrentLedMode;

    private int mSelectedColorIndex = 100;

    private ThingyListener mThingyListener = new ThingyListener() {
        private Handler mHandler = new Handler();

        @Override
        public void onDeviceConnected(BluetoothDevice device, int connectionState) {
        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device, int connectionState) {
            if (device.equals(mDevice)) {
                stopRecording();
                stopMicrophoneOverlayAnimation();
                stopThingyOverlayAnimation();
                mStartPlayingAudio = false;
            }
        }

        @Override
        public void onServiceDiscoveryCompleted(BluetoothDevice device) {
        }

        @Override
        public void onBatteryLevelChanged(final BluetoothDevice bluetoothDevice, final int batteryLevel) {

        }

        @Override
        public void onTemperatureValueChangedEvent(BluetoothDevice bluetoothDevice, String temperature) {
        }

        @Override
        public void onPressureValueChangedEvent(BluetoothDevice bluetoothDevice, final String pressure) {
        }

        @Override
        public void onHumidityValueChangedEvent(BluetoothDevice bluetoothDevice, final String humidity) {
        }

        @Override
        public void onAirQualityValueChangedEvent(BluetoothDevice bluetoothDevice, final int eco2, final int tvoc) {
        }

        @Override
        public void onColorIntensityValueChangedEvent(BluetoothDevice bluetoothDevice, final float red, final float green, final float blue, final float alpha) {
        }

        @Override
        public void onButtonStateChangedEvent(BluetoothDevice bluetoothDevice, int buttonState) {

        }

        @Override
        public void onTapValueChangedEvent(BluetoothDevice bluetoothDevice, int direction, int count) {

        }

        @Override
        public void onOrientationValueChangedEvent(BluetoothDevice bluetoothDevice, int orientation) {

        }

        @Override
        public void onQuaternionValueChangedEvent(BluetoothDevice bluetoothDevice, float w, float x, float y, float z) {

        }

        @Override
        public void onPedometerValueChangedEvent(BluetoothDevice bluetoothDevice, int steps, long duration) {

        }

        @Override
        public void onAccelerometerValueChangedEvent(BluetoothDevice bluetoothDevice, float x, float y, float z) {

        }

        @Override
        public void onGyroscopeValueChangedEvent(BluetoothDevice bluetoothDevice, float x, float y, float z) {

        }

        @Override
        public void onCompassValueChangedEvent(BluetoothDevice bluetoothDevice, float x, float y, float z) {

        }

        @Override
        public void onEulerAngleChangedEvent(BluetoothDevice bluetoothDevice, float roll, float pitch, float yaw) {

        }

        @Override
        public void onRotationMatrixValueChangedEvent(BluetoothDevice bluetoothDevice, byte[] matrix) {

        }

        @Override
        public void onHeadingValueChangedEvent(BluetoothDevice bluetoothDevice, float heading) {

        }

        @Override
        public void onGravityVectorChangedEvent(BluetoothDevice bluetoothDevice, float x, float y, float z) {

        }

        @Override
        public void onSpeakerStatusValueChangedEvent(BluetoothDevice bluetoothDevice, int status) {

        }

        @Override
        public void onMicrophoneValueChangedEvent(BluetoothDevice bluetoothDevice, final byte[] data) {
            Log.v("data", bluetoothDevice.getAddress());
            if (data != null) {
                if (data.length != 0) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mVoiceVisualizer.draw(data);

                        }
                    });

                    //PSG edit No.1
                    //audio receive event
                    if( mStartPlayingAudio = true) {
                        double[] y = new double[data.length];
                        double total = 0;
                        for(int i = 0; i < data.length; i++) {
                            y[i] = b0*Integer.parseInt(String.valueOf(data[i])) + b1*xmem1 + b2*xmem2 - a1*ymem1 - a2*ymem2;

                            xmem2 = xmem1;
                            xmem1 = Integer.parseInt(String.valueOf(data[i]));
                            ymem2 = ymem1;
                            ymem1 = y[i];
                            total += y[i];
                        }

                        double avg0 = total/data.length;
                        xmem1 = 0; xmem2 = 0; ymem1 = 0; ymem2 = 0;
                        for(int i = 0; i < data.length; i++) {
                            y[i] = b10*Integer.parseInt(String.valueOf(data[i])) + b10*xmem1 + b20*xmem2 - a10*ymem1 - a20*ymem2;

                            xmem2 = xmem1;
                            xmem1 = Integer.parseInt(String.valueOf(data[i]));
                            ymem2 = ymem1; // test
                            ymem1 = y[i];
                            total += y[i];
                        }

                        double avg1 = total/data.length;

                        double diff = avg0 - avg1;
                        if(diff > 20)
                            Log.v("test", String.valueOf(diff));
                        /*if(diff > 70) {
                            Log.i("DATA", "CLAPPING DETECTED I GUESSS!!");
                            setupLedMode(ThingyUtils.ONE_SHOT);

                            mClhAdvertiser.addAdvSoundData(data);
                        }*/

//                        for(int i = 0; i < data.length; i+=2) {
                           if(diff > 70) {
                               if(!soundDetected) {
                                   //Toast.makeText(context, "CLAP DETECTED!!!", Toast.LENGTH_LONG).show();
                                   Log.v("MESSAGE", "CLAP DETECTED!!!");
                                   setupLedMode(ThingyUtils.ONE_SHOT, bluetoothDevice);
                                   soundDetected = true;
                                  soundCounter = 0;
                              } else {
                                   soundCounter = 0;
                               }
                          } else if(soundDetected || soundCounter != 0) {
                               soundCounter++;
                           }
                           if(soundCounter >= WINDOW_RANGE) {
                               soundCounter = 0;
                              soundDetected = false;
                           }

//                        }



                    }
                    //End PSG edit No.1

                }
            }
        }
    };

    private void setupLedMode(final byte ledMode, BluetoothDevice device) {
        if (device != null) {
            final Thingy thingy = mDatabaseHelper.getSavedDevice(device.getAddress());
//            final BluetoothDevice device = mDevice;
            if (mThingySdkManager.isConnected(device)) {
                switch (ledMode) {

                    case ThingyUtils.ONE_SHOT:
                        if (mCurrentLedMode != ThingyUtils.OFF) {
                            if (mCurrentLedMode != ThingyUtils.CONSTANT) {
                                final int ledIntensity = mCurrentIntensity = 100;
                                int colorIndex = mSelectedColorIndex;
                                if (colorIndex == 0) {
                                    colorIndex = mSelectedColorIndex = getColorFromIndex(colorIndex);
                                }
                                mThingySdkManager.setOneShotLedMode(device, ThingyUtils.LED_RED, 100);
                            } else {

                                mThingySdkManager.setOneShotLedMode(device, ThingyUtils.LED_RED, 100);

                            }
                        } else {
                            mCurrentIntensity = 100;
                            mCurrentDelay = 5000;

                            mThingySdkManager.setOneShotLedMode(device, ThingyUtils.LED_RED, 100);
                        }
                        mCurrentLedMode = ThingyUtils.ONE_SHOT;
                        break;
                    case ThingyUtils.OFF:
                        if (mCurrentLedMode != ThingyUtils.OFF) {
                            mThingySdkManager.turnOffLed(device);
                        }
                        mCurrentLedMode = ThingyUtils.OFF;
                        break;
                }
            } else {
                Utils.showToast(getActivity(), "Please configureThingy to " + thingy.getDeviceName() + " before you proceed!");
            }
        }
    }

    private int getColorFromIndex(final int colorIndex) {
        switch (colorIndex) {
            case ThingyUtils.LED_RED:
                return Color.RED;
            case ThingyUtils.LED_GREEN:
                return Color.GREEN;
            case ThingyUtils.LED_YELLOW:
                return Color.YELLOW;
            case ThingyUtils.LED_BLUE:
                return Color.BLUE;
            case ThingyUtils.LED_PURPLE:
                return Color.MAGENTA;
            case ThingyUtils.LED_CYAN:
                return Color.CYAN;
            case ThingyUtils.LED_WHITE:
                return Color.WHITE;
            default:
                return Color.CYAN;
        }
    }

     private int getIndexFromColor(final int color) {
        switch (color) {
            case Color.RED:
                return ThingyUtils.LED_RED;
            case Color.GREEN:
                return ThingyUtils.LED_GREEN;
            case Color.YELLOW:
                return ThingyUtils.LED_YELLOW;
            case Color.BLUE:
                return ThingyUtils.LED_BLUE;
            case Color.MAGENTA:
                return ThingyUtils.LED_PURPLE;
            case Color.CYAN:
                return ThingyUtils.LED_CYAN;
            case Color.WHITE:
                return ThingyUtils.LED_WHITE;
            default:
                return ThingyUtils.LED_CYAN;
        }
    }



    private BroadcastReceiver mAudioRecordBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.startsWith(Utils.EXTRA_DATA_AUDIO_RECORD)) {
                final byte[] tempPcmData = intent.getExtras().getByteArray(ThingyUtils.EXTRA_DATA_PCM);
                final int length = intent.getExtras().getInt(ThingyUtils.EXTRA_DATA);
                if (tempPcmData != null) {
                    if (length != 0) {
                        mVoiceVisualizer.draw(tempPcmData);
                    }
                }
            } else if (action.equals(Utils.ERROR_AUDIO_RECORD)) {
                final String error = intent.getExtras().getString(Utils.EXTRA_DATA);
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
            }
        }
    };

    public static SoundFragment newInstance(final BluetoothDevice device) {
        SoundFragment fragment = new SoundFragment();
        final Bundle args = new Bundle();
        args.putParcelable(Utils.CURRENT_DEVICE, device);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDevice = getArguments().getParcelable(Utils.CURRENT_DEVICE);
        }
        mThingySdkManager = ThingySdkManager.getInstance();
        mDatabaseHelper = new DatabaseHelper(getActivity());
    }


    //PSG edit No.2---------
    //var declare and init

    private Button mAdvertiseButton;
    private EditText mClhIDInput;
    private TextView mClhLog;
    private final String LOG_TAG="CLH Sound";

    private ClhAdvertisedData mClhData=new ClhAdvertisedData();
    private ClhRoutingData mClhDiscovery = new ClhRoutingData();
    private boolean mIsSink=false;
    private byte mClhID=2;
    private byte mClhDestID=0;
    private byte mClhHops=0;
    private byte mClhThingyID=1;
    private byte mClhThingyType=1;
    private int mClhThingySoundPower=100;
    ClusterHead mClh;
    ClhAdvertise mClhAdvertiser;
    ClhScan mClhScanner;
    ClhProcessData mClhProcessor;

    //End PSG edit No.2----------------------------


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_sound, container, false);

        final Toolbar speakerToolbar = rootView.findViewById(R.id.speaker_toolbar);
        speakerToolbar.inflateMenu(R.menu.audio_warning);
        speakerToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final int id = item.getItemId();
                switch (id) {
                    case R.id.action_audio_warning:
                        MessageDialogFragment fragment = MessageDialogFragment.newInstance(getString(R.string.info), getString(R.string.mtu_warning));
                        fragment.show(getChildFragmentManager(), null);
                        break;
                }
                return false;
            }
        });

        final Toolbar microphoneToolbar = rootView.findViewById(R.id.microphone_toolbar);
        microphoneToolbar.inflateMenu(R.menu.audio_warning);
        microphoneToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final int id = item.getItemId();
                switch (id) {
                    case R.id.action_audio_warning:
                        MessageDialogFragment fragment = MessageDialogFragment.newInstance(getString(R.string.info), getString(R.string.mtu_warning));
                        fragment.show(getChildFragmentManager(), null);
                        break;
                }
                return false;
            }
        });

        mMicrophone = rootView.findViewById(R.id.microphone);
        mMicrophoneOverlay = rootView.findViewById(R.id.microphoneOverlay);
        mThingy = rootView.findViewById(R.id.thingy);
        mThingyOverlay = rootView.findViewById(R.id.thingyOverlay);
        mVoiceVisualizer = rootView.findViewById(R.id.voice_visualizer);

        // Prepare the sliding tab layout and the view pager
        final TabLayout mTabLayout = rootView.findViewById(R.id.sliding_tabs);
        final ViewPager pager = rootView.findViewById(R.id.view_pager);
        mFragmentAdapter = new FragmentAdapter(getChildFragmentManager());
        pager.setAdapter(mFragmentAdapter);
        mTabLayout.setupWithViewPager(pager);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(final int position) {
                switch (position) {
                    case 1:
                        mFragmentAdapter.setSelectedFragment(position);
                        break;
                    default:
                        mFragmentAdapter.setSelectedFragment(position);
                        break;
                }
            }

            @Override
            public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(final int state) {
            }
        });

        mMicrophone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mThingySdkManager.isConnected(mDevice)) {
                    if (!mStartRecordingAudio) {
                        checkMicrophonePermissions();
                    } else {
                        stopRecording();
                    }
                }
            }
        });


         mThingy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mThingySdkManager.isConnected(mDevice)) {
                    if (!mStartPlayingAudio) {
                        mStartPlayingAudio = true;
                        startThingyOverlayAnimation();

                        // making sure that when the thingy button for listening is pressed all connected thingys start to listen.
                        for(int i = 0; i<mThingySdkManager.getConnectedDevices().size(); i++) {
                            BluetoothDevice device = mThingySdkManager.getConnectedDevices().get(i); // selecting the device in the list in corresponding with i
                            Log.v("data", "Device is: " + device.getName());
                            mThingySdkManager.enableThingyMicrophone(device, true); // enabeling all thingys to listen
                        }
                    } else {
                        // making sure that when the thingy button for listening is pressed all connected thingys start to listen.
                        for(int i = 0; i<mThingySdkManager.getConnectedDevices().size(); i++) {
                            BluetoothDevice device = mThingySdkManager.getConnectedDevices().get(i); // selecting the device in the list in corresponding with i
                            mThingySdkManager.enableThingyMicrophone(device, false); // disabeling all thingys from listening
                        }                        stopThingyOverlayAnimation();
                        mStartPlayingAudio = false;
                    }
                }
            }
        });

        if (savedInstanceState != null) {
            mStartPlayingAudio = savedInstanceState.getBoolean(AUDIO_PLAYING_STATE);
            mStartRecordingAudio = savedInstanceState.getBoolean(AUDIO_RECORDING_STATE);

            if (mStartPlayingAudio) {
                startThingyOverlayAnimation();
            }

            if (mStartRecordingAudio) {
                if (mThingySdkManager.isConnected(mDevice)) {
                    startMicrophoneOverlayAnimation();
                    sendAudiRecordingBroadcast();
                }
            }
        }

        loadFeatureDiscoverySequence();


        //PSG edit No.3----------------------------
        mAdvertiseButton = rootView.findViewById(R.id.startClh_btn);
        mClhIDInput= rootView.findViewById(R.id.clhIDInput_text);
        mClhLog= rootView.findViewById(R.id.logClh_text);

        //initial Clusterhead: advertiser, scanner, processor
        mClh=new ClusterHead(mClhID);
        mClh.initClhBLE(ClhConst.ADVERTISING_INTERVAL);
        mClhAdvertiser=mClh.getClhAdvertiser();
        mClhScanner=mClh.getClhScanner();
        mClhProcessor=mClh.getClhProcessor();

        //timer 1000 ms for SINK to process receive data(display data to text box)
        final Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 1000); //loop every cycle
                if(mIsSink)
                {
                    ArrayList<ClhAdvertisedData> procList=mClhProcessor.getProcessDataList();
                    for(int i=0; i<procList.size();i++)
                    {
                        int soundPower = procList.get(i).getSoundPower();
                        mClhLog.append("Clap detected!\r\n Sound power: " + soundPower);
                        mClhLog.append("\r\n");
                    }
                    procList.clear();
                }
            }
        }, 1000); //the time you want to delay in milliseconds

        //"Start" button Click Hander
        // get Cluster Head ID (0-127) in text box to initialize advertiser
        //Then Start advertising
        //ID=0: Sink
        //ID=1..126: normal Cluster head, get sound data from Thingy and advertise
        //ID=127: test cluster Head, send dummy data for testing purpose
        mAdvertiseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Resources res = getResources();

                Log.i(LOG_TAG, mAdvertiseButton.getText().toString());
                if (mAdvertiseButton.getText().toString().equals("Start")) {
                    mAdvertiseButton.setText("Stop");
                    mClhIDInput.setEnabled(false);

                    mClh.clearClhAdvList(); //empty list before starting

                    //check input text must in rang 0..127
                    String strEnteredVal = mClhIDInput.getText().toString();
                    if ((strEnteredVal.compareTo("") == 0) || (strEnteredVal == null)) {
                        mClhIDInput.setText(String.format( "%d", mClhID));
                        Log.i(LOG_TAG, "error: ClhID must be in 0-127");
                        Log.i(LOG_TAG, "set ClhID default:"+mClhID);

                    } else {
                        int num = Integer.valueOf(strEnteredVal);
                        if (num>127) num=mClhID;
                        mClhID = (byte) num;
                        mIsSink = mClh.setClhID(mClhID);
                        Log.i(LOG_TAG, "set ClhID:"+mClhID);
                    }

                    //ID=127, set dummy data include 100 elements for testing purpose
                    if(mClhID==127) {
                        //mClhID = 1;
                        byte clhPacketID = 1;
                        mClhThingySoundPower = 100;
                        mClhData.setSourceID(mClhID);
                        mClhData.setPacketID(clhPacketID);
                        mClhData.setDestId(mClhDestID);
                        mClhData.setHopCount(mClhHops);
                        mClhData.setThingyId(mClhThingyID);
                        mClhData.setThingyDataType(mClhThingyType);
                        mClhData.setSoundPower(mClhThingySoundPower);
                        mClhAdvertiser.addAdvPacketToBuffer(mClhData, true);
                        for (int i = 0; i < 100; i++) {
                            ClhAdvertisedData clh = new ClhAdvertisedData();
                            clh.Copy(mClhData);
                            //Log.i(LOG_TAG, "Array old:" + Arrays.toString(clh.getParcelClhData()));
                            mClhThingySoundPower += 10;
                            clh.setSoundPower(mClhThingySoundPower);
                            mClhAdvertiser.addAdvPacketToBuffer(clh, true);

                            Log.i(LOG_TAG, "Add array:" + Arrays.toString(clh.getParcelClhData()));
                            Log.i(LOG_TAG, "Array new size:" + mClhAdvertiser.getAdvertiseList().size());
                        }
                    } else if (mClhID>0) {
                        byte clhPacketID = 0; // Packet ID 0 is discovery packet
                        mClhDiscovery.setPacketID(clhPacketID);
                        mClhDiscovery.setSourceID(mClhID); // Source ID = this CH's ID
                        mClhDiscovery.setDestId(mClhDestID); // Destination is always the sink (0)
                        mClhDiscovery.setHopCount(mClhHops); // Max amount of hops for this packet
                        mClhDiscovery.setNextHop((byte) -1); //for broadcast
                        mClhDiscovery.addToRouting(mClhID);
                        //TODO retransmission
                        mClhAdvertiser.addAdvPacketToBuffer(mClhDiscovery, true);
                        Log.i("Discovery sent", "Discovery sent");
                    }

                    mClhAdvertiser.nextAdvertisingPacket(); //start advertising
                }
                else
                {//stop advertising
                    mAdvertiseButton.setText("Start");
                    mClhIDInput.setEnabled(true);
                    mClhAdvertiser.stopAdvertiseClhData();
                }
            }
        });
        mClhIDInput.setText(Integer.toString((int)mClhID));
        //End PSG edit No.3----------------------------



        return rootView;
    }

    private void sendAudiRecordingBroadcast() {
        Intent startAudioRecording = new Intent(getActivity(), ThingyMicrophoneService.class);
        startAudioRecording.setAction(Utils.START_RECORDING);
        startAudioRecording.putExtra(Utils.EXTRA_DEVICE, mDevice);
        getActivity().startService(startAudioRecording);
    }

    private void stop() {
        final Intent s = new Intent(Utils.STOP_RECORDING);
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(s);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUDIO_PLAYING_STATE, mStartPlayingAudio);
        outState.putBoolean(AUDIO_RECORDING_STATE, mStartRecordingAudio);
    }

    @Override
    public void onResume() {
        super.onResume();
        ThingyListenerHelper.registerThingyListener(getContext(), mThingyListener, mDevice);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mAudioRecordBroadcastReceiver, createAudioRecordIntentFilter(mDevice.getAddress()));
    }

    @Override
    public void onPause() {
        super.onPause();
        ThingyListenerHelper.unregisterThingyListener(getContext(), mThingyListener);
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mAudioRecordBroadcastReceiver);
        mVoiceVisualizer.stopDrawing();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopRecording();
        stopThingyOverlayAnimation();
    }

    @Override
    public void onRequestPermission(final String permission, final int requestCode) {
        // Since the nested child fragment (activity > fragment > fragment) wasn't getting called
        // the exact fragment index has to be used to get the fragment.
        // Also super.onRequestPermissionResult had to be used in both the main activity, fragment
        // in order to propagate the request permission callback to the nested fragment
        requestPermissions(new String[]{permission}, requestCode);
    }

    @Override
    public void onCancellingPermissionRationale() {
        Utils.showToast(getActivity(), getString(R.string.requested_permission_not_granted_rationale));
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Utils.REQ_PERMISSION_RECORD_AUDIO:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Utils.showToast(getActivity(), getString(R.string.rationale_permission_denied));
                } else {
                    startRecording();
                }
        }
    }

    private void checkMicrophonePermissions() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            final PermissionRationaleDialogFragment dialog = PermissionRationaleDialogFragment.getInstance(Manifest.permission.RECORD_AUDIO,
                    Utils.REQ_PERMISSION_RECORD_AUDIO, getString(R.string.microphone_permission_text));
            dialog.show(getChildFragmentManager(), null);
        }
    }

    private void startRecording() {
        startMicrophoneOverlayAnimation();
        sendAudiRecordingBroadcast();
        mStartRecordingAudio = true;
    }

    private void stopRecording() {
        stopMicrophoneOverlayAnimation();
        stop();
        mStartRecordingAudio = false;
    }

    private void startMicrophoneOverlayAnimation() {
        mThingy.setEnabled(false);
        mMicrophone.setImageResource(R.drawable.ic_mic_white_off);
        mMicrophone.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_device_bg_red));
        mMicrophoneOverlay.animate().alpha(ALPHA_MAX).setDuration(DURATION).withEndAction(new Runnable() {
            @Override
            public void run() {
                if (mMicrophoneOverlay.getAlpha() == ALPHA_MAX) {
                    mMicrophoneOverlay.animate().alpha(ALPHA_MIN).setDuration(DURATION).withEndAction(this).start();
                } else {
                    mMicrophoneOverlay.animate().alpha(ALPHA_MAX).setDuration(DURATION).withEndAction(this).start();
                }
            }
        }).start();
    }

    private void stopMicrophoneOverlayAnimation() {
        mThingy.setEnabled(true);
        mStartRecordingAudio = false;
        mMicrophoneOverlay.animate().cancel();
        mMicrophoneOverlay.setAlpha(ALPHA_MIN);
        mMicrophone.setImageResource(R.drawable.ic_mic_white);
        mMicrophone.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_device_bg_blue));
    }

    private void startThingyOverlayAnimation() {
        mMicrophone.setEnabled(false);
        mThingy.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_device_bg_red));
        mThingyOverlay.animate().alpha(ALPHA_MAX).setDuration(DURATION).withEndAction(new Runnable() {
            @Override
            public void run() {
                if (mThingyOverlay.getAlpha() == ALPHA_MAX) {
                    mThingyOverlay.animate().alpha(ALPHA_MIN).setDuration(DURATION).withEndAction(this).start();
                } else {
                    mThingyOverlay.animate().alpha(ALPHA_MAX).setDuration(DURATION).withEndAction(this).start();
                }
            }
        }).start();
    }

    private void stopThingyOverlayAnimation() {
        mMicrophone.setEnabled(true);
        mThingyOverlay.animate().cancel();
        mThingyOverlay.setAlpha(ALPHA_MIN);
        mThingy.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_device_bg_blue));
        mStartPlayingAudio = false;
    }

    private class FragmentAdapter extends FragmentPagerAdapter {
        private int mSelectedFragmentTab = 0;

        FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return FrequencyModeFragment.newInstance(mDevice);
                case 1:
                    return PcmModeFragment.newInstance(mDevice);
                default:
                case 2:
                    return SampleModeFragment.newInstance(mDevice);
            }
        }

        @Override
        public int getCount() {
            return mClhHops;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getResources().getStringArray(R.array.sound_tab_title)[position];
        }

        void setSelectedFragment(final int selectedTab) {
            mSelectedFragmentTab = selectedTab;
        }

        public int getSelectedFragment() {
            return mSelectedFragmentTab;
        }
    }

    private static IntentFilter createAudioRecordIntentFilter(final String address) {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(Utils.EXTRA_DATA_AUDIO_RECORD + address);
        intentFilter.addAction(Utils.ERROR_AUDIO_RECORD);
        return intentFilter;
    }

    private void displayStreamingInformationDialog() {
        final SharedPreferences sp = requireActivity().getSharedPreferences(Utils.PREFS_INITIAL_SETUP, Context.MODE_PRIVATE);
        final boolean showStreamingDialog = sp.getBoolean(Utils.INITIAL_AUDIO_STREAMING_INFO, true);
        if (showStreamingDialog) {
            MessageDialogFragment fragment = MessageDialogFragment.newInstance(getString(R.string.info), getString(R.string.mtu_warning));
            fragment.show(getChildFragmentManager(), null);

            final SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(Utils.INITIAL_AUDIO_STREAMING_INFO, false);
            editor.apply();
        }
    }

    private void loadFeatureDiscoverySequence() {
        if (!Utils.checkIfSequenceIsCompleted(requireContext(), Utils.INITIAL_SOUND_TUTORIAL)) {

            final SpannableString microphone = new SpannableString(getString(R.string.start_talking_to_thingy));
            final SpannableString thingy = new SpannableString(getString(R.string.start_talking_from_thingy));

            final TapTargetSequence sequence = new TapTargetSequence(requireActivity());
            sequence.continueOnCancel(true);
            sequence.targets(
                    TapTarget.forView(mMicrophone, microphone).
                            transparentTarget(true).
                            dimColor(R.color.grey).
                            outerCircleColor(R.color.accent).id(0),
                    TapTarget.forView(mThingy, thingy).
                            transparentTarget(true).
                            dimColor(R.color.grey).
                            outerCircleColor(R.color.accent).id(1)
            ).listener(new TapTargetSequence.Listener() {
                @Override
                public void onSequenceFinish() {
                    Utils.saveSequenceCompletion(requireContext(), Utils.INITIAL_SOUND_TUTORIAL);
                    displayStreamingInformationDialog();
                }

                @Override
                public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {

                }

                @Override
                public void onSequenceCanceled(TapTarget lastTarget) {

                }
            }).start();
        }
    }
}