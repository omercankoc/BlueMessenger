package com.omercankoc.bluemessenger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class ChatClass {
    private Context context;
    private final Handler handler;
    private BluetoothAdapter bluetoothAdapter;

    private ConnectThread connectThread;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;

    private final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String APP_NAME = "BlueMessenger";

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private int state;

    public ChatClass(Context context, Handler handler){
        this.context = context;
        this.handler = handler;

        state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public int getState(){
        return state;
    }

    public synchronized void setState(int state){
        this.state = state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED,state,-1).sendToTarget();
    }

    private synchronized void start(){
        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        if(acceptThread == null){
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        setState(STATE_LISTEN);
    }

    public synchronized void stop(){
        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        if(acceptThread != null){
            acceptThread.cancel();
            acceptThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        setState(STATE_NONE);
    }

    public void connect(BluetoothDevice bluetoothDevice){
        if(state == STATE_CONNECTING){
            connectThread.cancel();
            connectThread = null;
        }

        connectThread = new ConnectThread(bluetoothDevice);
        connectThread.start();

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_CONNECTING);
    }

    public void write(byte[] buffer) {
        ConnectedThread connThread;
        synchronized (this) {
            if (state != STATE_CONNECTED) {
                return;
            }

            connThread = connectedThread;
        }

        connThread.write(buffer);
    }

    /*

     */
    private class AcceptThread extends Thread{
        private BluetoothServerSocket serverSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,APP_UUID);
            } catch (IOException e){
                Log.e("Accept -> Constructor",e.toString());
            }

            serverSocket = tmp;
        }

        @Override
        public void run() {
            BluetoothSocket bluetoothSocket = null;
            try{
                bluetoothSocket = serverSocket.accept();
            } catch (IOException e){
                Log.e("Accept -> Run",e.toString());
                try {
                    serverSocket.close();
                } catch (IOException e1){
                    Log.e("Accept -> Close",e.toString());
                }
            }

            if(bluetoothSocket != null){
                switch (state){
                    case STATE_LISTEN:
                    case STATE_CONNECTING:
                        connected(bluetoothSocket,bluetoothSocket.getRemoteDevice());
                        break;
                    case STATE_NONE:
                    case STATE_CONNECTED:
                        try {
                            bluetoothSocket.close();
                        } catch (IOException e){
                            Log.e("Accept -> Close Socket",e.toString());
                        }
                        break;
                }
            }
        }

        public void cancel(){
            try {
                serverSocket.close();
            } catch (IOException e){
                Log.e("Accept -> Close Server",e.toString());
            }
        }
    }

    private class ConnectThread extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectThread(BluetoothDevice bluetoothDevice){
            this.bluetoothDevice = bluetoothDevice;
            BluetoothSocket tmp = null;
            try {
                tmp = bluetoothDevice.createRfcommSocketToServiceRecord(APP_UUID);
            } catch(IOException e){
                Log.e("Connect -> Constructor",e.toString());
            }
            bluetoothSocket = tmp;
        }

        public void run(){
            try {
                bluetoothSocket.connect();
            } catch (IOException e){
                Log.e("Connect -> Run",e.toString());
                try {
                    bluetoothSocket.close();
                } catch (IOException e1){
                    Log.e("Connect -> Close Socket",e1.toString());
                }
                connectionFailed();
                return;
            }

            synchronized (ChatClass.this){
                connectThread = null;
            }

            connected(bluetoothSocket,bluetoothDevice);
        }

        public void cancel(){
            try {
                bluetoothSocket.close();
            } catch (IOException e){
                Log.e("Connect -> Cancel",e.toString());
            }
        }
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            try {
                bytes = inputStream.read(buffer);

                handler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
            } catch (IOException e) {
                connectionLost();
            }
        }

        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
                handler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e("Do not write message!",e.toString());
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Do not cancel thread!",e.toString());
            }
        }
    }

    private void connectionLost() {
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Connection Lost");
        message.setData(bundle);
        handler.sendMessage(message);

        ChatClass.this.start();
    }

    /*

     */
    private synchronized void connectionFailed(){
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST,"Can not connected device!");
        message.setData(bundle);
        handler.sendMessage(message);

        ChatClass.this.start();
    }

    /*

     */
    private synchronized void connected(BluetoothSocket bluetoothSocket, BluetoothDevice bluetoothDevice){
        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();

        Message message = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME,bluetoothDevice.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }
}
