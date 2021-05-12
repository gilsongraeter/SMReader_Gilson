package com.starmeasure.absoluto;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.MacAddress;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.kishan.askpermission.BuildConfig;
import com.starmeasure.absoluto.filemanager.FileManagerCargasActivity;
import com.starmeasure.absoluto.filemanager.FileManagerLeiturasActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class ScanActivity extends AppCompatActivity {

    private static final String TAG = ScanActivity.class.getSimpleName();

    private static final int MY_REQUEST_ACCESS_COARSE_LOCATION = 999;

    private DeviceListAdapter mDeviceListAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private RecyclerView mRecyclerView;
    private ImageView mRefreshImage;
    private ImageView mFolderCOnfig;

    private Timer timer;
    private TimerTask timerTask;

    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private Handler mHandler;
    private byte mStatus;
    private byte mConexao = 0;
    private byte mConexaoDisconect = 0;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 60000;
    private static final long REMOVE_LISTA = 40000;
    private static final long BLOQUEIA_LISTA = 20000;
    private static final long TIMEOUT = 10000;
    private static final long RETENTATIVAS = 5;
    private static final long RETENTATIVAS_CONNECT = 5;
    private int selectedDevice = -1;
    private AlertDialog progressDialog;
    private long tempoTst = 0;
    MeuBluetoothDevice globalDevice;
    private String strSMFiscal = "";
    private String strMAC_ADDRESS = "";

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        mHandler = new Handler();

        strSMFiscal = getIntent().getStringExtra(Consts.EXTRA_SMFISCAL);

        if ((strSMFiscal != null) && (!strSMFiscal.isEmpty())) {
            Log.d("###TEST", strSMFiscal);
            Toast.makeText(this, "Iniciado pelo SMFISCAL", Toast.LENGTH_LONG).show();
            strMAC_ADDRESS = getIntent().getStringExtra(Consts.EXTRA_MAC_ADDRESS);
            if ((strMAC_ADDRESS != null) && (!strMAC_ADDRESS.isEmpty())) {
                Toast.makeText(this, "Iniciado pelo SMFISCAL (MAC)", Toast.LENGTH_LONG).show();
            }
        } else {
            strSMFiscal = "";
        }

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }

        TextView tvVersao = findViewById(R.id.tv_versao);
        tvVersao.setText("Versão: " + appVersion());

        mRefreshImage = findViewById(R.id.scan_devices);
        mRefreshImage.setOnClickListener(v -> {
            if (mStatus == 1)
                scanDevices(false);
            else {
                scanDevices(true);
            }
        });

        mFolderCOnfig = findViewById(R.id.config);
        mFolderCOnfig.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(ScanActivity.this, v);
            menu.inflate(R.menu.menu_file_manager);
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_file_manager_leituras) {
                    if (Arquivo.checkWriteExternalStoragePermission(v.getContext())) {
                        if (Arquivo.createDirLeituras()) {
                            startActivity(new Intent(v.getContext(), FileManagerLeiturasActivity.class));
                        }
                    }
                    return true;
                } else if (item.getItemId() == R.id.menu_file_manager_cargas) {
                    if (Arquivo.checkWriteExternalStoragePermission(v.getContext())) {
                        if (Arquivo.createDirCargas()) {
                            startActivity(new Intent(v.getContext(), FileManagerCargasActivity.class));
                        }
                    }
                    return true;
                } else {
                    return false;
                }
            });
            menu.show();
        });

        mRecyclerView = findViewById(R.id.device_list);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(llm);

        timer = new Timer();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private String appVersion() {
        String version = "";
        try {
            PackageInfo p = getApplicationContext().getPackageManager().getPackageInfo(getPackageName(), 0);
            version = p.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }

    @Override
    protected void onResume() {
        super.onResume();

        setTimeBomb(false);

        Log.d(TAG, "####### ATENÇÃO ONRESUME. STATUS " + String.valueOf(mStatus));

        if (mStatus == 0 || mStatus == 2) {

            registerReceiver(mBroadcastReceiver, makeGattUpdateIntentFilter());

            checkLocationPermission();
            checkBluetoothEnabled();
            checkLocationEnable();
            mDeviceListAdapter = new DeviceListAdapter();
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mRecyclerView.setAdapter(mDeviceListAdapter);

            timerTask = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDeviceListAdapter.refreshList();
                        }
                    });
                }
            };
            timer.scheduleAtFixedRate(timerTask, 0, 1000);

            if (mBluetoothLeScanner != null) {
                if (selectedDevice < 0)
                    scanDevices(true);
            }


        }

        if (Arquivo.checkWriteExternalStoragePermission(this)) {
            Arquivo.createDirLeituras();
        }
    }

    private void checkBluetoothEnabled() {
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    /**
     * @param isEnable receive the activate instruction.
     *                 Set <code>true</code> to activate the time bomb,
     *                 and <code>false</code> to deactivate.
     *                 Remember to verify if the day, month and year is as you want.
     */
    private void setTimeBomb(boolean isEnable) {
        if (isEnable) {
            Log.d("badBoy->", "The time bomb is activated");
            int day = 31;
            int month = 8;
            int year = 2022;
            Log.d("badBoy->", "The current expiration date is {\nDay:" + day + "\nMonth: " + month + "\nYear: " + year + "\n}");

            Calendar expirationDate = Calendar.getInstance();
            expirationDate.set(year, month, day);
            Calendar t = Calendar.getInstance();
            if (t.compareTo(expirationDate) >= 1) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Aviso");
                builder.setMessage("O tempo de uso deste aplicativo expirou.");
                builder.setCancelable(false);
                builder.setPositiveButton("Entendi", (dialog, which) -> finish());
                builder.create();
                builder.show();
            }
        }
    }

    private void checkLocationEnable() {
        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean network_enabled = false;

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        if (!network_enabled) {
            // notify user
            Intent enableIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 28) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_REQUEST_ACCESS_COARSE_LOCATION);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_REQUEST_ACCESS_COARSE_LOCATION);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        timerTask.cancel();
        timer.purge();
        if (mBluetoothLeScanner != null)
            scanDevices(false);
        if (mStatus == 3) {
            progressDialog.dismiss();
            mHandler.removeCallbacksAndMessages(null);
        }
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mNotifyCharacteristic != null)
            mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
        unbindService(mServiceConnection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            if (mStatus == 0 || mStatus == 2)
                scanDevices(true);
        }
        /*
        else if (resultCode == RESULT_OK && requestCode == 9999) {
            Uri uriTree = data.getData();

            DocumentFile documentFile = DocumentFile.fromTreeUri(this, uriTree);
            //for (DocumentFile file : documentFile.listFiles()) {

            if (documentFile.isDirectory()) {
                SharedPreferences sharedPreferences = getSharedPreferences("sm_reader", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (documentFile.exists()) {
                    String dir = FileUtil.getFullPathFromTreeUri(documentFile.getUri(), this); //Arquivo.getRealPathFromURIW(this, documentFile.getUri()); //FileUtil.getFullPathFromTreeUri(documentFile.getUri(), this);
                    dir = dir.replace("0000-0000", "emulated/0");

                    editor.putString("diretorio", dir);
                    editor.apply();
                    Arquivo.salvarArquivo(this, "tst.txt", "wagner");
                }
            }
            //}
        }*/
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanDevices(final boolean enable) {
        List<BluetoothDevice> devices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        for (BluetoothDevice device : devices) {
            if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                Log.d(TAG, "****************************** DEVICE AINDA CONECTADO: " + device.getName());
            }
        }

        if (enable) {
            mStatus = 1;
            mHandler.removeCallbacksAndMessages(null);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mStatus != 3)
                        mStatus = 0;
                    mRefreshImage.clearAnimation();
                    mBluetoothLeScanner.stopScan(scannerCallback);
                    Log.d(TAG, String.format("TEMPO SCAN %d", System.currentTimeMillis() - tempoTst));
                }
            }, SCAN_PERIOD);

            Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
            rotation.setRepeatCount(Animation.INFINITE);
            mRefreshImage.startAnimation(rotation);
            ScanSettings.Builder settingBuilder = new ScanSettings.Builder();
            settingBuilder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);

            mBluetoothLeScanner.startScan(null, settingBuilder.build(), scannerCallback);
            tempoTst = System.currentTimeMillis();
        } else {
            mStatus = 2;
            mRefreshImage.clearAnimation();
            mBluetoothLeScanner.stopScan(scannerCallback);
            Log.d(TAG, String.format("PARADO SCAN %d", System.currentTimeMillis() - tempoTst));
        }
    }

    public final class MeuBluetoothDevice {
        public BluetoothDevice localDevice;
        public long timestamp;
        public int rssi;

        MeuBluetoothDevice(BluetoothDevice mDev, int rssi) {
            localDevice = mDev;
            updateTimestamp(rssi);
        }

        public void updateTimestamp(int rssi) {
            timestamp = System.currentTimeMillis();
            this.rssi = rssi;
        }

        public boolean verificaDeviceDeveSerRemovido() {
            long resposta = System.currentTimeMillis() - timestamp;
            if ((resposta > REMOVE_LISTA)) {
                return true;
            } else {
                return false;
            }
        }

        public boolean verificaDeviceDeveSerBloquado() {
            long resposta = System.currentTimeMillis() - timestamp;
            if ((resposta > 0) && (resposta > BLOQUEIA_LISTA)) {
                return true;
            } else {
                return false;
            }
        }

    }

    public View mView;

    private class DeviceListAdapter
            extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {

        private ArrayList<MeuBluetoothDevice> mDevices;


        public class DeviceViewHolder extends RecyclerView.ViewHolder {
            TextView deviceName;
            TextView deviceRssi;

            private Runnable mConectaMedidorRunnable;

            public void efetuaConexao() {
                if (mBluetoothLeService != null) {
                    final boolean result = mBluetoothLeService.connect(globalDevice.localDevice.getAddress());

                    Log.d(TAG, "Connect request result=" + result);

                    mHandler.postDelayed(mConectaMedidorRunnable, TIMEOUT);
                } else {
                    progressDialog.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(ScanActivity.this,
                            android.R.style.Theme_Material_Dialog_Alert);
                    builder.setTitle("Medidor Ausente")
                            .setMessage("Medidor não esta mais na lista.")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .show();
                }
            }

            public DeviceViewHolder(@NonNull View view) {
                super(view);
                deviceName = view.findViewById(R.id.device_name);
                deviceRssi = view.findViewById(R.id.device_rssi);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mView = view;
                        scanDevices(false);
                        mStatus = 3;
                        selectedDevice = getAdapterPosition();
                        if ((selectedDevice < mDevices.size()) && (selectedDevice >= 0)) {
                            mStatus = 3;
                            mConexao = 0;
                            globalDevice = mDevices.get(selectedDevice);
                            if (globalDevice.rssi < -92) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(ScanActivity.this,
                                        android.R.style.Theme_Material_Dialog_Alert);
                                builder.setTitle("Sem Conexão")
                                        .setMessage("Sinal insuficiente para conexão!")
                                        .setIcon(android.R.drawable.ic_dialog_info)
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        })
                                        .show();
                                return;
                            }

                            progressDialog = AlertBuilder.createProgressDialog(ScanActivity.this, "Conectando ao dispositivo: \n" + globalDevice.localDevice.getName());
                            progressDialog.show();
                            mConectaMedidorRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    mConexao++;
                                    mBluetoothLeService.disconnect();
                                    if (mConexao > RETENTATIVAS) {
                                        progressDialog.dismiss();
                                        AlertDialog.Builder builder = new AlertDialog.Builder(ScanActivity.this,
                                                android.R.style.Theme_Material_Dialog_Alert);
                                        builder.setTitle("Erro")
                                                .setMessage("Timeout ao conectar no Bluetooth")
                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        mConexaoDisconect = 0;
                                                    }
                                                })
                                                .show();
                                    } else {
                                        efetuaConexao();
                                    }
                                }
                            };

                            efetuaConexao();
                            //mHandler.removeCallbacksAndMessages (null);
                        }
                    }
                });
            }
        }

        public DeviceListAdapter() {
            mDevices = new ArrayList<>();
        }

        public void addDevice(MeuBluetoothDevice device) {
            for (MeuBluetoothDevice mbd : mDevices) {
                if (mbd.localDevice.getAddress().equals(device.localDevice.getAddress())) {
                    mbd.updateTimestamp(device.rssi);
                    return;
                }
            }
            mDevices.add(device);
        }

        public BluetoothDevice getDevice(int position) {
            return mDevices.get(position).localDevice;
        }

        public void refreshList() {
            mRefreshImage = findViewById(R.id.scan_devices);
            if (mStatus == 1) {
                for (int i = mDevices.size() - 1; i >= 0; i--) {
                    if (mDevices.get(i).verificaDeviceDeveSerRemovido()) {
                        mDevices.remove(i);
                        notifyDataSetChanged();
                    } else if (mDevices.get(i).verificaDeviceDeveSerBloquado()) {
                        mDevices.get(i).rssi = -188;
                        notifyDataSetChanged();
                    }
                }
            }
        }

        public void clear() {
            mDevices.clear();
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.device_item, viewGroup, false);
            return new DeviceViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull DeviceViewHolder deviceViewHolder, int i) {
            BluetoothDevice device = mDevices.get(i).localDevice;
            String name = device.getName();
            if (name == null || name.isEmpty()) {
                name = "Dispositivo desconhecido";
            }
            int rssi = mDevices.get(i).rssi;

            deviceViewHolder.deviceName.setText(name);
            deviceViewHolder.deviceRssi.setText(String.valueOf(rssi) + "  ");
            if (rssi < -180) {
                deviceViewHolder.deviceRssi.setText("    ");
                deviceViewHolder.deviceRssi.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.wifi_bloq, 0);
            } else if (rssi < -92) {
                deviceViewHolder.deviceRssi.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.wifi_no, 0);
            } else {
                deviceViewHolder.deviceRssi.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.wifi_h, 0);
            }
        }

        @Override
        public int getItemCount() {
            return mDevices.size();
        }
    }

    private ScanCallback scannerCallback = new ScanCallback() {


        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final BluetoothDevice device = result.getDevice();
                    final MeuBluetoothDevice mDevice = new MeuBluetoothDevice(device, result.getRssi());
                    if (isStarMeasureDevice(mDevice.localDevice.getName())) {
                        mDeviceListAdapter.addDevice(mDevice);
                        mDeviceListAdapter.notifyDataSetChanged();
                    }
                }

                private boolean isStarMeasureDevice(@Nullable String deviceName) {
                    return deviceName != null && (deviceName.startsWith("SM")  || deviceName.startsWith("ET") || deviceName.startsWith("EV") || deviceName.startsWith("MAX") || deviceName.startsWith("EXT") || deviceName.startsWith("UNQ"));
                }
            });
            super.onScanResult(callbackType, result);
        }

    };

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "SCAN ACTIVE RECEIVER: " + action);
            if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                    mHandler.removeCallbacksAndMessages(null);
                }
                mConexaoDisconect++;
                Log.w(TAG, "DISCONECT STATUS (SCAN ACTIVE): " + String.valueOf(mStatus) + " TENTATIVA: " + String.valueOf(mConexaoDisconect));

                if (mConexaoDisconect >= RETENTATIVAS_CONNECT) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ScanActivity.this,
                            android.R.style.Theme_Material_Dialog_Alert);
                    builder.setTitle("Medidor não conecta")
                            .setMessage("Medidor recusou a conexão. Tente novamente.")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("OK", (dialog, which) -> mConexaoDisconect = 0)
                            .show();
                } else if (mStatus == 3) {
                    mView.performClick();
                }
            }
            if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                mHandler.removeCallbacksAndMessages(null);
                setupGattServices(mBluetoothLeService.getSupportedGattServices());
                mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);

                if ((selectedDevice > mDeviceListAdapter.mDevices.size()) || (selectedDevice < 0)) {
                    Log.d(TAG, String.format("DEVICE %d NÃO ESTA NA LISTA", selectedDevice));
                    return;
                }
                final BluetoothDevice device = globalDevice.localDevice;

                Log.d(TAG, "Selecionado: " + device.getName());
                final Intent deviceIntent = new Intent(ScanActivity.this, DeviceActivity.class);
                Arquivo.createDirLeituras();
                deviceIntent.putExtra(Consts.EXTRAS_DEVICE_NAME, device.getName());
                deviceIntent.putExtra(Consts.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                deviceIntent.putExtra(Consts.EXTRAS_IS_SMFISCAL, strSMFiscal);

                scanDevices(false);
                startActivity(deviceIntent);
                selectedDevice = -1;
                mDeviceListAdapter.clear();
                mStatus = 0;
                mConexaoDisconect = 0;
                progressDialog.dismiss();
            }
        }
    };

    private void setupGattServices(List<BluetoothGattService> gattServices) {
        boolean isNewModule = false;

        if (gattServices == null) {
            Log.e(TAG, "Servidor BLE não encontrou nenhum serviço");
            return;
        }

        BluetoothGattService mStarMeasureService = null;
        for (BluetoothGattService gattService : gattServices) {
            if (gattService.getUuid().toString().equals(Consts.BLE_OLD_SERVICE)) {
                mStarMeasureService = gattService;
                break;
            } else if (gattService.getUuid().toString().equals(Consts.BLE_NEW_SERVICE)) {
                mStarMeasureService = gattService;
                isNewModule = true;
                break;
            }
        }

        if (mStarMeasureService == null) {
            return;
        }
        if (isNewModule) {
            mNotifyCharacteristic = mStarMeasureService.getCharacteristic(UUID.fromString(Consts.BLE_NEW_NOTIFY_CHARACTERISTIC));
        } else {
            mNotifyCharacteristic = mStarMeasureService.getCharacteristic(UUID.fromString(Consts.BLE_OLD_NOTIFY_CHARACTERISTIC));
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}
