package com.xuhao.didi.core.iocore;

import com.xuhao.didi.core.exceptions.ReadException;
import com.xuhao.didi.core.iocore.interfaces.IOAction;
import com.xuhao.didi.core.pojo.OriginalData;
import com.xuhao.didi.core.protocol.IReaderProtocol;
import com.xuhao.didi.core.utils.BytesUtils;
import com.xuhao.didi.core.utils.SLog;

import java.io.IOException;

/**
 * Created by xuhao on 2017/5/31.
 */

public class ReaderImpl extends AbsReader {

    @Override
    public void read() throws RuntimeException {
        OriginalData originalData = new OriginalData();
        IReaderProtocol headerProtocol = mOkOptions.getReaderProtocol();
        int headerLength = headerProtocol.getHeaderLength();
        try {
            byte[] headBytes = new byte[headerLength];
            readFully(headBytes, 0, headBytes.length);
            originalData.setHeadBytes(headBytes);
            if (SLog.isDebug()) {
                SLog.i("read head: " + BytesUtils.toHexStringForLog(headBytes));
            }
            int bodyLength = headerProtocol.getBodyLength(originalData.getHeadBytes(), mOkOptions.getReadByteOrder());
            if (SLog.isDebug()) {
                SLog.i("need read body length: " + bodyLength);
            }
            if (bodyLength > 0) {
                if (bodyLength > mOkOptions.getMaxReadDataMB() * 1024 * 1024) {
                    throw new ReadException("Need to follow the transmission protocol.\r\n" +
                            "Please check the client/server code.\r\n" +
                            "According to the packet header data in the transport protocol, the package length is " + bodyLength + " Bytes.\r\n" +
                            "You need check your <ReaderProtocol> definition");
                }
                byte[] bodyBytes = new byte[bodyLength];
                readBodyFromChannel(bodyBytes);
                originalData.setBodyBytes(bodyBytes);
            } else if (bodyLength == 0) {
                originalData.setBodyBytes(new byte[0]);
            } else if (bodyLength < 0) {
                throw new ReadException(
                        "read body is wrong,this socket input stream is end of file read " + bodyLength + " ,that mean this socket is disconnected by server");
            }
            mStateSender.sendBroadcast(IOAction.ACTION_READ_COMPLETE, originalData);
        } catch (ReadException e) {
            throw e;
        } catch (Exception e) {
            throw new ReadException(e);
        }
    }

    private void readFully(byte[] target, int offset, int length) throws IOException {
        int totalRead = 0;
        while (totalRead < length) {
            int readSize = mInputStream.read(target, offset + totalRead, length - totalRead);
            if (readSize == -1) {
                throw new ReadException(
                        "socket input stream reached end of file while reading " + length + " bytes");
            }
            totalRead += readSize;
        }
    }

    private void readBodyFromChannel(byte[] bodyBytes) throws IOException {
        int readPackageBytes = mOkOptions.getReadPackageBytes();
        if (readPackageBytes <= 0) {
            throw new ReadException("read package bytes must be greater than 0");
        }

        int offset = 0;
        while (offset < bodyBytes.length) {
            int readSize = Math.min(readPackageBytes, bodyBytes.length - offset);
            readFully(bodyBytes, offset, readSize);
            offset += readSize;
        }
        if (SLog.isDebug()) {
            SLog.i("read total bytes: " + BytesUtils.toHexStringForLog(bodyBytes));
            SLog.i("read total length:" + bodyBytes.length);
        }
    }

}
