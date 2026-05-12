package com.xuhao.didi.oksocket.data;


import com.xuhao.didi.core.iocore.interfaces.IPulseSendable;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class PulseBean implements IPulseSendable {
    private String str = "";

    public PulseBean() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("cmd", 14);
            str = jsonObject.toString();
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to create pulse payload", e);
        }
    }

    @Override
    public byte[] parse() {
        byte[] body = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.allocate(4 + body.length);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(body.length);
        bb.put(body);
        return bb.array();
    }
}
