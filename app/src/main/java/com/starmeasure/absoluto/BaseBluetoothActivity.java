package com.starmeasure.absoluto;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

public class BaseBluetoothActivity extends AppCompatActivity {

    protected BluetoothLeService mBluetoothLeService;
    protected BluetoothGattService mStarMeasureService;
    private boolean isNewModule = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void bindGattService(ServiceConnection serviceConnection) {
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    protected void unbindGattService(ServiceConnection serviceConnection) {
        unbindService(serviceConnection);
    }

    protected long sendData(byte[] data) {
        BluetoothGattCharacteristic writeCharacteristic;
        if (isNewModule) {
            writeCharacteristic = mStarMeasureService.getCharacteristic(UUID.fromString(Consts.BLE_NEW_WRITE_CHARACTERISTIC));
        } else {
            writeCharacteristic = mStarMeasureService.getCharacteristic(UUID.fromString(Consts.BLE_OLD_WRITE_CHARACTERISTIC));
        }
        if (writeCharacteristic == null)
            return 0;
        writeCharacteristic.setValue(data);
        mBluetoothLeService.writeCharacteristic(writeCharacteristic);
        return System.currentTimeMillis();
    }

    protected void setupGattService() {
        if (mBluetoothLeService != null) {
            for (BluetoothGattService gattService : mBluetoothLeService.getSupportedGattServices()) {
                if (gattService.getUuid().toString().equals(Consts.BLE_OLD_SERVICE)) {
                    mStarMeasureService = gattService;
                    break;
                } else if (gattService.getUuid().toString().equals(Consts.BLE_NEW_SERVICE)) {
                    mStarMeasureService = gattService;
                    isNewModule = true;
                    break;
                }
            }
        }
    }

}
