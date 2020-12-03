package com.starmeasure.absoluto.filemanager;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.starmeasure.absoluto.Arquivo;
import com.starmeasure.absoluto.R;
import com.starmeasure.absoluto.filemanager.adapter.FileManagerCargasAdapter;
import com.starmeasure.absoluto.filemanager.controller.CargaController;
import com.starmeasure.absoluto.filemanager.model.Carga;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class FileManagerCargasActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String classTag = "FM-Cargas->";

    private Thread downloadThread;
    private FTPClient ftpClient;
    private boolean isDownloadRunning = false;
    private FileManagerCargasAdapter adapter;
    private RecyclerView rvList;
    private TextView downloadTxt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager_cargas);

        rvList = findViewById(R.id.afm_cargas_rv_list);

        findViewById(R.id.afm_cargas_imgbtn_close).setOnClickListener(this);
        findViewById(R.id.afm_cargas_imgbtn_download).setOnClickListener(this);
        downloadTxt = findViewById(R.id.afm_downloading_text);
    }

    @Override
    protected void onStart() {
        super.onStart();

        setAdapter();

        ftpClient = new FTPClient();
    }

    @Override
    public void onClick(@NotNull View v) {
        if (v.getId() == R.id.afm_cargas_imgbtn_close) {
            finish();
        } else if (v.getId() == R.id.afm_cargas_imgbtn_download) {
            try {
                runDownloadThread();
            } catch (IllegalThreadStateException e) {
                downloadThread = null;
                runDownloadThread();
                e.printStackTrace();
            }
        }
    }

    private void setAdapter() {
        adapter = new FileManagerCargasAdapter(this, getFileList());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvList.setLayoutManager(layoutManager);
        rvList.setAdapter(adapter);

        adapter.setOnItemClickListener((position, v) -> {
            File file = adapter.getFile(position);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Aviso");
            builder.setMessage("Você tem certeza que deseja excluir o arquivo:\n" + file.getName() + " ?\n\nDados excluídos não poderão ser recuperados.");
            builder.setPositiveButton("Sim", (dialog, which) -> {
                adapter.deleteFile(file, position);
            });
            builder.setNegativeButton("Não", null);
            builder.create();
            builder.show();
        });
    }

    @NonNull
    private ArrayList<File> getFileList() {
        ArrayList<File> mList = new ArrayList<>();
        File dir = new File(Arquivo.pathCargas());
        if (dir.exists()) {
            File[] list = dir.listFiles();
            if (list != null && list.length > 0) {
                for (File f : list) {
                    if (f.getName().endsWith("zip")) {
                        f.delete();
                    } else {
                        mList.add(f);
                    }
                }
            }
        } else {
            if (!dir.mkdir()) {
                Log.e(classTag, "Diretorio de cargas não existe");
            }
        }

        return mList;
    }

    private void runDownloadThread() throws IllegalThreadStateException {
        downloadThread = new Thread(downloadRunnable);
        if (!isDownloadRunning) {
            isDownloadRunning = true;
            downloadThread.start();
            hideDownloadButton();
        } else {
            Log.e(classTag, "O processo de download já está rodando");
        }
    }

    private void hideDownloadButton() {
        findViewById(R.id.afm_cargas_imgbtn_download).setVisibility(View.GONE);
        showDownloadView();
    }

    private void showDownloadButton() {
        findViewById(R.id.afm_cargas_imgbtn_download).setVisibility(View.VISIBLE);
        hideDownloadView();
    }

    private void showDownloadView() {
        findViewById(R.id.afm_downloading_root).setVisibility(View.VISIBLE);
    }

    private void hideDownloadView() {
        findViewById(R.id.afm_downloading_root).setVisibility(View.GONE);
    }

    private void showGoodMessage(String message) {
        runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                downloadTxt.setTextColor(getColor(R.color.starmeasure_turquoise));
            } else {
                downloadTxt.setTextColor(getResources().getColor(R.color.starmeasure_turquoise));
            }
            downloadTxt.setText(message);
            Log.i(classTag, message);
        });
    }

    private void showErrorMessage(String message) {
        runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                downloadTxt.setTextColor(getColor(R.color.red_fail));
            } else {
                downloadTxt.setTextColor(getResources().getColor(R.color.red_fail));
            }
            downloadTxt.setText(message);
            Log.e(classTag, message);
        });
    }

    private Runnable downloadRunnable = () -> {
        FileOutputStream os;
        if (ftpClient != null) {
            try {
                if (ftpClient.isConnected()) {
                    Log.i(classTag, "Já há uma conexão com o servidor, iniciando desconexão");
                    ftpClient.disconnect();
                }

                showGoodMessage("Iniciando tentantiva de conexão com o servidor");
                ftpClient.connect("mgers.dyndns.org", 60009);

                if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                    showGoodMessage("Conexão com o servidor realizada\nIniciando tentativa de login");

                    if (ftpClient.login("SGE", "Xyzmge123")) {
                        showGoodMessage("login realizado com sucesso");
                        showGoodMessage("Iniciando configuração da conexão");
                        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                        ftpClient.enterLocalPassiveMode();

                        Log.i(classTag, "Listando arquivos do diretorio");
                        FTPFile[] ftpFiles = ftpClient.listFiles();
                        if (ftpFiles != null && ftpFiles.length > 0) {
                            for (FTPFile ftpFile : ftpFiles) {
                                if (!ftpFile.isFile()) {
                                    continue;
                                }
                                showGoodMessage("Baixando arquivo: " + ftpFile.getName());
                                Carga cargaRemota = CargaController.build(ftpFile.getName());
                                if (cargaRemota != null) {
                                    Log.i(classTag, "Carga remota: " + cargaRemota.toString());
                                    boolean canDownload = true;

                                    ArrayList<File> mLocalList = getFileList();
                                    if (mLocalList.size() > 0) {
                                        for (File mLocalFile : mLocalList) {
                                            Carga cargaLocal = CargaController.build(mLocalFile.getName(), mLocalFile);
                                            if (cargaLocal != null) {
                                                if (cargaLocal.getModelo().equals(cargaRemota.getModelo())) {
                                                    Log.e(classTag, "Já há uma carga deste modelo - \nDetalhes remota: " + cargaRemota + "\nDetalhes local: " + cargaLocal);
                                                    if (cargaLocal.getVersao() == cargaRemota.getVersao()) {
                                                        if (cargaRemota.getRevisao() > cargaLocal.getRevisao()) {
                                                            cargaLocal.getArquivo().delete();
                                                        } else {
                                                            Log.e(classTag, "Revisão remota menor ou igual a local\nDetalhes remota: " + cargaRemota + "\nDetalhes local: " + cargaLocal);
                                                            canDownload = false;
                                                        }
                                                    } else {
                                                        if (cargaRemota.getVersao() > cargaLocal.getVersao()) {
                                                            cargaLocal.getArquivo().delete();
                                                        } else {
                                                            canDownload = false;
                                                            Log.e(classTag, "Versão remota menor ou igual a local\nDetalhes remota: " + cargaRemota + "\nDetalhes local: " + cargaLocal);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (canDownload) {
                                        File localFile = new File(Arquivo.pathCargas(), ftpFile.getName());

                                        os = new FileOutputStream(localFile);
                                        boolean downloadResult = ftpClient.retrieveFile(ftpFile.getName(), os);
                                        if (!downloadResult) {
                                            showErrorMessage("Falha ao baixar arquivo: " + ftpFile.getName());
                                            localFile.delete();
                                        } else {
                                            if (!validaArquivo(localFile)) {
                                                localFile.delete();
                                                showErrorMessage("Falha ao baixar arquivo: " + ftpFile.getName());
                                            } else {
                                                showGoodMessage("Sucesso ao baixar arquivo: " + ftpFile.getName());
                                                if (localFile.getName().endsWith("zip")) {
                                                    Log.i(classTag, "É zip");
                                                    if (Arquivo.isValidZip(localFile)) {
                                                        Log.i(classTag, "Zip valido");
                                                        Arquivo.unzip(localFile, new File(Arquivo.pathCargas()));
                                                    } else {
                                                        Log.i(classTag, "Zip invalido");
                                                    }
                                                } else {
                                                    Log.i(classTag, "Não é zip");
                                                }
                                            }
                                        }
                                        os.close();
                                    } else {
                                        Log.e(classTag, "Can't download");
                                    }
                                } else {
                                    Log.e(classTag, " Carga remota nula: " + ftpFile.getName());
                                }
                            }
                        } else {
                            Log.e(classTag, "Falha ao lista arquivos - diretorio vazio");
                        }
                    } else {
                        showErrorMessage("Falha ao realizar login - Usuário ou senha incorretos");
                    }

                } else {
                    showErrorMessage("Falha ao conectar com o servidor - " + ftpClient.getReplyString());
                }

            } catch (IOException e) {
                e.printStackTrace();
                if (e.getMessage() != null) {
                    showErrorMessage(e.getMessage());
                }
            } finally {
                Log.i(classTag, "Cheguei no finally");
                isDownloadRunning = false;
                runOnUiThread(this::showDownloadButton);
                try {
                    if (ftpClient != null) {
                        if (ftpClient.isConnected()) {
                            Log.i(classTag, "Desconectando do servidor");
                            ftpClient.disconnect();
                        } else {
                            Log.i(classTag, "Não há nenhuma conexão no momento");
                        }
                    } else {
                        Log.e(classTag, "FTPClient nulo - falha ao disconectar");
                    }

                    if (adapter != null) runOnUiThread(() -> adapter.updateList(getFileList()));
                } catch (IOException e) {
                    e.printStackTrace();
                    if (e.getMessage() != null) {
                        Log.e(classTag, e.getMessage());
                    }
                }
            }
        }
    };

    private boolean validaArquivo(File arquivo) throws IOException {
        String mTag = "VALDown";
        boolean resultado = false;
        if (!ftpClient.isConnected()) {
            ftpClient.connect("mgers.dyndns.org", 60009);
            if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                if (ftpClient.login("SGE", "Xyzmge123")) {
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    ftpClient.enterLocalPassiveMode();
                } else {
                    Log.e(mTag, "Usuário ou senha incorretos");
                }
            } else {
                Log.e(mTag, "Falha ao conectar com o servidor");
            }
        }

        FTPFile[] ftpFiles = ftpClient.listFiles();
        if (ftpFiles != null && ftpFiles.length > 0) {
            for (FTPFile ftpFile : ftpFiles) {
                if (!ftpFile.isFile()) {
                    continue;
                }

                if (ftpFile.getName().equals(arquivo.getName())) {
                    if (ftpFile.getSize() == arquivo.length()) {
                        resultado = true;
                    }
                    break;
                }
            }
        }

        return resultado;
    }

}
