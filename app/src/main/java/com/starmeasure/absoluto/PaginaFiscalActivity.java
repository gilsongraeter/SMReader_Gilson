package com.starmeasure.absoluto;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.starmeasure.absoluto.fasores.DiagramaFasorialView;
import com.starmeasure.absoluto.fasores.DiagramaFasorialView.Fasor;
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
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class PaginaFiscalActivity extends BaseBluetoothActivity {

    private static final String TAG = PaginaFiscalActivity.class.getSimpleName();
    public static final int TEMPO_LEITURA_PERIODICA = 1000;

    private String mNumeroMedidor;
    private String mUnidadeConsumidora;
    private String mNumeroMedidorArquivo;
    private String mDeviceName;
    private String mDataMedidor;
    private String mVersaoMedidor;
    private int mByteCount = 0;
    private boolean isEasyTrafo = false;

    private ByteArrayOutputStream mByteArrayData = new ByteArrayOutputStream();
    RespostaAbsoluto.GrandezasInstantaneas g;

    private ImageButton mAtualizar;
    private ImageButton mSalvar;
    private TextView mHorarioMedidor;
    private TextView mTempoPacote;
    private TextView mHorarioLocal;
    private TextView mTemperatura;
    private TextView mVersao;
    private TextView mVa;
    private TextView mVb;
    private TextView mVc;
    private TextView mVab;
    private TextView mVbc;
    private TextView mVca;
    private TextView mAnguloVa;
    private TextView mAnguloVb;
    private TextView mAnguloVc;
    private TextView mIa;
    private TextView mIb;
    private TextView mIc;
    private TextView mDefasagemIa;
    private TextView mDefasagemIb;
    private TextView mDefasagemIc;
    private TextView mFrequencia;
    private TextView mPa;
    private TextView mPb;
    private TextView mPc;
    private TextView mPt;
    private TextView mQa;
    private TextView mQb;
    private TextView mQc;
    private TextView mQt;
    private TextView mSa;
    private TextView mSb;
    private TextView mSc;
    private TextView mSt;
    private TextView mFPa;
    private TextView mFPb;
    private TextView mFPc;
    private TextView mFPt;
    private TextView mCFa;
    private TextView mCFb;
    private TextView mCFc;
    private TextView mCFt;
    private TextView mDhtA;
    private TextView mDhtB;
    private TextView mDhtC;
    private TextView mDhcA;
    private TextView mDhcB;
    private TextView mDhcC;

    private DiagramaFasorialView mDiagramaFasorial;

    private TimerTask mAtualizarTask;
    private Timer mAtualizarTimer = new Timer();

    private long tempoEnvio = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pagina_fiscal);

        final Intent intent = getIntent();
        mNumeroMedidor = intent.getStringExtra(Consts.EXTRAS_NUMERO_MEDIDOR);
        mUnidadeConsumidora = intent.getStringExtra(Consts.EXTRAS_UNIDADE_CONSUMIDORA);
        mDeviceName = intent.getStringExtra(Consts.EXTRAS_DEVICE_NAME);
        isEasyTrafo = intent.getBooleanExtra(Consts.EXTRAS_EASY_TRAFO, false);
        mVersaoMedidor = intent.getStringExtra(Consts.EXTRAS_VERSAO_MEDIDOR);

        String formato_unidade = "UC: %s";
        String formato_medidor = "Medidor: %s";

        if (isEasyTrafo) {
            formato_unidade = "Ponto: %s";
        }

        ((TextView) findViewById(R.id.dados_medidor)).setText(
                String.format(formato_medidor, mNumeroMedidor));
        ((TextView) findViewById(R.id.unidade_consumidora)).setText(
                String.format(formato_unidade, mUnidadeConsumidora));

        findViewById(R.id.voltar_tela).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mSalvar = findViewById(R.id.salvar_dados);
        mSalvar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                salvarArquivo();
            }
        });

        mAtualizar = findViewById(R.id.atualizar_dados);
        mAtualizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAtualizar.getAnimation() == null) {
                    iniciarLeituraPeriodica();
                } else {
                    pararLeituraPeriodica();
                }
            }
        });
        mTemperatura = findViewById(R.id.temperatura);
        mVersao = findViewById(R.id.pagina_fiscal_versao);
        mHorarioLocal = findViewById(R.id.horario_local);
        Date data = Calendar.getInstance().getTime();
        mHorarioLocal.setText(String.format(getString(R.string.horario_local),
                new SimpleDateFormat("dd/MM/yyyy' 'HH:mm:ss").format(data)));
        mHorarioMedidor = findViewById(R.id.horario_medidor);
        mTempoPacote = findViewById(R.id.tempo_pacote);
        mVa = findViewById(R.id.va);
        mVb = findViewById(R.id.vb);
        mVc = findViewById(R.id.vc);
        mVab = findViewById(R.id.vab);
        mVbc = findViewById(R.id.vbc);
        mVca = findViewById(R.id.vca);
        mAnguloVa = findViewById(R.id.angulo_va);
        mAnguloVb = findViewById(R.id.angulo_vb);
        mAnguloVc = findViewById(R.id.angulo_vc);
        mIa = findViewById(R.id.ia);
        mIb = findViewById(R.id.ib);
        mIc = findViewById(R.id.ic);
        mDefasagemIa = findViewById(R.id.defasagem_ia);
        mDefasagemIb = findViewById(R.id.defasagem_ib);
        mDefasagemIc = findViewById(R.id.defasagem_ic);
        mFrequencia = findViewById(R.id.frequencia);
        mPa = findViewById(R.id.pa);
        mPb = findViewById(R.id.pb);
        mPc = findViewById(R.id.pc);
        mPt = findViewById(R.id.pt);
        mQa = findViewById(R.id.qa);
        mQb = findViewById(R.id.qb);
        mQc = findViewById(R.id.qc);
        mQt = findViewById(R.id.qt);
        mSa = findViewById(R.id.sa);
        mSb = findViewById(R.id.sb);
        mSc = findViewById(R.id.sc);
        mSt = findViewById(R.id.st);
        mFPa = findViewById(R.id.fpa);
        mFPb = findViewById(R.id.fpb);
        mFPc = findViewById(R.id.fpc);
        mFPt = findViewById(R.id.fpt);
        mCFa = findViewById(R.id.cfa);
        mCFb = findViewById(R.id.cfb);
        mCFc = findViewById(R.id.cfc);
        mCFt = findViewById(R.id.cft);
        mDhtA = findViewById(R.id.dht_a);
        mDhtB = findViewById(R.id.dht_b);
        mDhtC = findViewById(R.id.dht_c);
        mDhcA = findViewById(R.id.dhc_a);
        mDhcB = findViewById(R.id.dhc_b);
        mDhcC = findViewById(R.id.dhc_c);
        mDiagramaFasorial = findViewById(R.id.diagrama_fasorial);

        bindGattService(mServiceConnection);
    }

    private void iniciarLeituraPeriodica() {
        iniciarAnimacaoAtualizar();

        mAtualizarTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enviarLeituraGrandezasInstantaneas();
                    }
                });
            }
        };
        mAtualizarTimer.scheduleAtFixedRate(mAtualizarTask, 0, TEMPO_LEITURA_PERIODICA);
    }

    private void pararLeituraPeriodica() {
        pararAnimacaoAtualizar();

        mAtualizarTask.cancel();
        mAtualizarTimer.purge();
    }

    private void iniciarAnimacaoAtualizar() {
        Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
        rotation.setRepeatCount(Animation.INFINITE);
        mAtualizar.startAnimation(rotation);
    }

    private void pararAnimacaoAtualizar() {
        mAtualizar.clearAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            setupGattService();
            iniciarLeituraPeriodica();
        } else {
            mBluetoothLeService = new BluetoothLeService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pararLeituraPeriodica();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindGattService(mServiceConnection);
    }

    protected ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            setupGattService();
            iniciarLeituraPeriodica();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private void enviarLeituraGrandezasInstantaneas() {
        byte[] data = new ComandoAbsoluto.ABNT14GrandezasInstantaneas()
                .comMedidorNumero(mNumeroMedidor)
                .build(!isEasyTrafo);
        tempoEnvio = sendData(data);
    }

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
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_BYTES);
                try {
                    mByteArrayData.write(data);
                    mByteCount += data.length;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (mByteCount >= 258) {
                    Log.d(TAG, "################# TEMPO DA PAGINA FISCAL: " + Float.toString(System.currentTimeMillis() - tempoEnvio) + "ms");
                    mTempoPacote.setText("Tempo: " + (System.currentTimeMillis() - tempoEnvio) + "ms");
                    processData();
                    mByteCount = 0;
                    mByteArrayData.reset();
                }
            }
        }
    };

    private void enviarLimparOcorrencia(String numeroMedidor, byte codigoOcorrencia) {
        final byte[] comando = new ComandoAbsoluto.LimpezaOcorrenciasMedidor()
                .comMedidorNumero(numeroMedidor)
                .comCodigoOcorrencia(codigoOcorrencia)
                .build(!isEasyTrafo);
        sendData(comando);
    }

    private void processarRespostaOcorrencia(RespostaAbsoluto respostaAbsoluto) {
        Toast.makeText(PaginaFiscalActivity.this,
                getString(R.string.text_ocorrencia_medidor),
                Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Ocorrência: " + respostaAbsoluto.toString());
        enviarLimparOcorrencia(respostaAbsoluto.getSufixoNumeroMedidor(),
                respostaAbsoluto.getCodigoOcorrencia());
    }

    private void processData() {
        byte[] data = mByteArrayData.toByteArray();
        RespostaAbsoluto resposta = new RespostaAbsoluto(data);
        if (resposta.isOcorrencia()) {
            processarRespostaOcorrencia(resposta);
        }
        else if (resposta.isRespostaOcorrencias()) {
            //TODO FIM
        }
        else if (resposta.isGrandezasInstantaneas()) {
            processarRespostaGrandezasInstantaneas(resposta);
        } else {
            Log.d(TAG, "Recebeu pacote não tratado: " + Util.ByteArrayToHexString(data));
        }
    }

    private void processarRespostaGrandezasInstantaneas(RespostaAbsoluto resposta) {
        g = resposta.interpretarGrandezasInstantaneas();
        Locale br = Locale.forLanguageTag("BR");

        g.VAB = calcularTensaoDeLinha(g.VA, g.anguloVA, g.VB, g.anguloVB);
        g.VBC = calcularTensaoDeLinha(g.VB, g.anguloVB, g.VC, g.anguloVC);
        g.VCA = calcularTensaoDeLinha(g.VC, g.anguloVC, g.VA, g.anguloVA);

        g.cosphiA = Math.abs((float) Math.cos(g.defasagemIA * Math.PI / 180));
        g.cosphiB = Math.abs((float) Math.cos(g.defasagemIB * Math.PI / 180));
        g.cosphiC = Math.abs((float) Math.cos(g.defasagemIC * Math.PI / 180));
        g.SA = Math.abs(g.PA / g.cosphiA);
        g.SB = Math.abs(g.PB / g.cosphiA);
        g.SC = Math.abs(g.PC / g.cosphiA);
        g.ST = Math.abs((float) Math.sqrt(g.PT * g.PT + g.QT * g.QT));
        if (g.ST > 0)
            g.cosphiT = Math.abs(g.PT) / g.ST;
        else
            g.cosphiT = 1;

        g.FPA = 100 * g.cosphiA;
        g.FPB = 100 * g.cosphiB;
        g.FPC = 100 * g.cosphiC;
        g.FPT = 100 * g.cosphiT;
        mTemperatura.setText(String.format(br, getString(R.string.temperatura), g.temperatura));
        mVersao.setText(getString(R.string.text_versao, mVersaoMedidor));
        mHorarioLocal.setText(String.format(getString(R.string.horario_local),
                new SimpleDateFormat("dd/MM/yyyy' 'HH:mm:ss").format(Calendar.getInstance().getTime())));
        mHorarioMedidor.setText(String.format(getString(R.string.horario_medidor), g.horario));
        mVa.setText(String.format(br, getResources().getString(R.string.formata_tensao), formataValor(g.VA)));
        mVb.setText(String.format(br, getResources().getString(R.string.formata_tensao), formataValor(g.VB)));
        mVc.setText(String.format(br, getResources().getString(R.string.formata_tensao), formataValor(g.VC)));
        mVab.setText(String.format(br, getResources().getString(R.string.formata_tensao), formataValor(g.VAB)));
        mVbc.setText(String.format(br, getResources().getString(R.string.formata_tensao), formataValor(g.VBC)));
        mVca.setText(String.format(br, getResources().getString(R.string.formata_tensao), formataValor(g.VCA)));
        mIa.setText(String.format(br, getResources().getString(R.string.formata_corrente), formataValor(g.IA)));
        mIb.setText(String.format(br, getResources().getString(R.string.formata_corrente), formataValor(g.IB)));
        mIc.setText(String.format(br, getResources().getString(R.string.formata_corrente), formataValor(g.IC)));
        mAnguloVa.setText(String.format(br, getResources().getString(R.string.formata_angulo), g.anguloVA));
        mAnguloVb.setText(String.format(br, getResources().getString(R.string.formata_angulo), g.anguloVB));
        mAnguloVc.setText(String.format(br, getResources().getString(R.string.formata_angulo), g.anguloVC));
        mDefasagemIa.setText(String.format(br, getResources().getString(R.string.formata_angulo), g.defasagemIA));
        mDefasagemIb.setText(String.format(br, getResources().getString(R.string.formata_angulo), g.defasagemIB));
        mDefasagemIc.setText(String.format(br, getResources().getString(R.string.formata_angulo), g.defasagemIC));
        mFrequencia.setText(String.format(br, getResources().getString(R.string.formata_frequencia), g.frequencia));
        mPa.setText(String.format(br, getString(R.string.formata_potencia_ativa), formataValor(g.PA)));
        mPb.setText(String.format(br, getString(R.string.formata_potencia_ativa), formataValor(g.PB)));
        mPc.setText(String.format(br, getString(R.string.formata_potencia_ativa), formataValor(g.PC)));
        mPt.setText(String.format(br, getString(R.string.formata_potencia_ativa), formataValor(g.PT)));
        mQa.setText(String.format(br, getString(R.string.formata_potencia_reativa), formataValor(g.QA)));
        mQb.setText(String.format(br, getString(R.string.formata_potencia_reativa), formataValor(g.QB)));
        mQc.setText(String.format(br, getString(R.string.formata_potencia_reativa), formataValor(g.QC)));
        mQt.setText(String.format(br, getString(R.string.formata_potencia_reativa), formataValor(g.QT)));
        mSa.setText(String.format(br, getString(R.string.formata_potencia_aparente), formataValor(g.SA)));
        mSb.setText(String.format(br, getString(R.string.formata_potencia_aparente), formataValor(g.SB)));
        mSc.setText(String.format(br, getString(R.string.formata_potencia_aparente), formataValor(g.SC)));
        mSt.setText(String.format(br, getString(R.string.formata_potencia_aparente), formataValor(g.ST)));
        mFPa.setText(String.format(br, getString(R.string.formata_fator_potencia), g.FPA));
        mFPb.setText(String.format(br, getString(R.string.formata_fator_potencia), g.FPB));
        mFPc.setText(String.format(br, getString(R.string.formata_fator_potencia), g.FPC));
        mFPt.setText(String.format(br, getString(R.string.formata_fator_potencia), g.FPT));
        mCFa.setText(String.format(br, getString(R.string.formata_cosseno_fi), g.cosphiA));
        mCFb.setText(String.format(br, getString(R.string.formata_cosseno_fi), g.cosphiB));
        mCFc.setText(String.format(br, getString(R.string.formata_cosseno_fi), g.cosphiC));
        mCFt.setText(String.format(br, getString(R.string.formata_cosseno_fi), g.cosphiT));
        mDhtA.setText(String.format(br, getString(R.string.formata_dht), g.DHT_A));
        mDhtB.setText(String.format(br, getString(R.string.formata_dht), g.DHT_B));
        mDhtC.setText(String.format(br, getString(R.string.formata_dht), g.DHT_C));
        mDhcA.setText(String.format(br, getString(R.string.formata_dht), g.DHC_A));
        mDhcB.setText(String.format(br, getString(R.string.formata_dht), g.DHC_B));
        mDhcC.setText(String.format(br, getString(R.string.formata_dht), g.DHC_C));

        float maxV = Math.max(g.VA, Math.max(g.VB, g.VC)) / (float) 0.9;
        if (maxV == 0)
            maxV = 1;
        float maxI = Math.max(g.IA, Math.max(g.IB, g.IC)) / (float) 0.6;
        if (maxI == 0)
            maxI = 1;

        List<Fasor> fasores = new ArrayList<>();
        fasores.add(new Fasor(g.VA/maxV, g.anguloVA, 0xffff0000, "VA", Fasor.HeadType.ARROW));
        fasores.add(new Fasor(g.VB/maxV, g.anguloVB, 0xff0000ff, "VB", Fasor.HeadType.ARROW));
        fasores.add(new Fasor(g.VC/maxV, g.anguloVC, 0xff00ff00, "VC", Fasor.HeadType.ARROW));
        fasores.add(new Fasor(g.IA /maxI, g.defasagemIA + g.anguloVA, 0xffff007f, "IA", Fasor.HeadType.ARROW));
        fasores.add(new Fasor(g.IB /maxI, g.defasagemIB + g.anguloVB, 0xff007fff, "IB", Fasor.HeadType.ARROW));
        fasores.add(new Fasor(g.IC /maxI, g.defasagemIC + g.anguloVC, 0xff7fff00, "IC", Fasor.HeadType.ARROW));
        mDiagramaFasorial.setFasors(fasores);
        mNumeroMedidorArquivo = resposta.getNumeroMedidor();
    }

    private String formataValor(float valor) {
        Locale br = Locale.forLanguageTag("BR");
        String sufixo = "";
        String resultado;

        float v = Math.abs(valor);
        if (v > 1000000000) {
            valor /= 1000000000;
            sufixo = "G";
        } else if (v > 1000000) {
            valor /= 1000000;
            sufixo = "M";
        } else if (valor > 1000){
            valor /= 1000;
            sufixo = "k";
        }

        v = Math.abs(valor);
        if (v < 10)
            resultado = String.format(br, "%.3f %s", valor, sufixo);
        else if (v < 100)
            resultado = String.format(br, "%.2f %s", valor, sufixo);
        else if (v < 1000)
            resultado = String.format(br, "%.1f %s", valor, sufixo);
        else
            resultado = String.format(br, "%.0f %s", valor, sufixo);
        return resultado;
    }

    private float calcularTensaoDeLinha(float v1, float anguloV1, float v2, float anguloV2) {
        float anguloVAB = converterGrausParaRadianos(corrigirAngulo(anguloV1 - anguloV2));
        return (float) Math.sqrt(Math.abs(((v1 * v1) + (v2 * v2))
                - 2 * v1 * v2 * Math.cos(anguloVAB)));
    }

    private float corrigirAngulo(float angulo) {
        if (angulo > 180)
            return angulo - 360;
        if (angulo < -180)
            return angulo + 360;
        return angulo;
    }

    private float converterGrausParaRadianos(float anguloGraus) {
        return (float) (anguloGraus * Math.PI / 180);
    }

    private void salvarArquivo() {
        Date date = Calendar.getInstance().getTime();
        String nomeArquivo = String.format("PaginaFiscal_%s_%s_%s.csv",  mNumeroMedidorArquivo, mDeviceName.substring(4).trim(),
                new SimpleDateFormat("ddMMyyyy'_'HHmmss").format(date));
        if (!Arquivo.salvarArquivo(PaginaFiscalActivity.this, nomeArquivo, getDadosArquivo())) {
            Toast.makeText(PaginaFiscalActivity.this, "Erro ao salvar arquivo",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(PaginaFiscalActivity.this, "Arquivo salvo com sucesso:\n" + nomeArquivo,
                    Toast.LENGTH_LONG).show();
        }
    }

    private String getDadosArquivo() {
        DecimalFormat f = new DecimalFormat("0000000000");
        Date date = Calendar.getInstance().getTime();
        return
                "Medidor;" + mNumeroMedidor + "\n" +
                        "Data/Hora Medidor;" + mDataMedidor + "\n" +
                        "Data/Hora Local;" + new SimpleDateFormat("dd/MM/yyyy' 'HH:mm:ss").format(date) + "\n" +
                        "VA;" + mVa.getText() + "\n" +
                        "VB;" + mVb.getText() + "\n" +
                        "VC;" + mVc.getText() + "\n" +
                        "VAB;" + mVab.getText() + "\n" +
                        "VBC;" + mVbc.getText() + "\n" +
                        "VCA;" + mVca.getText() + "\n" +
                        "Angulo Fase A;" + mAnguloVa.getText() + "\n" +
                        "Angulo Fase B;" + mAnguloVb.getText() + "\n" +
                        "Angulo Fase C;" + mAnguloVc.getText() + "\n" +
                        "IA;" + mIa.getText() + "\n" +
                        "IB;" + mIb.getText() + "\n" +
                        "IC;" + mIc.getText() + "\n" +
                        "Defasagem A;" + mDefasagemIa.getText() + "\n" +
                        "Defasagem B;" + mDefasagemIb.getText() + "\n" +
                        "Defasagem C;" + mDefasagemIc.getText() + "\n" +
                        "Frequencia;" + mFrequencia.getText() + "\n" +
                        "Potencia Ativa A;" + mPa.getText() + "\n" +
                        "Potencia Ativa B;" + mPb.getText() + "\n" +
                        "Potencia Ativa C;" + mPc.getText() + "\n" +
                        "Potencia Ativa Trifasico;" + mPt.getText() + "\n" +
                        "Potencia Reativa A;" + mQa.getText() + "\n" +
                        "Potencia Reativa B;" + mQb.getText() + "\n" +
                        "Potencia Reativa C;" + mQc.getText() + "\n" +
                        "Potencia Reativa Trifasico;" + mQt.getText() + "\n" +
                        "Potencia Aparente A;" + mSa.getText() + "\n" +
                        "Potencia Aparente B;" + mSb.getText() + "\n" +
                        "Potencia Aparente C;" + mSc.getText() + "\n" +
                        "Potencia Aparente Trifasico;" + mSt.getText() + "\n" +
                        "Fator de Potencia A;" + mFPa.getText() + "\n" +
                        "Fator de Potencia B;" + mFPb.getText() + "\n" +
                        "Fator de Potencia C;" + mFPc.getText() + "\n" +
                        "Fator de Potencia Trifasico;" + mFPt.getText() + "\n" +
                        "Coeficiente Fi A;" + mCFa.getText() + "\n" +
                        "Coeficiente Fi B;" + mCFb.getText() + "\n" +
                        "Coeficiente Fi C;" + mCFc.getText() + "\n" +
                        "Coeficiente Fi Trifasico;" + mCFt.getText() + "\n" +
                        "Temperatura;" + String.format(Locale.forLanguageTag("BR"),"%.1fº", g.temperatura) + "\n";
    }
}
