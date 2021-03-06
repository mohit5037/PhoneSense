package com.probisticktechnologies.PhoneSense;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.params.BlackLevelPattern;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.probisticktechnologies.R;
import com.probisticktechnologies.bluetoothchat.*;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A placeholder fragment containing a simple view.
 */
public class PhoneSensorDataCaptureFragment extends Fragment {

    // Declaring Buttons
    private Button startRecord;
    private Button stopRecord;

    // Capture Speed Radio Group
    private RadioGroup captureSpeed;

    // Data storage mode Radio Group
    private RadioGroup dataStorageMode;



    // Sensor Delay
    public static int SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL;

    // Data Storage Mode
    public static final int WRITE_TO_FILE = 1;
    public static final int SEND_VIA_BLUETOOTH = 2;
    public static int DATA_STORAGE_MODE = WRITE_TO_FILE;



    // File in which data is to be stored
    private String FILENAME;
    private final String dirName = "PhoneSense";

    // Data Structure to hold headings to be written to file
    List<String> headings = Arrays.asList("Timestamp",
            "Accelerometer X",
            "Accelerometer Y",
            "Accelerometer Z",
            "Gyroscope X",
            "Gyroscope Y",
            "Gyroscope Z",
            "Magnetometer X",
            "Magnetometer Y",
            "Magnetometer Z");


    // Map to hold sensors available on device
    public static Map<String, Sensor> deviceSensors = new HashMap<String, Sensor>();

    // Map to hold sensors to search on device
    public static Map<String, Integer> sensorIds = new HashMap<String, Integer>();

    // Sensor Manager
    public static SensorManager sensorManager;

    // Intent to launch Sensor Service
    Intent sensorServiceIntent;

    // Initializing data structures
    public  static ArrayList<ArrayList<Float>> masterData = new ArrayList<>();
    public  static ArrayList<Long> masterDataTimestamp = new ArrayList<>();

    // Constructor
    public PhoneSensorDataCaptureFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflating the view
        View rootView = inflater.inflate(R.layout.fragment_phone_sensor_data_capture, container, false);

        // Finding the sensors available on the device
        findDeviceSensors();

        // Getting the buttons instances from layout
        startRecord = (Button) rootView.findViewById(R.id.start_record_button);
        stopRecord = (Button) rootView.findViewById(R.id.stop_record_button);

        // Getting the Radio Group
        captureSpeed = (RadioGroup) rootView.findViewById(R.id.capture_speed_radio_group);
        dataStorageMode = (RadioGroup) rootView.findViewById(R.id.data_storage_mode_radioGroup);


        // Checking whether any sensor is present in the device of not and notifying user acc
        if(deviceSensors.size()!=0){
            if(deviceSensors.containsKey("Accelerometer")){
                ((TextView) rootView.findViewById(R.id.accelerometer_availability_status)).setText("Present");
            }
            if(deviceSensors.containsKey("Gyroscope")){
                ((TextView) rootView.findViewById(R.id.gyroscope_availability_status)).setText("Present");
            }
            if(deviceSensors.containsKey("Magnetometer")){
                ((TextView) rootView.findViewById(R.id.magenetometer_availability_status)).setText("Present");
            }

            // Making the buttons clickable
            enableButton(startRecord);
            disableButton(stopRecord);

        }
        else{
            Toast.makeText(getActivity(), "Sorry, No sensors available on your device!",
                    Toast.LENGTH_SHORT).show();
            disableButton(startRecord);
            disableButton(stopRecord);
        }

        // Setting listeners on buttons
        startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //v.setFocusableInTouchMode(false);
                disableButton(startRecord);
                disableRadioGroup(captureSpeed);
                startRecording();
            }
        });

        stopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //v.setFocusableInTouchMode(false);
                disableButton(stopRecord);
                stopRecording(v);
            }
        });

        // Setting listener on Radio group
        captureSpeed.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.fast_record_radio_button:
                        SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST;
                        break;
                    case R.id.medium_record_radio_button:
                        SENSOR_DELAY = SensorManager.SENSOR_DELAY_UI;
                        break;
                    case R.id.normal_record_radio_button:
                        SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL;
                        break;
                    default:
                        SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL;
                }
            }
        });

        dataStorageMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.write_to_file_radioButton:
                        DATA_STORAGE_MODE = WRITE_TO_FILE;
                        break;
                    case R.id.send_via_bluetooth_radioButton:
                        DATA_STORAGE_MODE = SEND_VIA_BLUETOOTH;
                        break;
                    default:
                        DATA_STORAGE_MODE = WRITE_TO_FILE;
                }
            }
        });



        return rootView;
    }

    private void disableButton(View button) {
        button.setEnabled(false);
        button.setClickable(false);
    }

    private void enableButton(View button) {
        button.setEnabled(true);
        button.setClickable(true);
    }

    // Finds the available sensors on device
    private void findDeviceSensors() {

        // We are only interested in accelerometer, gyroscope and magnetometer
        sensorIds.put("Accelerometer", Sensor.TYPE_ACCELEROMETER);
        sensorIds.put("Gyroscope", Sensor.TYPE_GYROSCOPE);
        sensorIds.put("Magnetometer", Sensor.TYPE_MAGNETIC_FIELD);

        // Getting sensor manager
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        // Checking availability
        Iterator sensors = sensorIds.entrySet().iterator();
        while (sensors.hasNext()){
            Map.Entry<String,Integer> thisEntry = (Map.Entry<String,Integer>) sensors.next();
            String sensorName = thisEntry.getKey();
            Integer sensorId = thisEntry.getValue();

            // If sensor is present on device then store it
            if(sensorManager.getDefaultSensor(sensorId)!= null){
                deviceSensors.put(sensorName, sensorManager.getDefaultSensor(sensorId));
            }
        }

    }

    // When start recording button is clicked
    private void startRecording() {

        if(DATA_STORAGE_MODE == WRITE_TO_FILE) {
            // Getting file name from user
            PromptDialog dlg = new PromptDialog(getActivity(), "Please Enter file Name", "(In which data is to be stored)") {
                @Override
                public boolean onOkClicked(String input) {

                    // do something
                    FILENAME = "PhoneSense" + '_' + input + '_' + (new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss'.csv'").format(new Date()));
                    Toast.makeText(getActivity(), "Recording to " + FILENAME, Toast.LENGTH_SHORT).show();

                    // Making an intent
                    sensorServiceIntent = new Intent(getActivity(), SensorService.class);

                    // Starting the service
                    getActivity().startService(sensorServiceIntent);

                    // making stop button enabled
                    enableButton(stopRecord);

                    return true; // true = close dialog
                }

                @Override
                public void onCancelClicked() {
                    enableButton(startRecord);
                    enableRadioGroup(captureSpeed);
                }
            };
            dlg.show();
        }else if(DATA_STORAGE_MODE == SEND_VIA_BLUETOOTH){
            // Notify user
            Toast.makeText(getActivity(), "Recording Data to be sent via bluetooth!!", Toast.LENGTH_SHORT).show();

            // Making an intent
            sensorServiceIntent = new Intent(getActivity(), SensorService.class);

            // Starting the service
            getActivity().startService(sensorServiceIntent);

            // making stop button enabled
            enableButton(stopRecord);
        }
    }





    // When start Button is clicked
    public  void writeToFile(){

        // Checking if External Storage is available or not
        if(isExternalStorageWritable() == false){
            Toast.makeText(getActivity(),"External Storage Not Available!!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Creating Directory Path
        File myFilesDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + dirName);

        // Checking if it exists or not
        if(!myFilesDir.exists()){
            myFilesDir.mkdirs();
        }

        // Creating new file
        String root = Environment.getExternalStorageDirectory().toString() + File.separator + dirName;
        File dataFile = new File(root, FILENAME);
        if(dataFile.exists()){
            dataFile.delete();
        }
        try{
            // Opening output Stream
            FileOutputStream fos = new FileOutputStream(dataFile);
            StringBuilder sb = new StringBuilder();

            // Writing Headings
            for(String heading: headings){
                sb.append(heading);
                sb.append(',');
            }
            sb.delete(sb.length()-1, sb.length());
            sb.append('\n');
            fos.write(sb.toString().getBytes());
            sb.setLength(0);

            // Writing data rows
            for(int i=0; i<masterData.size(); i++){
                // Appending time stamp
                sb.append(masterDataTimestamp.get(i));
                sb.append(',');
                for(Float value: masterData.get(i)){
                    sb.append(value);
                    sb.append(',');
                }
                sb.delete(sb.length()-1, sb.length());
                sb.append('\n');
                fos.write(sb.toString().getBytes());
                sb.setLength(0);
            }

            // Closing file and notifying user
            fos.flush();
            fos.close();

            Toast.makeText(getActivity(), "Data Written to " + dirName + File.separator + FILENAME, Toast.LENGTH_LONG).show();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    // When stop recording button is clicked
    public void stopRecording(View view){

        // Stopping the service
        SensorService.isContinued = false;

        // Notifying user
        Toast.makeText(getActivity(), "Recording Stopped!!", Toast.LENGTH_SHORT).show();

        // Checking for Data Storage Mode
        if(DATA_STORAGE_MODE == WRITE_TO_FILE){
            // Writing stored data to file
            writeToFile();
        }else if(DATA_STORAGE_MODE == SEND_VIA_BLUETOOTH){
            Intent bluetoothIntent = new Intent(getActivity(),MainActivity.class );
            startActivity(bluetoothIntent);
        }

//        // Free the memory
//        masterDataTimestamp.clear();
//        masterData.clear();


        // Changing buttons config
        enableButton(startRecord);
        enableRadioGroup(captureSpeed);
        disableButton(stopRecord);
    }

    private void enableRadioGroup(RadioGroup captureSpeed) {

        for (int i = 0; i < captureSpeed.getChildCount(); i++) {
            captureSpeed.getChildAt(i).setEnabled(true);
        }
    }

    private void disableRadioGroup(RadioGroup captureSpeed) {

        for (int i = 0; i < captureSpeed.getChildCount(); i++) {
            captureSpeed.getChildAt(i).setEnabled(false);
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        // Checking if recording is in progress
        if(!startRecord.isEnabled()){
            stopRecording(stopRecord);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

