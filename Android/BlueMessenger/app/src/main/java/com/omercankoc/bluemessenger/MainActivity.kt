package com.omercankoc.bluemessenger

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.bluetooth.BluetoothAdapter
import com.omercankoc.bluemessenger.ChatClass
import com.omercankoc.bluemessenger.MainActivity
import android.os.Bundle
import com.omercankoc.bluemessenger.R
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.content.Intent
import com.omercankoc.bluemessenger.DeviceListActivity
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {
    private var context: Context? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var chatClass: ChatClass? = null
    private var editTextMessage: EditText? = null
    private var buttonSend: Button? = null
    private var listViewMessages: ListView? = null
    private var adapterMessages: ArrayAdapter<String>? = null
    private val LOCATION_PERMISSION_REQUEST = 101
    private val SELECT_DEVICE = 102
    private var connectedDevice: String? = null
    private val handler = Handler { message ->
        when (message.what) {
            MESSAGE_STATE_CHANGED -> {
                when (message.arg1) {
                    ChatClass.STATE_NONE -> setState("Not Connected!")
                    ChatClass.STATE_LISTEN -> setState("Not Connected...")
                    ChatClass.STATE_CONNECTING -> setState("Connecting...")
                    ChatClass.STATE_CONNECTED -> setState("Connected $connectedDevice")
                }
                val buffer1 = message.obj as ByteArray
                val outputBuffer = String(buffer1)
                adapterMessages!!.add("Me: $outputBuffer")
            }
            MESSAGE_WRITE -> {
                val buffer1 = message.obj as ByteArray
                val outputBuffer = String(buffer1)
                adapterMessages!!.add("Me: $outputBuffer")
            }
            MESSAGE_READ -> {
                val buffer = message.obj as ByteArray
                val inputBuffer = String(buffer, 0, message.arg1)
                adapterMessages!!.add("$connectedDevice: $inputBuffer")
            }
            MESSAGE_DEVICE_NAME -> {
                connectedDevice = message.data.getString(DEVICE_NAME)
                Toast.makeText(context, connectedDevice, Toast.LENGTH_LONG).show()
            }
            MESSAGE_TOAST -> Toast.makeText(context, message.data.getString(TOAST), Toast.LENGTH_LONG).show()
        }
        false
    }

    private fun setState(subTitle: CharSequence) {
        supportActionBar!!.subtitle = subTitle
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this
        initialize()
        initBluetooth()
        chatClass = ChatClass(context, handler)
    }

    private fun initialize() {
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)
        listViewMessages = findViewById(R.id.listViewMessages)
        adapterMessages = ArrayAdapter(context!!, R.layout.message_layout)
        listViewMessages.setAdapter(adapterMessages)
        buttonSend.setOnClickListener(View.OnClickListener {
            val message = editTextMessage.getText().toString()
            if (message.isEmpty()) {
                editTextMessage.setText("")
                chatClass!!.write(message.toByteArray())
            }
        })
    }

    /*

     */
    private fun initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "No Bluetooth Found!", Toast.LENGTH_LONG).show()
        }
    }

    /*

     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /*

     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search_devices -> {
                checkPermission()
                true
            }
            R.id.menu_bluetooth_on_off -> {
                enableBluetooth()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /*

     */
    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        } else {
            val intent = Intent(context, DeviceListActivity::class.java)
            startActivityForResult(intent, SELECT_DEVICE)
        }
    }

    /*
    DeviceListActivity'den Intent ile gönderilen Device Address bilgisini al.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SELECT_DEVICE && resultCode == RESULT_OK) {
            val address = data!!.getStringExtra("deviceAddress")
            chatClass!!.connect(bluetoothAdapter!!.getRemoteDevice(address))
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(context, DeviceListActivity::class.java)
                startActivityForResult(intent, SELECT_DEVICE)
            } else {
                AlertDialog.Builder(context!!)
                        .setCancelable(false)
                        .setMessage("Location Permission is Required! \n Please Grant!")
                        .setPositiveButton("Grant") { dialogInterface, i -> checkPermission() }
                        .setNegativeButton("Deny") { dialogInterface, i -> finish() }
                        .show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    /*

     */
    private fun enableBluetooth() {
        if (bluetoothAdapter!!.isEnabled) {
            bluetoothAdapter!!.enable()
        }
        if (bluetoothAdapter!!.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val discoveryIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(discoveryIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (chatClass != null) {
            chatClass!!.stop()
        }
    }

    companion object {
        const val MESSAGE_STATE_CHANGED = 0
        const val MESSAGE_READ = 1
        const val MESSAGE_WRITE = 2
        const val MESSAGE_DEVICE_NAME = 3
        const val MESSAGE_TOAST = 4
        const val DEVICE_NAME = "device_name"
        const val TOAST = "toast"
    }
}