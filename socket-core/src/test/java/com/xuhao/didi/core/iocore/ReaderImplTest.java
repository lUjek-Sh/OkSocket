package com.xuhao.didi.core.iocore;

import com.xuhao.didi.core.exceptions.ReadException;
import com.xuhao.didi.core.iocore.interfaces.IIOCoreOptions;
import com.xuhao.didi.core.iocore.interfaces.IStateSender;
import com.xuhao.didi.core.protocol.IReaderProtocol;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.ByteOrder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ReaderImplTest {
    @Test
    public void readShouldNotWrapReadExceptionTwice() {
        ReaderImpl reader = new ReaderImpl();
        reader.setOption(new TestOptions());
        reader.initialize(new ByteArrayInputStream(new byte[]{0x01}), new NoOpStateSender());

        try {
            reader.read();
        } catch (ReadException e) {
            assertEquals("socket input stream reached end of file while reading 4 bytes", e.getMessage());
            assertNull(e.getCause());
            return;
        }

        throw new AssertionError("Expected ReadException");
    }

    private static final class TestOptions implements IIOCoreOptions {
        private final IReaderProtocol protocol = new IReaderProtocol() {
            @Override
            public int getHeaderLength() {
                return 4;
            }

            @Override
            public int getBodyLength(byte[] header, ByteOrder byteOrder) {
                return 0;
            }
        };

        @Override
        public ByteOrder getReadByteOrder() {
            return ByteOrder.BIG_ENDIAN;
        }

        @Override
        public int getMaxReadDataMB() {
            return 1;
        }

        @Override
        public IReaderProtocol getReaderProtocol() {
            return protocol;
        }

        @Override
        public ByteOrder getWriteByteOrder() {
            return ByteOrder.BIG_ENDIAN;
        }

        @Override
        public int getReadPackageBytes() {
            return 1;
        }

        @Override
        public int getWritePackageBytes() {
            return 1;
        }

        @Override
        public boolean isDebug() {
            return false;
        }
    }

    private static final class NoOpStateSender implements IStateSender {
        @Override
        public void sendBroadcast(String action, Serializable serializable) {
        }

        @Override
        public void sendBroadcast(String action) {
        }
    }
}
