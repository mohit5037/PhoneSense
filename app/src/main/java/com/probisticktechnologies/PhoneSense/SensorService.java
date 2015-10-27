package com.probisticktechnologies.PhoneSense;

import android.app.IntentService;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.ManagerFactoryParameters;

public class SensorService extends IntentService implements SensorEventListener {

    // Boolean variable to check continuance of Service
    public static boolean isContinued = true;

    // Data Structures to hold current sensor Values and Data
    TreeMap<String,Float[]> currentSensorValues = new TreeMap<>();
    TreeMap<String,Float[]> previousSensorValues = new TreeMap<>();

    // Map to hold sensors available on device
    Map<String, Sensor> deviceSensors;

    // Map to hold sensors to search on device
    Map<String, Integer> sensorIds;

    SensorManager sensorManager;

    private int SENSOR_DELAY;

    // Time Divide Constant
    private final Long TIME_DIVIDE_CONSTANT = 1000000L;

    public SensorService() {
        super("SensorService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        // Making the continuance flag true
        this.isContinued = true;

        // Getting the passed data
        this.deviceSensors = PhoneSensorDataCaptureFragment.deviceSensors;
        this.sensorIds = PhoneSensorDataCaptureFragment.sensorIds;
        this.SENSOR_DELAY = PhoneSensorDataCaptureFragment.SENSOR_DELAY;
        this.sensorManager = PhoneSensorDataCaptureFragment.sensorManager;

        // Starting collecting data
        getDataFromSensors();

    }

    // Start storing data in the data structures
    private void getDataFromSensors() {

        // Initializing default sensor values
        currentSensorValues.put("Accelerometer", null);
        currentSensorValues.put("Gyroscope", null);
        currentSensorValues.put("Magnetometer", null);

        // Initializing default sensor values
        previousSensorValues.put("Accelerometer", new Float[]{0.0f,0.0f,0.0f});
        previousSensorValues.put("Gyroscope", new Float[]{0.0f,0.0f,0.0f});
        previousSensorValues.put("Magnetometer", new Float[]{0.0f,0.0f,0.0f});

        // Registering Sensors
        Iterator iterator = deviceSensors.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, Sensor> thisEntry = (Map.Entry) iterator.next();
            sensorManager.registerListener(this,thisEntry.getValue(), SENSOR_DELAY);
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // Checking the continuance flag
        if(isContinued == false){
            // UN-Registering Sensors
            Iterator iterator = deviceSensors.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String, Sensor> thisEntry = (Map.Entry) iterator.next();
                sensorManager.unregisterListener(this, thisEntry.getValue());
            }

            this.stopSelf();
            return;
        }


        // Getting the sensor which is changed
        Sensor sensor = event.sensor;
        String sensorName;

        // Finding which type of sensor has changed its value
        switch (sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER :
                sensorName = "Accelerometer";
                break;
            case Sensor.TYPE_GYROSCOPE:
                sensorName = "Gyroscope";
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                sensorName = "Magnetometer";
                break;
            default: return;
        }

        // Storing the new values
        if(currentSensorValues.get(sensorName) == null){
            currentSensorValues.put(sensorName, new Float[]{event.values[0], event.values[1],
                    event.values[2]});
        }else{
            Float[] temp = new Float[9];
            int i = 0;
            // Storing this line of value in masterData
            for (Map.Entry entry: currentSensorValues.entrySet()){

                if(entry.getValue() != null) {
                    previousSensorValues.put(entry.getKey().toString(), (Float[]) entry.getValue());


                    for (Float value : ((Float[]) entry.getValue())) {
                        temp[i] = value;
                        i++;
                    }
                }else{
                    for (Float value :  previousSensorValues.get(entry.getKey().toString())) {
                        temp[i] = value;
                        i++;
                    }
                }
            }

            PhoneSensorDataCaptureFragment.masterData.add(new ArrayList<Float>(Arrays.asList(temp)));
            PhoneSensorDataCaptureFragment.masterDataTimestamp.add((new Timestamp(event.timestamp)).getTime() / TIME_DIVIDE_CONSTANT);

            // Making the values null
            for (Map.Entry entry: currentSensorValues.entrySet()){
                currentSensorValues.put(entry.getKey().toString(), null);
            }

            // Storing the new value now
            currentSensorValues.put(sensorName, new Float[]{event.values[0], event.values[1],
                    event.values[2]});
        }

        // Checking if full house
        if(currentSensorValues.get("Accelerometer") != null &&
                currentSensorValues.get("Gyroscope") != null &&
                currentSensorValues.get("Magnetometer") != null){

            Float[] temp = new Float[9];
            int i = 0;
            // Storing this line of value in masterData
            for (Map.Entry entry: currentSensorValues.entrySet()){

                previousSensorValues.put(entry.getKey().toString(), (Float[])entry.getValue());

                for(Float value: ((Float[]) entry.getValue())){
                    temp[i] = value;
                    i++;
                }
            }

            PhoneSensorDataCaptureFragment.masterData.add(new ArrayList<Float>(Arrays.asList(temp)));
            PhoneSensorDataCaptureFragment.masterDataTimestamp.add((new Timestamp(event.timestamp)).getTime() / TIME_DIVIDE_CONSTANT);

            // Making the values null
            for (Map.Entry entry: currentSensorValues.entrySet()){
                currentSensorValues.put(entry.getKey().toString(), null);
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}