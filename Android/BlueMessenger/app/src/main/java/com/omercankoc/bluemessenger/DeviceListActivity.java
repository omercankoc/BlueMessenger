package com.omercankoc.bluemessenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_scan_devices:
                scanDevices();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private BroadcastReceiver bluetoothDeviceListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED){
                    adapterAvailableDevices.add(device.getName() + "\n" + device.getAddress());
                }
            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                if(adapterAvailableDevices.getCount() == 0){
                    Toast.makeText(context,"No new device found!",Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context,"Click on the device to start the chat!",Toast.LENGTH_LONG).show();
                }
            }
        }
    };

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

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothDeviceListener,intentFilter);
        IntentFilter intentFilter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothDeviceListener,intentFilter1);
    }

    private void scanDevices(){
        adapterAvailableDevices.clear();
        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }
}