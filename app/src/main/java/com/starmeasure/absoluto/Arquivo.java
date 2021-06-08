package com.starmeasure.absoluto;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import android.util.Log;

import com.starmeasure.absoluto.filemanager.controller.CargaController;
import com.starmeasure.absoluto.filemanager.model.Carga;
import com.starmeasure.absoluto.filemanager.model.FileLeituraPojo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static android.content.ContentResolver.SCHEME_CONTENT;

public class Arquivo {
    private final static String TAG = Arquivo.class.getSimpleName();

    private static final int MY_REQUEST_WRITE_EXTERNAL_STORAGE = 998;

    public static String pathLeituras() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/SMReader/leituras";
    }

    public static String pathCargas() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/SMReader/cargas";
    }

    static boolean salvarArquivo(final Context context, String nome, String conteudo) {
        if (!checkWriteExternalStoragePermission(context)) {
            return false;
        }
        if (!isExternalStorageWritable())
            return false;

        File arquivo = getPublicDocumentsDir(pathLeituras(), nome);

        try {
            FileOutputStream f = new FileOutputStream(arquivo.getAbsolutePath(), false);
            f.write(conteudo.getBytes(Charset.defaultCharset()));
            f.close();
            MediaScannerConnection.scanFile(context, new String[]{arquivo.getAbsolutePath()}, null, null);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "IOException: " + e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    /* Checks if external storage is available for read and write */
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static boolean checkWriteExternalStoragePermission(final Context context) {
        int PermissaoEscrita = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (PermissaoEscrita != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert)
                        .setTitle("Permissão de escrita")
                        .setMessage("Para salvar os arquivos é necessária a permissão para acesso ao sistema de arquivos. É necessário salvar o arquivo novamente após concedida a permissão.")
                        .setPositiveButton("Eu entendi", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions((Activity) context,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        MY_REQUEST_WRITE_EXTERNAL_STORAGE);
                            }
                        })
                        .show();
            } else {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
            return false;
        }
        return true;
    }

    @Nullable
    public static Carga getCargaDeProgramaPorModelo(String modelo) {
        ArrayList<File> list = getCargasDePrograma();
        if (list.size() > 0) {
            for (File file : list) {
                Carga carga = CargaController.build(file.getName(), file);
                if (carga != null) {
                    if (carga.getModelo().equals(modelo)) {
                        return carga;
                    }
                }
            }
        }
        return null;
    }

    public static boolean isValidZip(File file) {
        boolean result = false;
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
            Log.i("isValidZip", "Zip valido: " + zipFile.getName());
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("isValidZip", e.getMessage());
        } finally {
            try {
                if (zipFile != null) {
                    zipFile.close();
                    zipFile = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static void unzip(File zipFile, File targetDirectory) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry zipEntry;
            int count;
            byte[] buffer = new byte[8192];
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                File file = new File(targetDirectory, zipEntry.getName());
                File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " + dir.getAbsolutePath());
                if (zipEntry.isDirectory())
                    continue;
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    while ((count = zipInputStream.read(buffer)) != -1)
                        fileOutputStream.write(buffer, 0, count);
                }
            /* if time should be restored as well
            long time = ze.getTime();
            if (time > 0)
                file.setLastModified(time);
            */
            }
        }
    }

    @NotNull
    public static ArrayList<File> getCargasDePrograma() {
        ArrayList<File> cargas = new ArrayList<>();
        File dir = new File(pathCargas());
        if (dir.exists()) {
            File[] list = dir.listFiles();
            if (list != null && list.length > 0) {
                for (File file : list) {
                    if (file.getName().endsWith(Carga.extensao)) {
                        cargas.add(file);
                    }
                }
            }
        }
        return cargas;
    }

    public static ArrayList<FileLeituraPojo> getAllFilesOnDirectory(Context context) {
        ArrayList<FileLeituraPojo> appFiles = new ArrayList<>();
        File f = new File(pathLeituras());
        if (f.exists()) {
            File[] files = f.listFiles();
            if (files != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
                } else {
                    Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
                }
                if (files.length > 0) {
                    for (File file : files) {
                        String[] splitFileName = file.getName().split("_");
                        FileLeituraPojo fileLeituraPojo = new FileLeituraPojo();
                        fileLeituraPojo.setType(splitFileName[0]);
                        fileLeituraPojo.setEquipment(splitFileName[1]);
                        fileLeituraPojo.setPoint(splitFileName[2]);
                        fileLeituraPojo.setDate(Util.dateBuilder(splitFileName[3]));
                        fileLeituraPojo.setTime(Util.timeBuilder(splitFileName[4]));
                        fileLeituraPojo.setSize(file.length());
                        fileLeituraPojo.setFile(file);
                        fileLeituraPojo.setChecked(false);
                        appFiles.add(fileLeituraPojo);
                    }
                }
            }
        }
        return appFiles;
    }

    private static File getPublicDocumentsDir(String dir, String fileName) {
        Log.d(TAG, "PUBLIC        -> " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        Log.d(TAG, "DIR           -> " + dir);

        File f = new File(dir);
        if (f.exists()) {
            return new File(dir, fileName);
        }
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
/*
        if (dir.isEmpty()) {
            return new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), fileName);
        }
        else {
            return new File(dir, fileName);
        }
*/
    }

    public static String getPath(ContentResolver resolver, Uri uri) {
        if (uri == null) {
            return null;
        }
        if (SCHEME_CONTENT.equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = resolver.query(uri, new String[]{MediaStore.Images.ImageColumns.DATA},
                        null, null, null);
                if (cursor == null || !cursor.moveToFirst()) {
                    return null;
                }
                return cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return uri.getPath();
    }

    public static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;//ww  w.j  a  va2  s. c om
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj,
                    null, null, null);
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static String getRealPathFromURIW(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String str = cursor.getString(column_index);
            return str;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static boolean createDirLeituras() {
        File f = new File(pathLeituras());
        if (!f.exists()) {
            return f.mkdirs();
        } else {
            return true;
        }
    }

    public static boolean createDirCargas() {
        File f = new File(pathCargas());
        if (!f.exists()) {
            return f.mkdirs();
        } else {
            return true;
        }
    }


}

