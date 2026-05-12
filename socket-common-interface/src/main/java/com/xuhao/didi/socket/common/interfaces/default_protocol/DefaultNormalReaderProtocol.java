package com.xuhao.didi.socket.common.interfaces.default_protocol;



import com.xuhao.didi.core.protocol.IReaderProtocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DefaultNormalReaderProtocol implements IReaderProtocol {

    @Override
    public int getHeaderLength() {
        return 4;
    }

    @Override
    public int getBodyLength(byte[] header, ByteOrder byteOrder) {
        if (header == null) {
            throw new IllegalArgumentException("Protocol header can not be null");
        }
        if (header.length != getHeaderLength()) {
            throw new IllegalArgumentException(
                    "Protocol header length mismatch, expected "
                            + getHeaderLength()
                            + " bytes but was "
                            + header.length);
        }
        ByteBuffer bb = ByteBuffer.wrap(header);
        bb.order(byteOrder);
        return bb.getInt();
    }
}
