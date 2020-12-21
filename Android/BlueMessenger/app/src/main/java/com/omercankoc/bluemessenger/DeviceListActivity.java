package com.omercankoc.bluemessenger;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private ListView listViewPairedDevices;
    private ListView listViewAvailableDevices;

    private ArrayAdapter<String> adapterPairedDevices;
    private ArrayAdapter<String> adapterAvailableDevices;

    private Context context;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        context = this;

        init();
    }

    private void init(){
        listViewPairedDevices = findViewById(R.id.listViewPairedDevices);
        listViewAvailableDevices = findViewById(R.id.listViewAvailableDevices);

        adapterPairedDevices = new ArrayAdapter<String>(context,R.layout.device_list_item);
        adapterAvailableDevices = new ArrayAdapter<String>(context,R.layout.device_list_item);

        listViewPairedDevices.setAdapter(adapterPairedDevices);
        listViewAvailableDevices.setAdapter(adapterAvailableDevices);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if(pairedDevices != null && pairedDevices.size() > 0){
            for(BluetoothDevice device : pairedDevices){
                adapterPairedDevices.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }
}