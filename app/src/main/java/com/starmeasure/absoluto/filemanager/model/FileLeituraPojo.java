package com.starmeasure.absoluto.filemanager.model;

import androidx.annotation.NonNull;

import java.io.File;

public class FileLeituraPojo {

    private String type;
    private String point;
    private String equipment;
    private String date;
    private String time;
    private boolean checked;
    private long size;
    private File file;

    public FileLeituraPojo() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPoint() {
        return point;
    }

    public void setPoint(String point) {
        this.point = point;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public String convertType() {
        String newType = "";
        if (type.contains("MemoriaMassa")) {
            if (type.contains("EnergiaDireta")) {
                newType = "Memória de Massa Energia Direta";
            } else if (type.contains("THDCorrente")) {
                newType = "Memória de Massa THD Corrente";
            } else if (type.contains("THDTensao")) {
                newType = "Memória de Massa THD Tensão";
            } else if (type.contains("TensaoMinima")) {
                newType = "Memória de Massa Tensão Mínima";
            } else if (type.contains("TensaoMaxima")) {
                newType = "Memória de Massa Tensão Máxima";
            } else if (type.contains("EnergiaReversa")) {
                newType = "Memória de Massa Energia Reversa";
            } else if (type.contains("Tensao")) {
                newType = "Memória de Massa Tensão";
            } else if (type.contains("Corrente")) {
                newType = "Memória de Massa Corrente";
            }
        } else if (type.contains("PaginaFiscal")) {
            newType = "Página Fiscal";
        } else if (type.contains("Registradores")) {
            newType = type;
        } else if (type.contains("MemoriaQEE")) {
            newType = "Memória QEE";
            if (type.contains("Tensao")) {
                newType+=" Tensão";
            }
        }
        return newType;
    }

    @Override
    @NonNull
    public String toString() {
        return "FilePojo{" +
                "\nType: " + type +
                "\nPoint: " + point +
                "\nEquipment: " + equipment +
                "\nDate: " + date +
                "\nTime: " + time +
                "\nSize: " + size +
                "\nFile: " + file +
                "\n}";
    }
}
