package com.xioshe.only.java.base.lang.character;

import com.xioshe.only.java.base.misc.SomethingUnsafe;
import org.assertj.core.internal.Bytes;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static com.xioshe.only.java.base.lang.character.SecretInCharacter.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 关于编码与 char 的一些知识点
 *
 * @author xioshe 2022-04-03
 */
class SecretInCharacterTest {

    @Test
    void unicode_and_encodings() throws UnsupportedEncodingException, NoSuchFieldException, IllegalAccessException {
        /*
        ## Unicode字符集
        - Unicode 是一个庞大的字符集（Charset），为世界上几乎所有的字符都分配了一个对应的编号。
          - 这些编号的范围很广，从 0x000000 到 0x10ffff。
            - 0x000000-0x00ffff 称为**基本平面**，常见的中英文基本都包括在其中。
            - 0x010000-0x10ffff 称为**扩展平面**，包含了一些不常见的字符。
          - 这个独一无二的编号，称为 **codepoint**。对应的字符称为 **code unit**。
        - Unicode 仅仅给字符分配了符号，并没有规定相应的二进制表示方式。
          - 由于 codepoint 范围很广，最大值需要 3byte 才能表示，最小值却只需要 1byte 表示，如果采用等长格式存储，空间利用率不高。
          - 基于不同的空间利用效率，产生了不同的 Unicode 存储方案。这些方案就称为编码（encoding），常见的为 UTF-8，UTF-16，UTF-32。
        - Unicode 字符集兼容 ASCII 字符集。
        */
        char ch = 0x4e2d;
        char zhong = '中';
        assertThat(ch).isEqualTo(zhong);
        char ascii_97 = 97;
        char a = 'a';
        assertThat(ascii_97).isEqualTo(a);

        /*
        ## 编码
        - UTF-32 格式即以 32 bit 的定长格式来存储 codepoint 的编码。每个字符都占用 4byte 空间。这种方案空间利用率低，但是存储效率高。
          - UTF-32 即直接将 codepoint 转换为 32bit 的二进制格式。
        - UTF-16 格式是一种变长编码方案，将基本平面的字符编码为 2byte，扩展平面的字符编码为 4byte。扩展平面使用频率很高，有效节约空间。
          - 基本平面的有效位与 codepoint 的二进制有效位是一样的。
          - 扩展平面使用特殊前缀来表示。
        - UTF-8 格式是一种变长编码方案，将基本平面分为三部分，第一部分为 1byte，第二部分为 2byte，第三部分为 3byte。扩展平面使用 4byte。
          - 只有小于 0x7f 的二进制即 ASCII 部分与 codepoint 二进制一致。
         */

        // 对比 a 的不同编码的字节数组长度
        // a 的 codepoint 直接二进制表示与三种 UTF 编码有效位一致
        var binaryStr_97 = "01100001";
        var aStr = String.valueOf(a);
        assertThat(stringToBytes(aStr, "utf-32").length)
                .as("将 a 转换为 UTF-32 是四字节")
                .isEqualTo(4);
        assertThat(toReadableBinaryStr(stringToBytes(aStr, "utf-32"))).endsWith(binaryStr_97);
        assertThat(stringToBytes(aStr, "utf-16").length)
                .as("将 a 转换为 UTF-16 是两字节")
                .isEqualTo(2);
        assertThat(toReadableBinaryStr(stringToBytes(aStr, "utf-16"))).endsWith(binaryStr_97);
        assertThat(stringToBytes(aStr, "utf-8").length)
                .as("将 a 转换为 UTF-8  是一字节")
                .isEqualTo(1);
        assertThat(toReadableBinaryStr(stringToBytes(aStr, "utf-8"))).endsWith(binaryStr_97);

        // 'З' 的 codepoint 直接二进制与 utf-32 utf-16 是一致的，utf-8 不同，16bit，增加了前缀
        char ze = 0x417; // 'З' in Russian
        var zeStr = String.valueOf(ze);
        var binaryStr_ze = "0100 00010111";
        assertThat(toReadableBinaryStr(stringToBytes(zeStr, "utf-32"))).endsWith(binaryStr_ze);
        assertThat(toReadableBinaryStr(stringToBytes(zeStr, "utf-16"))).endsWith(binaryStr_ze);
        assertThat(toReadableBinaryStr(stringToBytes(zeStr, "utf-8")))
                .doesNotEndWith(binaryStr_ze)
                .startsWith("110");

        // ch 的 codepoint 直接二进制与 utf-32 utf-16 是一致的，utf-8 不同，24bit 增加了前缀
        var chStr = String.valueOf(ch);
        var binaryStr_ch = "01001110 00101101";
        assertThat(toReadableBinaryStr(stringToBytes(chStr, "utf-32"))).endsWith(binaryStr_ch);
        assertThat(toReadableBinaryStr(stringToBytes(chStr, "utf-16"))).endsWith(binaryStr_ch);
        assertThat(toReadableBinaryStr(stringToBytes(chStr, "utf-8")))
                .doesNotEndWith(binaryStr_ch)
                .startsWith("1110");

        // 😊 的 codepoint 直接二进制仅与 utf-32 是一致的，utf-16 为 32bit，前缀，utf-8 为 32bit，另一种前缀
        String emojiStr = "😊"; // 0x1f60a
        var binaryStr_emoji = "0001 11110110 00001010";
        assertThat(toReadableBinaryStr(stringToBytes(emojiStr, "utf-32"))).endsWith(binaryStr_emoji);
        assertThat(toReadableBinaryStr(stringToBytes(emojiStr, "utf-16")))
                .doesNotEndWith(binaryStr_ch)
                        .startsWith("110110");
        assertThat(toReadableBinaryStr(stringToBytes(emojiStr, "utf-8")))
                .doesNotEndWith(binaryStr_emoji)
                .startsWith("11110");
        /*
        ## Java 中的字符
        - Java 的 `char` 是一个2字节的无符号整数，值为 Unicode 的 codepoint。
        - 2 字节只能表示基本平面，不能表示扩展平面。如果需要扩展平面，只能用两个 char 或者字符串拼接。
        - Java 内部采用 UTF-16LE 格式存储字符，字符串内部的 `char[] value`(in JDK9 is `byte[]`) 为 UTF-16 格式。
         */
        // Java 中给 char 赋值
        char c0 = 'a';
        char c1 = 97;
        char c2 = 0x61;
        char c3 = '\u0061'; // 必须对齐至 16bit
        assertThat(c0).isEqualTo(c1).isEqualTo(c2).isEqualTo(c3);
        // char 可以转为整数';
        int i = c0;
        int j = c3;
        assertThat(i).isEqualTo(j).isEqualTo(97);

        // 无法直接用 char 表示扩展平面，但可以用两个 char 或者 String
//        char e0 = '😊'; // error
//        char e1 = 0x1f60a; // error
        char high = '\uD83D';
        char low = '\uDE0A';
        assertThat(emojiStr).isEqualTo(new String(new char[]{high, low}));

        // 查看 value
        String str = "aЗ中😊";
        byte[] bytesInMemory = (byte[]) SomethingUnsafe.peakFeild(str, String.class, "value");
        byte[] utf32Bytes = str.getBytes("utf-32");
        byte[] utf16BEBytes = str.getBytes(StandardCharsets.UTF_16LE);
        byte[] utf8Bytes = str.getBytes(StandardCharsets.UTF_8);
        assertThat(bytesInMemory).isEqualTo(utf16BEBytes);
        assertThat(utf32Bytes.length).isEqualTo(4 * 4);
        assertThat(utf16BEBytes.length).isEqualTo(3 * 2 + 4);
        assertThat(utf8Bytes.length).isEqualTo(1 + 2 + 3 + 4);
    }
}