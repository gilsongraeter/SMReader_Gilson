package com.starmeasure.absoluto.filemanager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.starmeasure.absoluto.Arquivo;
import com.starmeasure.absoluto.R;
import com.starmeasure.absoluto.filemanager.adapter.FileManagerLeiturasAdapter;

public class FileManagerLeiturasActivity extends AppCompatActivity implements View.OnClickListener {

    private RecyclerView rvList;
    private FileManagerLeiturasAdapter adapter;
    private static Button btnSelect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager_leituras);

        rvList = findViewById(R.id.afm_leituras_rv_list);
        btnSelect = findViewById(R.id.fmb_btn_select);


        btnSelect.setOnClickListener(this);
        findViewById(R.id.afm_leituras_imgbtn_close).setOnClickListener(this);
        findViewById(R.id.fmb_btn_share).setOnClickListener(this);
        findViewById(R.id.fmb_btn_delete).setOnClickListener(this);

        Arquivo.createDirCargas();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            if (v.getId() == R.id.afm_leituras_imgbtn_close) {
                finish();
            } else if (v.getId() == R.id.fmb_btn_select) {
                if (adapter.allChecked) {
                    adapter.uncheckAll();
                } else {
                    adapter.checkAll();
                }
            } else if (v.getId() == R.id.fmb_btn_share) {
                if (adapter.filesChecked.size() > 0) {
                    adapter.shareCheckedFiles();
                } else {
                    Toast.makeText(this, "Nenhum arquivo selecionado.", Toast.LENGTH_SHORT).show();
                }
            } else if (v.getId() == R.id.fmb_btn_delete) {
                if (adapter.filesChecked.size() > 0) {
                    AlertDialog.Builder b = new AlertDialog.Builder(v.getContext());
                    b.setTitle("Aviso");
                    b.setMessage("Você tem certeza que deseja excluir os arquivos selecionados?");
                    b.setNegativeButton("Não", null);
                    b.setPositiveButton("Sim", (dialog, which) -> {
                        String error = adapter.deleteCheckedFiles();
                        if (!error.equals("")) {
                            Toast.makeText(FileManagerLeiturasActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                    b.create();
                    b.show();
                } else {
                    Toast.makeText(this, "Nenhum arquivo selecionado.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter == null) {
            setRecyclerView();
        } else {
            adapter.updateFileList(Arquivo.getAllFilesOnDirectory(this));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapter != null) {
            adapter.uncheckAll();
            changeSelectedFilesIconView(adapter.allChecked);
        }
    }

    private void setRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvList.setLayoutManager(layoutManager);
        adapter = new FileManagerLeiturasAdapter(this, Arquivo.getAllFilesOnDirectory(this));
        rvList.setAdapter(adapter);
    }

    public static void changeSelectedFilesIconView(boolean change) {
        if (change) {
            btnSelect.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_checked_box_black_24dp, 0, 0);
        } else {
            btnSelect.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_to_check_black_24dp, 0, 0);
        }
    }
}
