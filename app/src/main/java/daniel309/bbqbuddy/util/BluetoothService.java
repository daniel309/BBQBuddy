package daniel309.bbqbuddy.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import daniel309.bbqbuddy.MainActivity;


// Based on https://developer.android.com/samples/BluetoothChat/index.html
public class BluetoothService {
    private static final String TAG = BluetoothService.class.getSimpleName();

    /**
     * ===========================
     * BLUETOOTH CONNECTIVITY INFO
     * ===========================
     */
    // Name for the SDP record when creating server socket
    private static final String REMOTE_SERIAL_DEVICE_NAME = "HC-05";

    // Hint: If you are connecting to a Bluetooth serial board then try using the
    // well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB.
    private static final UUID REMOTE_SERIAL_DEVICE_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    /**
     * ===============================
     * END BLUETOOTH CONNECTIVITY INFO
     * ===============================
     */

    private final BluetoothAdapter mAdapter;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private DemoThread mDemoThread;
    private State mState = State.None;
    private final boolean mSecure;
    private int mNumRestarts = 0;
    private final MainActivity mMainActivity;


    // Constants that indicate the current connection state
    public enum State {
        None,
        Starting,
        Connecting, // now initiating an outgoing connection
        Connected, // now connected to a remote device
        Disabled,
        Error,
        Demo
    }

    // Message types sent from the BluetoothChatService Handler
    public static final int HANDLER_MESSAGE_STATE_CHANGED = 1;
    public static final int HANDLER_MESSAGE_TOAST = 2;
    public static final int HANDLER_MESSAGE_BBQ_TEMP = 3;

    //Key names
    public static final String KEY_TOAST = "toast";
    public static final String KEY_STATE = "state";
    public static final String KEY_BBQ = "bbq";



    public BluetoothService(MainActivity main, boolean secure) {
        mAdapter = main.mBluetoothAdapter;
        mState = State.None;
        mSecure = secure;
        mMainActivity = main;
    }

    public void start() {
        Log.d(TAG, "start()");
        stop(); // clear everything

        setState(State.Starting);

        if (mNumRestarts++ >= 3) {
            setState(State.Error);
            Log.e(TAG, "Too many Bluetooth connection restarts, giving up");
            sendToast("Too many Bluetooth connection restarts, giving up");
            return;
        }

        if (mAdapter != null) {
            // Look for a paired BT device with name: REMOTE_SERIAL_DEVICE_NAME
            BluetoothDevice device = null;
            Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
            for (BluetoothDevice dev : pairedDevices) {
                Log.d(TAG,dev.getName() + ", mac=" + dev.getAddress());
                if (dev.getName().equals(REMOTE_SERIAL_DEVICE_NAME)) {
                    device = dev;
                    break;
                }
            }

            if (device == null) {
                setState(State.Error);
                sendToast("Could not find a paired Bluetooth device with name " + REMOTE_SERIAL_DEVICE_NAME);
                Log.e(TAG, "Could not find a paired Bluetooth device with name " + REMOTE_SERIAL_DEVICE_NAME);
                return;
            }

            connect(device, mSecure);
        }
        else {
            setState(State.Disabled);
        }
    }

    public void startDemo() {
        Log.d(TAG, "startDemo()");
        stop();

        mDemoThread = new DemoThread();
        mDemoThread.start();
        setState(State.Demo);
    }

    private void sendToast(String text) {
        Message msg = mMainActivity.mHandler.obtainMessage(HANDLER_MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TOAST, text);
        msg.setData(bundle);
        mMainActivity.mHandler.sendMessage(msg);
    }

    private synchronized void setState(State state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Send the new state back to the UI
        Message msg =  mMainActivity.mHandler.obtainMessage(HANDLER_MESSAGE_STATE_CHANGED);
        Bundle bundle = new Bundle();
        bundle.putString(KEY_STATE, state.name());
        msg.setData(bundle);
        mMainActivity.mHandler.sendMessage(msg);
    }

    public synchronized State getState() {
        return mState;
    }

    private synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == State.Connecting) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mDemoThread != null) {
            mDemoThread.cancel();
            mDemoThread = null;
        }

        // Start the thread to connect with the given device
        setState(State.Connecting);
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mDemoThread != null) {
            mDemoThread.cancel();
            mDemoThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        sendToast("Connected to device: " + device.getName());

        setState(State.Connected);
    }

    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mDemoThread != null) {
            mDemoThread.cancel();
            mDemoThread = null;
        }

        setState(State.None);
    }

    private void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != State.Connected) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    public void sendMessage(int brightness, boolean powerSaveEnabled) {
        this.write((brightness + "|" + (powerSaveEnabled==true ? "1" : "0") + "$").getBytes());
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        Log.w(TAG, "Unable to connect to device");

        sendToast("Unable to connect device");
        SystemClock.sleep(1000); // sleep 1s

        // Start the service over
        start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        Log.w(TAG, "Device connection was lost");
        sendToast("Device connection was lost");
        start(); // Start the service over
    }

    private BBQBuddyMessage parseRawTemperatureEvent(byte[] event, int len) {
        BBQBuddyMessage ret = null;

        // parse event
        try {
            String rawEvent = new String(event, 0, len);
            Log.d(TAG, "BT data: " + rawEvent);

            String tokens[] = rawEvent.substring(0, len - 1).split("\\|"); // strip trailing '$' and tokenize by '|'
            if (tokens.length != 4) {
                Log.w(TAG, "Invalid #tokens from bt: " + rawEvent + " tokens: " + tokens.length);
            }
            else {
                ret = new BBQBuddyMessage(
                        Integer.parseInt(tokens[0]),
                        Integer.parseInt(tokens[1]),
                        Float.parseFloat(tokens[2]),
                        Float.parseFloat(tokens[3]),
                        Calendar.getInstance().getTime());
            }
        }
        catch (Exception ex) {
            Log.w(TAG, "Error parsing BT values: " + ex);
        }

        return ret;
    }

    private void updateUI(BBQBuddyMessage bbq) {
        // Send the new state back to the UI
        if (bbq != null) {
            Message msg = mMainActivity.mHandler.obtainMessage(HANDLER_MESSAGE_BBQ_TEMP);
            Bundle bundle = new Bundle();
            bundle.putSerializable(KEY_BBQ, bbq);
            msg.setData(bundle);
            mMainActivity.mHandler.sendMessage(msg);
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;
        private boolean mCancelled = false;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            REMOTE_SERIAL_DEVICE_UUID);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            REMOTE_SERIAL_DEVICE_UUID);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                Log.w(TAG, e);

                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }

                if (!mCancelled) {
                    connectionFailed();
                }
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            if (!mCancelled) {
                connected(mmSocket, mmDevice, mSocketType);
            }
        }

        public void cancel() {
            mCancelled = true;
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
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
        private boolean mCancelled = false;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytesRead = 0;

            // Keep listening to the InputStream while connected
            while (!mCancelled) {
                try {
                    int btByte = mmInStream.read();
                    if (btByte != -1) {
                        buffer[bytesRead++] = (byte)btByte;

                        if (btByte == '$') {
                            BBQBuddyMessage msg = parseRawTemperatureEvent(buffer, bytesRead);

                            if (msg != null) {
                                updateUI(msg);
                                mMainActivity.mTemperatureHistoryManager.addCurrentTemperatureEvent(
                                        msg,
                                        mMainActivity.mSettings.getProbe1Alarm(),
                                        mMainActivity.mSettings.getProbe2Alarm());
                            }
                            bytesRead = 0; // wrap buffer, we have seen and processed everything including the end char ('$')
                        }
                        if (bytesRead >= buffer.length) {
                            bytesRead = 0; // wrap buffer, ignore contents (no '$' seen yet)
                        }
                    }
                } catch (IOException e) {
                    // Start the service over to restart listening mode
                    if (!mCancelled) {
                        connectionLost();
                    }
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            mCancelled = true;
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }



    public static class BBQBuddyMessage implements Serializable {
        public BBQBuddyMessage(int bat, int disp, float probe1, float probe2, Date time) {
            batteryPercent = bat;
            displayBrightness = disp;
            probe1Temp = probe1;
            probe2Temp = probe2;
            timeMeasured = time;
        }

        public int batteryPercent;
        public int displayBrightness;
        public float probe1Temp;
        public float probe2Temp;
        public Date timeMeasured;
    }


    private class DemoThread extends Thread {
        private boolean mCancelled = false;

        public void run() {
            Log.i(TAG, "BEGIN mDemoThread");
            float counter = 0;
            while (!mCancelled) {
                counter += 0.01;
                BBQBuddyMessage msg = new BBQBuddyMessage(
                        70,40,
                        (float)Math.abs(Math.sin(counter)*90.0+Math.random()) + 20f,
                        (float)Math.abs(Math.sin(counter * 0.6)*60.0+Math.random()) + 25f,
                        Calendar.getInstance().getTime()
                );
                mMainActivity.mTemperatureHistoryManager.addCurrentTemperatureEvent(
                        msg,
                        mMainActivity.mSettings.getProbe1Alarm(),
                        mMainActivity.mSettings.getProbe2Alarm());
                updateUI(msg);
                SystemClock.sleep(1000); //1s
            }
            Log.i(TAG, "END mDemoThread");
        }

        public void cancel() {
            mCancelled = true;
        }
    }

}
