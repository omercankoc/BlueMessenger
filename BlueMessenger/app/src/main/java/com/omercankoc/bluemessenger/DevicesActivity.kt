package com.omercankoc.bluemessenger

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import org.w3c.dom.Text

class DevicesActivity : AppCompatActivity() {

    private lateinit var textViewPaired : TextView
    private lateinit var textViewAvailable : TextView

    private lateinit var listViewPaired : ListView
    private lateinit var listViewAvailable : ListView

    private lateinit var adapterPairedDevices : ArrayAdapter<String>
    private lateinit var adapterAvailableDevices : ArrayAdapter<String>

    private lateinit var context : Context

    private lateinit var bluetoothAdapter: BluetoothAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        context = applicationContext

        initialize()
    }

    //2
    private fun initialize(){
        textViewPaired = findViewById(R.id.textViewPaired)
        textViewAvailable = findViewById(R.id.textViewAvailable)
        listViewPaired = findViewById(R.id.listViewPaired)
        listViewAvailable = findViewById(R.id.listViewAvailable)

        adapterPairedDevices = ArrayAdapter<String>(context,R.layout.devices_item)
        adapterAvailableDevices = ArrayAdapter<String>(context,R.layout.devices_item)

        listViewPaired.adapter = adapterPairedDevices
        listViewAvailable.adapter = adapterAvailableDevices

        // 4. ONEMLI
        listViewAvailable.setOnItemClickListener(object : AdapterView.OnItemClickListener{
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                //var info : String = view.
                //var address : String = info.substring(info.length)

                var info : String? = adapterAvailableDevices.getItem(position)
                var address : String? = info!!.substring(info!!.length - 17)

                Log.d("Address", address!!)

                val intent : Intent = Intent()
                intent.putExtra("deviceAddress",address)
                setResult(RESULT_OK,intent)
                finish()
            }
        })

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        var pairedDevices : Set<BluetoothDevice> = bluetoothAdapter.bondedDevices

        if(pairedDevices != null && pairedDevices.isNotEmpty()){
            for(device in pairedDevices){
                adapterPairedDevices.add(device.name + "\n" + device.address)
            }
        }

        // 3
        var intentFilterFirst : IntentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothDeviceListener,intentFilterFirst)

        var intentFilterSecond : IntentFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(bluetoothDeviceListener,intentFilterSecond)

        // 3. ONEMLI
        listViewPaired.setOnItemClickListener(object : AdapterView.OnItemClickListener{
            override fun onItemClick(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                bluetoothAdapter.cancelDiscovery()

                //var info : String = view.toString()
                //var address : String = info.substring(info.length)

                var info : String? = adapterAvailableDevices.getItem(position)
                var address : String? = info!!.substring(info!!.length - 17)

                Log.d("Address", address!!)

                val intent : Intent = Intent()
                intent.putExtra("deviceAddress",address)

                setResult(Activity.RESULT_OK,intent)
                finish()
            }

        })
    }

    // 3 : TEHLIKELI
    private val bluetoothDeviceListener : BroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            var action : String = intent?.action!!
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                var bluetoothDevice : BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if(bluetoothDevice?.bondState != BluetoothDevice.BOND_BONDED){
                    adapterAvailableDevices.add(bluetoothDevice?.name + "\n" + bluetoothDevice?.address)
                }
            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                if(adapterAvailableDevices.count == 0){
                    Toast.makeText(context,"No new device found!",Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context,"Click on the device to start the chat!",Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    //3
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_devices,menu)
        return super.onCreateOptionsMenu(menu)
    }

    // 3
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.menu_devices -> {
                scanDevices()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    //3
    private fun scanDevices(){
        adapterAvailableDevices.clear()
        Toast.makeText(context,"Scan Started",Toast.LENGTH_LONG).show()
        if(bluetoothAdapter.isDiscovering){
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()
    }
}