package com.starmeasure.absoluto.filemanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.starmeasure.absoluto.R;

import java.io.File;
import java.util.ArrayList;

public class FileManagerCargasAdapter extends RecyclerView.Adapter {

    private Context context;
    private ArrayList<File> files;
    private static FileManagerCargasAdapter.ClickListener clickListener;

    public FileManagerCargasAdapter(@NonNull Context context, @NonNull ArrayList<File> files) {
        this.context = context;
        this.files = files;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FileManagerViewHolder(LayoutInflater.from(context).inflate(R.layout.file_manager_cargas_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FileManagerViewHolder hold = (FileManagerViewHolder) holder;
        hold.tvName.setText(files.get(position).getName());
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    private class FileManagerViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        final TextView tvName;

        FileManagerViewHolder(@NonNull View view) {
            super(view);
            tvName = view.findViewById(R.id.fmci_tv_name);

            itemView.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
            clickListener.onItemLongClick(getAdapterPosition(), v);
            return true;
        }
    }

    public void updateList(ArrayList<File> files) {
        if (this.files != null) {

            this.files.clear();
            this.files.addAll(files);
            notifyDataSetChanged();
        }
    }

    public void deleteFile(File file, int position) {
        boolean result = file.delete();
        if (result) {
            files.remove(file);
            notifyItemRemoved(position);
        }
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        FileManagerCargasAdapter.clickListener = clickListener;
    }

    public interface ClickListener{
        void onItemLongClick(int position, View v);
    }

    public File getFile(int position) {
        return files.get(position);
    }
}
