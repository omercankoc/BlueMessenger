package com.omercankoc.bluemessenger

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.jar.Manifest

class MessengerActivity : AppCompatActivity() {

    private lateinit var context : Context

    private lateinit var bluetoothAdapter : BluetoothAdapter

    private val LOCATION_PERMISSION_REQUEST : Int = 101
    private val SELECT_DEVICE :Int = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messenger)

        context = applicationContext

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
}