package com.starmeasure.absoluto;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.starmeasure.absoluto.filemanager.FileManagerLeiturasActivity;
import com.starmeasure.absoluto.filemanager.controller.CargaController;
import com.starmeasure.absoluto.filemanager.model.Carga;
import com.starmeasure.protocoloabsoluto.ComandoAbsoluto;
import com.starmeasure.protocoloabsoluto.MedidorAbsoluto;
import com.starmeasure.protocoloabsoluto.RespostaAbsoluto;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


public class DeviceActivity extends AppCompatActivity
        implements PopupMenu.OnMenuItemClickListener {

    private static final String TAG = DeviceActivity.class.getSimpleName();

    private boolean isNewModule = false;

    private Dialog dialogMonitoramento;
    private ComandoAbsoluto.EB90 mEB90;
    private String monitoramentoNumeroEasy;

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattService mStarMeasureService;
    private MeterListAdapter mMeterListAdapter;
    private MeterEasyTrafoAdapter mMeterEasyTrafoAdapter;
    private RecyclerView mRecyclerView;
    private ImageButton mRefreshStatus;
    private List<MedidorAbsoluto> medidores;
    private boolean rodandoCarga = false;
    private boolean rodandoEB90 = false;
    private Carga mCarga;
    private ComandoAbsoluto.CargaDePrograma mCargaDePrograma;
    private int contaTentativasContador = 0;

    private Handler mHandler;
    private Runnable mLeituraMedidorRunnable;
    private Runnable mMMRunnable;
    private static final long TIMEOUT = 10000;
    private static final long TIMEOUT_MM = 5000;

    private AlertDialog progressDialog;

    public enum TipoOperacao {
        RegistradoresAtuais,
        PaginaFiscal,
        CorteReliga,
        DataHora,
        NomeUnidade,
        MemoriaMassa,
        ResetRegistradores,
        ParametrosQEE,
        InicioMemoriaMassaQEE,
        MemoriaMassaQEE,
        IniciarCargaDePrograma,
        MonitoramentoDeTransformador,
        ModoTeste,
        IntervaloMM,
        FIM
    }

    public enum TipoMedidor {
        ABOSOLUTO,
        EASY_TRAFO,
        EASY_VER,
        EASY_VOLT
    }

    private Dialog dialogModoTeste = null;
    private int mByteCount = 0;
    private ByteArrayOutputStream mBytesReceived;
    private ArrayList<byte[]> mRespostaComposta = new ArrayList<>();
    private boolean deveLerTudo = false;
    private boolean leuMedidoresIndividuais = false;
    private boolean leuEasyTrafo = false;
    private TipoMedidor tipo_medidor = TipoMedidor.ABOSOLUTO;
    private boolean medidorSemRele = false;
    private TipoOperacao funcaoEmExecucao = TipoOperacao.RegistradoresAtuais;
    private Handler mTimeoutHandler;
    private Runnable mMedidoresIndividuaisRunnable;
    private String dataMedidor = "xx/xx/xxxx xx:xx:xx";
    private String versaoMedidor = "00.00";
    private byte[] iuMedidor = new byte[]{};
    private byte[] android_id = new byte[]{};
    private String modeloMultiponto = "";
    private String tipoModulo = "";
    private String estadoModulo = "";
    private String sinalModulo = "";
    private String numMedidor = "";
    private String mNsMedidor = "";
    private MedidorAbsoluto medidorSelecionado;
    private RespostaAbsoluto.LeituraDados51 dadosAbnt51;
    private RespostaAbsoluto.LeituraDados52 processa52;
    private RespostaAbsoluto.LeituraCabecalhoQEE dadosCabecalhoQEE;
    private RespostaAbsoluto.LeituraQEE processaQEE;
    private ComandoAbsoluto.EB92 eb92;
    private byte[] mRespostaQEE;


    private byte mRetentativas = 0;
    private int codigoCanal = 0;

    private String strSMFiscal = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(Consts.EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(Consts.EXTRAS_DEVICE_ADDRESS);
        strSMFiscal = intent.getStringExtra(Consts.EXTRAS_IS_SMFISCAL);

        mHandler = new Handler(

        );
        findViewById(R.id.ad_imgbtn_openFileManager).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(v.getContext(), FileManagerLeiturasActivity.class));
            }
        });
        mRefreshStatus = findViewById(R.id.atualizar_dados);
        mRefreshStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRefreshStatus.getAnimation() == null) {
                    animateRefresh();
                    if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
                        if (medidores != null && medidores.size() > 0)
                            enviarProximoStatusMedidor("99999999");
                    } else {
                        enviarLeituraMedidoresIndividuais();
                    }
                }
            }
        });

        mTimeoutHandler = new Handler();

        findViewById(R.id.fechar_medidor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothLeService.disconnect();
                //mStarMeasureService.g
                onBackPressed();
            }
        });

        mRecyclerView = findViewById(R.id.meter_list);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(llm);
        if (mDeviceName.startsWith("ET") || mDeviceName.startsWith("MAX")) {
            tipo_medidor = TipoMedidor.EASY_TRAFO;
        } else if (mDeviceName.startsWith("EV")) {
            tipo_medidor = TipoMedidor.EASY_VER;
        }


        if (tipo_medidor != TipoMedidor.ABOSOLUTO) {
            CardView mainLayout = this.findViewById(R.id.unidades_consumidoras_header);
            mainLayout.setVisibility(LinearLayout.GONE);
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private boolean sendData(byte[] data) {
        BluetoothGattCharacteristic writeCharacteristic;
        if (isNewModule) {
            writeCharacteristic = mStarMeasureService
                    .getCharacteristic(UUID.fromString(Consts.BLE_NEW_WRITE_CHARACTERISTIC));
        } else {
            writeCharacteristic = mStarMeasureService
                    .getCharacteristic(UUID.fromString(Consts.BLE_OLD_WRITE_CHARACTERISTIC));
        }
        if (writeCharacteristic == null)
            return false;
        writeCharacteristic.setValue(data);
        mBluetoothLeService.writeCharacteristic(writeCharacteristic);
        mByteCount = 0;
        mBytesReceived = new ByteArrayOutputStream();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (rodandoCarga) {
            enviarCargaDePrograma();
        } else {

            if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
                showProgressBar("Lendo medidores individuais");
            } else {
                if (tipo_medidor == TipoMedidor.EASY_TRAFO) {
                    showProgressBar("Lendo Easy Trafo");
                } else {
                    showProgressBar("Lendo Easy Ver");
                }
            }

            leuMedidoresIndividuais = false;
            leuEasyTrafo = false;

            enviarLeituraMedidoresIndividuais();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTimeoutHandler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
        contaTentativasContador = 0;
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AGORA FINALIZOU A CONEXAO");
        mBluetoothLeService.disconnect();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            mStarMeasureService = null;
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

            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "ACTIVE DEVICE RECEIVER " + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(TAG, "***************************** LEITURA ATENCAO");

                BluetoothGattCharacteristic mNotifyCharacteristic;
                if (isNewModule) {
                    mNotifyCharacteristic = mStarMeasureService.getCharacteristic(UUID.fromString(Consts.BLE_NEW_NOTIFY_CHARACTERISTIC));
                } else {
                    mNotifyCharacteristic = mStarMeasureService.getCharacteristic(UUID.fromString(Consts.BLE_OLD_NOTIFY_CHARACTERISTIC));
                }
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
                }
                //mRefreshStatus.callOnClick();*/
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mBluetoothLeService.connect(mDeviceAddress);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_BYTES);
                try {
                    mBytesReceived.write(data);
                    mByteCount += data.length;
                    Log.d(TAG, "REC->" + Util.ByteArrayToHexString(mBytesReceived.toByteArray()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (mByteCount >= 258 || rodandoCarga) {
                    processData();
                    mByteCount = 0;
                    mBytesReceived.reset();
                }
            }
        }
    };

    private void showTimeOutMM() {
        if (mMMRunnable == null) {
            mMMRunnable = () -> {
                progressDialog.hide();
                progressDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(DeviceActivity.this,
                        android.R.style.Theme_Material_Dialog_Alert);
                builder.setTitle("Medidor Desconectado")
                        .setMessage("Medidor não conectado!")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("OK", (dialog, which) -> {
                            onBackPressed();
                        })
                        .show();
            };
        }
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(mMMRunnable, TIMEOUT_MM);
    }


    private void showProgressBar(String mensagem) {
        mLeituraMedidorRunnable = new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(DeviceActivity.this,
                        android.R.style.Theme_Material_Dialog_Alert);
                builder.setTitle("Medidor Desconectado")
                        .setMessage("Medidor não conectado!")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onBackPressed();
                            }
                        })
                        .show();
            }
        };

        mHandler.postDelayed(mLeituraMedidorRunnable, TIMEOUT);

        progressDialog = AlertBuilder.createProgressDialog(this, mensagem);
        progressDialog.show();
    }

    private void enviarLeituraMedidoresIndividuais() {
        new Handler().post(this::enviarAbnt21);
        if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
            mMedidoresIndividuaisRunnable = () -> {
                if (!leuMedidoresIndividuais) {
                    leuMedidoresIndividuais = true;
                    progressDialog.dismiss();
                    mHandler.removeCallbacksAndMessages(null);
                    AlertDialog.Builder builder = new AlertDialog.Builder(DeviceActivity.this,
                            android.R.style.Theme_Material_Dialog_Alert);
                    builder.setTitle("Erro")
                            .setMessage("Timeout na leitura dos medidores individuais")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mBluetoothLeService.disconnect();
                                    onBackPressed();
                                }
                            })
                            .show();
                }
            };
            mTimeoutHandler.postDelayed(mMedidoresIndividuaisRunnable, 5000);
        }
    }

    private void enviarAB08() {
        Log.i("enviando", "Enviando AB08");
        enviarComandoComposto(new ComandoAbsoluto.AB08().build());
    }

    private void processarAB08(RespostaAbsoluto respostaAbsoluto) {
        Log.i("processarAB08", "Processando AB08");
        byte[] data = respostaAbsoluto.getData();
        mTimeoutHandler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
        progressDialog.dismiss();
        if (verificaErroAB08(data[7])) {
            if (mCarga != null) {
                android_id = getDeviceId();
                mCargaDePrograma = new ComandoAbsoluto.CargaDePrograma(iuMedidor, android_id);
                byte[] dadosBrutos = CargaController.getBytesCargaDePrograma(mCarga);
                if (dadosBrutos != null && dadosBrutos.length > 15) {
                    mCarga.setDadosBrutos(dadosBrutos);
                    byte[] size = new byte[]{dadosBrutos[14], dadosBrutos[13]};
                    short i = ByteBuffer.wrap(size).getShort();
                    Log.i("incredible", String.valueOf(i));
                    mCarga.setMaxSize(i);
                    ArrayList<byte[]> bPack = CargaController.dividePacks(mCarga);
                    mCarga.setDataToSend(bPack);
                    enviarCargaDePrograma();
                } else {
                    Log.e("processarAB08", "Dados brutos nulo");
                }
            }
        }
    }

    private void enviarCargaDePrograma() {
        Log.i("carga", "Enviando carga de programa");
        rodandoCarga = true;
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
        mTimeoutHandler.postDelayed(() -> {
            Log.e("error", "TIME OUT CARGA DE PROGRAMA");
            AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
            builder.setTitle("Aviso");
            builder.setMessage("Falha ao enviar carga de programa.\nConecte-se novamente.");
            builder.setCancelable(false);
            builder.setNegativeButton("Entendi", (p1, p2) -> {
                mBluetoothLeService.disconnect();
                finish();
            });
            builder.create();
            builder.show();
            mTimeoutHandler.removeCallbacksAndMessages(null);
            mHandler.removeCallbacksAndMessages(null);
            rodandoCarga = false;
        }, 10000);
        if (mCargaDePrograma == null) {
            Log.i("carga", "Instanciando carga de programa");
            mCargaDePrograma = new ComandoAbsoluto.CargaDePrograma(iuMedidor, android_id);
        }
        byte[] dataToSend = null;
        try {
            if (mCargaDePrograma.getPacoteAtual() == 0) {
                Log.i("carga", "Pacote atual 0 - enviando primeiro comando");
                mCargaDePrograma.setDados(mCarga.getDataToSend().get(0));
                mCargaDePrograma.setMensagemFinal(1);
                mCargaDePrograma.setComandoAtual(ComandoAbsoluto.CargaDePrograma.iniciaCarga);
                progressDialog.setMessage("Iniciando carga de programa");
            } else {
                Log.i("carga", "Pacote atual " + mCargaDePrograma.getPacoteAtual() + " - comando de sequencia");
                byte[] dados = mCarga.getDataToSend().get(mCargaDePrograma.getPacoteAtual());
                mCargaDePrograma.setComandoAtual(ComandoAbsoluto.CargaDePrograma.transferenciaDeCarga);
                if (mCargaDePrograma.getPacoteAtual() == (mCarga.getDataToSend().size() - 1)) {
                    mCargaDePrograma.setMensagemFinal(1);
                } else {
                    mCargaDePrograma.setMensagemFinal(0);
                }
                mCargaDePrograma.setDados(dados);
                progressDialog.setMessage("Enviando pacote " + mCargaDePrograma.getPacoteAtual() + " de " + (mCarga.getDataToSend().size() - 1));
            }

            dataToSend = mCargaDePrograma.build();
        } catch (IOException e) {
            e.printStackTrace();
            mTimeoutHandler.removeCallbacksAndMessages(null);
            mHandler.removeCallbacksAndMessages(null);
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Toast.makeText(mBluetoothLeService, "ERRO: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (dataToSend != null) {
                if (!progressDialog.isShowing()) {
                    progressDialog.show();
                }
                Log.i("comandoenviar", Util.ByteArrayToHexString(dataToSend));
                enviarComando(dataToSend);
            }
        }
    }

    private void processarCargaDePrograma(RespostaAbsoluto respostaAbsoluto) {
        Log.i("processaCarga", "Recebeu dados");
        mTimeoutHandler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
        byte[] data = respostaAbsoluto.getData();
        Log.i("processaCarga", "DADOS-> " + Util.ByteArrayToHexString(data));
        byte[] cabecalho = new byte[]{data[0], data[1]};
        if ((cabecalho[1] & 0x01) == 1) {
            Log.e("processaCarga", "Erro | enviando mesmo comando");
            int sequenciadorAtual = mCargaDePrograma.getSequenciador() + 1;
            mCargaDePrograma.setSequenciador(sequenciadorAtual);
            enviarCargaDePrograma();
            return;
        }

        // Não utilizado ainda
        byte[] ocorrencias = new byte[]{data[2], data[3], data[4], data[5]};

        byte[] dados;

        byte retorno = data[6];
        if (retorno == 0x02) {
            dados = new byte[]{data[7]};
            byte erro = dados[0];
            String errorMsg = null;
            if (erro == 0x20) {
                errorMsg = "Tipo de equipamento inválido";
            } else if (erro == 0x21) {
                errorMsg = "Versão/Revisão de software não suportada";
            } else if (erro == 0x22) {
                errorMsg = "Tamanho do programa inválido";
            } else if (erro == 0x23) {
                errorMsg = "Offset do ponto de entrada do programa inválido";
            } else if (erro == 0x24) {
                errorMsg = "Tamanho do pacote de transferência não suportado";
            } else if (erro == 0x25) {
                if (contaTentativasContador < 5) {
                    contaTentativasContador++;
                    mCargaDePrograma.atualizarSequenciador();
                    mCargaDePrograma.atualizarContadorDePacote();

                    if (mCarga.getDataToSend().size() != mCargaDePrograma.getPacoteAtual()) {
                        enviarCargaDePrograma();
                    } else {
                        mBluetoothLeService.disconnect();
                        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                        builder.setTitle("Aviso");
                        builder.setMessage("Carga de programa finalizada. O medidor está reiniciando.");
                        builder.setCancelable(false);
                        builder.setPositiveButton("Entendi", (p1, p2) -> finish());
                        builder.create();
                        builder.show();
                    }
                } else {
                    errorMsg = "Contador do pacote de transferência inválido";
                }
            } else if (erro == 0x26) {
                errorMsg = "Tentativa de transferência de carga sem inicialização";
            } else if (erro == 0x27) {
                errorMsg = "Erro na finalização da carga de programa";
            } else {
                errorMsg = "Erro desconhecido";
            }

            if (errorMsg != null) {
                Log.e("processaCarga", errorMsg);

                AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                builder.setTitle("Erro - Carga de Programa");
                builder.setMessage(errorMsg);
                builder.setCancelable(false);
                builder.setPositiveButton("Entendi", (p1, p2) -> {
                    mBluetoothLeService.disconnect();
                    finish();
                });
                builder.create();
                builder.show();
            }

        } else if (retorno == 0x0C) {
            dados = new byte[]{data[10], data[9], data[8], data[7]};
            mCargaDePrograma.setSequenciador(ByteBuffer.wrap(dados).getInt());
            Log.i("processaCarga", "Novo sequenciador necessário: " + ByteBuffer.wrap(dados).getInt());
            enviarCargaDePrograma();
        } else if (retorno == 0x00) {
            mCargaDePrograma.atualizarSequenciador();
            mCargaDePrograma.atualizarContadorDePacote();

            if (mCarga.getDataToSend().size() != mCargaDePrograma.getPacoteAtual()) {
                enviarCargaDePrograma();
            } else {
                mBluetoothLeService.disconnect();
                AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                builder.setTitle("Aviso");
                builder.setMessage("Carga de programa finalizada. O medidor está reiniciando.");
                builder.setCancelable(false);
                builder.setPositiveButton("Entendi", (p1, p2) -> finish());
                builder.create();
                builder.show();
            }
        } else if (retorno == 0x01) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
            builder.setTitle("Erro - Carga de Programa");
            builder.setMessage("Comando não reconhecido pelo medidor");
            builder.setCancelable(false);
            builder.setPositiveButton("Entendi", (p1, p2) -> {
                mBluetoothLeService.disconnect();
                finish();
            });
            builder.create();
            builder.show();
        } else {
            Log.e("processaCarga", "ERRO");
        }
    }

    private boolean verificaErroAB08(byte data) {
        if (data == 0) {
            return true;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("Aviso");
        if (data == 1) {
            builder.setMessage("Interface não suportada");
        } else if (data == 2) {
            builder.setMessage("Protocolo não suportado");
        }
        builder.setNegativeButton("Entendi", null);
        builder.create();
        builder.show();
        return false;
    }

    @NotNull
    private byte[] getDeviceId() {
        @SuppressLint("HardwareIds")
        String deviceID = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.i("getBluetoothMac", deviceID);
        byte[] list = deviceID.getBytes();

        if (list != null && list.length > 0) {
            if (list.length > 4) {
                return new byte[]{list[0], list[1], list[2], list[3], (byte) 0x91};
            } else if (list.length == 3) {
                return new byte[]{list[0], list[1], list[2], 0x00, (byte) 0x91};
            } else if (list.length == 2) {
                return new byte[]{list[0], list[1], 0x00, 0x00, (byte) 0x91};
            } else if (list.length == 1) {
                return new byte[]{list[0], 0x00, 0x00, 0x00, (byte) 0x91};
            }
        }

        return new byte[]{0x00, 0x00, 0x00, 0x00, (byte) 0x91};
    }

    private void enviarLeituraConfiguracao() {
        enviarComandoComposto(
                new ComandoAbsoluto.LeituraConfiguracaoMedidor()
                        .build(tipo_medidor == TipoMedidor.ABOSOLUTO)
        );
    }

    private void enviarLeituraParametrosHospedeiro() {
        enviarComandoComposto(
                new ComandoAbsoluto
                        .LeituraParametrosHospedeiro()
                        .build()
        );
    }

    private void enviarAbnt21() {
        Log.d(TAG, "enviarAbnt21");
        leuEasyTrafo = false;
        mTimeoutHandler.postDelayed(() -> {
            mRetentativas++;
            if (mRetentativas > 5) {
                mTimeoutHandler.removeCallbacksAndMessages(null);
                mRefreshStatus.clearAnimation();
                progressDialog.dismiss();
                mHandler.removeCallbacksAndMessages(null);
                AlertDialog.Builder builder = new AlertDialog.Builder(DeviceActivity.this,
                        android.R.style.Theme_Material_Dialog_Alert);
                builder.setTitle("Erro")
                        .setMessage("Timeout na leitura do Medidor")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mBluetoothLeService.disconnect();
                                onBackPressed();
                            }
                        })
                        .show();
            } else {
                byte[] data = new ComandoAbsoluto
                        .ABNT21()
                        .comMedidorNumero("000000")
                        .build((tipo_medidor == TipoMedidor.ABOSOLUTO));
                enviarComando(data);
                Log.d(TAG, "RETRY ET: " + Util.ByteArrayToHexString(data));
            }
        }, 3000);


        byte[] data = new ComandoAbsoluto
                .ABNT21()
                .comMedidorNumero("000000")
                .build((tipo_medidor == TipoMedidor.ABOSOLUTO));
        enviarComando(data);
        Log.d(TAG, "Enviando ABNT21: " + Util.ByteArrayToHexString(data));
        mRetentativas = 1;
    }

    private void enviarLeitura87() {
        Log.d(TAG, "enviarAbnt87 EasyTrafo/Ver");

        byte[] data = new ComandoAbsoluto
                .ABNT87()
                .comMedidorNumero("000000")
                .build((tipo_medidor == TipoMedidor.ABOSOLUTO));
        enviarComando(data);
        Log.d(TAG, "Enviando ET: " + Util.ByteArrayToHexString(data));
    }

    private void enviarLeituraStatusMedidores(String numeroMedidor) {
        mTimeoutHandler.removeCallbacksAndMessages(null);
        mTimeoutHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (deveLerTudo) {
                    deveLerTudo = false;
                    mRefreshStatus.clearAnimation();
                    showErrorMessage(getString(R.string.text_timeout_leitura));
                }
            }
        }, 3000);
        enviarComando(
                new ComandoAbsoluto.AB06AlteracaoCorteReligamento()
                        .comMedidorNumero(numeroMedidor)
                        .efetuaLeitura()
                        .build((tipo_medidor == TipoMedidor.ABOSOLUTO))
        );
    }

    private void enviarAberturaSessao(String numeroMedidor) {
        final View alertText = getLayoutInflater().inflate(R.layout.alert_text, null);
        final EditText password = alertText.findViewById(R.id.alert_text);

        final String nm = numeroMedidor;
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                android.R.style.Theme_Material_Dialog_Alert);
        AlertDialog alertDialog = builder
                .setTitle("Abertura de sessão")
                .setMessage("Digite o código de acesso")
                .setView(alertText)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        byte[] comando = new ComandoAbsoluto.AB11SolicitacaoAberturaSessao()
                                .comMedidorNumero(nm)
                                .comSenha(password.getText().toString())
                                .build((tipo_medidor == TipoMedidor.ABOSOLUTO));
                        enviarComando(comando);
                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        if (imm != null)
                            imm.hideSoftInputFromWindow(alertText.getWindowToken(), 0);
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        if (imm != null)
                            imm.hideSoftInputFromWindow(alertText.getWindowToken(), 0);
                        // do nothing
                    }
                }).create();
        alertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertDialog.show();
    }

    private void processData() {
        final byte[] data = mBytesReceived.toByteArray();
        RespostaAbsoluto respostaAbsoluto = new RespostaAbsoluto(data);
        if (respostaAbsoluto.isOcorrencia()) {
            processarRespostaOcorrencia(respostaAbsoluto);
            leuMedidoresIndividuais = false;
        } else if (respostaAbsoluto.isLeituraConfiguracaoMedidor()) {
            if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
                processarRespostaConfiguracaoMedidorAbsoluto(respostaAbsoluto);
            } else {
                processarRespostaConfiguracaoMedidorEasy(respostaAbsoluto);
            }
        } else if (respostaAbsoluto.isLeituraParametrosHospedeiro()) {
            processarRespostaLeituraParametrosHospedeiro(respostaAbsoluto);
        } else if (respostaAbsoluto.isLeituraStatusMedidor()) {
            processarRespostaLeituraStatusMedidor(respostaAbsoluto);
        } else if (respostaAbsoluto.isCorteReligamento()) {
            processarRespostaCorteReligamento(respostaAbsoluto);
        } else if (respostaAbsoluto.isAberturaSessao()) {
            processarRespostaAberturaSessao(data[7], respostaAbsoluto);
        } else if (respostaAbsoluto.isAbnt21()) {
            if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
                processarRespostaAbnt21Absoluto(respostaAbsoluto);
            } else {
                processarRespostaAbnt21(respostaAbsoluto);
            }
        } else if (respostaAbsoluto.isAbnt87()) {
            processarRespostaAbnt87(respostaAbsoluto);
            if (TipoOperacao.NomeUnidade == funcaoEmExecucao) {
                Toast.makeText(this, "Nome do ponto alterado com sucesso.", Toast.LENGTH_SHORT).show();
                funcaoEmExecucao = TipoOperacao.FIM;
                mRefreshStatus.callOnClick();
            }
        } else if (respostaAbsoluto.isAbnt73()) {
            processarRespostaAbnt73(respostaAbsoluto);
            if (funcaoEmExecucao == TipoOperacao.IntervaloMM) {
                Toast.makeText(this, "Intervalo da MM alterado com sucesso.", Toast.LENGTH_SHORT).show();
                funcaoEmExecucao = TipoOperacao.FIM;
                mRefreshStatus.callOnClick();
            }
        } else if (respostaAbsoluto.isRespostaOcorrencias()) {
            mRefreshStatus.clearAnimation();
            leuMedidoresIndividuais = false;
            enviarLeituraMedidoresIndividuais();
        } else if (respostaAbsoluto.isTensaoReferencia()) {
            enviarTipoLigacao(medidorSelecionado.numero);
        } else if (respostaAbsoluto.isLeituraGenerica()) {
            Toast.makeText(this, "QEE parametrizado com sucesso.", Toast.LENGTH_SHORT).show();
        } else if (respostaAbsoluto.isRespostaData()) {
            ajustaDataHora(false, medidorSelecionado);
        } else if (respostaAbsoluto.isRespostaHora()) {
            Toast.makeText(this, "Data e hora ajustada com sucesso.", Toast.LENGTH_SHORT).show();
            funcaoEmExecucao = TipoOperacao.FIM;
            mRefreshStatus.callOnClick();
        } else if (respostaAbsoluto.isRespostaReset()) {
            Toast.makeText(this, "Reset dos registradores realizado com sucesso.", Toast.LENGTH_SHORT).show();
        } else if (respostaAbsoluto.isRespostaLeituraParametros()) {
            processarRespostaAbnt51(respostaAbsoluto);
        } else if (respostaAbsoluto.isRespostaMemoriaMassaQEE()) {
            if (funcaoEmExecucao == TipoOperacao.ParametrosQEE) {
                enviarQEEParametrizacao(respostaAbsoluto, medidorSelecionado.numero);
            } else if (funcaoEmExecucao == TipoOperacao.MonitoramentoDeTransformador) {
                processarRespostaEB11ParaMonitoramento(respostaAbsoluto);
            } else {
                processarRespostaEB11(respostaAbsoluto);
            }
        } else if (respostaAbsoluto.isRespostaMemoriaMassa()) {
            processarRespostaAbnt52(respostaAbsoluto);
        } else if (respostaAbsoluto.isAb08()) {
            processarAB08(respostaAbsoluto);
        } else if (rodandoCarga) {
            processarCargaDePrograma(respostaAbsoluto);
        } else if (respostaAbsoluto.isEB90()) {
            processaEB90(respostaAbsoluto);
        } else if (respostaAbsoluto.isEB92()) {
            processaEB92(respostaAbsoluto);
        } else {
            processarRespostaNaoTratada(data);
        }
    }

    private void enviaEB90() {
        Log.i("mEB90", mEB90.toString());
        enviarComando(mEB90.build());
        rodandoEB90 = true;
        dialogMonitoramento.findViewById(R.id.monitoramento_loading).setVisibility(View.VISIBLE);
        dialogMonitoramento.findViewById(R.id.monitoramento_btn_luz).setEnabled(false);
        dialogMonitoramento.findViewById(R.id.monitoramento_btn_forca_1).setEnabled(false);
        dialogMonitoramento.findViewById(R.id.monitoramento_btn_forca_2).setEnabled(false);
        dialogMonitoramento.findViewById(R.id.monitoramento_enviar_alteracao).setEnabled(false);
    }

    private void processaEB90(RespostaAbsoluto respostaAbsoluto) {
        RespostaAbsoluto.LeituraEB90 mLeitura = respostaAbsoluto.interpretaEB90();
        Log.i("pcEB90", mLeitura.toString());
        mEB90.setLeitura(true);

        rodandoEB90 = false;
        dialogMonitoramento.findViewById(R.id.monitoramento_loading).setVisibility(View.INVISIBLE);
        dialogMonitoramento.findViewById(R.id.monitoramento_btn_luz).setEnabled(true);
        dialogMonitoramento.findViewById(R.id.monitoramento_btn_forca_1).setEnabled(true);
        dialogMonitoramento.findViewById(R.id.monitoramento_btn_forca_2).setEnabled(true);
        dialogMonitoramento.findViewById(R.id.monitoramento_enviar_alteracao).setEnabled(true);

        Spinner spPotenciaNominal = dialogMonitoramento.findViewById(R.id.monitoramento_potencia_nominal);

        switch (mLeitura.transformadorLido) {
            case 0:
                dialogMonitoramento.findViewById(R.id.monitoramento_btn_luz).setEnabled(false);
                break;
            case 1:
                dialogMonitoramento.findViewById(R.id.monitoramento_btn_forca_1).setEnabled(false);
                break;
            case 2:
                dialogMonitoramento.findViewById(R.id.monitoramento_btn_forca_2).setEnabled(false);
                break;
        }
        switch (mLeitura.potenciaNominal) {
            case 250:
            case 1125:
                spPotenciaNominal.setSelection(0);
                break;
            case 375:
            case 1500:
                spPotenciaNominal.setSelection(1);
                break;
            case 500:
            case 2250:
                spPotenciaNominal.setSelection(2);
                break;
            case 750:
                spPotenciaNominal.setSelection(3);
                break;
            case 1000:
                spPotenciaNominal.setSelection(4);
                break;

        }

        atualizaDialogMonitoramento(mLeitura);
    }

    private void atualizaDialogMonitoramento(RespostaAbsoluto.LeituraEB90 mLeitura) {
        mEB90.setSobretensaoA(mLeitura.nivelSobretensaoFaseA);
        mEB90.setSobretensaoB(mLeitura.nivelSobretensaoFaseB);
        mEB90.setSobretensaoC(mLeitura.nivelSobretensaoFaseC);
        mEB90.setSubtensaoA(mLeitura.nivelSubtensaoFaseA);
        mEB90.setSubtensaoB(mLeitura.nivelSubtensaoFaseB);
        mEB90.setSubtensaoC(mLeitura.nivelSubtensaoFaseC);

        Switch switchPapelTermoestabilizado = dialogMonitoramento.findViewById(R.id.monitoramento_papel_termoestabilizado);
        TextView tvTemperaturaNivel1 = dialogMonitoramento.findViewById(R.id.monitoramento_temperatura_nivel_1_result);
        TextView tvTemperaturaNivel2 = dialogMonitoramento.findViewById(R.id.monitoramento_temperatura_nivel_2_result);
        TextView tvExpoenteDoOleo = dialogMonitoramento.findViewById(R.id.monitoramento_expoente_do_oleo_result);
        TextView tvExpoenteDoEnrolamento = dialogMonitoramento.findViewById(R.id.monitoramento_expoente_do_enrolamento_result);
        TextView tvConstanteDoTempoDoOleo = dialogMonitoramento.findViewById(R.id.monitoramento_constante_tempo_oleo_result);
        TextView tvConstanteDoTempoDoEnrolamento = dialogMonitoramento.findViewById(R.id.monitoramento_constante_tempo_enrolamento_result);
        TextView tvK11 = dialogMonitoramento.findViewById(R.id.monitoramento_k11_result);
        TextView tvK21 = dialogMonitoramento.findViewById(R.id.monitoramento_k21_result);
        TextView tvK22 = dialogMonitoramento.findViewById(R.id.monitoramento_k22_result);
        TextView tvConstanteDiscretizacao = dialogMonitoramento.findViewById(R.id.monitoramento_constante_discretizacao_result);
        TextView tvRelacaoPerdasCorrenteNominalVazio = dialogMonitoramento.findViewById(R.id.monitoramento_relacao_perdas_corrente_nominal_vazio_result);
        TextView tvTemperaturaPontoMaisQuenteNominal = dialogMonitoramento.findViewById(R.id.monitoramento_temperatura_ponto_mais_quente_nominal_result);
        TextView tvElevacaoTemperaturaTopoOleo = dialogMonitoramento.findViewById(R.id.monitoramento_elevacao_temperatura_topo_oleo_result);
        TextView tvGradienteDiferencaTemperaturasPontoMaisQuenteTopoOleo = dialogMonitoramento.findViewById(R.id.monitoramento_gradiente_diferenca_temperaturas_ponto_mais_quente_topo_oleo_result);
        TextView tvNivel1AlarmeSobretemperaturaDoPontoMaisQuenteDoTransformador = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_1_alarme_sobretemperatura_do_mais_quente_do_transformador_result);
        TextView tvNivel2AlarmeSobretemperaturaDoPontoMaisQuenteDoTransformador = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_2_alarme_sobretemperatura_do_mais_quente_do_transformador_result);
        TextView tvSobrecorrenteFaseA = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobrecorrente_fase_A_result);
        TextView tvSobrecorrenteFaseB = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobrecorrente_fase_B_result);
        TextView tvSobrecorrenteFaseC = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobrecorrente_fase_C_result);
        EditText etSobretensaoFaseA = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobretensao_fase_A_result);
        EditText etSobretensaoFaseB = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobretensao_fase_B_result);
        EditText etSobretensaoFaseC = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobretensao_fase_C_result);
        EditText etSubtensaoFaseA = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_subtensao_fase_A_result);
        EditText etSubtensaoFaseB = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_subtensao_fase_B_result);
        EditText etSubtensaoFaseC = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_subtensao_fase_C_result);

        switchPapelTermoestabilizado.setChecked(mLeitura.temPapeltermoestabilizado);
        tvTemperaturaNivel1.setText(String.valueOf(mLeitura.valorTemperaturaNivel1));
        tvTemperaturaNivel2.setText(String.valueOf(mLeitura.valorTemperaturaNivel2));
        tvExpoenteDoOleo.setText(String.valueOf(mLeitura.expoenteDoOleo));
        tvExpoenteDoEnrolamento.setText(String.valueOf(mLeitura.expoenteDoEnrolamento));
        tvConstanteDoTempoDoOleo.setText(String.valueOf(mLeitura.constanteDeTempoDoOleo));
        tvConstanteDoTempoDoEnrolamento.setText(String.valueOf(mLeitura.constanteDeTempoDoEnrolamento));
        tvK11.setText(String.valueOf(mLeitura.constanteDoModeloTermico11));
        tvK21.setText(String.valueOf(mLeitura.constanteDoModeloTermico21));
        tvK22.setText(String.valueOf(mLeitura.constanteDoModeloTermico22));
        tvConstanteDiscretizacao.setText(String.valueOf(mLeitura.constanteDeDiscretizacao));
        tvRelacaoPerdasCorrenteNominalVazio.setText(String.valueOf(mLeitura.relacaoDePerdasDeCargaNaCorrenteNominalParaPerdasEmVazio));
        tvTemperaturaPontoMaisQuenteNominal.setText(String.valueOf(mLeitura.temperaturaDoPontoMaisQuenteNominal));
        tvElevacaoTemperaturaTopoOleo.setText(String.valueOf(mLeitura.elevacaoDeTemperaturaDoTopoDoOleo));
        tvGradienteDiferencaTemperaturasPontoMaisQuenteTopoOleo.setText(String.valueOf(mLeitura.gradienteObtidoPelaDiferencaEntreTemperaturaDoPontoMaisQuenteEDoTopoDoOleo));
        tvNivel1AlarmeSobretemperaturaDoPontoMaisQuenteDoTransformador.setText(String.valueOf(mLeitura.nivel1DeAlarmeParaSobretemperaturaDoPontoMaisQuenteDoTransformador));
        tvNivel2AlarmeSobretemperaturaDoPontoMaisQuenteDoTransformador.setText(String.valueOf(mLeitura.nivel2DeAlarmeParaSobretemperaturaDoPontoMaisQuenteDoTransformador));
        tvSobrecorrenteFaseA.setText(String.valueOf(mLeitura.nivelSobrecorrenteFaseA));
        tvSobrecorrenteFaseB.setText(String.valueOf(mLeitura.nivelSobrecorrenteFaseB));
        tvSobrecorrenteFaseC.setText(String.valueOf(mLeitura.nivelSobrecorrenteFaseC));
        etSobretensaoFaseA.setHint(String.valueOf(mLeitura.nivelSobretensaoFaseA));
        etSobretensaoFaseA.setText("");
        etSobretensaoFaseB.setHint(String.valueOf(mLeitura.nivelSobretensaoFaseB));
        etSobretensaoFaseB.setText("");
        etSobretensaoFaseC.setHint(String.valueOf(mLeitura.nivelSobretensaoFaseC));
        etSobretensaoFaseC.setText("");
        etSubtensaoFaseA.setHint(String.valueOf(mLeitura.nivelSubtensaoFaseA));
        etSubtensaoFaseA.setText("");
        etSubtensaoFaseB.setHint(String.valueOf(mLeitura.nivelSubtensaoFaseB));
        etSubtensaoFaseB.setText("");
        etSubtensaoFaseC.setHint(String.valueOf(mLeitura.nivelSubtensaoFaseC));
        etSubtensaoFaseC.setText("");
    }

    private void createDialogMonitoramento() {
        dialogMonitoramento = new Dialog(this);
        dialogMonitoramento.setContentView(R.layout.dialog_monitoramento_easy_trafo);
        if (dialogMonitoramento.getWindow() != null)
            dialogMonitoramento.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        someComBotoesDoMonitoramento();

        CheckBox cbPotenciaNominal = dialogMonitoramento.findViewById(R.id.monitoramento_potencia_nominal_check);
        Spinner spPotenciaNominal = dialogMonitoramento.findViewById(R.id.monitoramento_potencia_nominal);
        CheckBox cbPapelTermoestabilizado = dialogMonitoramento.findViewById(R.id.monitoramento_papel_termoestabilizado_check);
        Switch switchPapelTermoestabilizado = dialogMonitoramento.findViewById(R.id.monitoramento_papel_termoestabilizado);
        CheckBox cbSobreTensaoA = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobretensao_fase_A_check);
        EditText etSobreTensaoA = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobretensao_fase_A_result);
        CheckBox cbSobreTensaoB = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobretensao_fase_B_check);
        EditText etSobreTensaoB = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobretensao_fase_B_result);
        CheckBox cbSobreTensaoC = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobretensao_fase_C_check);
        EditText etSobreTensaoC = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobretensao_fase_C_result);
        CheckBox cbSubTensaoA = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_subtensao_fase_A_check);
        EditText etSubTensaoA = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_subtensao_fase_A_result);
        CheckBox cbSubTensaoB = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_subtensao_fase_B_check);
        EditText etSubTensaoB = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_subtensao_fase_B_result);
        CheckBox cbSubTensaoC = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_subtensao_fase_C_check);
        EditText etSubTensaoC = dialogMonitoramento.findViewById(R.id.monitoramento_nivel_subtensao_fase_C_result);

        dialogMonitoramento.findViewById(R.id.monitoramento_btn_close).setOnClickListener(v -> dialogMonitoramento.dismiss());

        dialogMonitoramento.findViewById(R.id.monitoramento_btn_luz).setOnClickListener(v -> {
            mEB90.setNumeroTransformador(0);
            enviaEB90();
        });

        dialogMonitoramento.findViewById(R.id.monitoramento_btn_forca_1).setOnClickListener(v -> {
            mEB90.setNumeroTransformador(1);
            enviaEB90();
        });

        dialogMonitoramento.findViewById(R.id.monitoramento_btn_forca_2).setOnClickListener(v -> {
            mEB90.setNumeroTransformador(2);
            enviaEB90();
        });

        dialogMonitoramento.findViewById(R.id.monitoramento_enviar_alteracao).setOnClickListener(v -> {
            mEB90.setLeitura(false);

            //Papel Termoestabilizado
            mEB90.setAlteracaoPapelTermoestabilizado(cbPapelTermoestabilizado.isChecked());
            mEB90.setColocarPapelTermoestabilizado(switchPapelTermoestabilizado.isChecked());

            //Potencia Nominal
            mEB90.setAlteracaoPotenciaNominal(cbPotenciaNominal.isChecked());
            mEB90.setPotenciaNominalTransformador(traduzSpinnerPotenciaNominalMonitoramento(spPotenciaNominal.getSelectedItemPosition()));

            mEB90.setAlteracaoSobretensaoA(cbSobreTensaoA.isChecked());
            if (!etSobreTensaoA.getText().toString().isEmpty()) {
                int sobreTensaoA = Integer.parseInt(etSobreTensaoA.getText().toString());
                if (testaSobreTensaoMonitoramento(sobreTensaoA)) {
                    mEB90.setSobretensaoA(sobreTensaoA * 10);
                } else {
                    Toast.makeText(DeviceActivity.this, "Nível de Sobretensão deve estar entre 100 e 300", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            mEB90.setAlteracaoSobretensaoB(cbSobreTensaoB.isChecked());
            if (!etSobreTensaoB.getText().toString().isEmpty()) {
                int sobreTensaoB = Integer.parseInt(etSobreTensaoB.getText().toString());
                if (testaSobreTensaoMonitoramento(sobreTensaoB)) {
                    mEB90.setSobretensaoB(sobreTensaoB * 10);
                } else {
                    Toast.makeText(DeviceActivity.this, "Nível de Sobretensão deve estar entre 100 e 300", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            mEB90.setAlteracaoSobretensaoC(cbSobreTensaoC.isChecked());
            if (!etSobreTensaoC.getText().toString().isEmpty()) {
                int sobreTensaoC = Integer.parseInt(etSobreTensaoC.getText().toString());
                if (testaSobreTensaoMonitoramento(sobreTensaoC)) {
                    mEB90.setSobretensaoC(sobreTensaoC * 10);
                } else {
                    Toast.makeText(DeviceActivity.this, "Nível de Sobretensão deve estar entre 100 e 300", Toast.LENGTH_SHORT).show();
                    return;
                }
            }


            mEB90.setAlteracaoSubtensaoA(cbSubTensaoA.isChecked());
            if (!etSubTensaoA.getText().toString().isEmpty()) {
                int subTensaoA = Integer.parseInt(etSubTensaoA.getText().toString());
                if (testaSubTensaoMonitoramento(subTensaoA)) {
                    mEB90.setSubtensaoA(subTensaoA * 10);
                } else {
                    Toast.makeText(DeviceActivity.this, "Nível de Subtensão deve estar entre 0 e 200", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            mEB90.setAlteracaoSubtensaoB(cbSubTensaoB.isChecked());

            if (!etSubTensaoB.getText().toString().isEmpty()) {
                int subTensaoB = Integer.parseInt(etSubTensaoB.getText().toString());
                if (testaSubTensaoMonitoramento(subTensaoB)) {
                    mEB90.setSubtensaoB(subTensaoB * 10);
                } else {
                    Toast.makeText(DeviceActivity.this, "Nível de Subtensão deve estar entre 0 e 200", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            mEB90.setAlteracaoSubtensaoC(cbSubTensaoC.isChecked());
            if (!etSubTensaoC.getText().toString().isEmpty()) {
                int subTensaoC = Integer.parseInt(etSubTensaoC.getText().toString());
                if (testaSubTensaoMonitoramento(subTensaoC)) {
                    mEB90.setSubtensaoC(subTensaoC * 10);
                } else {
                    Toast.makeText(DeviceActivity.this, "Nível de Subtensão deve estar entre 0 e 200", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            enviaEB90();

            cbPapelTermoestabilizado.setChecked(false);
            cbPotenciaNominal.setChecked(false);
            cbSobreTensaoA.setChecked(false);
            cbSobreTensaoB.setChecked(false);
            cbSobreTensaoC.setChecked(false);
            cbSubTensaoA.setChecked(false);
            cbSubTensaoB.setChecked(false);
            cbSubTensaoC.setChecked(false);
        });

        if (dadosCabecalhoQEE.tipoLigacao == 6) { //verificando se é delta aterrado
            dialogMonitoramento.findViewById(R.id.monitoramento_btn_forca_1).setVisibility(View.VISIBLE);
            dialogMonitoramento.findViewById(R.id.monitoramento_btn_forca_2).setVisibility(View.VISIBLE);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.potencia_nominal_monitoramento_menor_que_cem));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spPotenciaNominal.setAdapter(adapter);
        } else if (dadosCabecalhoQEE.tipoLigacao == 4) {
            dialogMonitoramento.findViewById(R.id.monitoramento_btn_luz).setVisibility(View.GONE);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.potencia_nominal_monitoramento_menor_que_cem));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spPotenciaNominal.setAdapter(adapter);
        } else {
            dialogMonitoramento.findViewById(R.id.monitoramento_btn_luz).setVisibility(View.GONE);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.potencia_nominal_monitoramento_maior_que_cem));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spPotenciaNominal.setAdapter(adapter);
        }

        dialogMonitoramento.setCancelable(false);
        dialogMonitoramento.create();
        dialogMonitoramento.show();
    }

    private boolean testaSobreTensaoMonitoramento(int sobretensao) {
        return sobretensao >= 100 && sobretensao <= 300;
    }

    private boolean testaSubTensaoMonitoramento(int subtensao) {
        return subtensao >= 0 && subtensao <= 240;
    }

    private void someComBotoesDoMonitoramento() {
        if (tipo_medidor == TipoMedidor.EASY_VOLT) {
            dialogMonitoramento.findViewById(R.id.monitoramento_potencia_nominal_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_potencia_nominal).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_papel_termoestabilizado_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_papel_termoestabilizado).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_temperatura_nivel_1_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_temperatura_nivel_1_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_temperatura_nivel_2_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_temperatura_nivel_2_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_expoente_do_oleo_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_expoente_do_oleo_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_expoente_do_enrolamento_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_expoente_do_enrolamento_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_constante_tempo_oleo_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_constante_tempo_oleo_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_constante_tempo_enrolamento_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_constante_tempo_enrolamento_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_k11_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_k11_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_k21_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_k21_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_k22_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_k22_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_constante_discretizacao_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_constante_discretizacao_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_relacao_perdas_corrente_nominal_vazio_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_relacao_perdas_corrente_nominal_vazio_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_temperatura_ponto_mais_quente_nominal_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_temperatura_ponto_mais_quente_nominal_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_elevacao_temperatura_topo_oleo_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_elevacao_temperatura_topo_oleo_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_gradiente_diferenca_temperaturas_ponto_mais_quente_topo_oleo_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_gradiente_diferenca_temperaturas_ponto_mais_quente_topo_oleo_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_nivel_1_alarme_sobretemperatura_do_mais_quente_do_transformador_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_nivel_1_alarme_sobretemperatura_do_mais_quente_do_transformador_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_nivel_2_alarme_sobretemperatura_do_mais_quente_do_transformador_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_nivel_2_alarme_sobretemperatura_do_mais_quente_do_transformador_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobrecorrente_fase_A_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobrecorrente_fase_A_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobrecorrente_fase_B_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobrecorrente_fase_B_result).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobrecorrente_fase_C_check).setVisibility(View.GONE);
            dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobrecorrente_fase_C_result).setVisibility(View.GONE);
        }
        /*
        dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobretensao_fase_A_result);
        dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobretensao_fase_B_result);
        dialogMonitoramento.findViewById(R.id.monitoramento_nivel_sobretensao_fase_C_result);
        dialogMonitoramento.findViewById(R.id.monitoramento_nivel_subtensao_fase_A_result);
        dialogMonitoramento.findViewById(R.id.monitoramento_nivel_subtensao_fase_B_result);
        dialogMonitoramento.findViewById(R.id.monitoramento_nivel_subtensao_fase_C_result);
        */
    }

    private void processarRespostaEB11ParaMonitoramento(RespostaAbsoluto respostaAbsoluto) {
        mTimeoutHandler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
        progressDialog.dismiss();

        dadosCabecalhoQEE = respostaAbsoluto.interpretaRespostaAB11Cabecalho();
        mEB90 = new ComandoAbsoluto.EB90().comNumerMedidor(monitoramentoNumeroEasy);
        if (dadosCabecalhoQEE.tipoLigacao == 6 || dadosCabecalhoQEE.tipoLigacao == 1) {
            createDialogMonitoramento();
            enviaEB90();
        } else {
            Toast.makeText(mBluetoothLeService, "Monitoramento não é válido para este tipo de ligação.\nVerifique a parametrização QEE", Toast.LENGTH_LONG).show();
        }
    }

    private int traduzSpinnerPotenciaNominalMonitoramento(int selected) {
        int i = 0;
        switch (selected) {
            case 0:
                if (dadosCabecalhoQEE.tipoLigacao == 6 || dadosCabecalhoQEE.tipoLigacao == 4) {
                    i = 250;
                } else {
                    i = 1125;
                }
                break;
            case 1:
                if (dadosCabecalhoQEE.tipoLigacao == 6 || dadosCabecalhoQEE.tipoLigacao == 4) {
                    i = 375;
                } else {
                    i = 1500;
                }
                break;
            case 2:
                if (dadosCabecalhoQEE.tipoLigacao == 6 || dadosCabecalhoQEE.tipoLigacao == 4) {
                    i = 500;
                } else {
                    i = 2250;
                }
                break;
            case 3:
                i = 750;
                break;
            case 4:
                i = 1000;
                break;
        }
        return i;
    }

    private void processarRespostaEB11(RespostaAbsoluto respostaAbsoluto) {
        Log.d(TAG, "## Resposta EB11 ...");
        byte pacote = 1;
        if (funcaoEmExecucao == TipoOperacao.InicioMemoriaMassaQEE) {
            funcaoEmExecucao = TipoOperacao.MemoriaMassaQEE;
            dadosCabecalhoQEE = respostaAbsoluto.interpretaRespostaAB11Cabecalho();
            mRespostaQEE = new byte[dadosCabecalhoQEE.numeroBytes];
            progressDialog = AlertBuilder.createProgressDialog(this, "Enviando pacote " + dadosCabecalhoQEE.pacoteAtual + " de " + dadosCabecalhoQEE.numeroPacotes);
            progressDialog.show();
            showTimeOutMM();
            enviaComandoEB11(pacote);
        } else {
            int tamanho = (respostaAbsoluto.mData.length - 11);
            if ((tamanho + dadosCabecalhoQEE.posicaoAtual) > mRespostaQEE.length) {
                tamanho = mRespostaQEE.length - dadosCabecalhoQEE.posicaoAtual;
            }
            System.arraycopy(respostaAbsoluto.getData(), 9, mRespostaQEE, dadosCabecalhoQEE.posicaoAtual, tamanho);
            dadosCabecalhoQEE.posicaoAtual += tamanho;
            //mRespostaComposta.add(respostaAbsoluto.getData());
            if (!respostaAbsoluto.interpretaRespostaEB11()) {
                dadosCabecalhoQEE.pacoteAtual++;
                progressDialog.setMessage("Enviando pacote " + dadosCabecalhoQEE.pacoteAtual + " de " + dadosCabecalhoQEE.numeroPacotes);
                showTimeOutMM();
                enviaComandoEB11(pacote);
            } else {
                mMMRunnable = null;
                mHandler.removeCallbacksAndMessages(null);
                progressDialog.setMessage("Aguarde, Processando QEE.");
                processaQEE = respostaAbsoluto.preparaQEE();
                //long numero_registros = dadosCabecalhoQEE.numeroRegistros;
                for (int j = 0; j < mRespostaQEE.length; j += 31) {
                    processaQEE.preencheDados(Arrays.copyOfRange(mRespostaQEE, j, j + 31));
                }
                Log.i(TAG, "Data de Inicio: " + dadosCabecalhoQEE.textdataInicio + " | " + dadosCabecalhoQEE.textdataFim);

                Locale br = Locale.forLanguageTag("BR");

                StringBuilder dadosCSV = new StringBuilder();
                StringBuilder dadosCSVCompleto = new StringBuilder();
                dadosCSV.append("StarMeasure\n");
                dadosCSV.append("Ponto;" + mDeviceName.substring(4) + "\n");
                dadosCSV.append("Area;" + "\n");
                dadosCSV.append("Numero de Registros;" + dadosCabecalhoQEE.numeroRegistros + "\n");
                dadosCSV.append("Intervalo da memoria QEE;10\n");
                dadosCSV.append("Data/Hora de Início;" + dadosCabecalhoQEE.textdataInicio + "\n");
                dadosCSV.append("Data/Hora de Final;" + dadosCabecalhoQEE.textdataFim + "\n");
                dadosCSV.append("TP Original;1\n");
                dadosCSV.append("TP;1\n");

                String[] items = new String[]{"Indefinido", "Estrela", "Delta", "Bifásico", "Monofásico", "Série/paralelo", "Delta aterrado"};
                dadosCSV.append("Tipo de Ligação;" + items[dadosCabecalhoQEE.tipoLigacao] + "\n");
                dadosCSV.append("Data;Hora;VA;VB;VC;Status\n");
                String format = "%s;%s;%.2f;%.2f;%.2f;%s;\n";

                dadosCSVCompleto.append("NS do medidor;" + respostaAbsoluto.getNumeroMedidor() + "\n");
                dadosCSVCompleto.append("Data/Hora de Início;" + dadosCabecalhoQEE.textdataInicio + "\n");
                dadosCSVCompleto.append("Data/Hora de Final;" + dadosCabecalhoQEE.textdataFim + "\n");
                dadosCSVCompleto.append("Numero de Registros;" + dadosCabecalhoQEE.numeroRegistros + "\n");
                dadosCSVCompleto.append("Numero de Registros Válidos;" + dadosCabecalhoQEE.numeroRegistrosValidos + "\n");
                //dadosCSVCompleto.append("Intervalo da memoria QEE;" + String.valueOf(dadosCabecalhoQEE.intevaloQEE) + "\n");
                dadosCSVCompleto.append("Intervalo da memoria QEE;10\n");
                dadosCSVCompleto.append("Tensão de Referência;" + String.format("%.2f", dadosCabecalhoQEE.tensaoReferencia) + "\n");
                dadosCSVCompleto.append("Percentual para a tensão precária superior;" + String.format(br, "%.2f", dadosCabecalhoQEE.percentualTensaoPrecariaSuperior) + "\n");
                dadosCSVCompleto.append("Percentual para a tensão precária inferior;" + String.format(br, "%.2f", dadosCabecalhoQEE.percentualTensaoPrecariaInferior) + "\n");
                dadosCSVCompleto.append("Percentual para a tensão crítica superior;" + String.format(br, "%.2f", dadosCabecalhoQEE.percentualTensaoCriticaSuperior) + "\n");
                dadosCSVCompleto.append("Percentual para a tensão crítica inferior;" + String.format(br, "%.2f", dadosCabecalhoQEE.percentualTensaoCriticaInferior) + "\n");
                dadosCSVCompleto.append("DRP(%);" + String.format(br, "%.2f", dadosCabecalhoQEE.DRP) + "\n");
                dadosCSVCompleto.append("DRC(%);" + String.format(br, "%.2f", dadosCabecalhoQEE.DRC) + "\n");
                dadosCSVCompleto.append("DTT95%;" + String.format(br, "%.2f", dadosCabecalhoQEE.DTT95) + "\n");
                dadosCSVCompleto.append("FD95%;" + String.format(br, "%.2f", dadosCabecalhoQEE.FD95) + "\n");
                dadosCSVCompleto.append("Tipo de Ligação;" + items[dadosCabecalhoQEE.tipoLigacao] + "\n");
                dadosCSVCompleto.append("Data;Hora;VA;VB;VC;Desequilíbio;DHTA(%);DHTB(%);DHTC(%);FPA(%);FPB(%);FPC(%);FP3(%);f(Hz);T(°C);VTCDM;VTCDT;Varf;Interrupções;Status\n");

                String formatCompleto = "%s;%s;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%d;%d;%d;%d;%s;\n";


                Calendar local_date = dadosCabecalhoQEE.dataInicio;
                // vou plotar os resultados...
                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

                for (int i = 0; i < processaQEE.mDadosQEE.size(); i++) {
                    RespostaAbsoluto.DadosQEE dados = processaQEE.mDadosQEE.get(i);
                    local_date.add(Calendar.MINUTE, 10);
                    String data = dateFormat.format(local_date.getTime());
                    String hora = timeFormat.format(local_date.getTime());
                    String debug = "###### ITEM: " + i + " > Data: " + data + " " + hora + "\r\n" +
                            String.format(br, "Eventos......... %d | %d | %d | %d | %d", dados.status, dados.VTCDMomentaneos, dados.VTCDTemporarios, dados.variacoesFrequencia, dados.interrupcoes) + "\r\n" +
                            String.format(br, "Frequencia...... %d | %.2f", dados.frequencia, (float) dados.frequencia * dadosCabecalhoQEE.constanteMultiplicacaoFrequencia) + "\r\n" +
                            String.format(br, "Temperatura..... %d | %.2f", dados.temperatura, (float) dados.temperatura * dadosCabecalhoQEE.constanteMultiplicacaoTemperatura) + "\r\n" +
                            String.format(br, "Tensa A......... %d | %.2f", dados.tensaoA, (float) dados.tensaoA * dadosCabecalhoQEE.constanteMultiplicacaoTensao) + "\r\n" +
                            String.format(br, "Tensa B......... %d | %.2f", dados.tensaoB, (float) dados.tensaoB * dadosCabecalhoQEE.constanteMultiplicacaoTensao) + "\r\n" +
                            String.format(br, "Tensa C......... %d | %.2f", dados.tensaoC, (float) dados.tensaoC * dadosCabecalhoQEE.constanteMultiplicacaoTensao) + "\r\n" +
                            String.format(br, "Desequilibrio... %d | %.2f", dados.desequilibrio, (float) dados.desequilibrio * dadosCabecalhoQEE.constanteMultiplicacaoDesequilibrio) + "\r\n" +
                            String.format(br, "DHTA............ %d | %.2f", dados.DHTA, (float) dados.DHTA * dadosCabecalhoQEE.constanteMultiplicacaoHarmonicas) + "\r\n" +
                            String.format(br, "DHTB............ %d | %.2f", dados.DHTB, (float) dados.DHTB * dadosCabecalhoQEE.constanteMultiplicacaoHarmonicas) + "\r\n" +
                            String.format(br, "DHTC............ %d | %.2f", dados.DHTC, (float) dados.DHTC * dadosCabecalhoQEE.constanteMultiplicacaoHarmonicas) + "\r\n" +
                            String.format(br, "Fat. PotA....... %d | %.2f", dados.fatPotenciaA, (float) dados.fatPotenciaA * dadosCabecalhoQEE.constanteMultiplicacaoFatorPotencia) + "\r\n" +
                            String.format(br, "Fat. PotB....... %d | %.2f", dados.fatPotenciaA, (float) dados.fatPotenciaA * dadosCabecalhoQEE.constanteMultiplicacaoFatorPotencia) + "\r\n" +
                            String.format(br, "Fat. PotC....... %d | %.2f", dados.fatPotenciaA, (float) dados.fatPotenciaA * dadosCabecalhoQEE.constanteMultiplicacaoFatorPotencia) + "\r\n" +
                            String.format(br, "Fat. Pot.Trif... %d | %.2f", dados.fatPotenciaTrifasico, (float) dados.fatPotenciaTrifasico * dadosCabecalhoQEE.constanteMultiplicacaoFatorPotencia);

                    Log.i(TAG, debug);

                    String status = (dados.status > 0) ? "Inválido" : "Válido";
                    dadosCSV.append(
                            String.format(format, data, hora,
                                    (float) dados.tensaoA * dadosCabecalhoQEE.constanteMultiplicacaoTensao,
                                    (float) dados.tensaoB * dadosCabecalhoQEE.constanteMultiplicacaoTensao,
                                    (float) dados.tensaoC * dadosCabecalhoQEE.constanteMultiplicacaoTensao,
                                    status));
                    dadosCSVCompleto.append(
                            String.format(formatCompleto, data, hora,
                                    (float) dados.tensaoA * dadosCabecalhoQEE.constanteMultiplicacaoTensao,
                                    (float) dados.tensaoB * dadosCabecalhoQEE.constanteMultiplicacaoTensao,
                                    (float) dados.tensaoC * dadosCabecalhoQEE.constanteMultiplicacaoTensao,
                                    (float) dados.desequilibrio * dadosCabecalhoQEE.constanteMultiplicacaoDesequilibrio,
                                    (float) dados.DHTA * dadosCabecalhoQEE.constanteMultiplicacaoHarmonicas,
                                    (float) dados.DHTB * dadosCabecalhoQEE.constanteMultiplicacaoHarmonicas,
                                    (float) dados.DHTC * dadosCabecalhoQEE.constanteMultiplicacaoHarmonicas,
                                    (float) dados.fatPotenciaA * dadosCabecalhoQEE.constanteMultiplicacaoFatorPotencia,
                                    (float) dados.fatPotenciaB * dadosCabecalhoQEE.constanteMultiplicacaoFatorPotencia,
                                    (float) dados.fatPotenciaC * dadosCabecalhoQEE.constanteMultiplicacaoFatorPotencia,
                                    (float) dados.fatPotenciaTrifasico * dadosCabecalhoQEE.constanteMultiplicacaoFatorPotencia,
                                    (float) dados.frequencia * dadosCabecalhoQEE.constanteMultiplicacaoFrequencia,
                                    (float) dados.temperatura * dadosCabecalhoQEE.constanteMultiplicacaoTemperatura,
                                    dados.VTCDMomentaneos,
                                    dados.VTCDTemporarios,
                                    dados.variacoesFrequencia,
                                    dados.interrupcoes,
                                    status));
                }

                String nomeArquivo = String.format("MemoriaQEETensao_%s_%s_%s.csv", respostaAbsoluto.getNumeroMedidor(), mDeviceName.substring(4).trim(),
                        new SimpleDateFormat("ddMMyyyy'_'HHmmss").format(Calendar.getInstance().getTime()));

                String nomeArquivoCompleto = String.format("MemoriaQEE_%s_%s_%s.csv", respostaAbsoluto.getNumeroMedidor(), mDeviceName.substring(4).trim(),
                        new SimpleDateFormat("ddMMyyyy'_'HHmmss").format(Calendar.getInstance().getTime()));

                if (!Arquivo.salvarArquivo(this, nomeArquivo, dadosCSV.toString())) {
                    Toast.makeText(this, "Erro ao salvar arquivo da memória QEE",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Arquivo de QEE salvo com sucesso:\n" + nomeArquivo,
                            Toast.LENGTH_LONG).show();
                }

                if (!Arquivo.salvarArquivo(this, nomeArquivoCompleto, dadosCSVCompleto.toString())) {
                    Toast.makeText(this, "Erro ao salvar arquivo completo da memória QEE",
                            Toast.LENGTH_SHORT).show();
                } else {

                }

                progressDialog.hide();
                progressDialog.dismiss();
                mHandler.removeCallbacksAndMessages(null);
            }
            //dadosAbnt51 = respostaAbsoluto.interpretaResposta51();
        }
    }

    private void enviarQEEParametrizacao(RespostaAbsoluto respostaAbsoluto, String
            numeroMedidor) {

        dadosCabecalhoQEE = respostaAbsoluto.interpretaRespostaAB11Cabecalho();

        funcaoEmExecucao = TipoOperacao.FIM;
        final View conf_qee = getLayoutInflater().inflate(R.layout.conf_qee, null);
        final EditText tensao = conf_qee.findViewById(R.id.tensao_nominal);
        tensao.setText(String.format("%.2f", dadosCabecalhoQEE.tensaoReferencia));

        final Spinner tipo_ligacao = conf_qee.findViewById(R.id.tipo_ligacao);
        String[] items = new String[]{"Monofásico", "Bifásico", "Estrela", "Delta aterrado"};
        if (tipo_medidor == TipoMedidor.EASY_VOLT) {
            items = new String[]{"Monofásico", "Bifásico", "Estrela"};
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tipo_ligacao.setAdapter(adapter);

        if (dadosCabecalhoQEE.tipoLigacao == 4) {
            tipo_ligacao.setSelection(0);
        } else if (dadosCabecalhoQEE.tipoLigacao == 3) {
            tipo_ligacao.setSelection(1);
        } else if (dadosCabecalhoQEE.tipoLigacao == 1) {
            tipo_ligacao.setSelection(2);
        } else if (dadosCabecalhoQEE.tipoLigacao == 6) {
            tipo_ligacao.setSelection(3);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                android.R.style.Theme_Material_Dialog_Alert);
        AlertDialog alertDialog = builder
                .setTitle("Parametrização da QEE")
                .setMessage("Tipo de ligação atual: " + traduzTipoLigacaoIntToString(dadosCabecalhoQEE.tipoLigacao))
                .setView(conf_qee)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (tensao.getText().toString().isEmpty()) {
                        tensao.setText("0");
                    }
                    int v_tensao = Integer.parseInt(tensao.getText().toString());
                    codigoCanal = traduzTipoLigacaoStringToInt(tipo_ligacao.getSelectedItem().toString());
                    if (v_tensao >= 0) {
                        String num_medidor = "";

                        byte[] comando = new ComandoAbsoluto.ABNT9895()
                                .comMedidorNumero(num_medidor)
                                .build(v_tensao);
                        enviarComando(comando);
                        Log.i("amigo->", "Amigo estou aqui!\n" + "Voltagem: " + v_tensao + "\n" + Util.ByteArrayToHexString(comando));

                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        if (imm != null)
                            imm.hideSoftInputFromWindow(conf_qee.getWindowToken(), 0);
                    } else {
                        Toast.makeText(DeviceActivity.super.getApplicationContext(),
                                "Informe a tensão nominal de 80 a 280.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    if (imm != null)
                        imm.hideSoftInputFromWindow(conf_qee.getWindowToken(), 0);
                    // do nothing
                }).create();


        alertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertDialog.show();
    }

    @Contract(pure = true)
    private int traduzTipoLigacaoStringToInt(String tipoLigacao) {
        int i = 0;

        switch (tipoLigacao) {
            case "Estrela":
                i = 1;
                break;
            case "Delta":
                i = 2;
                break;
            case "Bifásico":
                i = 3;
                break;
            case "Monofásico":
                i = 4;
                break;
            case "Série/paralelo":
                i = 5;
                break;
            case "Delta aterrado":
                i = 6;
                break;
        }
        return i;
    }

    @Contract(pure = true)
    private String traduzTipoLigacaoIntToString(int tipoLigacao) {
        String s = "Indefinido";

        switch (tipoLigacao) {
            case 1:
                s = "Estrela";
                break;
            case 2:
                s = "Delta";
                break;
            case 3:
                s = "Bifásico";
                break;
            case 4:
                s = "Monofásico";
                break;
            case 5:
                s = "Série/paralelo";
                break;
            case 6:
                s = "Delta aterrado";
                break;
        }
        return s;
    }

    private void processarRespostaOcorrencia(RespostaAbsoluto respostaAbsoluto) {
        Toast.makeText(DeviceActivity.this,
                getString(R.string.text_ocorrencia_medidor),
                Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Ocorrência: " + respostaAbsoluto.toString());
        enviarLimparOcorrencia(respostaAbsoluto.getSufixoNumeroMedidor(), respostaAbsoluto.getCodigoOcorrencia());
    }

    private void processarRespostaConfiguracaoMedidorEasy(RespostaAbsoluto respostaAbsoluto) {
        byte[] data = respostaAbsoluto.getData();
        iuMedidor = new byte[]{data[15], data[16], data[17], data[18], data[19]};
        Log.i(TAG, "mIu: " + Util.ByteArrayToHexString(iuMedidor));
        enviarLeitura87();
    }

    private void processarRespostaConfiguracaoMedidorAbsoluto(RespostaAbsoluto respostaAbsoluto) {
        byte[] data = respostaAbsoluto.getData();
        iuMedidor = new byte[]{data[16], data[17], data[18], data[19], data[20]};
        Log.i(TAG, "mIu: " + Util.ByteArrayToHexString(iuMedidor));

        medidorSemRele = (respostaAbsoluto.mData[22] == 0);


        if (medidorSemRele) {
            for (MedidorAbsoluto medidor : medidores) {
                medidor.status = 0x01;
                medidor.semRele = true;
            }
        }

        mMeterListAdapter = new MeterListAdapter(medidores);
        mRecyclerView.setAdapter(mMeterListAdapter);
        mMeterListAdapter.notifyDataSetChanged();

        if (!medidorSemRele) {
            enviarProximoStatusMedidor("99999999");
            animateRefresh();
        }
    }

    private void processarRespostaLeituraParametrosHospedeiro(RespostaAbsoluto resposta) {
        if (leuMedidoresIndividuais)
            return;
        mRespostaComposta.add(resposta.getData());
        mTimeoutHandler.removeCallbacks(mMedidoresIndividuaisRunnable);
        leuMedidoresIndividuais = true;
        progressDialog.dismiss();
        mHandler.removeCallbacksAndMessages(null);
        RespostaAbsoluto respostaComposta = new RespostaAbsoluto(mRespostaComposta);
        medidores = respostaComposta.listaMedidores();

        enviarLeituraConfiguracao();
    }

    private void animateRefresh() {
        Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
        rotation.setRepeatCount(Animation.INFINITE);
        mRefreshStatus.startAnimation(rotation);
    }

    private void processarRespostaLeituraStatusMedidor(RespostaAbsoluto respostaAbsoluto) {
        String numeroMedidor = respostaAbsoluto.getSufixoNumeroMedidor();

        RespostaAbsoluto.LeituraStatusMedidor statusMedidor = respostaAbsoluto.interpretarLeituraStatusMedidor();
        Log.d(TAG, "LeituraStatusMedidor: " + numeroMedidor);
        boolean found = false;
        for (MedidorAbsoluto medidor : medidores) {
            Log.i("test->", medidor.toString());
            if (medidor.numero.endsWith(numeroMedidor)) {
                atualizarStatusMedidor(medidor, statusMedidor);
                found = true;
            }
        }
        if (!found)
            atualizarStatusMedidor(medidores.get(0), statusMedidor);
        if (deveLerTudo)
            enviarProximoStatusMedidor(numeroMedidor);
    }

    private void enviaComando52(byte pacote) {
        String num_medidor = "";
        if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
            num_medidor = medidorSelecionado.numero;
        }
        byte[] comando = new ComandoAbsoluto.ABNT52()
                .comMedidorNumero(num_medidor)
                .build(pacote, (tipo_medidor == TipoMedidor.ABOSOLUTO));
        enviarComando(comando);
    }

    private void processarRespostaAbnt51(RespostaAbsoluto respostaAbsoluto) {
        Log.d(TAG, "## Resposta 51 ...");
        dadosAbnt51 = respostaAbsoluto.interpretaResposta51();
        mRespostaComposta.clear();
        progressDialog = AlertBuilder.createProgressDialog(this, "Enviando pacote " + dadosAbnt51.pacoteAtual + " de " + dadosAbnt51.numeroPacotes);
        progressDialog.show();
        byte pacote = 0;
        enviaComando52(pacote);
    }

    private void processarRespostaAbnt52(RespostaAbsoluto respostaAbsoluto) {
        Log.d(TAG, "## Resposta 52: " + dadosAbnt51.pacoteAtual + " de " + dadosAbnt51.numeroPacotes);
        if (!respostaAbsoluto.interpretaResposta52()) {
            mRespostaComposta.add(respostaAbsoluto.getData());
            dadosAbnt51.pacoteAtual++;
            progressDialog.setMessage("Enviando pacote " + dadosAbnt51.pacoteAtual + " de " + dadosAbnt51.numeroPacotes);
            byte pacote = 1;
            showTimeOutMM();
            enviaComando52(pacote);
        } else {
            // agora processa a memoria de masssa...
            mMMRunnable = null;
            mHandler.removeCallbacksAndMessages(null);
            mRespostaComposta.add(respostaAbsoluto.getData());
            processa52 = respostaAbsoluto.prepara52();
            progressDialog.setMessage("Aguarde, Processando a Memória de Massa.");
            for (int i = 0; i < mRespostaComposta.size(); i++) {
                byte[] data = mRespostaComposta.get(i);
                int inicio = 7;
                if ((i % 3) == 0) {
                    while (inicio < 255) { // Começa no canal 1
                        long valor = Util.unsignedByteToInt(data[inicio]) + ((Util.unsignedByteToInt(data[inicio + 1]) & 0xF0) << 4);
                        processa52.mGrandezasC1.add((long) Util.unsignedByteToInt(data[inicio]) + ((Util.unsignedByteToInt(data[inicio + 1]) & 0xF0) << 4));
                        processa52.mGrandezasC2.add((long) Util.unsignedByteToInt(data[inicio + 2]) + ((Util.unsignedByteToInt(data[inicio + 1]) & 0x0F) << 8));
                        processa52.mGrandezasC3.add((long) Util.unsignedByteToInt(data[inicio + 3]) + ((Util.unsignedByteToInt(data[inicio + 4]) & 0xF0) << 4));

                        processa52.mGrandezasC1.add((long) Util.unsignedByteToInt(data[inicio + 5]) + ((Util.unsignedByteToInt(data[inicio + 4]) & 0x0F) << 8));
                        if (inicio != 250) {
                            processa52.mGrandezasC2.add((long) Util.unsignedByteToInt(data[inicio + 6]) + ((Util.unsignedByteToInt(data[inicio + 7]) & 0xF0) << 4));
                            processa52.mGrandezasC3.add((long) Util.unsignedByteToInt(data[inicio + 8]) + ((Util.unsignedByteToInt(data[inicio + 7]) & 0x0F) << 8));
                        }
                        inicio += 9;
                    }
                } else if ((i % 3) == 1) { // começa no canal 2
                    while (inicio < 255) {
                        processa52.mGrandezasC2.add((long) Util.unsignedByteToInt(data[inicio]) + ((Util.unsignedByteToInt(data[inicio + 1]) & 0xF0) << 4));
                        processa52.mGrandezasC3.add((long) Util.unsignedByteToInt(data[inicio + 2]) + ((Util.unsignedByteToInt(data[inicio + 1]) & 0x0F) << 8));
                        processa52.mGrandezasC1.add((long) Util.unsignedByteToInt(data[inicio + 3]) + ((Util.unsignedByteToInt(data[inicio + 4]) & 0xF0) << 4));

                        processa52.mGrandezasC2.add((long) Util.unsignedByteToInt(data[inicio + 5]) + ((Util.unsignedByteToInt(data[inicio + 4]) & 0x0F) << 8));
                        if (inicio != 250) {
                            processa52.mGrandezasC3.add((long) Util.unsignedByteToInt(data[inicio + 6]) + ((Util.unsignedByteToInt(data[inicio + 7]) & 0xF0) << 4));
                            processa52.mGrandezasC1.add((long) Util.unsignedByteToInt(data[inicio + 8]) + ((Util.unsignedByteToInt(data[inicio + 7]) & 0x0F) << 8));
                        }
                        inicio += 9;
                    }
                } else if ((i % 3) == 2) { // começa no canal 3
                    while (inicio < 255) {
                        processa52.mGrandezasC3.add((long) Util.unsignedByteToInt(data[inicio]) + ((Util.unsignedByteToInt(data[inicio + 1]) & 0xF0) << 4));
                        processa52.mGrandezasC1.add((long) Util.unsignedByteToInt(data[inicio + 2]) + ((Util.unsignedByteToInt(data[inicio + 1]) & 0x0F) << 8));
                        processa52.mGrandezasC2.add((long) Util.unsignedByteToInt(data[inicio + 3]) + ((Util.unsignedByteToInt(data[inicio + 4]) & 0xF0) << 4));

                        processa52.mGrandezasC3.add((long) Util.unsignedByteToInt(data[inicio + 5]) + ((Util.unsignedByteToInt(data[inicio + 4]) & 0x0F) << 8));
                        if (inicio != 250) {
                            processa52.mGrandezasC1.add((long) Util.unsignedByteToInt(data[inicio + 6]) + ((Util.unsignedByteToInt(data[inicio + 7]) & 0xF0) << 4));
                            processa52.mGrandezasC2.add((long) Util.unsignedByteToInt(data[inicio + 8]) + ((Util.unsignedByteToInt(data[inicio + 7]) & 0x0F) << 8));
                        }
                        inicio += 9;
                    }
                }
            }
            SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            StringBuilder dadosCSV = new StringBuilder();
            if (tipo_medidor != TipoMedidor.ABOSOLUTO) {
                dadosCSV.append("Ponto;" + mDeviceName.substring(4) + "\n");
                dadosCSV.append("NS do medidor;" + respostaAbsoluto.getNumeroMedidor() + "\n");
            } else {
                dadosCSV.append("UC;" + medidorSelecionado.unidadeConsumidora + "\n");
                dadosCSV.append("NS do medidor;" + medidorSelecionado.numero + "\n");
            }

            dadosCSV.append("Data/Hora do ultimo periodo integrado;" + dadosAbnt51.textUltimoPeriodo + "\n");
            dadosCSV.append("Data/Hora da ultima fatura;" + dadosAbnt51.textUltimaFatura + "\n");
            dadosCSV.append("Numero de palavras;" + dadosAbnt51.numeroPalavras + "\n");
            dadosCSV.append("Intervalo da memoria de massa;" + dadosAbnt51.periodoIntegracao + "\n");
            dadosCSV.append("Constantes do Canal 1;" + dadosAbnt51.numeradorCanal1 + "/" + dadosAbnt51.denominadorCanal1 + "\n");
            dadosCSV.append("Constantes do Canal 2;" + dadosAbnt51.numeradorCanal2 + "/" + dadosAbnt51.denominadorCanal2 + "\n");
            dadosCSV.append("Constantes do Canal 3;" + dadosAbnt51.numeradorCanal3 + "/" + dadosAbnt51.denominadorCanal3 + "\n");


            String tmp = "Reg;Data/Hora;";
            String format = "";
            String strCanal = "";
            int fator_multiplicacao = 1;

            if (tipo_medidor == TipoMedidor.EASY_VOLT) {
                switch (codigoCanal) {
                    case 0:
                        format = ";%.02f;%d;%.02f;%d;%.02f;%d;\n";
                        tmp += "V_a;C1;V_b;C2;V_c;C3";
                        strCanal = "Tensao";
                        break;
                    case 1:
                        format = ";%.02f;%d;%.02f;%d;%.02f;%d;\n";
                        tmp += "THD_VA;C1;THD_VB;C2;THD_VC;C3";
                        strCanal = "THDTensao";
                        break;
                    case 2:
                        format = ";%.02f;%d;%.02f;%d;%.02f;%d;\n";
                        tmp += "VAminh;C1;VBminh;C2;VCminh;C3";
                        strCanal = "TensaoMinima";
                        break;
                    case 3:
                        format = ";%.02f;%d;%.02f;%d;%.02f;%d;\n";
                        tmp += "VAmaxh;C1;VBmaxh;C2;VCmaxh;C3";
                        strCanal = "TensaoMaxima";
                        break;
                }
            } else {
                switch (codigoCanal) {
                    case 0:
                        tmp += "W;C1;varIND;C2;varCAP;C3";
                        format = ";%.00f;%d;%.00f;%d;%.00f;%d;\n";
                        fator_multiplicacao = 1000;
                        strCanal = "EnergiaDireta";
                        break;
                    case 1:
                        tmp += "-W;C1;varIND;C2;varCAP;C3";
                        format = ";%.00f;%d;%.00f;%d;%.00f;%d;\n";
                        fator_multiplicacao = 1000;
                        strCanal = "EnergiaReversa";
                        break;
                    case 2:
                        format = ";%.02f;%d;%.02f;%d;%.02f;%d;\n";
                        tmp += "V_a;C1;V_b;C2;V_c;C3";
                        strCanal = "Tensao";
                        break;
                    case 3:
                        format = ";%.02f;%d;%.02f;%d;%.02f;%d;\n";
                        tmp += "I_a;C1;I_b;C2;I_c;C3";
                        strCanal = "Corrente";
                        break;
                    case 4:
                        format = ";%.02f;%d;%.02f;%d;%.02f;%d;\n";
                        tmp += "THD_VA;C1;THD_VB;C2;THD_VC;C3";
                        strCanal = "THDTensao";
                        break;
                    case 5:
                        format = ";%.02f;%d;%.02f;%d;%.02f;%d;\n";
                        tmp += "THD_IA;C1;THD_IB;C2;THD_IC;C3";
                        strCanal = "THDCorrente";
                        break;
                }
            }
            tmp += "\n";
            dadosCSV.append(tmp);
            for (int i = 0; i < dadosAbnt51.totalGrandezas; i++) {
                float v1 = processa52.mGrandezasC1.get(i) * ((float) dadosAbnt51.numeradorCanal1 / (float) dadosAbnt51.denominadorCanal1 * (float) (60 / dadosAbnt51.periodoIntegracao)) * fator_multiplicacao;
                float v2 = processa52.mGrandezasC2.get(i) * ((float) dadosAbnt51.numeradorCanal2 / (float) dadosAbnt51.denominadorCanal2 * (float) (60 / dadosAbnt51.periodoIntegracao)) * fator_multiplicacao;
                float v3 = processa52.mGrandezasC3.get(i) * ((float) dadosAbnt51.numeradorCanal3 / (float) dadosAbnt51.denominadorCanal3 * (float) (60 / dadosAbnt51.periodoIntegracao)) * fator_multiplicacao;
                String linha = (i + 1) + ";" +
                        formatDate.format(dadosAbnt51.ultimoPeriodo.getTime()) +
                        String.format(format,
                                v1, processa52.mGrandezasC1.get(i), v2, processa52.mGrandezasC2.get(i), v3, processa52.mGrandezasC3.get(i));
                dadosCSV.append(linha);
                dadosAbnt51.ultimoPeriodo.add(Calendar.MINUTE, dadosAbnt51.periodoIntegracao);
            }

            String nomeArquivo = String.format("MemoriaMassa%s_%s_%s_%s.csv", strCanal, respostaAbsoluto.getNumeroMedidor(), mDeviceName.substring(4).trim(), new SimpleDateFormat("ddMMyyyy'_'HHmmss").format(Calendar.getInstance().getTime()));

            if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
                nomeArquivo = String.format("MemoriaMassa%s_%s_%s_%s.csv", strCanal, medidorSelecionado.numero, medidorSelecionado.unidadeConsumidora /*mDeviceName.substring(4).trim()*/,
                        new SimpleDateFormat("ddMMyyyy'_'HHmmss").format(Calendar.getInstance().getTime()));
            }

            if (!Arquivo.salvarArquivo(this, nomeArquivo, dadosCSV.toString())) {
                Toast.makeText(this, "Erro ao salvar arquivo da memória de massa",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Arquivo salvo com sucesso:\n" + nomeArquivo,
                        Toast.LENGTH_LONG).show();
            }

            progressDialog.hide();
            progressDialog.dismiss();
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    private void processarRespostaAbnt73(RespostaAbsoluto resposta) {
        if (leuEasyTrafo)
            return;

        mTimeoutHandler.removeCallbacksAndMessages(null);
        progressDialog.dismiss();
        mHandler.removeCallbacksAndMessages(null);
        leuEasyTrafo = true;
        //Log.d(TAG, "processarRespostaAbnt73.");
        byte[] data = resposta.getData();

        //numMedidor = new String(Arrays.copyOfRange(data, 7, 21));

        mNsMedidor = resposta.getNumeroMedidor();

        mMeterEasyTrafoAdapter = new MeterEasyTrafoAdapter(numMedidor, dataMedidor, mNsMedidor);
        mRecyclerView.setAdapter(mMeterEasyTrafoAdapter);
        mMeterEasyTrafoAdapter.notifyDataSetChanged();

        mRefreshStatus.clearAnimation();

    }
    private void processarRespostaAbnt87(RespostaAbsoluto resposta) {
        if (leuEasyTrafo)
            return;

        mTimeoutHandler.removeCallbacksAndMessages(null);
        progressDialog.dismiss();
        mHandler.removeCallbacksAndMessages(null);
        leuEasyTrafo = true;
        Log.d(TAG, "processarRespostaAbnt87.");
        byte[] data = resposta.getData();
        numMedidor = new String(Arrays.copyOfRange(data, 7, 21));

        mNsMedidor = resposta.getNumeroMedidor();

        mMeterEasyTrafoAdapter = new MeterEasyTrafoAdapter(numMedidor, dataMedidor, mNsMedidor);
        mRecyclerView.setAdapter(mMeterEasyTrafoAdapter);
        mMeterEasyTrafoAdapter.notifyDataSetChanged();

        mRefreshStatus.clearAnimation();

    }

    private void processarRespostaAbnt21Absoluto(RespostaAbsoluto resposta) {
        byte[] data = resposta.getData();
        dataMedidor = String.format("%02X/%02X/20%02X %02X:%02X:%02X",
                data[8], data[9], data[10], data[5], data[6], data[7]);

        if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
            versaoMedidor = String.format("%02X.%02X", data[30], data[31]);
        } else {
            versaoMedidor = String.format("%02X.%02X", data[147], data[148]);
        }
        modeloMultiponto = String.format("%02X%02X", data[152], data[153]);

        tipoModulo = String.format("%02X", data[38]);
        estadoModulo = String.format("%02X", data[39]);
        sinalModulo = String.valueOf(data[40]);
        switch (tipoModulo) {
            case "01":
                tipoModulo = "Star Measure";
                break;
            case "02":
                tipoModulo = "CAS";
                break;
            case "03":
                tipoModulo = "M2M";
                break;
            case "04":
                tipoModulo = "V2COM";
                break;
            case "05":
                tipoModulo = "SSE Gridtech";
                break;
            case "06":
                tipoModulo = "Honeywell";
                break;
            case "07":
                tipoModulo = "Landys+Gyr";
                break;
            case "08":
                tipoModulo = "Itron";
                break;
            default:
                tipoModulo = "";
                break;
        }
        estadoModulo = String.format("%02X", data[39]);
        if (estadoModulo.equals("01")) {
            estadoModulo = "Conectado";
        } else {
            estadoModulo = "Desconectado/Não informado";
        }
        sinalModulo = String.format("%02X", data[40]);

        Log.d(TAG, "\nProcessa ABNT 21 Absoluto { \nData: " + dataMedidor + "\nVersão Medidor: " + versaoMedidor + "\nModelo multiponto: " + modeloMultiponto + "}");
        enviarLeituraParametrosHospedeiro();
    }

    private void processarRespostaAbnt21(RespostaAbsoluto resposta) {
        if (leuEasyTrafo)
            return;

        byte[] data = resposta.getData();
        //date = Calendar.getInstance().getTime();
        dataMedidor = String.format("%02X/%02X/20%02X %02X:%02X:%02X",
                data[8], data[9], data[10], data[5], data[6], data[7]);

        versaoMedidor = String.format("%02X.%02X", data[147], data[148]);
        modeloMultiponto = String.format("%02X%02X", data[152], data[153]);

        if (modeloMultiponto.equals("1503")) {
            tipo_medidor = TipoMedidor.EASY_VOLT;
        }

        tipoModulo = String.format("%02X", data[37]);
        switch (tipoModulo) {
            case "00":
                tipoModulo = "";
                break;
            case "01":
                tipoModulo = "Star Measure";
                break;
            case "02":
                tipoModulo = "CAS";
                break;
            case "03":
                tipoModulo = "M2M";
                break;
            case "04":
                tipoModulo = "V2COM";
                break;
            case "05":
                tipoModulo = "SSE Gridtech";
                break;
            case "06":
                tipoModulo = "Honeywell";
                break;
            case "07":
                tipoModulo = "Landys+Gyr";
                break;
            case "08":
                tipoModulo = "Itron";
                break;
            default:
                tipoModulo = "";
                break;
        }
        estadoModulo = String.format("%02X", data[38]);
        if (estadoModulo.equals("01")) {
            estadoModulo = "Conectado";
        } else {
            estadoModulo = "Desconectado";
        }
        sinalModulo = String.valueOf(data[39]);

        Log.d(TAG, "\nProcessa ABNT 21 Easy { \nData: " + dataMedidor + "\nVersão Medidor: " + versaoMedidor + "\nModelo multiponto: " + modeloMultiponto + "}");
        enviarLeituraConfiguracao();
    }

    private void atualizarStatusMedidor(MedidorAbsoluto
                                                medidor, RespostaAbsoluto.LeituraStatusMedidor status) {
        medidor.status = status.EstadoUnidadeConsumidora;
        medidor.semRele = false;
        mMeterListAdapter.atualizarDados(medidores);
        mMeterListAdapter.notifyDataSetChanged();
    }

    private void processarRespostaCorteReligamento(RespostaAbsoluto respostaAbsoluto) {
        String numeroMedidor = respostaAbsoluto.getSufixoNumeroMedidor();
        enviarLeituraStatusMedidores(numeroMedidor);
    }

    private void processarRespostaAberturaSessao(byte codigoResposta, RespostaAbsoluto
            respostaAbsoluto) {
        String numeroMedidor = respostaAbsoluto.getSufixoNumeroMedidor();
        Log.d(TAG, "Abertura de sessão para o medidor: " + numeroMedidor + "\n" + respostaAbsoluto.toString());

        if (codigoResposta == 0x01) {
            Toast.makeText(DeviceActivity.this, getString(R.string.text_senha_expira),
                    Toast.LENGTH_SHORT).show();
        } else if (codigoResposta == 0x02) {
            showErrorMessage(getString(R.string.text_senha_expirada));
            return;
        } else if (codigoResposta == 0x03) {
            showErrorMessage(getString(R.string.text_medidor_protecao));
            return;
        } else if (codigoResposta == 0x04) {
            showErrorMessage(getString(R.string.text_senha_invalida));
            return;
        }

        for (MedidorAbsoluto medidor : medidores) {
            //medidor.
            if ((medidor.numero.endsWith(numeroMedidor)) || (medidor.numero.startsWith("99" + numeroMedidor))) {
                if (TipoOperacao.CorteReliga == funcaoEmExecucao) {
                    if (medidor.status == 0x00) {
                        enviarReligamento(numeroMedidor);
                    } else if (medidor.status == 0x01) {
                        enviarCorte(numeroMedidor);
                    }
                } else {
                    ajustaDataHora(true, medidor);
                }
            }
        }
    }

    private void showErrorMessage(String mensagem) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(DeviceActivity.this,
                    android.R.style.Theme_Material_Dialog_Alert);
            builder
                    .setTitle("Erro")
                    .setMessage(mensagem)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
        }
    }

    private void processarRespostaNaoTratada(byte[] data) {
        Log.d(TAG, "Resposta não tratada: " + Util.ByteArrayToHexString(data));
    }

    private void enviarProximoStatusMedidor(String numeroMedidor) {
        if (numeroMedidor.equals("99999999") && medidores.size() > 1) {
            deveLerTudo = true;
            String numero = medidores.get(0).numero;
            if (numero.endsWith("000000"))
                numero = numero.substring(0, numero.length() - 6);
            enviarLeituraStatusMedidores(numero);
            return;
        }
        for (int i = 0; i < medidores.size(); i++) {
            if (medidores.get(i).numero.endsWith(numeroMedidor)) {
                if (i != medidores.size() - 1) {
                    String numero = medidores.get(i + 1).numero;
                    enviarLeituraStatusMedidores(numero);
                    return;
                } else {
                    Log.d(TAG, "RemoveCallbacks status medidores");
                    mTimeoutHandler.removeCallbacksAndMessages(null);
                    mRefreshStatus.clearAnimation();
                    deveLerTudo = false;
                    return;
                }
            }
        }
        if (medidores.size() > 1)
            enviarLeituraStatusMedidores(medidores.get(1).numero);
    }

    private void enviarCorte(String numeroMedidor) {
        byte[] comando = new ComandoAbsoluto.AB06AlteracaoCorteReligamento()
                .comMedidorNumero(numeroMedidor)
                .efetuaCorte()
                .build((tipo_medidor == TipoMedidor.ABOSOLUTO));
        enviarComando(comando);
    }

    private void enviarReligamento(String numeroMedidor) {
        byte[] comando = new ComandoAbsoluto.AB06AlteracaoCorteReligamento()
                .comMedidorNumero(numeroMedidor)
                .efetuaReligamento()
                .build((tipo_medidor == TipoMedidor.ABOSOLUTO));
        enviarComando(comando);
    }

    private void enviarTipoLigacao(String numeroMedidor) {
        byte[] comando = new ComandoAbsoluto.ABNT95()
                .comMedidorNumero(numeroMedidor)
                .build((byte) codigoCanal);
        enviarComando(comando);
    }

    private void enviarLimparOcorrencia(String numeroMedidor, byte codigoOcorrencia) {
        final byte[] comando = new ComandoAbsoluto.LimpezaOcorrenciasMedidor()
                .comMedidorNumero(numeroMedidor)
                .comCodigoOcorrencia(codigoOcorrencia)
                .build((tipo_medidor == TipoMedidor.ABOSOLUTO));
        enviarComando(comando);
    }

    private void enviarComandoComposto(byte[] data) {
        mRespostaComposta.clear();
        Log.d(TAG, "ENC->" + Util.ByteArrayToHexString(data));
        //Log.d(TAG, Util.ByteArrayToString(data));
        if (!sendData(data))
            Log.d(TAG, "Erro ao enviar mensagem");

    }

    private void enviarComando(byte[] data) {
        Log.d(TAG, "ENS->" + Util.ByteArrayToHexString(data));
        if (!sendData(data))
            Log.d(TAG, "Erro ao enviar mensagem");
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private class MeterListAdapter
            extends RecyclerView.Adapter<MeterListAdapter.MeterViewHolder> {

        private List<MedidorAbsoluto> medidores;

        public void atualizarDados(List<MedidorAbsoluto> medidores) {
            this.medidores = medidores;
        }

        public class MeterViewHolder extends RecyclerView.ViewHolder {
            private final View.OnClickListener incidenteClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getAdapterPosition() >= 0) {
                        medidorSelecionado = medidores.get(getAdapterPosition());
                        exibirMenu(v);
                    }
                }
            };

            TextView unidadeConsumidoraInfo;
            TextView unidadeConsumidora;
            TextView medidorInfo;
            TextView medidor;
            TextView versaoInfo;
            TextView versao;
            TextView fases;
            ImageView statusMedidor;

            public MeterViewHolder(@NonNull View view) {
                super(view);
                unidadeConsumidora = view.findViewById(R.id.unidade_consumidora);
                unidadeConsumidoraInfo = view.findViewById(R.id.unidade_consumidora_info);
                medidor = view.findViewById(R.id.dados_medidor);
                medidorInfo = view.findViewById(R.id.dados_medidor_info);
                versao = view.findViewById(R.id.medidor_versao);
                versaoInfo = view.findViewById(R.id.medidor_versao_info);
                fases = view.findViewById(R.id.numero_fases_medidor);
                unidadeConsumidora.setOnClickListener(incidenteClick);
                statusMedidor = view.findViewById(R.id.status_medidor);
                view.findViewById(R.id.incidente_medidor).setOnClickListener(incidenteClick);
            }
        }

        public MeterListAdapter(List<MedidorAbsoluto> medidores) {
            this.medidores = medidores;
        }

        public void clear() {
            medidores.clear();
        }

        @NonNull
        @Override
        public MeterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.meter_item, viewGroup, false);
            return new MeterViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MeterViewHolder meterViewHolder, int i) {
            MedidorAbsoluto medidor = medidores.get(i);
            String formato = "%s";
            String str_medidor = medidor.unidadeConsumidora;
            String str_fases = String.format("%d", medidor.fases);
            int status = medidor.status;

            if (str_medidor.equalsIgnoreCase("000000000")) {
                meterViewHolder.unidadeConsumidoraInfo.setVisibility(LinearLayout.GONE);
                meterViewHolder.unidadeConsumidora.setTextColor(getResources().getColor(R.color.starmeasure_turquoise));
                meterViewHolder.versaoInfo.setVisibility(LinearLayout.VISIBLE);
                meterViewHolder.versao.setVisibility(LinearLayout.VISIBLE);
                meterViewHolder.versao.setText(versaoMedidor);
                str_medidor = "Medidor Hospedeiro";
                str_fases = "";
                status = -1;
            } else {
                meterViewHolder.versao.setVisibility(LinearLayout.GONE);
                meterViewHolder.versaoInfo.setVisibility(LinearLayout.GONE);
            }

            meterViewHolder.unidadeConsumidora.setText(String.format(formato, str_medidor));
            meterViewHolder.medidor.setText(medidor.numero);

            switch (status) {
                case 0x00:
                    meterViewHolder.statusMedidor.setImageResource(R.drawable.power_off);
                    break;
                case 0x01:
                    meterViewHolder.statusMedidor.setImageResource(R.drawable.status_ok);
                    break;
                case -1:
                    meterViewHolder.statusMedidor.setImageResource(android.R.color.transparent);
                    break;
                default:
                    meterViewHolder.statusMedidor.setImageResource(R.drawable.status_warning);
            }
            meterViewHolder.fases.setText(str_fases);
        }

        @Override
        public int getItemCount() {
            return medidores.size();
        }
    }

    private void exibirMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.menu_acoes);
        MenuItem item_corte_religa = popup.getMenu().findItem(R.id.menu_corte_religa);
        MenuItem item_qee = popup.getMenu().findItem(R.id.menu_qee);
        MenuItem item_monitoramento = popup.getMenu().findItem(R.id.menu_monitoramento);
        MenuItem item_registradores = popup.getMenu().findItem(R.id.menu_leitura_registradores);
        MenuItem item_reset_registradores = popup.getMenu().findItem(R.id.menu_reset_registradores);
        MenuItem item_modo_teste = popup.getMenu().findItem(R.id.menu_modo_test);
        item_qee.setVisible(false);

        if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
            if (item_corte_religa != null) {
                if (medidorSelecionado.status == 0x00)
                    item_corte_religa.setTitle(getString(R.string.text_religa));
                else if (medidorSelecionado.status == 0x01)
                    item_corte_religa.setTitle(getString(R.string.text_corte));
                else
                    item_corte_religa.setTitle(getString(R.string.text_corte_religa));
            }
            if (medidorSelecionado.semRele) {
                if (item_corte_religa != null) {
                    item_corte_religa.setVisible(false);
                }
            }
            item_corte_religa = popup.getMenu().findItem(R.id.menu_nome_unidade);
            item_corte_religa.setVisible(false);
            item_corte_religa = popup.getMenu().findItem(R.id.menu_reset_registradores);
            item_corte_religa.setVisible(false);
        } else if (tipo_medidor == TipoMedidor.EASY_TRAFO) {
            item_corte_religa.setVisible(false);
            item_qee.setVisible(true);
            item_monitoramento.setVisible(true);
        } else if (tipo_medidor == TipoMedidor.EASY_VOLT) {
            item_reset_registradores.setVisible(false);
            item_registradores.setVisible(false);
            item_corte_religa.setVisible(false);
            item_qee.setVisible(true);
            item_monitoramento.setVisible(true);
            item_modo_teste.setVisible(true);
        } else {
            item_corte_religa.setVisible(false);
        }
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_leitura_registradores:
                funcaoEmExecucao = TipoOperacao.RegistradoresAtuais;
                exibirLeituraParametros(medidorSelecionado);
                break;
            case R.id.menu_pagina_fiscal:
                funcaoEmExecucao = TipoOperacao.PaginaFiscal;
                exibirGrandezasInstantaneas(medidorSelecionado);
                break;
            case R.id.menu_corte_religa:
                funcaoEmExecucao = TipoOperacao.CorteReliga;
                enviarAberturaSessao(medidorSelecionado.numero);
                break;
            case R.id.menu_data_hora:
                funcaoEmExecucao = TipoOperacao.DataHora;
                if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
                    enviarAberturaSessao(medidorSelecionado.numero);
                } else {
                    ajustaDataHora(true, medidorSelecionado);
                }
                break;
            case R.id.menu_nome_unidade:
                funcaoEmExecucao = TipoOperacao.NomeUnidade;
                trocaNomeUnidade(medidorSelecionado);
                break;
            case R.id.menu_memoria_massa_abnt:
                funcaoEmExecucao = TipoOperacao.MemoriaMassa;
                iniciaMemoriaMassa(medidorSelecionado);
                break;
            case R.id.menu_memoria_massa_sm:
                funcaoEmExecucao = TipoOperacao.MemoriaMassa;
                iniciaMemoriaMassa(medidorSelecionado);
                break;
            case R.id.menu_memoria_massa_intervalo:
                funcaoEmExecucao = TipoOperacao.IntervaloMM;
                iniciaAlteracaoIntervaloMM(medidorSelecionado);
                break;
            case R.id.menu_memoria_massa_reset:
                funcaoEmExecucao = TipoOperacao.MemoriaMassa;
                iniciaMemoriaMassa(medidorSelecionado);
                break;
            case R.id.menu_reset_registradores:
                funcaoEmExecucao = TipoOperacao.ResetRegistradores;
                resetRegistradores(medidorSelecionado, (byte) 0);
                break;
            case R.id.menu_qee_parametrizacao:
                funcaoEmExecucao = TipoOperacao.ParametrosQEE;
                buscaQEEParametros();
                break;
            case R.id.menu_qee_reset:
                funcaoEmExecucao = TipoOperacao.ResetRegistradores;
                resetRegistradores(medidorSelecionado, (byte) 3);
                break;
            case R.id.menu_qee_leitura:
                funcaoEmExecucao = TipoOperacao.InicioMemoriaMassaQEE;
                iniciaMemoriaMassaQEE();
                break;
            case R.id.menu_carga_de_programa:
                funcaoEmExecucao = TipoOperacao.IniciarCargaDePrograma;
                lancaCargaDePrograma();
                break;
            case R.id.menu_monitoramento:
                funcaoEmExecucao = TipoOperacao.MonitoramentoDeTransformador;
                iniciaMonitoramento();
                break;
            case R.id.menu_modo_test:
                funcaoEmExecucao = TipoOperacao.ModoTeste;
                eb92 = new ComandoAbsoluto.EB92();
                iniciaModoTeste();
                break;
            default:
                return false;
        }
        return true;
    }

    private void iniciaModoTeste() {
        enviarComando(eb92.build());
        showProgressBar("Buscando alarmes...");
    }

    private void processaEB92(RespostaAbsoluto resposta) {
        RespostaAbsoluto.Alarmes alarmes = resposta.processaAlarmes();
        eb92.alarmeEstadoTampa = alarmes.estadoTampa;
        eb92.alarmeSubTensaoA = alarmes.subtensaoA;
        eb92.alarmeSubTensaoB = alarmes.subtensaoB;
        eb92.alarmeSubTensaoC = alarmes.subtensaoC;
        eb92.alarmeSobreTensaoA = alarmes.sobretensaoA;
        eb92.alarmeSobreTensaoB = alarmes.sobretensaoB;
        eb92.alarmeSobreTensaoC = alarmes.sobretensaoC;

        CheckBox cbAlarmeSobretensaoA;
        CheckBox cbAlarmeSobretensaoB;
        CheckBox cbAlarmeSobretensaoC;
        CheckBox cbAlarmeSubtensaoA;
        CheckBox cbAlarmeSubtensaoB;
        CheckBox cbAlarmeSubtensaoC;
        CheckBox cbEstadoTampa;

        if (dialogModoTeste == null) {
            mMMRunnable = null;
            mHandler.removeCallbacksAndMessages(null);
            progressDialog.dismiss();
            dialogModoTeste = new Dialog(this);
            dialogModoTeste.setContentView(R.layout.dialog_modo_teste);
            if (dialogModoTeste.getWindow() != null) {
                dialogModoTeste.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialogModoTeste.setCancelable(false);
            dialogModoTeste.setOnDismissListener(dialog -> dialogModoTeste = null);

            dialogModoTeste.findViewById(R.id.modo_teste_btn_close).setOnClickListener(view -> {
                dialogModoTeste.dismiss();
                eb92 = null;
                dialogModoTeste = null;
            });

            dialogModoTeste.findViewById(R.id.modo_teste_enviar).setOnClickListener(v -> {
                Toast.makeText(this, "Enviando alteração de teste", Toast.LENGTH_SHORT).show();
                dialogModoTeste.findViewById(R.id.modo_teste_loading).setVisibility(View.VISIBLE);
                eb92.isEscrita = true;
                enviarComando(eb92.build());
                v.setEnabled(false);
            });

            cbAlarmeSobretensaoA = dialogModoTeste.findViewById(R.id.cb_alarme_sobretensao_a);
            cbAlarmeSobretensaoA.setChecked(alarmes.sobretensaoA);
            cbAlarmeSobretensaoA.setOnCheckedChangeListener((buttonView, isChecked) -> eb92.alarmeSobreTensaoA = isChecked);

            cbAlarmeSobretensaoB = dialogModoTeste.findViewById(R.id.cb_alarme_sobretensao_b);
            cbAlarmeSobretensaoB.setChecked(alarmes.sobretensaoB);
            cbAlarmeSobretensaoB.setOnCheckedChangeListener((buttonView, isChecked) -> eb92.alarmeSobreTensaoB = isChecked);

            cbAlarmeSobretensaoC = dialogModoTeste.findViewById(R.id.cb_alarme_sobretensao_c);
            cbAlarmeSobretensaoC.setChecked(alarmes.sobretensaoC);
            cbAlarmeSobretensaoC.setOnCheckedChangeListener((buttonView, isChecked) -> eb92.alarmeSobreTensaoC = isChecked);

            cbAlarmeSubtensaoA = dialogModoTeste.findViewById(R.id.cb_alarme_subtensao_a);
            cbAlarmeSubtensaoA.setChecked(alarmes.subtensaoA);
            cbAlarmeSubtensaoA.setOnCheckedChangeListener((buttonView, isChecked) -> eb92.alarmeSubTensaoA = isChecked);

            cbAlarmeSubtensaoB = dialogModoTeste.findViewById(R.id.cb_alarme_subtensao_b);
            cbAlarmeSubtensaoB.setChecked(alarmes.subtensaoB);
            cbAlarmeSubtensaoB.setOnCheckedChangeListener((buttonView, isChecked) -> eb92.alarmeSubTensaoB = isChecked);

            cbAlarmeSubtensaoC = dialogModoTeste.findViewById(R.id.cb_alarme_subtensao_c);
            cbAlarmeSubtensaoC.setChecked(alarmes.subtensaoC);
            cbAlarmeSubtensaoC.setOnCheckedChangeListener((buttonView, isChecked) -> eb92.alarmeSubTensaoC = isChecked);

            cbEstadoTampa = dialogModoTeste.findViewById(R.id.cb_alarme_estado_tampa);
            cbEstadoTampa.setChecked(alarmes.estadoTampa);
            cbEstadoTampa.setOnCheckedChangeListener((buttonView, isChecked) -> eb92.alarmeEstadoTampa = isChecked);
            dialogModoTeste.show();
        } else {
            Toast.makeText(this, "Dados recebidos...", Toast.LENGTH_SHORT).show();
            dialogModoTeste.findViewById(R.id.modo_teste_loading).setVisibility(View.GONE);
            dialogModoTeste.findViewById(R.id.modo_teste_enviar).setEnabled(true);

            cbAlarmeSobretensaoA = dialogModoTeste.findViewById(R.id.cb_alarme_sobretensao_a);
            cbAlarmeSobretensaoA.setChecked(alarmes.sobretensaoA);
            cbAlarmeSobretensaoA.setOnCheckedChangeListener((buttonView, isChecked) -> eb92.alarmeSobreTensaoA = isChecked);

            cbAlarmeSobretensaoB = dialogModoTeste.findViewById(R.id.cb_alarme_sobretensao_b);
            cbAlarmeSobretensaoB.setChecked(alarmes.sobretensaoB);
            cbAlarmeSobretensaoB.setOnCheckedChangeListener((buttonView, isChecked) -> eb92.alarmeSobreTensaoB = isChecked);

            cbAlarmeSobretensaoC = dialogModoTeste.findViewById(R.id.cb_alarme_sobretensao_c);
            cbAlarmeSobretensaoC.setChecked(alarmes.sobretensaoC);
            cbAlarmeSobretensaoC.setOnCheckedChangeListener((buttonView, isChecked) -> eb92.alarmeSobreTensaoC = isChecked);

            cbAlarmeSubtensaoA = dialogModoTeste.findViewById(R.id.cb_alarme_subtensao_a);
            cbAlarmeSubtensaoA.setChecked(alarmes.subtensaoA);
            cbAlarmeSubtensaoA.setOnCheckedChangeListener((buttonView, isChecked) -> eb92.alarmeSubTensaoA = isChecked);

            cbAlarmeSubtensaoB = dialogModoTeste.findViewById(R.id.cb_alarme_subtensao_b);
            cbAlarmeSubtensaoB.setChecked(alarmes.subtensaoB);
            cbAlarmeSubtensaoB.setOnCheckedChangeListener((buttonView, isChecked) -> eb92.alarmeSubTensaoB = isChecked);

            cbAlarmeSubtensaoC = dialogModoTeste.findViewById(R.id.cb_alarme_subtensao_c);
            cbAlarmeSubtensaoC.setChecked(alarmes.subtensaoC);
            cbAlarmeSubtensaoC.setOnCheckedChangeListener((buttonView, isChecked) -> eb92.alarmeSubTensaoC = isChecked);

            cbEstadoTampa = dialogModoTeste.findViewById(R.id.cb_alarme_estado_tampa);
            cbEstadoTampa.setChecked(alarmes.estadoTampa);
            cbEstadoTampa.setOnCheckedChangeListener((buttonView, isChecked) -> eb92.alarmeEstadoTampa = isChecked);
        }
    }

    private void iniciaMonitoramento() {
        showProgressBar("Buscando dados do medidor");
        codigoCanal = 0;
        enviaComandoEB11((byte) 0);
    }

    private void lancaCargaDePrograma() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("Carga de Programa");
        if (Arquivo.getCargasDePrograma().size() > 0) {
            mCarga = Arquivo.getCargaDeProgramaPorModelo(modeloMultiponto);
            if (mCarga == null) {
                builder.setMessage("Carga de programa para este medidor não encontrada");
                builder.setNegativeButton("Entendi", null);
            } else {
                if (mCarga.getArquivo() != null) {
                    if (!CargaController.testaVersaoERevisaoIgual(mCarga.getArquivo().getName(), versaoMedidor.split("\\."))) {
                        if (CargaController.testaVersaoERevisaoDiferente(mCarga.getArquivo().getName(), versaoMedidor.split("\\."))) {
                            builder.setMessage("Há uma atualização disponivel para este medidor. Versão: " + mCarga.getVersao() + "." + mCarga.getRevisao() + "\nDeseja Atualizar?\nA atualização é irreversivel.");
                            builder.setPositiveButton("Sim", (p1, p2) -> {
                                showProgressBar("Alterando protocolo de interface");
                                mTimeoutHandler.postDelayed(() -> {
                                    progressDialog.dismiss();
                                    mCarga = null;
                                    AlertDialog.Builder mBuild = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                                    mBuild.setTitle("Erro");
                                    mBuild.setMessage("Falha ao alterar protocolo de interface");
                                    mBuild.setNegativeButton("Entendi", null);
                                    mBuild.create();
                                    mBuild.show();
                                }, 3000);
                                enviarAB08();
                            });
                            builder.setNegativeButton("Não", null);
                        } else {
                            mCarga = null;
                            builder.setMessage("Versão de carga de programa local inválida.");
                            builder.setNegativeButton("Entendi", null);
                        }
                    } else {
                        builder.setMessage("Medidor já está atualizado com a carga de programa local");
                        builder.setNegativeButton("Entendi", null);
                    }
                } else {
                    mCarga = null;
                    Log.e("mTag", "Arquivo da carga de programa é nulo");
                    builder.setMessage("Não há cargas de programa baixadas");
                    builder.setNegativeButton("Entendi", null);
                }
            }
        } else {
            mCarga = null;
            builder.setMessage("Não há cargas de programa baixadas");
            builder.setNegativeButton("Entendi", null);
        }
        builder.create();
        builder.show();
    }

    private void iniciaMemoriaMassaQEE() {
        final View conf_mm = getLayoutInflater().inflate(R.layout.conf_mm_qee, null);

        final Spinner intervalo = conf_mm.findViewById(R.id.intervalo);
        String[] items = new String[]{"Conjunto Atual", "Conjunto 1", "Conjunto 2", "Conjunto 3", "Conjunto 4"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        intervalo.setAdapter(adapter);
        intervalo.setSelection(0);

        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                android.R.style.Theme_Material_Dialog_Alert);
        AlertDialog alertDialog = builder
                .setTitle("Memória de Massa QEE")
                .setMessage("Informe o conjunto a ser lido")
                .setView(conf_mm)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        codigoCanal = intervalo.getSelectedItemPosition();
                        enviaComandoEB11((byte) 0x00);
                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        if (imm != null)
                            imm.hideSoftInputFromWindow(conf_mm.getWindowToken(), 0);
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        if (imm != null)
                            imm.hideSoftInputFromWindow(conf_mm.getWindowToken(), 0);
                        // do nothing
                    }
                }).create();


        alertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertDialog.show();
    }

    private void buscaQEEParametros() {
        codigoCanal = 0;
        enviaComandoEB11((byte) 0x00);
    }

    private void enviaComandoEB11(byte pacote) {
        String num_medidor = "";
        if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
            num_medidor = medidorSelecionado.numero;
        }

        byte[] comando = new ComandoAbsoluto.EB11QEE()
                .comMedidorNumero(num_medidor)
                .build(pacote, (byte) codigoCanal);
        enviarComando(comando);

    }

    private void iniciaMemoriaMassa(MedidorAbsoluto medidor) {
        final View conf_mm = getLayoutInflater().inflate(R.layout.conf_mm, null);
        final EditText nome = conf_mm.findViewById(R.id.numero_dias);
        nome.setFilters(new InputFilter[]{new MinMaxFilter("0", "99")});

        final Spinner canal = conf_mm.findViewById(R.id.numero_canal);
        String[] items = new String[]{"Energia Direta", "Energia Reversa", "Tensão", "Corrente", "THD Tensão", "THD Corrente"};
        if (tipo_medidor == TipoMedidor.EASY_VOLT) {
            items = new String[]{"Tensão", "THD Tensão", "Tensão Mínima", "Tensão Máxima"};
        } else if (tipo_medidor != TipoMedidor.EASY_TRAFO) {
            items = new String[]{"Energia Direta", "Energia Reversa", "Tensão", "Corrente"};
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this/*alertDialog.getContext()*/, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        canal.setAdapter(adapter);
        canal.setSelection(0);

        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                android.R.style.Theme_Material_Dialog_Alert);
        AlertDialog alertDialog = builder
                .setTitle("Memória de Massa")
                .setMessage("Dados da Memória de Massa")
                .setView(conf_mm)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (nome.getText().toString().isEmpty()) {
                            nome.setText("0");
                        }
                        int dias = Integer.valueOf(nome.getText().toString());
                        codigoCanal = canal.getSelectedItemPosition();
                        if (dias >= 0) {
                            String num_medidor = "";
                            if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
                                num_medidor = medidorSelecionado.numero;
                            }
                            byte[] comando = new ComandoAbsoluto.ABNT51()
                                    .comMedidorNumero(num_medidor)
                                    .build(dias, codigoCanal, (tipo_medidor == TipoMedidor.ABOSOLUTO));
                            enviarComando(comando);
                            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                            if (imm != null)
                                imm.hideSoftInputFromWindow(conf_mm.getWindowToken(), 0);
                        } else {
                            Toast.makeText(DeviceActivity.super.getApplicationContext(),
                                    "Informe o número de dias de 0 a 99.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        if (imm != null)
                            imm.hideSoftInputFromWindow(conf_mm.getWindowToken(), 0);
                        // do nothing
                    }
                }).create();


        alertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertDialog.show();
    }

    private void iniciaAlteracaoIntervaloMM(MedidorAbsoluto medidor) {

        final View intervalo_mm = getLayoutInflater().inflate(R.layout.intervalo_item, null);
        final RadioGroup rg_IntervaloMM = intervalo_mm.findViewById(R.id.rg_intervaloMM);
        final RadioButton rb_Intervalo1MM = intervalo_mm.findViewById(R.id.rd1minuto);
        final RadioButton rb_Intervalo5MM = intervalo_mm.findViewById(R.id.rd5minutos);
        final RadioButton rb_Intervalo15MM = intervalo_mm.findViewById(R.id.rd15minutos);
        final Button btn_IntervaloMM = intervalo_mm.findViewById(R.id.btn_intervaloMM);

        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                android.R.style.Theme_Material_Dialog_Alert);
        AlertDialog alertDialog = builder
                .setTitle("Memória de Massa")
                .setMessage("Intervalo")
                .setView(intervalo_mm)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String num_medidor = "";
                        int MinutosMM = 1;

                        if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
                            num_medidor = medidorSelecionado.numero;
                        }

                        int radioButtonID = rg_IntervaloMM.getCheckedRadioButtonId();
                        View radioButton = rg_IntervaloMM.findViewById(radioButtonID);
                        int idx = rg_IntervaloMM.indexOfChild(radioButton);

                        switch(idx){
                            case 0:
                                MinutosMM = 1;
                                break;
                            case 1:
                                MinutosMM = 5;
                                break;
                            case 2:
                                MinutosMM = 15;
                                break;
                        }

                        leuEasyTrafo = false;
                        byte[] comando = new ComandoAbsoluto.ABNT73()
                                .comMedidorNumero(num_medidor)
                                .build(MinutosMM, (tipo_medidor == TipoMedidor.ABOSOLUTO));
                        enviarComando(comando);
                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        if (imm != null)
                            imm.hideSoftInputFromWindow(intervalo_mm.getWindowToken(), 0);
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        if (imm != null)
                            imm.hideSoftInputFromWindow(intervalo_mm.getWindowToken(), 0);
                        // do nothing
                    }
                }).create();


        alertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertDialog.show();
    }

    private void ajustaDataHora(boolean data, MedidorAbsoluto medidor) {
        byte[] comando;
        Calendar localCalendar = Calendar.getInstance();
        String num_medidor = "";
        if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
            num_medidor = medidor.numero;
        }
        if (data) {
            comando = new ComandoAbsoluto.ABNT29()
                    .comMedidorNumero(num_medidor)
                    .build(localCalendar, (tipo_medidor == TipoMedidor.ABOSOLUTO));
        } else {
            comando = new ComandoAbsoluto.ABNT30()
                    .comMedidorNumero(num_medidor)
                    .build(localCalendar, (tipo_medidor == TipoMedidor.ABOSOLUTO));
        }
        enviarComando(comando);
    }

    private void alteracaoIntrvaloMM(MedidorAbsoluto medidor, byte Minutos) {
        byte[] comando = new ComandoAbsoluto.ABNT73()
                .comMedidorNumero(medidor.numero)
                .build(Minutos, (tipo_medidor == TipoMedidor.ABOSOLUTO));
        enviarComando(comando);
    }

    private void exibirLeituraParametros(MedidorAbsoluto medidor) {
        String numero = "";
        String unidadeConsumidora = "";
        if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
            numero = medidor.numero;
            unidadeConsumidora = medidor.unidadeConsumidora;
        } else {
            numero = mNsMedidor;
            unidadeConsumidora = numMedidor;
        }
        Log.i("test->", numero);
        final Intent intent = new Intent(DeviceActivity.this, ResponseActivity.class);
        intent.putExtra(Consts.EXTRAS_NUMERO_MEDIDOR, numero);
        intent.putExtra(Consts.EXTRAS_UNIDADE_CONSUMIDORA, unidadeConsumidora);
        intent.putExtra(Consts.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(Consts.EXTRAS_EASY_TRAFO, (tipo_medidor != TipoMedidor.ABOSOLUTO));
        intent.putExtra(Consts.EXTRAS_IS_SMFISCAL, strSMFiscal);

        startActivity(intent);
    }

    private void exibirGrandezasInstantaneas(MedidorAbsoluto medidor) {
        String numero = "";
        String unidadeConsumidora = "";
        if (tipo_medidor == TipoMedidor.ABOSOLUTO) {
            numero = medidor.numero;
            unidadeConsumidora = medidor.unidadeConsumidora;
        } else {
            numero = mNsMedidor;
            unidadeConsumidora = numMedidor;
        }
        final Intent intent = new Intent(DeviceActivity.this, PaginaFiscalActivity.class);
        intent.putExtra(Consts.EXTRAS_VERSAO_MEDIDOR, versaoMedidor);
        intent.putExtra(Consts.EXTRAS_NUMERO_MEDIDOR, numero);
        intent.putExtra(Consts.EXTRAS_UNIDADE_CONSUMIDORA, unidadeConsumidora);
        intent.putExtra(Consts.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(Consts.EXTRAS_EASY_TRAFO, (tipo_medidor != TipoMedidor.ABOSOLUTO));
        startActivity(intent);
    }

    private void trocaNomeUnidade(MedidorAbsoluto medidor) {
        final View alertText = getLayoutInflater().inflate(R.layout.alert_text, null);
        final EditText nome = alertText.findViewById(R.id.alert_text);
        nome.setFilters(new InputFilter[]{new InputFilter.LengthFilter(14)});
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                android.R.style.Theme_Material_Dialog_Alert);
        AlertDialog alertDialog = builder
                .setTitle("Trocar Nome")
                .setMessage("Digite o nome do ponto")
                .setView(alertText)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String novo_nome = nome.getText().toString();
                        if (!novo_nome.trim().isEmpty()) {
                            byte[] comando = new ComandoAbsoluto.ABNT87()
                                    .comMedidorNumero(medidor.numero)
                                    .build(novo_nome, (tipo_medidor == TipoMedidor.ABOSOLUTO));
                            enviarComando(comando);
                            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                            if (imm != null)
                                imm.hideSoftInputFromWindow(alertText.getWindowToken(), 0);
                        } else {
                            Toast.makeText(DeviceActivity.super.getApplicationContext(),
                                    "Informe o novo nome",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        if (imm != null)
                            imm.hideSoftInputFromWindow(alertText.getWindowToken(), 0);
                        // do nothing
                    }
                }).create();
        alertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertDialog.show();
    }

    private void resetRegistradores(MedidorAbsoluto medidor, byte tipo) {
        String titulo = "Reset de Registradores";
        String msg = "Você deseja realmente realizar o reset dos registradores?\r\nEsta operação é irreversível!";
        if (tipo == 3) {
            msg = "Você deseja realmente realizar o reset dos registradores de QEE?\r\nEsta operação é irreversível!";
            titulo = "Reset QEE";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                android.R.style.Theme_Material_Dialog_Alert);
        AlertDialog alertDialog = builder
                .setTitle(titulo)
                .setMessage(msg)
                .setPositiveButton("SIM", (dialog, which) -> {

                    byte[] comando = new ComandoAbsoluto.AB07ResetRegistradores()
                            .comMedidorNumero(medidor.numero)
                            .build((tipo_medidor == TipoMedidor.ABOSOLUTO), tipo);
                    enviarComando(comando);

                    if (comando != null) {
                        Log.d("teste", Arrays.toString(comando));
                    }

                })
                .setNegativeButton("NÃO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /*InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        if (imm != null)
                            imm.hideSoftInputFromWindow(alertText.getWindowToken(), 0);*/
                        // do nothing
                    }
                }).create();
        /*alertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);*/
        alertDialog.show();
    }

    private class MeterEasyTrafoAdapter
            extends RecyclerView.Adapter<MeterEasyTrafoAdapter.MeterETViewHolder> {

        private String numero_medidor = "";
        private String ns_medidor = "";
        private String data_hora = "";

        public class MeterETViewHolder extends RecyclerView.ViewHolder {
            private final View.OnClickListener incidenteClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //medidorSelecionado = medidores.get(getAdapterPosition());
                    exibirMenu(v);
                }
            };

            TextView unidadeConsumidora;
            TextView unidadeConsumidoraInfo;
            TextView nmrMedidorInfo;
            TextView nmrMedidor;
            TextView dataHora;
            TextView dataHoraInfo;
            TextView versao;
            TextView versaoInfo;
            TextView fases;
            TextView moduloNomeTitulo;
            TextView moduloNome;
            TextView moduloEstadoTitulo;
            TextView moduloEstado;
            TextView moduloSinalTitulo;
            TextView moduloSinal;
            ImageView statusMedidor;

            public MeterETViewHolder(@NonNull View view) {
                super(view);
                unidadeConsumidora = view.findViewById(R.id.unidade_consumidora);
                unidadeConsumidoraInfo = view.findViewById(R.id.unidade_consumidora_info);
                nmrMedidorInfo = view.findViewById(R.id.mi_tv_dadoMedidorTrafoInfo);
                nmrMedidor = view.findViewById(R.id.mi_tv_dadoMedidorTrafo);
                dataHora = view.findViewById(R.id.dados_medidor);
                dataHoraInfo = view.findViewById(R.id.dados_medidor_info);
                versao = view.findViewById(R.id.medidor_versao);
                versaoInfo = view.findViewById(R.id.medidor_versao_info);
                fases = view.findViewById(R.id.numero_fases_medidor);
                fases.setVisibility(LinearLayout.GONE);
                statusMedidor = view.findViewById(R.id.status_medidor);
                statusMedidor.setVisibility(LinearLayout.GONE);
                unidadeConsumidora.setOnClickListener(incidenteClick);
                view.findViewById(R.id.incidente_medidor).setOnClickListener(incidenteClick);
                unidadeConsumidora.setOnClickListener(incidenteClick);
                moduloNome = view.findViewById(R.id.mi_tv_modulo_nome);
                moduloNomeTitulo = view.findViewById(R.id.mi_tv_modulo_nome_title);
                moduloEstado = view.findViewById(R.id.mi_tv_modulo_estado);
                moduloEstadoTitulo = view.findViewById(R.id.mi_tv_modulo_estado_title);
                moduloSinal = view.findViewById(R.id.mi_tv_modulo_sinal);
                moduloSinalTitulo = view.findViewById(R.id.mi_tv_modulo_sinal_title);
            }
        }


        @NonNull
        @Override
        public MeterETViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.meter_item, viewGroup, false);
            return new MeterETViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MeterETViewHolder meterViewHolder, int i) {
            medidorSelecionado = new MedidorAbsoluto();
            medidorSelecionado.numero = ns_medidor;

            meterViewHolder.unidadeConsumidoraInfo.setText("Ponto:");
            meterViewHolder.unidadeConsumidora.setText(numero_medidor);

            meterViewHolder.nmrMedidorInfo.setVisibility(View.VISIBLE);
            meterViewHolder.nmrMedidor.setVisibility(View.VISIBLE);
            meterViewHolder.nmrMedidor.setText(medidorSelecionado.numero);
            monitoramentoNumeroEasy = medidorSelecionado.numero;

            meterViewHolder.dataHoraInfo.setText("Data e Hora:");
            meterViewHolder.dataHora.setText(data_hora);
            meterViewHolder.versaoInfo.setText("Versão:");
            meterViewHolder.versao.setText(versaoMedidor);

            if (!tipoModulo.isEmpty()) {
                meterViewHolder.moduloNomeTitulo.setVisibility(View.VISIBLE);
                meterViewHolder.moduloNome.setVisibility(View.VISIBLE);
                meterViewHolder.moduloEstado.setVisibility(View.VISIBLE);
                meterViewHolder.moduloEstadoTitulo.setVisibility(View.VISIBLE);
                meterViewHolder.moduloSinal.setVisibility(View.VISIBLE);
                meterViewHolder.moduloSinalTitulo.setVisibility(View.VISIBLE);

                meterViewHolder.moduloNome.setText(tipoModulo);
                meterViewHolder.moduloEstado.setText(estadoModulo);
                meterViewHolder.moduloSinal.setText(sinalModulo+" dBm");
            }


            SimpleDateFormat smf = new SimpleDateFormat("dd/MM/yyyy' 'HH:mm:ss");
            Date dt = new Date();
            try {
                dt = smf.parse(data_hora);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            long diff = System.currentTimeMillis() - dt.getTime();
            long seconds = diff / 1000;

            if (seconds < 0) {
                seconds *= -1;
            }

            if (seconds > 300) {
                meterViewHolder.dataHora.setTextColor(Color.RED);
            }

        }

        public MeterEasyTrafoAdapter(String medidor, String datahora, String numero) {
            this.data_hora = datahora;
            this.numero_medidor = medidor;
            this.ns_medidor = numero;
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }


}
