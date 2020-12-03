package com.starmeasure.absoluto;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.kishan.askpermission.AskPermission;
import com.kishan.askpermission.PermissionCallback;
import com.obsez.android.lib.filechooser.ChooserDialog;
import com.obsez.android.lib.filechooser.ChooserDialog.Result;

import java.io.File;
import java.io.IOException;

public class SelecaoPasta {
    private final int REQUEST = 123;
    
    private AskPermission.Builder askPermission;
    private ChooserDialog dialog;
    
    private String nomeNovaPasta;

    private OnSelecaoListener listener;
    
    interface OnSelecaoListener {
        void onPastaSelecionada(String pasta);
    }
    
    public void setOnSelecaoListener(OnSelecaoListener listener) {
        this.listener = listener;
    }
    
    public void abrePastas(String nomeNovaPasta) {
        this.nomeNovaPasta = nomeNovaPasta;

        askPermission.request(REQUEST);
    }
    
    public SelecaoPasta(Context context, String nomeNovaPasta) {
        dialog = new ChooserDialog(context)
                .withFilter(true, false)
                .withStartFile(nomeNovaPasta)
                .withStringResources("Seleciona a pasta", "   OK   ","Cancelar")
                .withChosenListener(new Result() {
                    @Override
                    public void onChoosePath(String dir, File dirFile) {


/*
                        if (!dir.isEmpty()) {
                            File novaPasta = new File(dir, nomeNovaPasta);
                            //File arquivo = new File(novaPasta, nomeArquivo);

                            if (!novaPasta.exists()) {
                                novaPasta.mkdirs();
                            }
                        }
                        */
                        File novaPasta = new File(dir);
                        File arquivo = new File(novaPasta, "mge_tst.txt");

                        if (!arquivo.exists()) {
                            try {
                                arquivo.createNewFile();
                                if (listener != null) {
                                    listener.onPastaSelecionada(dir);
                                }
                                arquivo.delete();
                            } catch (IOException e) {
                                Toast.makeText(context, "SEM PERMISSÃO PARA: " + dir, Toast.LENGTH_LONG).show();
                                dialog.build();
                            }
                        }
                    }
                })
                .disableTitle(false)
                .build();
        
        askPermission = new AskPermission.Builder((Activity) context)
                .setPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .setCallback(new PermissionCallback() {
                    @Override
                    public void onPermissionsGranted(int requestCode) {
                        dialog.show();
                    }
                    
                    @Override
                    public void onPermissionsDenied(int requestCode) {
                        //...
                    }
                });
    }
}
