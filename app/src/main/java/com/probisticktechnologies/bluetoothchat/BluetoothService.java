package com.probisticktechnologies.bluetoothchat;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * This class will have all functions to connect this device to client app and handle all the data
 * transfer operations.
 */
public class BluetoothService {

    private static final String TAG = "BluetoothChatService";

    // Member Fields
    // Bluetooth Adapter Initialization (This will be used for all bluetooth operations)
    private BluetoothAdapter mAdapter;

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";

    // Unique UUID for this device
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

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

        // Cancel any thread currently running a connection
        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a Bluetooth Server Socket
        if(mAcceptThread == null){
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }


    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private  class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                        MY_UUID_SECURE);
            } catch (IOException e){

            }
            mmServerSocket = tmp;

        }

        public void run() {

            // Setting name of thread
            setName("AcceptThread");

            BluetoothSocket socket = null;

            // Listen to the server socket if we'are not connected to client app
            while(mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }


                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected
                                try {
                                    socket.close();
                                } catch (IOException e) {

                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel(){
            try{
                mmServerSocket.close();
            }catch (IOException e){

            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output Stream
            try{
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }catch (IOException e){

            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while(true){
                try{
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity/Fragment
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                }catch (IOException e){
                    connectionLost();

                    // Start the service over to restart listening mode
                    BluetoothService.this.start();
                    break;
                }
            }
        }

        public void write(byte[] buffer){
            try{
                mmOutStream.write(buffer);
                mmOutStream.flush();

                // Share the sent message back to UI Activity/Fragment
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            }catch (IOException e){

            }
        }

        public void cancel(){
            try{
                mmSocket.close();
            }catch (IOException e){

            }
        }
    }

    /**
     * Indicate that the connection was lost and notify the UI Acitivty/Fragment
     */
    private void connectionLost(){
        // Send failure message first back to activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    /**
     * Write to the Connected thread in unsynchronised manner.
     * @param out
     */
    public void write(byte[] out){
        // Create temp object
        ConnectedThread r;

        // Synchronize a copy of the ConnectedThread
        synchronized (this){
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        // Perform the write
        r.write(out);
    }

    /**
     * Stop all Threads
     */
    public synchronized void stop(){

        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null){
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(STATE_NONE);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device)
    {
        // Cancel any thread currently running a connection
        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device for now
        if(mAcceptThread != null){
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle .putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }


}
