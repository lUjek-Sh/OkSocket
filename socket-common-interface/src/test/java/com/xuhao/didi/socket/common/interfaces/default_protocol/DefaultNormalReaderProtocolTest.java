package com.xuhao.didi.socket.common.interfaces.default_protocol;

import org.junit.Test;

import java.nio.ByteOrder;

public class DefaultNormalReaderProtocolTest {

    @Test(expected = IllegalArgumentException.class)
    public void getBodyLengthShouldRejectNullHeader() {
        new DefaultNormalReaderProtocol().getBodyLength(null, ByteOrder.BIG_ENDIAN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getBodyLengthShouldRejectShortHeader() {
        new DefaultNormalReaderProtocol().getBodyLength(new byte[]{0x00, 0x01}, ByteOrder.BIG_ENDIAN);
    }
}
