package com.xioshe.only.java.base.lang.character;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * 关于 char 和 {@link Character} 的一些细节
 * <br/>
 * 详细说明看对应测试类，此处主要是一些辅助工具类
 *
 * @author xioshe 2022-03-29
 */
public class SecretInCharacter {

    // 将字符串转换为对应的 Unicode codepoint
    public static int stringToCodepoint(String str) {
        return str.codePointAt(0);
    }

    // 实现 codepoint 转换为 UTF-8 编码的字节数组
    public static byte[] codeToUtf8Bytes(int code) {
        byte[] bytes = new byte[4];
        if (code < 0x80) {
            bytes[0] = (byte) code;
        } else if (code < 0x800) {
            bytes[0] = (byte) (0xC0 | (code >> 6));
            bytes[1] = (byte) (0x80 | (code & 0x3F));
        } else if (code < 0x10000) {
            bytes[0] = (byte) (0xE0 | (code >> 12));
            bytes[1] = (byte) (0x80 | ((code >> 6) & 0x3F));
            bytes[2] = (byte) (0x80 | (code & 0x3F));
        } else {
            bytes[0] = (byte) (0xF0 | (code >> 18));
            bytes[1] = (byte) (0x80 | ((code >> 12) & 0x3F));
            bytes[2] = (byte) (0x80 | ((code >> 6) & 0x3F));
            bytes[3] = (byte) (0x80 | (code & 0x3F));
        }
        return bytes;
    }

    public static byte[] codeToUtf16Bytes(int code) {
        byte[] bytes = new byte[2];
        if (code < 0x10000) {
            bytes[0] = (byte) (code >> 8);
            bytes[1] = (byte) (code & 0xFF);
        } else {
            bytes[0] = (byte) ((code >> 10) + 0xD800);
            bytes[1] = (byte) ((code & 0x3FF) + 0xDC00);
        }
        return bytes;
    }

    public static byte[] toUtf32Bytes(int code) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (code >> 24);
        bytes[1] = (byte) ((code >> 16) & 0xFF);
        bytes[2] = (byte) ((code >> 8) & 0xFF);
        bytes[3] = (byte) (code & 0xFF);
        return bytes;
    }

    // 将字符串按要求转为对应编码的字节数组
    public static byte[] stringToBytes(String str, String charset) {
        if (str == null) {
            return null;
        }
        if (!Charset.isSupported(charset)) {
            return null;
        }
        byte[] bytes = str.getBytes(Charset.forName(charset));
        // remove bom
        if (Byte.toUnsignedInt(bytes[0]) == 0xfe &&
                Byte.toUnsignedInt(bytes[1]) == 0xff) {
            bytes = Arrays.copyOfRange(bytes, 2, bytes.length);
        }
        return bytes;
    }

    /**
     * 格式化输出工具<br/>
     * 将字节数组转换为二进制字符串，并按空格字符区分
     * <pre>
     * 1           -> "00000000 00000000 00000000 00000001"
     * max_integer -> "01111111 11111111 11111111 11111111"
     * min_integer -> "10000000 00000000 00000000 00000000"
     * </pre>
     *
     * @param bytes byte array
     * @return binary string
     */
    public static String toReadableBinaryStr(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        var sb = new StringBuilder(bytes.length << 3);
        for (byte b : bytes) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(leftPadZeros(Integer.toBinaryString(Byte.toUnsignedInt(b)), 8));
        }
        return sb.toString();
    }

    // from apache common lang3

    // 用 0 填充字符串到制定长度
    private
    static String leftPadZeros(String str, int size) {
        if (str == null) {
            return null;
        }
        final int pads = size - str.length();
        if (pads <= 0) {
            return str; // returns original String when possible
        }
        return repeatZero(pads).concat(str);
    }

    private
    static String repeatZero(int repeat) {
        if (repeat <= 0) {
            return "";
        }
        final char[] buf = new char[repeat];
        Arrays.fill(buf, '0');
        return new String(buf);
    }
}
