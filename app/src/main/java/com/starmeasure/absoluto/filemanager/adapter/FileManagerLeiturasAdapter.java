package com.starmeasure.absoluto.filemanager.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.starmeasure.absoluto.R;
import com.starmeasure.absoluto.filemanager.FileManagerLeiturasActivity;
import com.starmeasure.absoluto.filemanager.model.FileLeituraPojo;

import java.io.File;
import java.util.ArrayList;

public class FileManagerLeiturasAdapter extends RecyclerView.Adapter {

    private Context context;
    private ArrayList<FileLeituraPojo> files;
    public ArrayList<FileLeituraPojo> filesChecked = new ArrayList<>();
    public boolean allChecked = false;

    public FileManagerLeiturasAdapter(@NonNull Context context, @NonNull ArrayList<FileLeituraPojo> files) {
        this.context = context;
        this.files = files;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FileManagerViewHolder(LayoutInflater.from(context).inflate(R.layout.file_manager_leituras_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FileManagerViewHolder hold = (FileManagerViewHolder) holder;
        hold.tvType.setText(files.get(position).convertType());
        hold.tvEquipment.setText(files.get(position).getEquipment());
        hold.tvPoint.setText(files.get(position).getPoint());
        hold.tvSize.setText(files.get(position).getSize()+" bytes");
        hold.tvDate.setText(files.get(position).getDate());
        hold.tvTime.setText(files.get(position).getTime());
        hold.chbChecked.setChecked(files.get(position).isChecked());
        hold.chbChecked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeChecked(position);
            }
        });
        hold.btnOpenFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class FileManagerViewHolder extends RecyclerView.ViewHolder {

        final TextView tvType;
        final TextView tvEquipment;
        final TextView tvPoint;
        final TextView tvSize;
        final TextView tvDate;
        final TextView tvTime;
        final CheckBox chbChecked;
        final Button btnOpenFile;


        FileManagerViewHolder(@NonNull View view) {
            super(view);

            tvType = view.findViewById(R.id.fmi_tv_type);
            tvEquipment = view.findViewById(R.id.fmi_tv_equipment);
            tvPoint = view.findViewById(R.id.fmi_tv_point);
            tvSize = view.findViewById(R.id.fmi_tv_size);
            tvDate = view.findViewById(R.id.fmi_tv_date);
            tvTime = view.findViewById(R.id.fmi_tv_time);
            chbChecked = view.findViewById(R.id.fmi_chb_checked);
            btnOpenFile = view.findViewById(R.id.fmi_btn_open);
        }
    }

    public void checkAll() {
        if (!allChecked) {
            filesChecked.clear();
            for (int i=0; i<files.size(); i++) {
                FileLeituraPojo fileLeituraPojo = files.get(i);
                fileLeituraPojo.setChecked(true);
                filesChecked.add(fileLeituraPojo);
                notifyItemChanged(i, fileLeituraPojo);
            }
            allChecked = true;
        }
        FileManagerLeiturasActivity.changeSelectedFilesIconView(allChecked);
    }

    public void uncheckAll() {
        if (filesChecked.size() > 0) {
            filesChecked.clear();
            for (int i=0; i<files.size(); i++) {
                FileLeituraPojo fileLeituraPojo = files.get(i);
                fileLeituraPojo.setChecked(false);
                notifyItemChanged(i, fileLeituraPojo);
            }
            filesChecked.clear();
            allChecked = false;
        }
        FileManagerLeiturasActivity.changeSelectedFilesIconView(allChecked);
    }

    public void updateFileList(ArrayList<FileLeituraPojo> files) {
        this.files.clear();
        this.files.addAll(files);
        notifyDataSetChanged();
    }

    private void changeChecked(int position) {
        FileLeituraPojo fileLeituraPojo = files.get(position);
        if (!fileLeituraPojo.isChecked()) {
            fileLeituraPojo.setChecked(true);
            filesChecked.add(fileLeituraPojo);
            if (filesChecked.size() == files.size()) {
                allChecked = true;
            }
        } else {
            filesChecked.remove(fileLeituraPojo);
            allChecked = false;
            fileLeituraPojo.setChecked(false);
        }

        notifyItemChanged(position, fileLeituraPojo);
        FileManagerLeiturasActivity.changeSelectedFilesIconView(allChecked);
    }

    private void openFile(int position) {
        FileLeituraPojo fileLeituraPojo = files.get(position);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(FileProvider.getUriForFile(context, "com.starmeasure.starmeasureabsoluto.fileprovider", fileLeituraPojo.getFile()), "text/csv");
        context.startActivity(Intent.createChooser(intent, "Open file with"));
    }

    public void shareCheckedFiles() {
        if (filesChecked.size() > 0) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.subject_sm_reader_reads));
            intent.setType("file/csv");
            ArrayList<Uri> filesToSend = new ArrayList<>();
            for (FileLeituraPojo file : filesChecked) {
                Uri uri = FileProvider.getUriForFile(context, "com.starmeasure.starmeasureabsoluto.fileprovider", file.getFile());
                filesToSend.add(uri);
            }
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, filesToSend);
            context.startActivity(intent);

            uncheckAll();
            notifyDataSetChanged();
        }

        FileManagerLeiturasActivity.changeSelectedFilesIconView(allChecked);
    }

    public String deleteCheckedFiles() {
        String error = "";
        if (filesChecked.size() > 0) {
            ArrayList<FileLeituraPojo> deletedFiles = deleteCheckedFiles(filesChecked);
            if (deletedFiles.size() > 0) {
                files.removeAll(deletedFiles);
                filesChecked.clear();
                allChecked = false;
                uncheckAll();
                notifyDataSetChanged();
                Log.d("###", "Deleted files["+deletedFiles.size()+"]: "+deletedFiles.toString());
            } else {
                error = "Error to delete files.";
            }
        }
        FileManagerLeiturasActivity.changeSelectedFilesIconView(allChecked);
        return error;
    }

    private ArrayList<FileLeituraPojo> deleteCheckedFiles(@NonNull ArrayList<FileLeituraPojo> filesToDelete) {
        ArrayList<FileLeituraPojo> deletedFiles = new ArrayList<>();
        for (FileLeituraPojo appFile : filesToDelete) {
            File file = appFile.getFile();
            if (file.delete())
                deletedFiles.add(appFile);
        }
        return deletedFiles;
    }

}
