package com.probisticktechnologies.PhoneSense;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.probisticktechnologies.R;
import com.probisticktechnologies.bluetoothchat.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A placeholder fragment containing a simple view.
 */
public class PhoneSensorDataCaptureFragment extends Fragment {

    private static final String TAG = "PhoneSensorDataCaptureFragment";

    // Switch to control SPSD power
    private Switch spsdPowerSwitch;

    // Declaring Buttons
    private Button startRecord;
    private Button stopRecord;

    // Capture Speed Radio Group
    private RadioGroup captureSpeed;

    // Sensor Delay default value
    public static int SENSOR_DELAY = SensorManager.SENSOR_DELAY_UI;

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

    // Map to hold sensors to search on device (In our case Accelo, gyro and magneto)
    public static Map<String, Integer> sensorIds = new HashMap<String, Integer>();

    // Sensor Manager
    public static SensorManager sensorManager;

    // Intent to launch Sensor Service
    Intent sensorServiceIntent;

    // Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    // Declaring bluetooth service object
    public static BluetoothService bluetoothService = null;

    // String Buffer for bluetooth messages
    StringBuffer mOutStringBuffer;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    // Declaring data structures to hold data for whole session
    public  static ArrayList<ArrayList<Float>> masterData = new ArrayList<>(); // for sensor values
    public  static ArrayList<Long> masterDataTimestamp = new ArrayList<>();  // for timestamps

    // Constructor
    public PhoneSensorDataCaptureFragment() {

        // We are only interested in accelerometer, gyroscope and magnetometer sensor
        sensorIds.put("Accelerometer", Sensor.TYPE_ACCELEROMETER);
        sensorIds.put("Gyroscope", Sensor.TYPE_GYROSCOPE);
        sensorIds.put("Magnetometer", Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // Getting the Switch
        spsdPowerSwitch = (Switch) rootView.findViewById(R.id.spsd_power_on_off_switch);

        // Checking whether any sensor is present in the device of not and notifying user acc
        if(deviceSensors.size()!= 0){
            if(deviceSensors.containsKey("Accelerometer")){
                ((TextView) rootView.findViewById(R.id.accelerometer_availability_status)).setText("Present");
            }
            if(deviceSensors.containsKey("Gyroscope")){
                ((TextView) rootView.findViewById(R.id.gyroscope_availability_status)).setText("Present");
            }
            if(deviceSensors.containsKey("Magnetometer")){
                ((TextView) rootView.findViewById(R.id.magenetometer_availability_status)).setText("Present");
            }

        }

        // Setting listeners on buttons
        startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //v.setFocusableInTouchMode(false);
                disableButton(startRecord);
                disableRadioGroup(captureSpeed);
                startCapturing();
            }
        });

        stopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //v.setFocusableInTouchMode(false);
                //disableButton(stopRecord);
                stopCapturing(v);
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

        // Setting listener on power switch
        spsdPowerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    // Enable the required components
                    enableRadioGroup(captureSpeed);
                    enableButton(startRecord);
                    disableButton(stopRecord);
                    if(bluetoothService!=null){
                        bluetoothService.start();
                    }else {
                        // Starting Bluetooth Services
                        enableBluetoothServices();
                    }

                }else{
                    stopCapturing(stopRecord);
                    stopBluetoothService();
                    disableButton(startRecord);
                    disableRadioGroup(captureSpeed);
                }
            }
        });

        disableRadioGroup(captureSpeed);
        disableButton(startRecord);
        disableButton(stopRecord);

        return rootView;
    }

    /**
     * Method to disable any view item, in this case a button
     * @param button
     */
    private void disableButton(View button) {
        button.setEnabled(false);
        button.setClickable(false);
    }

    /**
     * Method to enable any view item, in this case a button
     * @param button
     */
    private void enableButton(View button) {
        button.setEnabled(true);
        button.setClickable(true);
    }

    /**
     * Search for availability of required Sensors on the current device.
     */
    private void findDeviceSensors() {

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

    /**
     * This method is called when user presses the start button in order to capture the motion data.
     */
    private void startCapturing() {

        // Make sure that bluetooth services are up and running
        // enableBluetoothServices();



        // Checking if bluetooth service is already running or not
        if(bluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
            sendSensorData();
        }

        // Making stop button enabled
        enableButton(stopRecord);



//        if(DATA_STORAGE_MODE == WRITE_TO_FILE) {
//            // Getting file name from user
//            PromptDialog dlg = new PromptDialog(getActivity(), "Please Enter file Name", "(In which data is to be stored)") {
//                @Override
//                public boolean onOkClicked(String input) {
//
//                    // do something
//                    FILENAME = "PhoneSense" + '_' + input + '_' + (new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss'.csv'").format(new Date()));
//                    Toast.makeText(getActivity(), "Recording to " + FILENAME, Toast.LENGTH_SHORT).show();
//
//                    // Making an intent
//                    sensorServiceIntent = new Intent(getActivity(), SensorService.class);
//
//                    // Starting the service
//                    getActivity().startService(sensorServiceIntent);
//
//                    // making stop button enabled
//                    enableButton(stopRecord);
//
//                    return true; // true = close dialog
//                }
//
//                @Override
//                public void onCancelClicked() {
//                    enableButton(startRecord);
//                    enableRadioGroup(captureSpeed);
//                }
//            };
//            dlg.show();
//        }else if(DATA_STORAGE_MODE == SEND_VIA_BLUETOOTH){
//            // Notify user
//            Toast.makeText(getActivity(), "Recording Data to be sent via bluetooth!!", Toast.LENGTH_SHORT).show();
//
//            // Making an intent
//            sensorServiceIntent = new Intent(getActivity(), SensorService.class);
//
//            // Starting the service
//            getActivity().startService(sensorServiceIntent);
//
//            // making stop button enabled
//            enableButton(stopRecord);
//        }
    }

    private void stopBluetoothService(){

        if(bluetoothService!=null) {
            // Stopping the bluetooth Services
            bluetoothService.stop();
        }
    }

    /**
     * Invoke necessary bluetooth services for device to be discoverable and accept connections.
     */
    private void enableBluetoothServices() {

        // Getting Bluetooth Adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }else {
            if(bluetoothService == null){
                setupBluetoothService();
            }else {
                bluetoothService.start();
            }
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode){
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK){
                    // Bluetooth is now enabled, so set up Bluetooth Service
                     setupBluetoothService();
                } else{
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(getActivity(), "Bluetooth was not enabled. Leaving Application.",
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    private void setupBluetoothService() {

        // Initialize the bluetooth Service
        bluetoothService = new BluetoothService(getActivity(), mHandler);

        // Initialize the buffer for outgoing message
        mOutStringBuffer = new StringBuffer("");

        bluetoothService.start();
    }


    private void sendSensorData(){

        // Making an intent
        sensorServiceIntent = new Intent(getActivity(), SensorService.class);

        // Starting the service
        getActivity().startService(sensorServiceIntent);
    }


    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what){
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1){
                        case BluetoothService.STATE_CONNECTED:
                            //setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            //Toast.makeText(activity, "Data Transfer Begins Now!!", Toast.LENGTH_SHORT).show();
                            //sendSensorData();
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            //setStatus(R.string.not_connected);
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    break;
                case Constants.MESSAGE_READ:
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if(null != activity){
                        Toast.makeText(activity, "Connected to "
                        + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if(null != activity){
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };


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
    public void stopCapturing(View view){

        if((!startRecord.isEnabled()) && stopRecord.isEnabled()) {
            // Stopping the service
            SensorService.isContinued = false;

            // Stopping the bluetooth service
            // bluetoothService.stop();

            // Notifying user
            Toast.makeText(getActivity(), "Recording Stopped!!", Toast.LENGTH_SHORT).show();

//        // Checking for Data Storage Mode
//        if(DATA_STORAGE_MODE == WRITE_TO_FILE){
//            // Writing stored data to file
//            writeToFile();
//        }else if(DATA_STORAGE_MODE == SEND_VIA_BLUETOOTH){
//            Intent bluetoothIntent = new Intent(getActivity(),MainActivity.class );
//            startActivity(bluetoothIntent);
//        }

//        // Free the memory
//        masterDataTimestamp.clear();
//        masterData.clear();


            // Changing buttons config
            enableButton(startRecord);
            enableRadioGroup(captureSpeed);
            disableButton(stopRecord);
        }
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
            stopCapturing(stopRecord);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null){
            bluetoothService.stop();
        }
    }
}

