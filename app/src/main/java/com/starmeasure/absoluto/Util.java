package com.starmeasure.absoluto;

import org.jetbrains.annotations.NotNull;

public class Util {

    public static String ByteArrayToHexString(byte[] data) {
        final StringBuilder stringBuilder = new StringBuilder(data.length);
        for (byte byteChar : data)
            stringBuilder.append(String.format("%02X ", byteChar));
        return stringBuilder.toString();
    }

    public static String ByteArrayToASCIIString(byte[] data) {
        final StringBuilder stringBuilder = new StringBuilder(data.length);
        for (byte byteChar : data)
            stringBuilder.append(String.format("%02X ", byteChar));
        return stringBuilder.toString();
    }

    public static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }

    public static String dateBuilder(String toBuild) {
        StringBuilder builder = new StringBuilder();
        char[] mList = toBuild.toCharArray();
        for (int i = 0; i < mList.length; i++) {
            char mChar = mList[i];
            builder.append(mChar);
            if (i == 1 || i == 3) {
                builder.append("/");
            }
        }
        return builder.toString();
    }

    public static String timeBuilder(String toBuild) {
        StringBuilder builder = new StringBuilder();
        char[] mList = toBuild.toCharArray();
        for (int i = 0; i < mList.length; i++) {
            char mChar = mList[i];
            if (i < 6) {
                builder.append(mChar);
                if (i == 1 || i == 3) {
                    builder.append(":");
                }
            }
        }
        return builder.toString();
    }
}

