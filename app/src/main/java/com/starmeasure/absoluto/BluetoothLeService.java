/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.starmeasure.absoluto;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    public BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private String mBluetoothDeviceName;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String EXTRA_BYTES =
            "com.example.bluetooth.le.EXTRA_BYTES";

    private static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            Log.i(TAG, "+++++ NOVO STATUS GAT: " + status + " - " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "+++++ CONECTADO GATT SERVER.");
                // Attempts to discover services after successful connection.
                setConnectionPriority();
                Log.i(TAG, "DISCOVERY SERVICES: " + mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "+++++ DESCONECTADO GATT SERVER.");
                mBluetoothGatt.disconnect();//cancelConnection()
                mBluetoothGatt.close();
                //mBluetoothGatt = null;

                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("PHYUPDATE", "Phy updated");
                if (txPhy == BluetoothDevice.PHY_LE_2M_MASK) {
                    Log.d("PHYUPDATE", "transmitter  = PHY_LE_2M_MASK");
                } else if (txPhy == BluetoothDevice.PHY_LE_1M_MASK) {
                    Log.d("PHYUPDATE", "transmitter  = PHY_LE_1M_MASK");
                } else if (txPhy == BluetoothDevice.PHY_LE_CODED_MASK) {
                    Log.d("PHYUPDATE", "transmitter  = PHY_LE_CODED_MASK");
                }

                if (rxPhy == BluetoothDevice.PHY_LE_2M_MASK) {
                    Log.d("PHYUPDATE", "receiver   = PHY_LE_2M_MASK");
                } else if (rxPhy == BluetoothDevice.PHY_LE_1M_MASK) {
                    Log.d("PHYUPDATE", "receiver   = PHY_LE_1M_MASK");
                } else if (rxPhy == BluetoothDevice.PHY_LE_CODED_MASK) {
                    Log.d("PHYUPDATE", "receiver   = PHY_LE_CODED_MASK");
                }
            } else {
                Log.d("PHYUPDATE", "Phy fail to update");
            }
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
            if (txPhy == BluetoothDevice.PHY_LE_2M_MASK) {
                Log.d("PHYREAD", "transmitter  = PHY_LE_2M_MASK");
            } else if (txPhy == BluetoothDevice.PHY_LE_1M_MASK) {
                Log.d("PHYREAD", "transmitter  = PHY_LE_1M_MASK");
            } else if (txPhy == BluetoothDevice.PHY_LE_CODED_MASK) {
                Log.d("PHYREAD", "transmitter  = PHY_LE_CODED_MASK");
            }

            if (rxPhy == BluetoothDevice.PHY_LE_2M_MASK) {
                Log.d("PHYREAD", "receiver   = PHY_LE_2M_MASK");
            } else if (rxPhy == BluetoothDevice.PHY_LE_1M_MASK) {
                Log.d("PHYREAD", "receiver   = PHY_LE_1M_MASK");
            } else if (rxPhy == BluetoothDevice.PHY_LE_CODED_MASK) {
                Log.d("PHYREAD", "receiver   = PHY_LE_CODED_MASK");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }
            Log.w(TAG, "onServicesDiscovered received: " + status);

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            byte[] messageBytes = characteristic.getValue();

            Log.w(TAG, "onCharacteristicChanged: " + Util.ByteArrayToHexString(messageBytes));

            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            BluetoothGattCharacteristic characteristic = null;
            for (BluetoothGattService service : gatt.getServices()) {
                if (service.getUuid().equals(UUID.fromString(Consts.BLE_OLD_SERVICE))) {
                    characteristic = service.getCharacteristic(UUID.fromString(Consts.BLE_OLD_NOTIFY_CHARACTERISTIC));
                    break;
                } else if (service.getUuid().equals(UUID.fromString(Consts.BLE_NEW_SERVICE))) {
                    characteristic = service.getCharacteristic(UUID.fromString(Consts.BLE_NEW_NOTIFY_CHARACTERISTIC));
                    break;
                }
            }

            if (characteristic != null) {
                characteristic.setValue(new byte[]{1, 1});
                gatt.writeCharacteristic(characteristic);
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            intent.putExtra(EXTRA_BYTES, data);
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (address.equals(mBluetoothDeviceAddress) && (mConnectionState != STATE_DISCONNECTED) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            setConnectionPriority();
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;

                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, true, mGattCallback, 2);
        setConnectionPriority();
        Log.d(TAG, "RECONECTADO GATT.");
        mBluetoothDeviceAddress = address;
        mBluetoothDeviceName = device.getName();

        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothAdapter.cancelDiscovery();
        mBluetoothGatt.disconnect();

        mBluetoothGatt.close();
        mBluetoothGatt = null;

    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
        {
            return;
        }
        mBluetoothGatt.disconnect();

        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        if (characteristic.getUuid().equals(UUID.fromString(Consts.BLE_NEW_WRITE_CHARACTERISTIC))) {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

            if (Build.VERSION.SDK_INT >= 26) {
                if (mBluetoothAdapter.isLe2MPhySupported()) {
                    //qmBluetoothGatt.setPreferredPhy(BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_OPTION_NO_PREFERRED);
                    mBluetoothGatt.readPhy();
                }
            }
        } else {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        }
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            if (enabled) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }
            mBluetoothGatt.writeDescriptor(descriptor);
        }

        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    private void setConnectionPriority() {
        if (mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH))
        {
            Log.d(TAG, "PRIORITY SET AS HIGH");
        }
        else
        {
                Log.e(TAG, "CAN'T SET PRIORITY AS HIGH");

                if (mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED))
                {
                    Log.d(TAG, "PRIORITY SET AS BALANCED");
                }
                else
                {
                    Log.e(TAG, "CAN'T SET PRIORITY AS BALANCE");
                    if (mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER))
                    {
                        Log.d(TAG, "PRIORITY SET AS LOW");
                    }
                    else
                    {
                        Log.e(TAG, "CAN'T SET PRIORITY AS LOW");
                    }
                }
        }
    }

    public String getDeviceName() {
        return mBluetoothDeviceName;
    }

    public String getDeviceAddress() {
        return mBluetoothDeviceAddress;
    }
}

