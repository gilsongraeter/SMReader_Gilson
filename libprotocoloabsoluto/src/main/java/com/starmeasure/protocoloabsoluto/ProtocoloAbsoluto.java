package com.starmeasure.protocoloabsoluto;

import java.util.ArrayList;
import java.util.List;

public class ProtocoloAbsoluto {

    public static final byte ComandoAB = (byte) 0xab;
    public static final byte ComandoEB = (byte) 0xeb;
    static final byte Ocorrencia = 0x40;
    static final byte ParametrosMedidorHospedeiro = 0x02;
    static final byte ConfiguracoesMedidorHospedeiro = 0x01;
    static final byte AlteracaoCorteReligamento = 0x06;
    static final byte SolicitacaoAberturaSessaoAutenticada = 0x11;
    static final byte ResetRegistradores = 0x07;
    static final byte MemoriaMassaQEE = 0x11;
    static final byte AB08 = 0x08;
    static final byte EB90 = (byte) 0x90;
    static final byte EB92 = (byte) 0x92;

    static final byte AbntLeituraGrandezasInstantaneas = 0x14;
    static final byte AbntLeituraRegistradoresAtuais = 0x21;
    static final byte AbntAlteracaoData = 0x29;
    static final byte AbntAlteracaoHora = 0x30;
    static final byte AbntLimpezaOcorrenciasMedidor = 0x37;
    static final byte AbntLeituraParametros = 0x51;
    static final byte AbntComandoEstendido = (byte) 0x98;
    static final byte AbntLeituraMemoriaMassa = 0x52;
    static final byte AbntAlteracaoIntervaloMM = 0x73;
    static final byte AbntLeituraCodigoInstalacao = (byte) 0x87;
    static final byte AbntLeituraRegistradoresAtuaisEasyTrafo = 0x04;
    static final byte AbntTensaoNominal = (byte) 0x95;
    static final byte AbntLeituraGenerica = (byte) 0x95;

    public enum TipoMedidor {
        Hospedeiro,
        Monofasico01,
        Monofasico02,
        Monofasico03,
        Monofasico04,
        Monofasico05,
        Monofasico06,
        Monofasico07,
        Monofasico08,
        Monofasico09,
        Monofasico10,
        Monofasico11,
        Monofasico12,
        Bifasico0102,
        Bifasico0203,
        Bifasico0304,
        Bifasico0405,
        Bifasico0506,
        Bifasico0607,
        Bifasico0708,
        Bifasico0809,
        Bifasico0910,
        Bifasico1011,
        Bifasico1112,
        Trifasico010203,
        Trifasico020304,
        Trifasico030405,
        Trifasico040506,
        Trifasico050607,
        Trifasico060708,
        Trifasico070809,
        Trifasico080910,
        Trifasico091011,
        Trifasico101112,
        Trifasico30200,
        Outros
    }

    public static class Medidor {

        Medidor(TipoMedidor tipo, int numeroInterno, String numeroSerie, int numeroFases) {
            this.tipo = tipo;
            this.numeroInterno = numeroInterno;
            this.numeroSerie = numeroSerie;
            this.numeroFases = numeroFases;
        }

        private final TipoMedidor tipo;
        private final int numeroInterno;
        private final String numeroSerie;
        private final int numeroFases;
    }

    private final static List<Medidor> medidores = new ArrayList<Medidor>() {{
        add(new Medidor(TipoMedidor.Hospedeiro, 0, "000000", 0));
        add(new Medidor(TipoMedidor.Monofasico01, 1, "010000", 1));
        add(new Medidor(TipoMedidor.Monofasico02, 2, "020000", 1));
        add(new Medidor(TipoMedidor.Monofasico03, 3, "030000", 1));
        add(new Medidor(TipoMedidor.Monofasico04, 4, "040000", 1));
        add(new Medidor(TipoMedidor.Monofasico05, 5, "050000", 1));
        add(new Medidor(TipoMedidor.Monofasico06, 6, "060000", 1));
        add(new Medidor(TipoMedidor.Monofasico07, 7, "070000", 1));
        add(new Medidor(TipoMedidor.Monofasico08, 8, "080000", 1));
        add(new Medidor(TipoMedidor.Monofasico09, 9, "090000", 1));
        add(new Medidor(TipoMedidor.Monofasico10, 10, "100000", 1));
        add(new Medidor(TipoMedidor.Monofasico11, 11, "110000", 1));
        add(new Medidor(TipoMedidor.Monofasico12, 12, "120000", 1));
        add(new Medidor(TipoMedidor.Bifasico0102, 13, "010200", 2));
        add(new Medidor(TipoMedidor.Bifasico0203, 14, "020300", 2));
        add(new Medidor(TipoMedidor.Bifasico0304, 15, "030400", 2));
        add(new Medidor(TipoMedidor.Bifasico0405, 16, "040500", 2));
        add(new Medidor(TipoMedidor.Bifasico0506, 17, "050600", 2));
        add(new Medidor(TipoMedidor.Bifasico0607, 18, "060700", 2));
        add(new Medidor(TipoMedidor.Bifasico0708, 19, "070800", 2));
        add(new Medidor(TipoMedidor.Bifasico0809, 20, "080900", 2));
        add(new Medidor(TipoMedidor.Bifasico0910, 21, "091000", 2));
        add(new Medidor(TipoMedidor.Bifasico1011, 22, "101100", 2));
        add(new Medidor(TipoMedidor.Bifasico1112, 23, "111200", 2));
        add(new Medidor(TipoMedidor.Trifasico010203, 24, "010203", 3));
        add(new Medidor(TipoMedidor.Trifasico020304, 25, "020304", 3));
        add(new Medidor(TipoMedidor.Trifasico030405, 26, "030405", 3));
        add(new Medidor(TipoMedidor.Trifasico040506, 27, "040506", 3));
        add(new Medidor(TipoMedidor.Trifasico050607, 28, "050607", 3));
        add(new Medidor(TipoMedidor.Trifasico060708, 29, "060708", 3));
        add(new Medidor(TipoMedidor.Trifasico070809, 30, "070809", 3));
        add(new Medidor(TipoMedidor.Trifasico080910, 31, "080910", 3));
        add(new Medidor(TipoMedidor.Trifasico091011, 32, "091011", 3));
        add(new Medidor(TipoMedidor.Trifasico101112, 33, "101112", 3));
        add(new Medidor(TipoMedidor.Trifasico30200, 34, "142536", 3));

    }};

    public static TipoMedidor TipMedidorPeloNumero(int numero) {
        for (Medidor medidor : medidores) {
            if (medidor.numeroInterno == numero)
                return medidor.tipo;
        }
        return null;
    }

    public static TipoMedidor TipoMedidorPeloNumeroMedidor(String numeroMedidor) {
        if (numeroMedidor.length() < 6)
            return TipoMedidor.Hospedeiro;
        String ns = numeroMedidor.substring(numeroMedidor.length() - 6);
        for (Medidor medidor : medidores) {
            if (medidor.numeroSerie.equals(ns))
                return medidor.tipo;
        }
        return TipoMedidor.Hospedeiro;
    }

    public static String NumeroMedidorPeloNumeroSequencial(int numero) {
        for (Medidor medidor : medidores) {
            if (medidor.numeroInterno == numero)
                return medidor.numeroSerie;
        }
        return null;
    }

    public static int NumeroDeFasesPeloNumeroSequencial(int numero) {
        for (Medidor medidor : medidores) {
            if (medidor.numeroInterno == numero)
                return medidor.numeroFases;
        }
        return 0;
    }

    public static int byteToBCD(byte b) {
        assert 0 <= b && b <= 99; // two digits only.
        return (b / 10 << 4) | b % 10;
    }

    public static int BCDToByte(byte bcd) {
        return Integer.parseInt(String.format("%02X", bcd));
        //return bcd / 16 * 10 + bcd % 16;
    }

    /**
     * Retorna LSB to MSB
     * @param data
     * @return
     */
    static byte[] Int16ToByteArray(int data) {
        byte[] result = new byte[2];
        result[0] = (byte) ((data & 0x000000FF));
        result[1] = (byte) ((data & 0x0000FF00) >> 8);
        return result;
    }

    static byte[] Int32ToByteArray(int data) {
        byte[] result = new byte[2];
        result[0] = (byte) ((data & 0x000000FF));
        result[1] = (byte) ((data & 0x0000FF00) >> 8);
        return result;
    }

    public static byte[] longToBytes(float x) {
        //ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        //buffer.putShort(x);
        int bits = Float.floatToIntBits(x);
        byte[] buffer = new byte[4];
        buffer[0] = (byte)(bits & 0xff);
        buffer[1] = (byte)((bits >> 8) & 0xff);
        buffer[2] = (byte)((bits >> 16) & 0xff);
        buffer[3] = (byte)((bits >> 24) & 0xff);

        return buffer;
    }

}
