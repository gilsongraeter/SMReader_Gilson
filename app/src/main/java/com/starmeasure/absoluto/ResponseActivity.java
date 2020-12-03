package com.starmeasure.absoluto;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.starmeasure.protocoloabsoluto.ComandoAbsoluto;
import com.starmeasure.protocoloabsoluto.RespostaAbsoluto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ResponseActivity extends AppCompatActivity {

    private static final String TAG = ResponseActivity.class.getSimpleName();

    private boolean isNewModule = false;

    private String mNumeroMedidor;
    private String mNumeroMedidorArquivo;
    private String mUnidadeConsumidora;
    private String mDeviceName;
    private ResponseAdapter mResponseAdapter;
    private RecyclerView mResponseRecyclerView;

    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattService mBluetoothGattService;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private int mByteCount = 0;
    private ByteArrayOutputStream mByteArrayData;
    private RespostaAbsoluto.RegistradoresAtuais mRegistradoresAtuais;
    private Date date;
    private String dataMedidor = "xx/xx/xxxx xx:xx:xx";
    private String versaoMedidor = "00.00";
    private boolean isEasyTrafo = false;
    private String strSMFiscal = "";

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_response);

        final Intent intent = getIntent();
        mNumeroMedidor = intent.getStringExtra(Consts.EXTRAS_NUMERO_MEDIDOR);
        mUnidadeConsumidora = intent.getStringExtra(Consts.EXTRAS_UNIDADE_CONSUMIDORA);
        mDeviceName = intent.getStringExtra(Consts.EXTRAS_DEVICE_NAME);
        isEasyTrafo = intent.getBooleanExtra(Consts.EXTRAS_EASY_TRAFO, false);
        strSMFiscal = intent.getStringExtra(Consts.EXTRAS_IS_SMFISCAL);

        String formato = "UC: %s";
        String formato_unidade = "Medidor: %s";
        if (mUnidadeConsumidora.equalsIgnoreCase("000000000")) {
            formato = "%s";
            mUnidadeConsumidora = "Med Hospedeiro";
        }
        if (isEasyTrafo) {
            formato = "Ponto: %s";
            //formato_unidade = "%s";
        }
        ((TextView) findViewById(R.id.dados_resposta))
                .setText("Leitura de Registradores Atuais");
        ((TextView) findViewById(R.id.unidade_consumidora))
                .setText(String.format(formato, mUnidadeConsumidora));

        ((TextView) findViewById(R.id.dados_medidor))
                .setText(String.format(formato_unidade, mNumeroMedidor));

        findViewById(R.id.salvar_dados)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        salvarArquivo();
                    }
                });
        findViewById(R.id.atualizar_dados_registradores)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        iniciarLeituraRegistradores();
                    }
                });
        findViewById(R.id.fechar_dados_registradores)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackPressed();
                    }
                });

        mResponseRecyclerView = findViewById(R.id.response_list);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mResponseRecyclerView.setLayoutManager(llm);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        date = Calendar.getInstance().getTime();
    }

    private void iniciarLeituraRegistradores() {
        if (mResponseAdapter != null) {
            mResponseAdapter.clear();
            mResponseAdapter.notifyDataSetChanged();
        }
        enviarAbnt21();
    }

    private void salvarArquivo() {
        String nomeArquivo = geraNomeArquivo();
        if (!Arquivo.salvarArquivo(ResponseActivity.this, nomeArquivo, getDadosArquivo())) {
            Toast.makeText(ResponseActivity.this, "Erro ao salvar arquivo",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(ResponseActivity.this, "Arquivo salvo com sucesso:\n" + nomeArquivo,
                    Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    private String geraNomeArquivo() {
        return String.format("Registradores_%s_%s_%s.csv", mNumeroMedidorArquivo, mDeviceName.substring(4).trim(),
                new SimpleDateFormat("ddMMyyyy'_'HHmmss").format(date));
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            setupGattService();
            iniciarLeituraRegistradores();
        } else {
            mBluetoothLeService = new BluetoothLeService();
        }
    }

    private void setupGattService() {
        for (BluetoothGattService gattService : mBluetoothLeService.getSupportedGattServices()) {
            if (gattService.getUuid().toString().equals(Consts.BLE_OLD_SERVICE)) {
                mBluetoothGattService = gattService;
                break;
            } else if (gattService.getUuid().toString().equals(Consts.BLE_NEW_SERVICE)) {
                mBluetoothGattService = gattService;
                isNewModule = true;
                break;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private void enviarAbnt21() {
        Log.d(TAG, "enviarAbnt21");
        if (isNewModule) {
            mWriteCharacteristic = mBluetoothGattService.getCharacteristic(UUID.fromString(Consts.BLE_NEW_WRITE_CHARACTERISTIC));
        } else {
            mWriteCharacteristic = mBluetoothGattService.getCharacteristic(UUID.fromString(Consts.BLE_OLD_WRITE_CHARACTERISTIC));
        }
        if (mWriteCharacteristic != null) {
            mByteArrayData = new ByteArrayOutputStream();
            byte[] data = new ComandoAbsoluto
                    .ABNT21()
                    .comMedidorNumero(mNumeroMedidor)
                    .build(!isEasyTrafo);
            sendData(data);
            Log.d(TAG, "Enviando: " + Util.ByteArrayToHexString(data));
        } else {
            Log.e(TAG, "Não foi possível enviar mensagem");
        }
    }

    private void enviarLeituraRegistradores() {
        Log.d(TAG, "enviarAb21LeituraRegistradores");
        if (isNewModule) {
            mWriteCharacteristic = mBluetoothGattService.getCharacteristic(UUID.fromString(Consts.BLE_NEW_WRITE_CHARACTERISTIC));
        } else {
            mWriteCharacteristic = mBluetoothGattService.getCharacteristic(UUID.fromString(Consts.BLE_OLD_WRITE_CHARACTERISTIC));
        }
        if (mWriteCharacteristic != null) {
            mByteArrayData = new ByteArrayOutputStream();
            byte[] data = new ComandoAbsoluto
                    .AB21LeituraRegistradoresAtuais()
                    .comMedidorNumero(mNumeroMedidor, isEasyTrafo)
                    .build(!isEasyTrafo);
            Log.e("##Math", mNumeroMedidor + " " + isEasyTrafo + " " + !isEasyTrafo);
            sendData(data);
            Log.d(TAG, "Enviando: " + Util.ByteArrayToHexString(data));
        } else {
            Log.e(TAG, "Não foi possível enviar mensagem");
        }
    }

    private void sendData(byte[] data) {
        mWriteCharacteristic.setValue(data);
        mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
        mByteCount = 0;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            setupGattService();
            iniciarLeituraRegistradores();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_BYTES);
                try {
                    mByteArrayData.write(data);
                    mByteCount += data.length;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (mByteCount >= 258) {
                    processData();
                    mByteCount = 0;
                    mByteArrayData.reset();
                }
            }
        }
    };

    private void processData() {
        byte[] data = mByteArrayData.toByteArray();
        RespostaAbsoluto resposta = new RespostaAbsoluto(data);
        if (resposta.isOcorrencia()) {
            processarRespostaOcorrencia(resposta);
        } else if (resposta.isAbnt21()) {
            processarRespostaAbnt21(resposta);
        } else if (resposta.isRegistradoresAtuais()) {
            processaRegistradoresAtuais(resposta);
        } else if (resposta.isRegistradoresAtuaisEasyTrafo()) {
            processaRegistradoresAtuais(resposta);
        } else if (resposta.isRespostaOcorrencias()) {
            enviarAbnt21();
        }
    }

    private void processarRespostaOcorrencia(RespostaAbsoluto resposta) {
        Toast.makeText(this,
                "Ocorrência no medidor. Solicitando limpeza de ocorrências.",
                Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Ocorrência: " + resposta.toString());
        enviarLimparOcorrencia(resposta.getSufixoNumeroMedidor(),
                resposta.getCodigoOcorrencia());
    }

    private void enviarLimparOcorrencia(String numeroMedidor, byte codigoOcorrencia) {
        mByteArrayData = new ByteArrayOutputStream();
        final byte[] comando = new ComandoAbsoluto.LimpezaOcorrenciasMedidor()
                .comMedidorNumero(numeroMedidor)
                .comCodigoOcorrencia(codigoOcorrencia)
                .build(!isEasyTrafo);
        Log.d(TAG, "Enviando limpeza ocorrência: " + Util.ByteArrayToHexString(comando));
        sendData(comando);
    }

    private void processaRegistradoresAtuais(RespostaAbsoluto resposta) {
        mRegistradoresAtuais = resposta.interpretarRegistradoresAtuais(isEasyTrafo);

        List<ResponseData> responseData = new ArrayList<>();
        responseData.add(new ResponseData("Data e Hora do Medidor",
                dataMedidor));

        if (isEasyTrafo) {
            responseData.add(new ResponseData("Energia Ativa Geral Direta ", //AQUI!!!
                    mRegistradoresAtuais.TotalizadorGeralEnergiaAtivaDireta + " Wh"));
            responseData.add(new ResponseData("Energia Ativa Geral Reversa ",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaPonta + " Wh"));
            responseData.add(new ResponseData("Energia Reativa Geral Positiva ",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaForaPonta + " varh"));
            responseData.add(new ResponseData("Energia Reativa Geral Negativa ",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaReservado + " varh"));
            responseData.add(new ResponseData("Total Ativa Direta Entradas 1, 2 e 3 ",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaTarifaD + " Wh"));
            responseData.add(new ResponseData("Total Ativa Reversa Entradas 1, 2 e 3 ",
                    mRegistradoresAtuais.TotalizadorGeralEnergiaAtivaReversa + " Wh"));
            responseData.add(new ResponseData("Total Reativa Positiva Ent. 1, 2 e 3",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaPonta + " varh"));
            responseData.add(new ResponseData("Total Reativa Negativa Ent. 1, 2 e 3",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaForaPonta + " varh"));
            responseData.add(new ResponseData("Total Ativa Direta Entradas 4, 5 e 6",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaReservado + " Wh"));
            responseData.add(new ResponseData("Total Ativa Reversa Entradas 4, 5 e 6",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaTarifaD + " Wh"));
            responseData.add(new ResponseData("Total Reativa Positiva Ent. 4, 5 e 6",
                    mRegistradoresAtuais.TotalizadorGeralEnergiaReativaPositiva + " varh"));
            responseData.add(new ResponseData("Total Reativa Negativa Ent. 4, 5 e 6",
                    mRegistradoresAtuais.TotalizadorEnergiaReativaPositivaPonta + " varh"));


            //prepara dados para serem enviados ao SM_FISCAL
            if (!strSMFiscal.isEmpty()) {
                Intent sendIntent = getPackageManager().getLaunchIntentForPackage("mge.mobile.sm.inspect.com");
                if (sendIntent != null) {
                    Toast.makeText(getApplicationContext(), "Voltando para o SMInspect", Toast.LENGTH_LONG).show();

                    Log.d("#TESTE#", "{" +
                            "\n" + mRegistradoresAtuais.TotalizadorGeralEnergiaAtivaDireta +
                            "\n" + mRegistradoresAtuais.TotalizadorGeralEnergiaAtivaReversa +
                            "\n" + mUnidadeConsumidora +
                            "\n" + mNumeroMedidor +
                            "\n" + mNumeroMedidor +
                            "\n" + mBluetoothLeService.getDeviceAddress() +
                            "\n" + mBluetoothLeService.getDeviceName() +
                            "\n" + strSMFiscal + "}");

                    sendIntent.putExtra("EXTRA_FISCAL_ENERGIA_ATIVA_DIRETA", String.valueOf(mRegistradoresAtuais.TotalizadorGeralEnergiaAtivaDireta));
                    sendIntent.putExtra("EXTRA_FISCAL_ENERGIA_ATIVA_REVERSA", String.valueOf(mRegistradoresAtuais.TotalizadorGeralEnergiaAtivaReversa));
                    sendIntent.putExtra("EXTRA_FISCAL_UC", mUnidadeConsumidora);
                    sendIntent.putExtra("EXTRA_FISCAL_EQUIPAMENTO", mNumeroMedidor);
                    sendIntent.putExtra("EXTRA_FISCAL_MAC_ADDRESS", mBluetoothLeService.getDeviceAddress());
                    sendIntent.putExtra("EXTRA_FISCAL_DEVICE", mBluetoothLeService.getDeviceName());
                    sendIntent.putExtra("EXTRA_SMFISCAL", strSMFiscal);

                    mBluetoothLeService.disconnect();

                    startActivity(sendIntent);
                } else {
                    Toast.makeText(getApplicationContext(), "SM FISCAL NÃO ESTÁ INSTALADO!!", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            responseData.add(new ResponseData("Energia Ativa Geral Direta",
                    mRegistradoresAtuais.TotalizadorGeralEnergiaAtivaDireta + " Wh"));
            responseData.add(new ResponseData("Energia Ativa Ponta Direta ",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaPonta + " Wh"));
            responseData.add(new ResponseData("Energia Ativa Fora Ponta Direta ",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaForaPonta + " Wh"));
            responseData.add(new ResponseData("Energia Ativa Reservado Direta ",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaReservado + " Wh"));
            responseData.add(new ResponseData("Energia Ativa Intermediário Direta ",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaTarifaD + " Wh"));
            responseData.add(new ResponseData("Energia Ativa Geral Reversa ",
                    mRegistradoresAtuais.TotalizadorGeralEnergiaAtivaReversa + " Wh"));
            responseData.add(new ResponseData("Energia Ativa Ponta Reversa ",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaPonta + " Wh"));
            responseData.add(new ResponseData("Energia Ativa Fora Ponta Reversa ",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaForaPonta + " Wh"));
            responseData.add(new ResponseData("Energia Ativa Reservado Reversa ",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaReservado + " Wh"));
            responseData.add(new ResponseData("Energia Ativa Intermediário Reversa ",
                    mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaTarifaD + " Wh"));
            responseData.add(new ResponseData("Energia Reativa Geral Positiva ",
                    mRegistradoresAtuais.TotalizadorGeralEnergiaReativaPositiva + " varh"));
            responseData.add(new ResponseData("Energia Reativa Ponta Positiva ",
                    mRegistradoresAtuais.TotalizadorEnergiaReativaPositivaPonta + " varh"));
            responseData.add(new ResponseData("Energia Reativa Fora Ponta Positiva ",
                    mRegistradoresAtuais.TotalizadorEnergiaReativaPositivaForaPonta + " varh"));
            responseData.add(new ResponseData("Energia Reativa Reservado Positiva ",
                    mRegistradoresAtuais.TotalizadorEnergiaReativaPositivaReservado + " varh"));
            responseData.add(new ResponseData("Energia Reativa Intermediário Positiva ",
                    mRegistradoresAtuais.TotalizadorEnergiaReativaPositivaTarifaD + " varh"));
            responseData.add(new ResponseData("Energia Reativa Geral Negativa ",
                    mRegistradoresAtuais.TotalizadorGeralEnergiaReativaNegativa + " varh"));
            responseData.add(new ResponseData("Energia Reativa Ponta Negativa ",
                    mRegistradoresAtuais.TotalizadorEnergiaReativaNegativaPonta + " varh"));
            responseData.add(new ResponseData("Energia Reativa Fora Ponta Negativa ",
                    mRegistradoresAtuais.TotalizadorEnergiaReativaNegativaForaPonta + " varh"));
            responseData.add(new ResponseData("Energia Reativa Reservado Negativa ",
                    mRegistradoresAtuais.TotalizadorEnergiaReativaNegativaReservado + " varh"));
            responseData.add(new ResponseData("Energia Reativa Intermediário Negativa ",
                    mRegistradoresAtuais.TotalizadorEnergiaReativaNegativaTarifaD + " varh"));
            responseData.add(new ResponseData("Versão de Software ",
                    mRegistradoresAtuais.VersaoSoftware));
        }

        mResponseAdapter = new ResponseAdapter(responseData);
        mResponseRecyclerView.setAdapter(mResponseAdapter);
        mResponseAdapter.notifyDataSetChanged();
        mNumeroMedidorArquivo = resposta.getNumeroMedidor();
    }

    private void processarRespostaAbnt21(RespostaAbsoluto resposta) {
        Log.d(TAG, "processarRespostaAbnt21");
        byte[] data = resposta.getData();
        date = Calendar.getInstance().getTime();
        dataMedidor = String.format("%02X/%02X/20%02X %02X:%02X:%02X",
                data[8], data[9], data[10], data[5], data[6], data[7]);
        versaoMedidor = String.format("%02X.%02X", data[149], data[148]);
        enviarLeituraRegistradores();
    }

    private String getDadosArquivo() {
        DecimalFormat f = new DecimalFormat("0000000000");
        if (isEasyTrafo) {
            return
                    "Medidor;" + mNumeroMedidor + "\n" +
                            "Data/Hora Medidor;" + dataMedidor + "\n" +
                            "Data/Hora Local;" + new SimpleDateFormat("dd/MM/yyyy' 'HH:mm:ss").format(date) + "\n" +
                            "Energia Ativa Geral Direta (Wh);" + f.format(mRegistradoresAtuais.TotalizadorGeralEnergiaAtivaDireta) + "\n" +
                            "Energia Ativa Geral Reversa (Wh);" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaPonta) + "\n" +
                            "Energia Reativa Geral Positiva (varh);" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaForaPonta) + "\n" +
                            "Energia Reativa Geral Negativa (varh);" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaReservado) + "\n" +
                            "Total Ativa Direta Entradas 1, 2 e 3 (Wh);" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaTarifaD) + "\n" +
                            "Total Ativa Reversa Entradas 1, 2 e 3 (Wh);" + f.format(mRegistradoresAtuais.TotalizadorGeralEnergiaAtivaReversa) + "\n" +
                            "Total Reativa Positiva Ent. 1, 2 e 3 (varh);" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaPonta) + "\n" +
                            "Total Reativa Negativa Ent. 1, 2 e 3 (varh);" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaForaPonta) + "\n" +
                            "Total Ativa Direta Entradas 4, 5 e 6 (Wh);" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaReservado) + "\n" +
                            "Total Ativa Reversa Entradas 4, 5 e 6 (Wh);" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaTarifaD) + "\n" +
                            "Total Reativa Positiva Ent. 4, 5 e 6 (varh);" + f.format(mRegistradoresAtuais.TotalizadorGeralEnergiaReativaPositiva) + "\n" +
                            "Total Reativa Negativa Ent. 4, 5 e 6 (varh);" + f.format(mRegistradoresAtuais.TotalizadorEnergiaReativaPositivaPonta) + "\n";
        } else {
            return
                    "Medidor;" + mNumeroMedidor + "\n" +
                            "UC:" + mUnidadeConsumidora + "\n" +
                            "Data/Hora Medidor;" + dataMedidor + "\n" +
                            "Data/Hora Local;" + new SimpleDateFormat("dd/MM/yyyy' 'HH:mm:ss").format(date) + "\n" +
                            "Energia Ativa Geral Direta;" + f.format(mRegistradoresAtuais.TotalizadorGeralEnergiaAtivaDireta) + "\n" +
                            "Energia Ativa Ponta Direta;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaPonta) + "\n" +
                            "Energia Ativa Fora Ponta Direta;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaForaPonta) + "\n" +
                            "Energia Ativa Reservado Direta;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaReservado) + "\n" +
                            "Energia Ativa Intermediário Direta;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaDiretaTarifaD) + "\n" +
                            "Energia Ativa Geral Reversa;" + f.format(mRegistradoresAtuais.TotalizadorGeralEnergiaAtivaReversa) + "\n" +
                            "Energia Ativa Ponta Reversa;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaPonta) + "\n" +
                            "Energia Ativa Fora Ponta Reversa;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaForaPonta) + "\n" +
                            "Energia Ativa Reservado Reversa;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaReservado) + "\n" +
                            "Energia Ativa Intermediário Reversa;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaAtivaReversaTarifaD) + "\n" +
                            "Energia Reativa Geral Positiva;" + f.format(mRegistradoresAtuais.TotalizadorGeralEnergiaReativaPositiva) + "\n" +
                            "Energia Reativa Ponta Positiva;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaReativaPositivaPonta) + "\n" +
                            "Energia Reativa Fora Ponta Positiva;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaReativaPositivaForaPonta) + "\n" +
                            "Energia Reativa Reservado Positiva;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaReativaPositivaReservado) + "\n" +
                            "Energia Reativa Intermediário Positiva;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaReativaPositivaTarifaD) + "\n" +
                            "Energia Reativa Geral Negativa;" + f.format(mRegistradoresAtuais.TotalizadorGeralEnergiaReativaNegativa) + "\n" +
                            "Energia Reativa Ponta Negativa;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaReativaNegativaPonta) + "\n" +
                            "Energia Reativa Fora Ponta Negativa;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaReativaNegativaForaPonta) + "\n" +
                            "Energia Reativa Reservado Negativa;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaReativaNegativaReservado) + "\n" +
                            "Energia Reativa Intermediário Negativa;" + f.format(mRegistradoresAtuais.TotalizadorEnergiaReativaNegativaTarifaD) + "\n" +
                            "Versao de Software;" + mRegistradoresAtuais.VersaoSoftware + "\n"; //ToDo adicionar valores (Wh) e (varh)

        }
    }

    private class ResponseAdapter extends RecyclerView.Adapter<ResponseAdapter.ViewHolder> {

        List<ResponseData> responseData;

        ResponseAdapter(List<ResponseData> data) {
            responseData = data;
        }

        @NonNull
        @Override
        public ResponseAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.data_item, viewGroup, false);
            return new ResponseAdapter.ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ResponseAdapter.ViewHolder viewHolder, int i) {
            final ResponseData data = responseData.get(i);
            viewHolder.name.setText(data.name);
            viewHolder.value.setText(data.value);
        }

        @Override
        public int getItemCount() {
            return responseData.size();
        }

        public void clear() {
            responseData.clear();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            TextView value;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.data_name);
                value = itemView.findViewById(R.id.data_value);
            }
        }
    }

    private class ResponseData {
        final String name;
        final String value;

        public ResponseData(String name, long value) {
            this.name = name;
            this.value = String.format("%d", value);
        }

        public ResponseData(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

}
