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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class Messenger {
    private Context context;
    private Handler handler;
    private BluetoothAdapter bluetoothAdapter;

    private ConnectThread connectThread;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;

    private final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String APP_NAME = "Blue Messenger";

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private int state;

    public Messenger(Context context, Handler handler){
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
        handler.obtainMessage(MessengerActivity.MESSAGE_STATE_CHANGED,state,-1).sendToTarget();
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

        if(connectedThread != null){
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

        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_NONE);
    }

    public void connect(BluetoothDevice bluetoothDevice){
        if(state ==  STATE_CONNECTED){
            connectThread.cancel();
            connectThread = null;
        }

        connectThread = new ConnectThread(bluetoothDevice);
        connectThread.start();

        setState(STATE_CONNECTING);
    }

    public void write(byte[] buffer){
        ConnectedThread conThread;
        synchronized (this){
            if(state != STATE_CONNECTED){
                return;
            }
            conThread = connectedThread;
        }
        conThread.write(buffer);
    }

    //
    private class AcceptThread extends Thread{
        private BluetoothServerSocket bluetoothServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;
            try{
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,APP_UUID);
            } catch (IOException e){
                Log.e("Accept -> Constructor",e.toString());
            }
            bluetoothServerSocket = tmp;
        }

        public void run(){
            BluetoothSocket bluetoothSocket = null;
            try {
                bluetoothSocket = bluetoothServerSocket.accept();
            } catch (IOException e){
                Log.e("Accept -> Run",e.toString());
                try{
                   bluetoothServerSocket.close();
                } catch (IOException e1){
                    Log.e("Accept -> Close",e1.toString());
                }
            }

            if(bluetoothSocket != null){
                switch (state){
                    case STATE_LISTEN:
                    case STATE_CONNECTING:
                        connect(bluetoothSocket.getRemoteDevice());
                        break;
                    case STATE_NONE:
                    case STATE_CONNECTED:
                        try{
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
                bluetoothServerSocket.close();
            } catch (IOException e){
                Log.e("Accept -> Close Server",e.toString());
            }
        }
    }

    //
    private class ConnectThread extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectThread(BluetoothDevice bluetoothDevice){
            this.bluetoothDevice = bluetoothDevice;

            BluetoothSocket tmp = null;
            try {
                tmp = bluetoothDevice.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e){
                Log.e("Connect -> Constructor",e.toString());
            }
            bluetoothSocket = tmp;
        }

        public void run(){
            try{
                bluetoothSocket.connect();
            } catch (IOException e){
                Log.e("Connect -> Run",e.toString());
                try{
                    bluetoothSocket.close();
                } catch (IOException e1){
                    Log.e("Connect -> Close Socket",e1.toString());
                }
                connectionFailed();
                return;
            }

            synchronized (Messenger.this){
                connectThread = null;
            }

            connected(bluetoothSocket,bluetoothDevice);
        }

        public void cancel(){
            try{
                bluetoothSocket.close();
            } catch (IOException e){
                Log.e("Connect -> Close",e.toString());
            }
        }
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket bluetoothSocket){
            this.bluetoothSocket = bluetoothSocket;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try{
                tmpIn = bluetoothSocket.getInputStream();
                tmpOut = bluetoothSocket.getOutputStream();
            } catch (IOException e){

            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            try {
                bytes = inputStream.read(buffer);
                handler.obtainMessage(MessengerActivity.MESSAGE_READ,bytes,-1,buffer).sendToTarget();
            } catch (IOException e){
                connectionLost();
            }
        }

        public void write(byte[] buffer){
            try{
                outputStream.write(buffer);
                handler.obtainMessage(MessengerActivity.MESSAGE_WRITE,-1,-1,buffer).sendToTarget();
            } catch (IOException e){

            }
        }

        public void cancel(){
            try {
                bluetoothSocket.close();
            } catch (IOException e){

            }
        }
    }

    private void connectionLost(){
        Message message = handler.obtainMessage(MessengerActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MessengerActivity.TOAST,"Connection Lost");
        message.setData(bundle);
        handler.sendMessage(message);

        Messenger.this.start();
    }

    private synchronized void connectionFailed(){
        Message message = handler.obtainMessage(MessengerActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MessengerActivity.TOAST,"Can not connect to the device!");
        message.setData(bundle);
        handler.sendMessage(message);

        Messenger.this.start();
    }

    private synchronized void connected(BluetoothSocket bluetoothSocket, BluetoothDevice bluetoothDevice){
        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();

        Message message = handler.obtainMessage(MessengerActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MessengerActivity.DEVICE_NAME, bluetoothDevice.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }
}
