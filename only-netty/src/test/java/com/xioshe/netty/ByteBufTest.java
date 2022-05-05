package com.xioshe.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author xioshe 2022-05-03
 */
class ByteBufTest {

    @Test
    void read_from_another_ByteBuf_with_change_index() {
        var buf = Unpooled.copiedBuffer("Netty", StandardCharsets.UTF_8);
        var dest = Unpooled.copiedBuffer("Goodbye", StandardCharsets.UTF_8);
        var sri = buf.readerIndex();
        var swi = buf.writerIndex();
        var dri = dest.readerIndex();
        var dwi = dest.writerIndex();
        var dl = dest.readableBytes();
        // 会改变 dest 的 readerIndex
        buf.writeBytes(dest);
        assertThat(buf.readerIndex()).isEqualTo(sri);
        assertThat(buf.writerIndex()).isNotEqualTo(swi).isEqualTo(swi + dl);
        assertThat(dest.readerIndex()).isNotEqualTo(dri).isEqualTo(dri + dl);
        assertThat(dest.writerIndex()).isEqualTo(dwi);
    }

    @Test
    void read_from_another_ByteBuf_without_change_index() {
        var buf = Unpooled.copiedBuffer("Netty", StandardCharsets.UTF_8);
        var dest = Unpooled.copiedBuffer("Goodbye", StandardCharsets.UTF_8);
        var sri = buf.readerIndex();
        var swi = buf.writerIndex();
        var dri = dest.readerIndex();
        var dwi = dest.writerIndex();
        // 不会改变 dest 的 readerIndex
        buf.writeBytes(dest, 4, 3);
        assertThat(buf.readerIndex()).isEqualTo(sri);
        assertThat(buf.writerIndex()).isNotEqualTo(swi).isEqualTo(swi + 3);
        assertThat(dest.readerIndex()).isEqualTo(dri);
        assertThat(dest.writerIndex()).isEqualTo(dwi);
    }

}