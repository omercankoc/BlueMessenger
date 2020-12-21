package com.omercankoc.bluemessenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        initBluetooth();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_search_devices:
                Toast.makeText(context,"Clicked Search Devices",Toast.LENGTH_LONG).show();
                return true;
            case R.id.menu_bluetooth_on_off:
                enableBluetooth();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            Toast.makeText(context,"No Bluetooth Found!",Toast.LENGTH_LONG).show();
        }
    }

    private void enableBluetooth(){
        if (bluetoothAdapter.isEnabled()) {
            Toast.makeText(context,"Bluetooth Already Enabled!",Toast.LENGTH_LONG).show();
        } else {
            bluetoothAdapter.enable();
        }
    }


}