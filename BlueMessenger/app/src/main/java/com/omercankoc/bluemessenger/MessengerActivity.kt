package com.omercankoc.bluemessenger

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.CaseMap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.jar.Manifest
import kotlin.properties.Delegates

class MessengerActivity : AppCompatActivity() {

    private lateinit var context : Context
    private lateinit var bluetoothAdapter : BluetoothAdapter
    private lateinit var messenger: Messenger

    private val LOCATION_PERMISSION_REQUEST : Int = 101
    private val SELECT_DEVICE : Int = 102

    companion object {
        @kotlin.jvm.JvmField
        val MESSAGE_STATE_CHANGED: Int = 0
        @kotlin.jvm.JvmField
        val MESSAGE_READ : Int = 1
        @kotlin.jvm.JvmField
        val MESSAGE_WRITE : Int = 2
        @kotlin.jvm.JvmField
        val MESSAGE_DEVICE_NAME : Int = 3
        @kotlin.jvm.JvmField
        val MESSAGE_TOAST : Int = 4

        @kotlin.jvm.JvmField
        val DEVICE_NAME : String = "deviceName"
        @kotlin.jvm.JvmField
        var TOAST : String = "toast"
    }

    private var connectedDevice : String? = null

    private val handler = Handler{ message ->
        when(message.what){
            MESSAGE_STATE_CHANGED -> {
                when(message.arg1){
                    Messenger.STATE_NONE -> {
                        setState("NOT CONNECTED!")
                    }
                    Messenger.STATE_LISTEN -> {
                        setState("LISTEN!")
                    }
                    Messenger.STATE_CONNECTING -> {
                        setState("CONNECTING!")
                    }
                    Messenger.STATE_CONNECTED -> {
                        setState("CONNECTED $connectedDevice")
                    }
                }
                true
            }
            MESSAGE_READ -> {
                true
            }

            MESSAGE_WRITE -> {
                true
            }

            MESSAGE_DEVICE_NAME -> {
                connectedDevice = message.data.getString(DEVICE_NAME)
                Toast.makeText(context,connectedDevice,Toast.LENGTH_LONG).show()
                true
            }

            MESSAGE_TOAST -> {
                Toast.makeText(context,message.data.getString(TOAST),Toast.LENGTH_LONG).show()
                true
            }

            else -> {
                false
            }
        }
    }

    private fun setState(subTitle: CharSequence){
        supportActionBar?.subtitle = subTitle
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messenger)

        context = applicationContext

        messenger = Messenger(context,handler)

        initializeBluetooth()
    }

    // 1.
    // Menuyu arayuze bagla...
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_messenger_activity,menu)
        return super.onCreateOptionsMenu(menu)
    }

    // 1.
    // Menudeki itemlerden birini secince ilgili alana yonlendir...
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.menu_search_devices -> {
                checkPermission()
                return true
            }
            R.id.menu_bluetooth_on_off -> {
                enableBluetooth()
                return true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    //

    // 1.
    private fun initializeBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if(bluetoothAdapter == null){
            Toast.makeText(context,"Bluetooth Device Not Found!",Toast.LENGTH_LONG).show()
        }
    }

    // 2.
    private fun checkPermission(){
        if(ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_PERMISSION_REQUEST)
        } else {
            val intent : Intent = Intent(context,DevicesActivity::class.java)
            startActivityForResult(intent,SELECT_DEVICE)
        }
    }

    // 4. TEHLIKELI - GELEN VERIYI ALAN INTENT
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == SELECT_DEVICE && resultCode == RESULT_OK){
            var address : String? = data?.getStringExtra("deviceAddress")
            messenger.connect(bluetoothAdapter.getRemoteDevice(address))
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    // 3.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == LOCATION_PERMISSION_REQUEST){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                val intent : Intent = Intent(context,DevicesActivity::class.java)
                startActivityForResult(intent,SELECT_DEVICE)
            } else {
                val alertDialog : AlertDialog.Builder = AlertDialog.Builder(this@MessengerActivity)
                alertDialog.setCancelable(false)
                alertDialog.setMessage("Location Permission is Required! Please Grant!")
                alertDialog.setPositiveButton("Grant"){_,_-> checkPermission()}
                alertDialog.setNegativeButton("Deny"){_,_->this.finish()}
                val alert : AlertDialog = alertDialog.create()
                alert.show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    // 1.
    private fun enableBluetooth(){
        if(!bluetoothAdapter.isEnabled){
            bluetoothAdapter.enable()
        }

        // 3.
        if(bluetoothAdapter.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            var discoveryIntent : Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300)
            startActivity(discoveryIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(messenger != null){
            messenger.stop()
        }
    }
}