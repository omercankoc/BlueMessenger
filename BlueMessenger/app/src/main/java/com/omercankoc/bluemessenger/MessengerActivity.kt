package com.omercankoc.bluemessenger

import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

class MessengerActivity : AppCompatActivity() {

    private var context : Context? = null

    private lateinit var bluetoothAdapter : BluetoothAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messenger)

        context = applicationContext

        initializeBluetooth()
    }

    // Menuyu arayuze bagla...
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_messenger_activity,menu)
        return super.onCreateOptionsMenu(menu)
    }

    // Menudeki itemlerden birini secince ilgili alana yonlendir...
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.menu_search_devices -> {
                Toast.makeText(context,"Clicked Search Devices",Toast.LENGTH_LONG).show()
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

    private fun initializeBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if(bluetoothAdapter == null){
            Toast.makeText(context,"Bluetooth Device Not Found!",Toast.LENGTH_LONG).show()
        }
    }

    private fun enableBluetooth(){
        if(bluetoothAdapter.isEnabled){
            Toast.makeText(context,"Bluetooth Already Enabled!",Toast.LENGTH_LONG).show()
        } else {
            bluetoothAdapter.enable()
        }
    }
}