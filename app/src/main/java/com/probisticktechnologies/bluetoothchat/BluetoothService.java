package com.probisticktechnologies.bluetoothchat;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

/**
 * This class will have all functions to connect this device to client app and handle all the data
 * transfer operations.
 */
public class BluetoothService {

    private static final String TAG = "BluetoothChatService";

    // Member Fields
    // Bluetooth Adapter Initialization (This will be used for all bluetooth operations)
    private BluetoothAdapter mAdapter;

    // Variable to hold the bluetooth connection state
    private int mState;

    // Handler to communicate with UI Activity/Fragment
    private Handler mHandler;
    
    // Thread for accepting connections
    private AcceptThread mAcceptThread;

    // Thread for connected state/operations
    private ConnectedThread mConnectedThread;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device


    /**
     * Constructor
     *
     * @param context The UI Activity/Fragment Context
     * @param handler A handler to send message back to the UI Activity/Fragment
     */
    public BluetoothService(Context context, Handler handler) {

        // Getting Bluetooth Adapter
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        // Setting the state to None
        mState = STATE_NONE;

        // Setting the handler
        mHandler = handler;

    }

    /**
     * Set the current state of the bluetooth connection
     *
     * @param state
     */
    private synchronized void setState(int state){
        mState = state;

        // Notify the UI Activity/Fragment of the state change
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current bluetooth connection state
     */
    public synchronized int getState(){
        return mState;
    }

    /**
     * Start the Bluetooth Service in order to listen for incoming connections.
     */
    public synchronized void start(){

        //
    }



}
