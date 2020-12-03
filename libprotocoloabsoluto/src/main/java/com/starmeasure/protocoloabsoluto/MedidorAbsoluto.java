package com.starmeasure.protocoloabsoluto;

public class MedidorAbsoluto {
    public String unidadeConsumidora;
    public String numero;
    public int numMedidor;
    public int fases;
    public int status;
    public boolean semRele;

    @Override
    public String toString() {
        return "MedidorAbsoluto{" +
                "unidadeConsumidora='" + unidadeConsumidora + '\'' +
                ", numero='" + numero + '\'' +
                ", numMedidor=" + numMedidor +
                ", fases=" + fases +
                ", status=" + status +
                ", semRele=" + semRele +
                '}';
    }
}
