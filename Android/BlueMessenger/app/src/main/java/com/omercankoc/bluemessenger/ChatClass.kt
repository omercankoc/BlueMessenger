package com.omercankoc.bluemessenger

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.omercankoc.bluemessenger.ChatClass
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class ChatClass(private val context: Context, private val handler: Handler) {
    private val bluetoothAdapter: BluetoothAdapter
    private var connectThread: ConnectThread? = null
    private var acceptThread: AcceptThread? = null
    private var connectedThread: ConnectedThread? = null
    private val APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val APP_NAME = "BlueMessenger"
    private var state: Int
    fun getState(): Int {
        return state
    }

    @Synchronized
    fun setState(state: Int) {
        this.state = state
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED, state, -1).sendToTarget()
    }

    @Synchronized
    private fun start() {
        if (connectThread != null) {
            connectThread!!.cancel()
            connectThread = null
        }
        if (acceptThread == null) {
            acceptThread = AcceptThread()
            acceptThread!!.start()
        }
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
        setState(STATE_LISTEN)
    }

    @Synchronized
    fun stop() {
        if (connectThread != null) {
            connectThread!!.cancel()
            connectThread = null
        }
        if (acceptThread != null) {
            acceptThread!!.cancel()
            acceptThread = null
        }
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
        setState(STATE_NONE)
    }

    fun connect(bluetoothDevice: BluetoothDevice) {
        if (state == STATE_CONNECTING) {
            connectThread!!.cancel()
            connectThread = null
        }
        connectThread = ConnectThread(bluetoothDevice)
        connectThread!!.start()
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
        setState(STATE_CONNECTING)
    }

    fun write(buffer: ByteArray?) {
        var connThread: ConnectedThread?
        synchronized(this) {
            if (state != STATE_CONNECTED) {
                return
            }
            connThread = connectedThread
        }
        connThread!!.write(buffer)
    }

    /*

     */
    private inner class AcceptThread : Thread() {
        private val serverSocket: BluetoothServerSocket?
        override fun run() {
            var bluetoothSocket: BluetoothSocket? = null
            try {
                bluetoothSocket = serverSocket!!.accept()
            } catch (e: IOException) {
                Log.e("Accept -> Run", e.toString())
                try {
                    serverSocket!!.close()
                } catch (e1: IOException) {
                    Log.e("Accept -> Close", e.toString())
                }
            }
            if (bluetoothSocket != null) {
                when (state) {
                    STATE_LISTEN, STATE_CONNECTING -> connected(bluetoothSocket, bluetoothSocket.remoteDevice)
                    STATE_NONE, STATE_CONNECTED -> try {
                        bluetoothSocket.close()
                    } catch (e: IOException) {
                        Log.e("Accept -> Close Socket", e.toString())
                    }
                }
            }
        }

        fun cancel() {
            try {
                serverSocket!!.close()
            } catch (e: IOException) {
                Log.e("Accept -> Close Server", e.toString())
            }
        }

        init {
            var tmp: BluetoothServerSocket? = null
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
            } catch (e: IOException) {
                Log.e("Accept -> Constructor", e.toString())
            }
            serverSocket = tmp
        }
    }

    private inner class ConnectThread(private val bluetoothDevice: BluetoothDevice) : Thread() {
        private val bluetoothSocket: BluetoothSocket?
        override fun run() {
            try {
                bluetoothSocket!!.connect()
            } catch (e: IOException) {
                Log.e("Connect -> Run", e.toString())
                try {
                    bluetoothSocket!!.close()
                } catch (e1: IOException) {
                    Log.e("Connect -> Close Socket", e1.toString())
                }
                connectionFailed()
                return
            }
            synchronized(this@ChatClass) { connectThread = null }
            connected(bluetoothSocket, bluetoothDevice)
        }

        fun cancel() {
            try {
                bluetoothSocket!!.close()
            } catch (e: IOException) {
                Log.e("Connect -> Cancel", e.toString())
            }
        }

        init {
            var tmp: BluetoothSocket? = null
            try {
                tmp = bluetoothDevice.createRfcommSocketToServiceRecord(APP_UUID)
            } catch (e: IOException) {
                Log.e("Connect -> Constructor", e.toString())
            }
            bluetoothSocket = tmp
        }
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket?) : Thread() {
        private val inputStream: InputStream?
        private val outputStream: OutputStream?
        override fun run() {
            val buffer = ByteArray(1024)
            val bytes: Int
            try {
                bytes = inputStream!!.read(buffer)
                handler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget()
            } catch (e: IOException) {
                connectionLost()
            }
        }

        fun write(buffer: ByteArray?) {
            try {
                outputStream!!.write(buffer)
                handler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget()
            } catch (e: IOException) {
                Log.e("Do not write message!", e.toString())
            }
        }

        fun cancel() {
            try {
                socket!!.close()
            } catch (e: IOException) {
                Log.e("Do not cancel thread!", e.toString())
            }
        }

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = socket!!.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {
            }
            inputStream = tmpIn
            outputStream = tmpOut
        }
    }

    private fun connectionLost() {
        val message = handler.obtainMessage(MainActivity.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(MainActivity.TOAST, "Connection Lost")
        message.data = bundle
        handler.sendMessage(message)
        start()
    }

    /*

     */
    @Synchronized
    private fun connectionFailed() {
        val message = handler.obtainMessage(MainActivity.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(MainActivity.TOAST, "Can not connected device!")
        message.data = bundle
        handler.sendMessage(message)
        start()
    }

    /*

     */
    @Synchronized
    private fun connected(bluetoothSocket: BluetoothSocket?, bluetoothDevice: BluetoothDevice) {
        if (connectThread != null) {
            connectThread!!.cancel()
            connectThread = null
        }
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
        connectedThread = ConnectedThread(bluetoothSocket)
        connectedThread!!.start()
        val message = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(MainActivity.DEVICE_NAME, bluetoothDevice.name)
        message.data = bundle
        handler.sendMessage(message)
        setState(STATE_CONNECTED)
    }

    companion object {
        const val STATE_NONE = 0
        const val STATE_LISTEN = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3
    }

    init {
        state = STATE_NONE
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }
}