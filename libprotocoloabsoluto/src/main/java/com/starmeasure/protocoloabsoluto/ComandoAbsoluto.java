package com.starmeasure.protocoloabsoluto;

import com.starmeasure.crc.CRC;
import com.starmeasure.protocoloabsoluto.ProtocoloAbsoluto.TipoMedidor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Calendar;

public class ComandoAbsoluto {

    public static class EB11QEE {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        public EB11QEE comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build(byte pacote, byte conjunto) {
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPerguntaEstendida(ProtocoloAbsoluto.MemoriaMassaQEE);
            corpo[0] = ProtocoloAbsoluto.ComandoEB;
            corpo[5] = pacote;
            corpo[6] = 0x00;
            corpo[7] = conjunto;
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class EB17_2 {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        public EB17_2 comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build(byte tipo_comando) {
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPerguntaEstendida(ProtocoloAbsoluto.EB17);
            corpo[0] = ProtocoloAbsoluto.ComandoEB;
            corpo[5] = tipo_comando;
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class AB52 {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        public AB52 comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build(byte pacote, byte TipoLeitura, int Intervalo) {
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPerguntaEstendida(ProtocoloAbsoluto.AbntLeituraMemoriaMassa);
            corpo[0] = ProtocoloAbsoluto.ComandoAB;
            corpo[5] = pacote;
            corpo[6] = 0x00;
            corpo[7] = TipoLeitura;
            corpo[8] = (byte)(Intervalo & 0x0F);
            corpo[9] = (byte)((Intervalo >> 8) & 0x0F);
            corpo[10] = (byte)((Intervalo >> 16) & 0x0F);
            corpo[11] = (byte)((Intervalo >> 24) & 0x0F);
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    /**
     * Alteração do protocolo da interface
     */
    public static class AB08 {

        private String numeroMedidor = "";
        private TipoMedidor tipoMedidor = TipoMedidor.Hospedeiro;

        public AB08 comMedidorNumero(String numeroMedidor) {
            this.numeroMedidor = numeroMedidor;
            this.tipoMedidor = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            return this;
        }

        public byte[] build() {
            byte[] cabecalho = GeraNumeroMedidor(tipoMedidor, numeroMedidor);
            byte[] corpo = GeraCorpoPerguntaEstendida(ProtocoloAbsoluto.AB08);
            corpo[5] = 0x01;
            corpo[6] = 0x00;
            corpo[7] = 0x01;
            corpo[8] = 0x05;
            return GeraMensagem(cabecalho, corpo);
        }
    }

    public static class CargaDePrograma {

        public static final byte iniciaCarga = (byte) 0xFE;
        public static final byte transferenciaDeCarga = (byte) 0xFF;

        private byte[] dados;

        private int criptografia = 0;

        private int autenticacao = 0;

        private int mensagemFinal = 0;

        private int resultado = 0;

        private int sequenciador = 0;

        private byte[] iuMedidor;

        private byte[] iuCelular;

        private int pacoteAtual = 0;

        private byte comandoAtual = iniciaCarga;

        public CargaDePrograma(byte[] iuMedidor, byte[] iuCelular) {
            this.iuCelular = iuCelular;
            this.iuMedidor = iuMedidor;
        }

        public void setDados(byte[] dados) {
            this.dados = dados;
        }

        public int getSequenciador() {
            return sequenciador;
        }

        public void setSequenciador(int sequenciador) {
            this.sequenciador = sequenciador;
        }

        public void atualizarSequenciador() {
            this.sequenciador += 1;
        }

        public void atualizarContadorDePacote() {
            this.pacoteAtual += 1;
        }

        public int getPacoteAtual() {
            return pacoteAtual;
        }

        public void setMensagemFinal(int mensagemFinal) {
            this.mensagemFinal = mensagemFinal;
        }

        public void setComandoAtual(byte comandoAtual) {
            this.comandoAtual = comandoAtual;
        }

        public byte[] build() throws IOException {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(montaCabecalho()); //Cabecalho
            os.write(contadorToByteArray(sequenciador)); // Sequenciador
            os.write(iuMedidor); // Destino
            os.write(iuCelular); // Origem
            os.write(comandoAtual); // Comando
            os.write(0); // Indice do Medidor
            if (comandoAtual == transferenciaDeCarga) {
                os.write(ProtocoloAbsoluto.Int16ToByteArray(pacoteAtual));
            }
            os.write(dados); // Dados

            byte[] mEstrutura = os.toByteArray();
            byte[] crc = GeraCRC(mEstrutura);
            os.write(crc); //CRC

            return os.toByteArray();
        }

        private byte[] contadorToByteArray(int i) {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(i);
            return buffer.array();
        }

        private byte[] montaCabecalho() {
            int size;
            if (comandoAtual == transferenciaDeCarga) {
                size = (dados.length + 2) & 0xfff;
            } else {
                size = dados.length & 0xfff;
            }
            int mInt = size + (resultado << 12) + (mensagemFinal << 13) + (autenticacao << 14) + (criptografia << 15);
            return ProtocoloAbsoluto.Int16ToByteArray(mInt);
        }

    }

    public static class LeituraConfiguracaoMedidor {
        public byte[] build(boolean isAbsoluto) {
            byte[] cabecalho = GeraNumeroMedidor(TipoMedidor.Hospedeiro, "");
            byte[] corpo = GeraCorpoPerguntaEstendida(ProtocoloAbsoluto.ConfiguracoesMedidorHospedeiro);
            if (!isAbsoluto) {
                    corpo[0] = ProtocoloAbsoluto.ComandoEB;
            }
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class LeituraParametrosHospedeiro {
        public byte[] build() {
            byte[] cabecalho = GeraNumeroMedidor(TipoMedidor.Hospedeiro, "");
            byte[] corpo = GeraCorpoPerguntaEstendida(ProtocoloAbsoluto.ParametrosMedidorHospedeiro);
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class LeituraParametrosHospedeiroProximo {
        public byte[] build() {
            byte[] cabecalho = GeraNumeroMedidor(TipoMedidor.Hospedeiro, "");
            byte[] corpo = GeraCorpoPerguntaEstendida(ProtocoloAbsoluto.ParametrosMedidorHospedeiro);
            corpo[5] = 0x01; // indica próximo pacote
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class LimpezaOcorrenciasMedidor {
        private byte codigoOcorrencia = 0x00;
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        public LimpezaOcorrenciasMedidor comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public LimpezaOcorrenciasMedidor comCodigoOcorrencia(byte codigoOcorrencia) {
            this.codigoOcorrencia = codigoOcorrencia;
            return this;
        }

        public byte[] build(boolean isAbsoluto) {
            if (!isAbsoluto) {
                tipo = TipoMedidor.Outros;
            }
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPergunta(ProtocoloAbsoluto.AbntLimpezaOcorrenciasMedidor);
            corpo[4] = codigoOcorrencia;
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class AB06AlteracaoCorteReligamento {
        private enum TipoOperacao {
            Leitura,
            Corte,
            Religamento
        }

        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private TipoOperacao operacao = TipoOperacao.Leitura;
        private String ns_medidor = "";

        public AB06AlteracaoCorteReligamento comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public AB06AlteracaoCorteReligamento efetuaLeitura() {
            operacao = TipoOperacao.Leitura;
            return this;
        }

        public AB06AlteracaoCorteReligamento efetuaCorte() {
            operacao = TipoOperacao.Corte;
            return this;
        }

        public AB06AlteracaoCorteReligamento efetuaReligamento() {
            operacao = TipoOperacao.Religamento;
            return this;
        }

        public byte[] build(boolean isAbsoluto) {
            if (!isAbsoluto) {
                tipo = TipoMedidor.Outros;
            }

            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPerguntaEstendida(ProtocoloAbsoluto.AlteracaoCorteReligamento);
            corpo[5] = (byte) ((operacao == TipoOperacao.Leitura) ? 0x00 : 0x01);
            corpo[6] = (byte) ((operacao != TipoOperacao.Religamento) ? 0x00 : 0x01);
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class AB11SolicitacaoAberturaSessao {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String senha = "";
        private String ns_medidor = "";

        public AB11SolicitacaoAberturaSessao comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public AB11SolicitacaoAberturaSessao comSenha(String senha) {
            this.senha = senha;
            return this;
        }

        public byte[] build(boolean isAbsoluto) {
            if (!isAbsoluto) {
                tipo = TipoMedidor.Outros;
            }
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPerguntaEstendida(ProtocoloAbsoluto.SolicitacaoAberturaSessaoAutenticada);
            corpo[5] = 0x01;
            byte[] sn = senha.getBytes(Charset.forName("UTF-8"));
            System.arraycopy(sn, 0, corpo, 6, sn.length);
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class ABNT21 {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        public ABNT21 comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build(boolean isAbsoluto) {
            if (!isAbsoluto) {
                tipo = TipoMedidor.Outros;
            }
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPergunta(ProtocoloAbsoluto.AbntLeituraRegistradoresAtuais);
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class ABNT29 {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        public ABNT29 comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build(Calendar data, boolean isAbsoluto) {
            if (!isAbsoluto) {
                tipo = TipoMedidor.Outros;
            }
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPergunta(ProtocoloAbsoluto.AbntAlteracaoData);
            corpo[4] = (byte) ProtocoloAbsoluto.byteToBCD((byte) data.get(Calendar.DAY_OF_MONTH));
            corpo[5] = (byte) ProtocoloAbsoluto.byteToBCD((byte) (data.get(Calendar.MONTH) + 1));
            corpo[6] = (byte) ProtocoloAbsoluto.byteToBCD((byte) (data.get(Calendar.YEAR) - 2000));
            corpo[7] = (byte) ProtocoloAbsoluto.byteToBCD((byte) data.get(Calendar.DAY_OF_WEEK));

            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class ABNT30 {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        public ABNT30 comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build(Calendar hora, boolean isAbsoluto) {
            if (!isAbsoluto) {
                tipo = TipoMedidor.Outros;
            }
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPergunta(ProtocoloAbsoluto.AbntAlteracaoHora);
            corpo[4] = (byte) ProtocoloAbsoluto.byteToBCD((byte) hora.get(Calendar.HOUR_OF_DAY));
            corpo[5] = (byte) ProtocoloAbsoluto.byteToBCD((byte) hora.get(Calendar.MINUTE));
            corpo[6] = (byte) ProtocoloAbsoluto.byteToBCD((byte) hora.get(Calendar.SECOND));
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class ABNT51 {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        public ABNT51 comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build(int dias, int canal, boolean isAbsoluto) {
            if (!isAbsoluto) {
                tipo = TipoMedidor.Outros;
            }
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPergunta(ProtocoloAbsoluto.AbntLeituraParametros);
            corpo[4] = 0x00;
            corpo[5] = (byte) ProtocoloAbsoluto.byteToBCD((byte) canal);
            corpo[6] = 0x02;
            if (dias == 0) {
                corpo[6] = 0;
            }
            corpo[7] = 0x00;
            corpo[8] = (byte) ProtocoloAbsoluto.byteToBCD((byte) dias);

            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    //ToDo making EB17
    public static class EB17 {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";
        private boolean isLeitura = true;
        private byte ValeAlteracoes = 0;
        private boolean isAlteracaoTelefones = false;
        private String Telefone1 = "00000000000";
        private String Telefone2 = "00000000000";
        private String Telefone3 = "00000000000";
        private String Telefone4 = "00000000000";
        private String TelefonesDeteccaoFalhas = Telefone1 + Telefone2 + Telefone3 + Telefone4;
        private byte QtdTelefones = 0;
        private boolean isAlteracaoEventos = false;
        private byte Eventos = 0;
        private boolean isAlteracaoCiclos = false;
        private byte Ciclos = 0;
        private boolean isAlteracaoRepeticoes = false;
        private byte RepeticoesEtapa1 = 0;
        private boolean isAlteracaoIntervalo1 = false;
        private int Intervalo1 = 0;
        private boolean isAlteracaoIntervalo2 = false;
        private int Intervalo2 = 0;
        private boolean isAlteracaoTelefoneKeepAlive = false;
        private String TelefoneKeepAlive = "00000000000";
        private boolean isAlteracaoFrequenciaKeepAlive = false;
        private byte FrequenciaKeepAlive = 0;
        private String Digito1Aux = "";
        private String Digito2Aux = "";

        private byte ZeraFlags = 0;
        private boolean zeraFlagsAlteracaoTelefones = false;
        private boolean zeraFlagsAlteracaoEventos = false;
        private boolean zeraFlagsAlteracaoCiclos = false;
        private boolean zeraFlagsAlteracaoRepeticoesEtapa1 = false;
        private boolean zeraFlagsAlteracaoIntervalo1 = false;
        private boolean zeraFlagsAlteracaoIntervalo2 = false;
        private boolean zeraFlagsAlteracaoTelefoneKeepAlive = false;
        private boolean zeraFlagsAlteracaoFrequenciaKeepAlive = false;

        private byte Validade = 0;

        public EB17 comNumerMedidor(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build() {
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPerguntaEstendida(ProtocoloAbsoluto.EB17);
            corpo[0] = ProtocoloAbsoluto.ComandoEB;
            if (isLeitura) {
                corpo[5] = 0x00;
            } else {
                corpo[5] = 0x01;
            }

            if(isAlteracaoTelefones) {
                ValeAlteracoes |= 0x01;
            }

            if(isAlteracaoEventos) {
                ValeAlteracoes |= 0x02;
            }

            if(isAlteracaoCiclos) {
                ValeAlteracoes |= 0x04;
            }

            if(isAlteracaoRepeticoes) {
                ValeAlteracoes |= 0x08;
            }

            if(isAlteracaoIntervalo1) {
                ValeAlteracoes |= 0x10;
            }

            if(isAlteracaoIntervalo2) {
                ValeAlteracoes |= 0x20;
            }

            if(isAlteracaoTelefoneKeepAlive) {
                ValeAlteracoes |= 0x40;
            }

            if(isAlteracaoFrequenciaKeepAlive) {
                ValeAlteracoes |= 0x80;
            }

            corpo[6] = ValeAlteracoes;
            corpo[7] = 0x00; // NULL
            corpo[8] = 0x00; // Zera Flags - Leitura ou escrita ????
            corpo[9] = 0x00; // NULL

            corpo[10] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(0, 1)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(1, 2))));
            corpo[11] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(2, 3)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(3, 4))));
            corpo[12] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(4, 5)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(5, 6))));
            corpo[13] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(6, 7)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(7, 8))));
            corpo[14] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(8, 9)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(9, 10))));
            corpo[15] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(10, 11)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(11, 12))));
            corpo[16] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(12, 13)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(13, 14))));
            corpo[17] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(14, 15)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(15, 16))));
            corpo[18] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(16, 17)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(17, 18))));
            corpo[19] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(18, 19)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(19, 20))));
            corpo[20] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(20, 21)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(21, 22))));
            corpo[21] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(22, 23)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(23, 24))));
            corpo[22] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(24, 25)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(25, 26))));
            corpo[23] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(26, 27)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(27, 28))));
            corpo[24] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(28, 29)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(29, 30))));
            corpo[25] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(30, 31)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(31, 32))));
            corpo[26] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(32, 33)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(33, 34))));
            corpo[27] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(34, 35)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(35, 36))));
            corpo[28] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(36, 37)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(37, 38))));
            corpo[29] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(38, 39)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(39, 40))));
            corpo[30] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(40, 41)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(41, 42))));
            corpo[31] = ((byte)((Byte.parseByte(TelefonesDeteccaoFalhas.substring(42, 43)) * 16) + Byte.parseByte(TelefonesDeteccaoFalhas.substring(43, 44))));

            corpo[32] = QtdTelefones;
            corpo[33] = Eventos;
            corpo[34] = Ciclos;
            corpo[35] = RepeticoesEtapa1;
            corpo[36] = (byte)(Intervalo1 & 0xFF);
            corpo[37] = (byte)(Intervalo1 >> 8);
            corpo[38] = (byte)(Intervalo2 & 0xFF);
            corpo[39] = (byte)(Intervalo2 >> 8);

            corpo[40] = ((byte)((Byte.parseByte(TelefoneKeepAlive.substring(0, 1)) * 16) + Byte.parseByte(TelefoneKeepAlive.substring(1, 2))));
            corpo[41] = ((byte)((Byte.parseByte(TelefoneKeepAlive.substring(2, 3)) * 16) + Byte.parseByte(TelefoneKeepAlive.substring(3, 4))));
            corpo[42] = ((byte)((Byte.parseByte(TelefoneKeepAlive.substring(4, 5)) * 16) + Byte.parseByte(TelefoneKeepAlive.substring(5, 6))));
            corpo[43] = ((byte)((Byte.parseByte(TelefoneKeepAlive.substring(6, 7)) * 16) + Byte.parseByte(TelefoneKeepAlive.substring(7, 8))));
            corpo[44] = ((byte)((Byte.parseByte(TelefoneKeepAlive.substring(8, 9)) * 16) + Byte.parseByte(TelefoneKeepAlive.substring(9, 10))));
            corpo[45] = ((byte)((Byte.parseByte(TelefoneKeepAlive.substring(10, 11)) * 16) + Byte.parseByte("0")));

            corpo[46] = Validade;
            corpo[47] = FrequenciaKeepAlive;

            return GeraMensagem(cabecalho, corpo);
        }

        public void setAlteracaoTelefones(boolean bAlteracaoTelefones) {
            isAlteracaoTelefones = bAlteracaoTelefones;
        }

        public void setAlteracaoEventos(boolean bEventos) {
            isAlteracaoEventos = bEventos;
        }

        public void setAlteracaoCiclos(boolean bCiclos) {
            isAlteracaoCiclos = bCiclos;
        }

        public void setAlteracaoRepeticoes(boolean bRepeticoes) {
            isAlteracaoRepeticoes = bRepeticoes;
        }

        public void setAlteracaoIntervalo1(boolean bIntervalo) {
            isAlteracaoIntervalo1 = bIntervalo;
        }

        public void setAlteracaoIntervalo2(boolean bIntervalo) {
            isAlteracaoIntervalo2 = bIntervalo;
        }

        public void setAlteracaoTelefoneKeepAlive(boolean bTelefoneKeepAlive) {
            isAlteracaoTelefoneKeepAlive = bTelefoneKeepAlive;
        }

        public void setAlteracaoFrequenciaKeepAlive(boolean bFrequencia) {
            isAlteracaoFrequenciaKeepAlive = bFrequencia;
        }

        public boolean isLeitura() {
            return isLeitura;
        }

        public void setLeitura(boolean leitura) {
            isLeitura = leitura;
        }

        public byte getValeAlteracoes() {
            return ValeAlteracoes;
        }

        public void setValeAlteracoes(byte Alteracoes) {
            this.ValeAlteracoes = Alteracoes;
        }

        public byte getQtdTelefones() {
            return QtdTelefones;
        }

        public void setQtdTelefones(byte QtdTelefones) {
            this.QtdTelefones = QtdTelefones;
        }

        public byte getEventos() {
            return Eventos;
        }

        public void setEventos(byte Eventos) {
            this.Eventos = Eventos;
        }

        public byte getCiclos() {
            return Ciclos;
        }

        public void setCiclos(byte Ciclos) {
            this.Ciclos = Ciclos;
        }

        public byte getRepeticoesEtapa1() {
            return RepeticoesEtapa1;
        }

        public void setRepeticoesEtapa1(byte Repeticoes) {
            this.RepeticoesEtapa1 = Repeticoes;
        }

        public int getIntervalo1() {
            return Intervalo1;
        }

        public void setIntervalo1(int Intervalo) {
            this.Intervalo1 = Intervalo;
        }

        public int getIntervalo2() {
            return Intervalo2;
        }

        public void setIntervalo2(int Intervalo) {
            this.Intervalo2 = Intervalo;
        }

        public String getTelefonesDeteccaoFalhas() {
            return TelefonesDeteccaoFalhas;
        }

        public void setTelefonesDeteccaoFalhas(String Telefones) {
            this.TelefonesDeteccaoFalhas = Telefones;
        }

        public String getTelefone(byte numero)
        {
            String TelefoneAux = "";

            switch(numero)
            {
                case 1:
                    TelefoneAux = this.TelefonesDeteccaoFalhas.substring(0, 10);
                    break;
                case 2:
                    TelefoneAux = this.TelefonesDeteccaoFalhas.substring(11, 21);
                    break;
                case 3:
                    TelefoneAux = this.TelefonesDeteccaoFalhas.substring(22, 32);
                    break;
                case 4:
                    TelefoneAux = this.TelefonesDeteccaoFalhas.substring(33, 43);
                    break;
            }

            return TelefoneAux;
        }

        public void setTelefone(byte numero, String Telefone) {
            switch(numero)
            {
                case 1:
                    this.Telefone1 = Telefone;
                    break;
                case 2:
                    this.Telefone2 = Telefone;
                    break;
                case 3:
                    this.Telefone3 = Telefone;
                    break;
                case 4:
                    this.Telefone4 = Telefone;
                    break;
            }
        }

        public String getTelefoneKeepAlive() {
            return TelefoneKeepAlive;
        }

        public void setTelefoneKeepAlive(String TelefoneKeepAlive) {
            this.TelefoneKeepAlive = TelefoneKeepAlive;
        }

        public byte getValidade() {
            return Validade;
        }

        public void setValidade(byte Validade) {
            this.Validade = Validade;
        }

        public byte getFrequenciaKeepAlive() {
            return FrequenciaKeepAlive;
        }

        public void setFrequenciaKeepAlive(byte Frequencia) {
            this.FrequenciaKeepAlive = Frequencia;
        }

        @Override
        public String toString() {
            return "EB17 {" +
                    "\nTipo Medidor: " + tipo +
                    "\nNº Medidor: " + ns_medidor +
                    "\nE Leitura: " + isLeitura +
                    "\nVale Alteracoes: " + ValeAlteracoes +
                    "\nTem Alteracao Telefones: " + isAlteracaoTelefones +
                    "\nTem Alteracao Eventos: " + isAlteracaoEventos +
                    "\nTem Alteracao Ciclos: " + isAlteracaoCiclos +
                    "\nTem Alteracao Repetições: " + isAlteracaoRepeticoes +
                    "\nTem Alteracao Intervalo1: " + isAlteracaoIntervalo1 +
                    "\nTem Alteracao Intervalo2: " + isAlteracaoIntervalo2 +
                    "\nTem Alteracao Telefone Keep Alive: " + isAlteracaoTelefoneKeepAlive +
                    "\nTem Alteracao Frequencia Keep Alive: " + isAlteracaoFrequenciaKeepAlive +
                    "\nQtd Telefones: " + QtdTelefones +
                    "\nEventos: " + Eventos +
                    "\nCiclos: " + Ciclos +
                    "\nQtd Telefones: " + QtdTelefones +
                    "\nRepetições Etapa 1: " + RepeticoesEtapa1 +
                    "\nIntervalo1: " + Intervalo1 +
                    "\nIntervalo2: " + Intervalo2 +
                    "\nTelefones: " + TelefonesDeteccaoFalhas +
                    "\nTelefone Keep Alive: " + TelefoneKeepAlive +
                    "\nFrequencia Keep Alive: " + FrequenciaKeepAlive +
                    "\nValidade Keep Alive: " + Validade +
                    "\n}";
        }
    }

    //ToDo making EB90
    public static class EB90 {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";
        private boolean isLeitura = true;
        private int numeroTransformador = 0;
        private boolean isAlteracaoPotenciaNominal = false;
        private boolean isAlteracaoPapelTermoestabilizado = false;
        private int potenciaNominalTransformador = 0;
        private boolean colocarPapelTermoestabilizado = false;
        private boolean isAlteracaoSobretensaoA = false;
        private int sobretensaoA = 0;
        private boolean isAlteracaoSobretensaoB = false;
        private int sobretensaoB = 0;
        private boolean isAlteracaoSobretensaoC = false;
        private int sobretensaoC = 0;
        private boolean isAlteracaoSubtensaoA = false;
        private int subtensaoA = 0;
        private boolean isAlteracaoSubtensaoB = false;
        private int subtensaoB = 0;
        private boolean isAlteracaoSubtensaoC = false;
        private int subtensaoC = 0;

        public EB90 comNumerMedidor(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build() {
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPerguntaEstendida(ProtocoloAbsoluto.EB90);
            corpo[0] = ProtocoloAbsoluto.ComandoEB;
            if (isLeitura) {
                corpo[5] = 0x00;
            } else {
                corpo[5] = 0x01;
            }

            switch (numeroTransformador) {
                case 0:
                    corpo[6] = 0x00;
                    break;
                case 1:
                    corpo[6] = 0x01;
                    break;
                case 2:
                    corpo[6] = 0x02;
                    break;
            }

            if (isAlteracaoPotenciaNominal && isAlteracaoPapelTermoestabilizado) {
                corpo[7] = 0x03;
            } else if (isAlteracaoPapelTermoestabilizado) {
                corpo[7] = 0x02;
            } else if (isAlteracaoPotenciaNominal) {
                corpo[7] = 0x01;
            }

            int byte9 = 0x00;
            if (isAlteracaoSobretensaoA) {
                byte9 = byte9 + (1 << 5);
            }
            if (isAlteracaoSobretensaoB) {
                byte9 = byte9 + (1 << 6);
            }
            if (isAlteracaoSobretensaoC) {
                byte9 = byte9 + (1 << 7);
            }

            corpo[9] = (byte) byte9;

            int byte10 = 0x00;
            if (isAlteracaoSubtensaoA) {
                byte10 = byte10 + 1;
            }
            if (isAlteracaoSubtensaoB) {
                byte10 = byte10 + (1 << 1);
            }
            if (isAlteracaoSubtensaoC) {
                byte10 = byte10 + (1 << 2);
            }

            corpo[10] = (byte) byte10;

            //Ignorando octetos 08, 09 e 10

            byte[] potenciaNominal = ProtocoloAbsoluto.Int16ToByteArray(potenciaNominalTransformador);
            corpo[11] = potenciaNominal[0];
            corpo[12] = potenciaNominal[1];

            if (colocarPapelTermoestabilizado) {
                corpo[13] = 0x01;
            } else {
                corpo[13] = 0x00;
            }

            byte[] sobreTensaoA = ProtocoloAbsoluto.Int16ToByteArray(sobretensaoA);
            corpo[52] = sobreTensaoA[0];
            corpo[53] = sobreTensaoA[1];

            byte[] sobreTensaoB = ProtocoloAbsoluto.Int16ToByteArray(sobretensaoB);
            corpo[54] = sobreTensaoB[0];
            corpo[55] = sobreTensaoB[1];

            byte[] sobreTensaoC = ProtocoloAbsoluto.Int16ToByteArray(sobretensaoC);
            corpo[56] = sobreTensaoC[0];
            corpo[57] = sobreTensaoC[1];

            byte[] subTensaoA = ProtocoloAbsoluto.Int16ToByteArray(subtensaoA);
            corpo[58] = subTensaoA[0];
            corpo[59] = subTensaoA[1];

            byte[] subTensaoB = ProtocoloAbsoluto.Int16ToByteArray(subtensaoB);
            corpo[60] = subTensaoB[0];
            corpo[61] = subTensaoB[1];

            byte[] subTensaoC = ProtocoloAbsoluto.Int16ToByteArray(subtensaoC);
            corpo[62] = subTensaoC[0];
            corpo[63] = subTensaoC[1];

            return GeraMensagem(cabecalho, corpo);
        }

        public void setAlteracaoSobretensaoA(boolean sobretensaoA) {
            isAlteracaoSobretensaoA = sobretensaoA;
        }

        public void setAlteracaoSobretensaoB(boolean sobretensaoB) {
            isAlteracaoSobretensaoB = sobretensaoB;
        }

        public void setAlteracaoSobretensaoC(boolean sobretensaoC) {
            isAlteracaoSobretensaoC = sobretensaoC;
        }

        public void setAlteracaoSubtensaoA(boolean alteracaoSubtensaoA) {
            isAlteracaoSubtensaoA = alteracaoSubtensaoA;
        }

        public void setAlteracaoSubtensaoB(boolean alteracaoSubtensaoB) {
            isAlteracaoSubtensaoB = alteracaoSubtensaoB;
        }

        public void setAlteracaoSubtensaoC(boolean alteracaoSubtensaoC) {
            isAlteracaoSubtensaoC = alteracaoSubtensaoC;
        }

        public void setSobretensaoA(int alteracaoSobretensaoA) {
            this.sobretensaoA = alteracaoSobretensaoA;
        }

        public void setSobretensaoB(int alteracaoSobretensaoB) {
            this.sobretensaoB = alteracaoSobretensaoB;
        }

        public void setSobretensaoC(int alteracaoSobretensaoC) {
            this.sobretensaoC = alteracaoSobretensaoC;
        }

        public void setSubtensaoA(int subtensaoA) {
            this.subtensaoA = subtensaoA;
        }

        public void setSubtensaoB(int subtensaoB) {
            this.subtensaoB = subtensaoB;
        }

        public void setSubtensaoC(int subtensaoC) {
            this.subtensaoC = subtensaoC;
        }

        public boolean isLeitura() {
            return isLeitura;
        }

        public void setLeitura(boolean leitura) {
            isLeitura = leitura;
        }

        public int getNumeroTransformador() {
            return numeroTransformador;
        }

        public void setNumeroTransformador(int numeroTransformador) {
            this.numeroTransformador = numeroTransformador;
        }

        public boolean isAlteracaoPotenciaNominal() {
            return isAlteracaoPotenciaNominal;
        }

        public void setAlteracaoPotenciaNominal(boolean alteracaoPotenciaNominal) {
            isAlteracaoPotenciaNominal = alteracaoPotenciaNominal;
        }

        public boolean isAlteracaoPapelTermoestabilizado() {
            return isAlteracaoPapelTermoestabilizado;
        }

        public void setAlteracaoPapelTermoestabilizado(boolean alteracaoPapelTermoestabilizado) {
            isAlteracaoPapelTermoestabilizado = alteracaoPapelTermoestabilizado;
        }

        public int getPotenciaNominalTransformador() {
            return potenciaNominalTransformador;
        }

        public void setPotenciaNominalTransformador(int potenciaNominalTransformador) {
            this.potenciaNominalTransformador = potenciaNominalTransformador;
        }

        public boolean isColocarPapelTermoestabilizado() {
            return colocarPapelTermoestabilizado;
        }

        public void setColocarPapelTermoestabilizado(boolean colocarPapelTermoestabilizado) {
            this.colocarPapelTermoestabilizado = colocarPapelTermoestabilizado;
        }

        @Override
        public String toString() {
            return "EB90 {" +
                    "\nTipo Medidor: " + tipo +
                    "\nNº Medidor: " + ns_medidor +
                    "\nE Leitura: " + isLeitura +
                    "\nNumero Transformador: " + numeroTransformador +
                    "\nTem Alteracao Potencia Nominal: " + isAlteracaoPotenciaNominal +
                    "\nTem Alteracao Papel Termoestabilizado: " + isAlteracaoPapelTermoestabilizado +
                    "\nTem Alteracao Sobretensão A: " + isAlteracaoSobretensaoA +
                    "\nTem Alteracao Sobretensão B: " + isAlteracaoSobretensaoB +
                    "\nTem Alteracao Sobretensão C: " + isAlteracaoSobretensaoC +
                    "\nSobretensão A: " + sobretensaoA +
                    "\nSobretensão B: " + sobretensaoB +
                    "\nSobretensão C: " + sobretensaoC +
                    "\nPotencia Nominal Transformador: " + potenciaNominalTransformador +
                    "\nColocar Papel Termoestabilizado: " + colocarPapelTermoestabilizado +
                    "\n}";
        }
    }

    public static class EB92 {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";
        public boolean isEscrita = false;
        public boolean alarmeSobreTensaoA = false;
        public boolean alarmeSobreTensaoB = false;
        public boolean alarmeSobreTensaoC = false;
        public boolean alarmeSubTensaoA = false;
        public boolean alarmeSubTensaoB = false;
        public boolean alarmeSubTensaoC = false;
        public boolean alarmeEstadoTampa = false;


        public EB92 comNumerMedidor(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build() {
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPerguntaEstendida(ProtocoloAbsoluto.EB92);
            corpo[0] = ProtocoloAbsoluto.ComandoEB;

            if (isEscrita) corpo[5] = 0x01;

            int b7 = 0x00;
            if (alarmeSobreTensaoA) {
                b7 = b7 + (1 << 1);
            }
            if (alarmeSobreTensaoB) {
                b7 = b7 + (1 << 2);
            }
            if (alarmeSobreTensaoC) {
                b7 = b7 + (1 << 3);
            }
            if (alarmeSubTensaoA) {
                b7 = b7 + (1 << 4);
            }
            if (alarmeSubTensaoB) {
                b7 = b7 + (1 << 5);
            }
            if (alarmeSubTensaoC) {
                b7 = b7 + (1 << 6);
            }
            if (alarmeEstadoTampa) {
                b7 = b7 + (1 << 7);
            }
            corpo[7] = (byte) b7;

            return GeraMensagem(cabecalho, corpo);
        }

    }

    public static class ABNT52 {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        public ABNT52 comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build(byte pacote, boolean isAbsoluto) {
            if (!isAbsoluto) {
                tipo = TipoMedidor.Outros;
            }
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPergunta(ProtocoloAbsoluto.AbntLeituraMemoriaMassa);
            corpo[4] = pacote;
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class ABNT73 {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        public ABNT73 comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build(int Intervalo, boolean isAbsoluto) {
            if (!isAbsoluto) {
                tipo = TipoMedidor.Outros;
            }
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPergunta(ProtocoloAbsoluto.AbntAlteracaoIntervaloMM);
            corpo[4] = (byte) Intervalo;
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class ABNT87 {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        public ABNT87 comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build(boolean isAbsoluto) {
            if (!isAbsoluto) {
                tipo = TipoMedidor.Outros;
            }
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPergunta(ProtocoloAbsoluto.AbntLeituraCodigoInstalacao);
            corpo[4] = 0x01;
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }

        public byte[] build(String nome, boolean isAbsoluto) {
            if (!isAbsoluto) {
                tipo = TipoMedidor.Outros;
            }
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPergunta(ProtocoloAbsoluto.AbntLeituraCodigoInstalacao);
            corpo[4] = 0x00;
            if (nome.length() < 14) {
                nome = String.format("%1$" + 14 + "s", nome);
            }
            byte[] sn = nome.getBytes(Charset.forName("UTF-8"));
            System.arraycopy(sn, 0, corpo, 6, sn.length);
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class AB21LeituraRegistradoresAtuais {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        private boolean isEasyTrafo = false;

        public AB21LeituraRegistradoresAtuais comMedidorNumero(String numeroMedidor, boolean isEasyTrafo) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            this.isEasyTrafo = isEasyTrafo;
            return this;
        }

        public byte[] build(boolean isAbsoluto) {
            if (!isAbsoluto) {
                tipo = TipoMedidor.Outros;
            }
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPerguntaEstendida(ProtocoloAbsoluto.AbntLeituraRegistradoresAtuais);
            if (isEasyTrafo) {
                corpo = GeraCorpoPerguntaEstendida(ProtocoloAbsoluto.AbntLeituraRegistradoresAtuaisEasyTrafo);
                corpo[0] = ProtocoloAbsoluto.ComandoEB;
            }
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class AB07ResetRegistradores {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        public AB07ResetRegistradores comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build(boolean isAbsoluto, byte tipo_reset) {
            if (!isAbsoluto) {
                tipo = TipoMedidor.Outros;
            }
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPerguntaEstendida(ProtocoloAbsoluto.ResetRegistradores);
            corpo[0] = ProtocoloAbsoluto.ComandoEB;
            corpo[5] = 1;
            corpo[6] = tipo_reset;
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class ABNT9895 {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        public ABNT9895 comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build(int tensao) {
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPergunta(ProtocoloAbsoluto.AbntComandoEstendido);
            corpo[4] = ProtocoloAbsoluto.AbntTensaoNominal;
            byte[] ar_tensao = ProtocoloAbsoluto.longToBytes((float) tensao);
            System.arraycopy(ar_tensao, 0, corpo, 5, ar_tensao.length);
            corpo[62] = 0x01;

            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class ABNT95 {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        public ABNT95 comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build(byte ligacao) {
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPergunta(ProtocoloAbsoluto.AbntLeituraGenerica);
            corpo[37] = ligacao;
            corpo[63] = 0x01;

            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    public static class ABNT14GrandezasInstantaneas {
        private TipoMedidor tipo = TipoMedidor.Hospedeiro;
        private String ns_medidor = "";

        public ABNT14GrandezasInstantaneas comMedidorNumero(String numeroMedidor) {
            this.tipo = ProtocoloAbsoluto.TipoMedidorPeloNumeroMedidor(numeroMedidor);
            this.ns_medidor = numeroMedidor;
            return this;
        }

        public byte[] build(boolean isAbsoluto) {
            if (!isAbsoluto) {
                tipo = TipoMedidor.Outros;
            }
            byte[] cabecalho = GeraNumeroMedidor(tipo, ns_medidor);
            byte[] corpo = GeraCorpoPergunta(ProtocoloAbsoluto.AbntLeituraGrandezasInstantaneas);
            return ComandoAbsoluto.GeraMensagem(cabecalho, corpo);
        }
    }

    private static byte[] GeraNumeroMedidor(TipoMedidor tipoMedidor, String nsMedidor) {
        byte[] cabecalho = {0x00, 0x00, 0x00, 0x00};

        if (nsMedidor.length() >= 8) {
            cabecalho[0] = (byte) Integer.parseInt(nsMedidor.substring(0, 2), 16);
            cabecalho[1] = (byte) Integer.parseInt(nsMedidor.substring(2, 4), 16);
            cabecalho[2] = (byte) Integer.parseInt(nsMedidor.substring(4, 6), 16);
            cabecalho[3] = (byte) Integer.parseInt(nsMedidor.substring(6, 8), 16);
        }

        if (tipoMedidor != TipoMedidor.Outros) {
            if (tipoMedidor != TipoMedidor.Hospedeiro) {
                cabecalho[0] = cabecalho[3];
                cabecalho[1] = 0x00;
                cabecalho[2] = 0x00;
                cabecalho[3] = 0x00;
            }

            switch (tipoMedidor) {
                case Monofasico01:
                    cabecalho[1] = 0x01;
                    break;
                case Monofasico02:
                    cabecalho[1] = 0x02;
                    break;
                case Monofasico03:
                    cabecalho[1] = 0x03;
                    break;
                case Monofasico04:
                    cabecalho[1] = 0x04;
                    break;
                case Monofasico05:
                    cabecalho[1] = 0x05;
                    break;
                case Monofasico06:
                    cabecalho[1] = 0x06;
                    break;
                case Monofasico07:
                    cabecalho[1] = 0x07;
                    break;
                case Monofasico08:
                    cabecalho[1] = 0x08;
                    break;
                case Monofasico09:
                    cabecalho[1] = 0x09;
                    break;
                case Monofasico10:
                    cabecalho[1] = 0x10;
                    break;
                case Monofasico11:
                    cabecalho[1] = 0x11;
                    break;
                case Monofasico12:
                    cabecalho[1] = 0x12;
                    break;
                case Bifasico0102:
                    cabecalho[1] = 0x01;
                    cabecalho[2] = 0x02;
                    break;
                case Bifasico0203:
                    cabecalho[1] = 0x02;
                    cabecalho[2] = 0x03;
                    break;
                case Bifasico0304:
                    cabecalho[1] = 0x03;
                    cabecalho[2] = 0x04;
                    break;
                case Bifasico0405:
                    cabecalho[1] = 0x04;
                    cabecalho[2] = 0x05;
                    break;
                case Bifasico0506:
                    cabecalho[1] = 0x05;
                    cabecalho[2] = 0x06;
                    break;
                case Bifasico0607:
                    cabecalho[1] = 0x06;
                    cabecalho[2] = 0x07;
                    break;
                case Bifasico0708:
                    cabecalho[1] = 0x07;
                    cabecalho[2] = 0x08;
                    break;
                case Bifasico0809:
                    cabecalho[1] = 0x08;
                    cabecalho[2] = 0x09;
                    break;
                case Bifasico0910:
                    cabecalho[1] = 0x09;
                    cabecalho[2] = 0x10;
                    break;
                case Bifasico1011:
                    cabecalho[1] = 0x10;
                    cabecalho[2] = 0x11;
                    break;
                case Bifasico1112:
                    cabecalho[1] = 0x11;
                    cabecalho[2] = 0x12;
                    break;
                case Trifasico010203:
                    cabecalho[1] = 0x01;
                    cabecalho[2] = 0x02;
                    cabecalho[3] = 0x03;
                    break;
                case Trifasico020304:
                    cabecalho[1] = 0x02;
                    cabecalho[2] = 0x03;
                    cabecalho[3] = 0x04;
                    break;
                case Trifasico030405:
                    cabecalho[1] = 0x03;
                    cabecalho[2] = 0x04;
                    cabecalho[3] = 0x05;
                    break;
                case Trifasico040506:
                    cabecalho[1] = 0x04;
                    cabecalho[2] = 0x05;
                    cabecalho[3] = 0x06;
                    break;
                case Trifasico050607:
                    cabecalho[1] = 0x05;
                    cabecalho[2] = 0x06;
                    cabecalho[3] = 0x07;
                    break;
                case Trifasico060708:
                    cabecalho[1] = 0x06;
                    cabecalho[2] = 0x07;
                    cabecalho[3] = 0x08;
                    break;
                case Trifasico070809:
                    cabecalho[1] = 0x07;
                    cabecalho[2] = 0x08;
                    cabecalho[3] = 0x09;
                    break;
                case Trifasico080910:
                    cabecalho[1] = 0x08;
                    cabecalho[2] = 0x09;
                    cabecalho[3] = 0x10;
                    break;
                case Trifasico091011:
                    cabecalho[1] = 0x09;
                    cabecalho[2] = 0x10;
                    cabecalho[3] = 0x11;
                    break;
                case Trifasico101112:
                    cabecalho[1] = 0x10;
                    cabecalho[2] = 0x11;
                    cabecalho[3] = 0x12;
                    break;
                case Trifasico30200:
                    cabecalho[1] = 0x14;
                    cabecalho[2] = 0x25;
                    cabecalho[3] = 0x36;
                    break;
                case Hospedeiro:
                    cabecalho[0] = 0x00;
                    cabecalho[1] = 0x00;
                    cabecalho[2] = 0x00;
                    cabecalho[3] = 0x00;
                    break;
                default:
                    break;
            }
        }

        return cabecalho;
    }

    private static byte[] GeraCorpoPergunta(byte comando) {
        byte[] corpo = new byte[64];
        corpo[0] = comando;
        corpo[3] = 0x03; // numero leitor
        return corpo;
    }

    private static byte[] GeraCorpoPerguntaEstendida(byte subcomando, byte[] enderecoMac) {
        byte[] corpo = new byte[64];
        corpo[0] = ProtocoloAbsoluto.ComandoAB;
        corpo[1] = enderecoMac[2];
        corpo[2] = enderecoMac[1];
        corpo[3] = enderecoMac[0];
        corpo[4] = subcomando;
        return corpo;
    }

    private static byte[] GeraCorpoPerguntaEstendida(byte subcomando) {
        byte[] corpo = new byte[64];
        corpo[0] = ProtocoloAbsoluto.ComandoAB;
        corpo[3] = 0x03; // numero leitor
        corpo[4] = subcomando;
        return corpo;
    }

    private static byte[] GeraCRC(byte[] corpo) {
        byte[] crc = new byte[2];
        int crc16 = CRC.crc16(corpo);
        crc[0] = (byte) (crc16 & 0xff);
        crc[1] = (byte) ((crc16 >> 8) & 0xff);
        return crc;
    }

    private static byte[] GeraMensagem(byte[] numeroMedidor, byte[] corpo) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte[] crc = GeraCRC(corpo);
        try {
            stream.write((byte) 0x99);
            stream.write(numeroMedidor);
            stream.write(corpo);
            stream.write(crc);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stream.toByteArray();
    }

}
