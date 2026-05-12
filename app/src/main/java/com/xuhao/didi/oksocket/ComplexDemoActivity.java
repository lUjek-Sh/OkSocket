package com.xuhao.didi.oksocket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xuhao.didi.core.iocore.interfaces.IPulseSendable;
import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.core.pojo.OriginalData;
import com.xuhao.didi.oksocket.adapter.LogAdapter;
import com.xuhao.didi.oksocket.data.DefaultSendBean;
import com.xuhao.didi.oksocket.data.HandShakeBean;
import com.xuhao.didi.oksocket.data.LogBean;
import com.xuhao.didi.oksocket.data.PulseBean;
import com.xuhao.didi.socket.client.impl.client.action.ActionDispatcher;
import com.xuhao.didi.socket.client.sdk.OkSocket;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;
import com.xuhao.didi.socket.client.sdk.client.connection.NoneReconnect;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ComplexDemoActivity extends AppCompatActivity {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final LogAdapter mSendLogAdapter = new LogAdapter();
    private final LogAdapter mReceLogAdapter = new LogAdapter();
    private final SocketActionAdapter adapter = new SocketActionAdapter() {
        @Override
        public void onSocketConnectionSuccess(ConnectionInfo info, String action) {
            logRece("Connection successful");
            mManager.send(new HandShakeBean());
            mConnect.setText(R.string.disconnect);
            initSwitch();
            mManager.getPulseManager().setPulseSendable(new PulseBean());
            mIPET.setEnabled(true);
            mPortET.setEnabled(true);
        }

        @Override
        public void onSocketDisconnection(ConnectionInfo info, String action, Exception e) {
            if (e instanceof RedirectException) {
                logSend("Redirecting connection");
                mManager.switchConnectionInfo(((RedirectException) e).redirectInfo);
                mManager.connect();
                mIPET.setEnabled(true);
                mPortET.setEnabled(true);
            } else if (e != null) {
                logSend("Disconnected with exception: " + e.getMessage());
                mIPET.setEnabled(false);
                mPortET.setEnabled(false);
            } else {
                logSend("Disconnected manually");
                mIPET.setEnabled(false);
                mPortET.setEnabled(false);
            }
            mConnect.setText(R.string.connect);
        }

        @Override
        public void onSocketConnectionFailed(ConnectionInfo info, String action, Exception e) {
            logSend("Connection failed");
            mConnect.setText(R.string.connect);
            mIPET.setEnabled(false);
            mPortET.setEnabled(false);
        }

        @Override
        public void onSocketReadResponse(ConnectionInfo info, String action, OriginalData data) {
            String str = new String(data.getBodyBytes(), StandardCharsets.UTF_8);
            JsonObject jsonObject = JsonParser.parseString(str).getAsJsonObject();
            int cmd = jsonObject.get("cmd").getAsInt();
            if (cmd == 54) {
                String handshake = jsonObject.get("handshake").getAsString();
                logRece("Handshake success: " + handshake + ". Starting heartbeat.");
            } else if (cmd == 57) {
                String redirect = jsonObject.get("data").getAsString();
                String ip = redirect.split(":")[0];
                int port = Integer.parseInt(redirect.split(":")[1]);
                ConnectionInfo redirectInfo = new ConnectionInfo(ip, port);
                redirectInfo.setBackupInfo(mInfo.getBackupInfo());
                mManager.getReconnectionManager().addIgnoreException(RedirectException.class);
                mManager.disconnect(new RedirectException(redirectInfo));
            } else if (cmd == 14) {
                logRece("Heartbeat received");
                mManager.getPulseManager().feed();
            } else {
                logRece(str);
            }
        }

        @Override
        public void onSocketWriteResponse(ConnectionInfo info, String action, ISendable data) {
            String str = decodeSendable(data);
            JsonObject jsonObject = JsonParser.parseString(str).getAsJsonObject();
            int cmd = jsonObject.get("cmd").getAsInt();
            if (cmd == 54) {
                String handshake = jsonObject.get("handshake").getAsString();
                logSend("Sending handshake: " + handshake);
                mManager.getPulseManager().pulse();
            } else {
                logSend(str);
            }
        }

        @Override
        public void onPulseSend(ConnectionInfo info, IPulseSendable data) {
            String str = decodeSendable(data);
            JsonObject jsonObject = JsonParser.parseString(str).getAsJsonObject();
            if (jsonObject.get("cmd").getAsInt() == 14) {
                logSend("Sending heartbeat");
            }
        }
    };

    private ConnectionInfo mInfo;
    private Button mConnect;
    private IConnectionManager mManager;
    private EditText mIPET;
    private EditText mPortET;
    private EditText mFrequencyET;
    private SwitchCompat mReconnectSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complex);
        findViews();
        initData();
        setListener();
    }

    private void findViews() {
        RecyclerView mSendList = findViewById(R.id.send_list);
        RecyclerView mReceList = findViewById(R.id.rece_list);
        Button mClearLog = findViewById(R.id.clear_log);
        Button mSetFrequency = findViewById(R.id.set_pulse_frequency);
        mFrequencyET = findViewById(R.id.pulse_frequency);
        mConnect = findViewById(R.id.connect);
        mIPET = findViewById(R.id.ip);
        mPortET = findViewById(R.id.port);
        Button mRedirect = findViewById(R.id.redirect);
        Button mManualPulse = findViewById(R.id.manual_pulse);
        mReconnectSwitch = findViewById(R.id.switch_reconnect);

        mSendList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mSendList.setAdapter(mSendLogAdapter);
        mReceList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mReceList.setAdapter(mReceLogAdapter);

        mClearLog.setOnClickListener(v -> {
            mReceLogAdapter.clearLogs();
            mSendLogAdapter.clearLogs();
        });
        mRedirect.setOnClickListener(v -> redirectConnection());
        mSetFrequency.setOnClickListener(v -> updatePulseFrequency());
        mManualPulse.setOnClickListener(v -> {
            if (mManager != null) {
                mManager.getPulseManager().trigger();
            }
        });
    }

    private void initData() {
        mIPET.setEnabled(false);
        mPortET.setEnabled(false);

        mInfo = new ConnectionInfo(getString(R.string.default_ip), Integer.parseInt(getString(R.string.default_port)));
        mIPET.setText(mInfo.getIp());
        mPortET.setText(String.valueOf(mInfo.getPort()));

        OkSocketOptions.Builder builder = new OkSocketOptions.Builder()
                .setReconnectionManager(new NoneReconnect())
                .setCallbackThreadModeToken(new OkSocketOptions.ThreadModeToken() {
                    @Override
                    public void handleCallbackEvent(ActionDispatcher.ActionRunnable runnable) {
                        mainHandler.post(runnable);
                    }
                });
        mManager = OkSocket.open(mInfo).option(builder.build());
    }

    private void setListener() {
        mManager.registerReceiver(adapter);
        mReconnectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked && !(mManager.getReconnectionManager() instanceof NoneReconnect)) {
                mManager.option(new OkSocketOptions.Builder(mManager.getOption())
                        .setReconnectionManager(new NoneReconnect())
                        .build());
                logSend("Reconnect manager disabled");
            } else if (isChecked && mManager.getReconnectionManager() instanceof NoneReconnect) {
                mManager.option(new OkSocketOptions.Builder(mManager.getOption())
                        .setReconnectionManager(OkSocketOptions.getDefault().getReconnectionManager())
                        .build());
                logSend("Reconnect manager enabled");
            }
        });

        mConnect.setOnClickListener(v -> {
            if (mManager == null) {
                return;
            }
            if (!mManager.isConnect()) {
                mManager.connect();
            } else {
                mConnect.setText(R.string.disconnecting);
                mManager.disconnect();
            }
        });
    }

    private void initSwitch() {
        OkSocketOptions okSocketOptions = mManager.getOption();
        mReconnectSwitch.setChecked(!(okSocketOptions.getReconnectionManager() instanceof NoneReconnect));
    }

    private void redirectConnection() {
        if (mManager == null) {
            return;
        }
        String ip = mIPET.getText().toString();
        String portStr = mPortET.getText().toString();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("cmd", 57);
        jsonObject.addProperty("data", ip + ":" + portStr);
        DefaultSendBean bean = new DefaultSendBean();
        bean.setContent(new Gson().toJson(jsonObject));
        mManager.send(bean);
    }

    private void updatePulseFrequency() {
        if (mManager == null) {
            return;
        }
        String frequencyStr = mFrequencyET.getText().toString();
        try {
            long frequency = Long.parseLong(frequencyStr);
            OkSocketOptions okOptions = new OkSocketOptions.Builder(mManager.getOption())
                    .setPulseFrequency(frequency)
                    .build();
            mManager.option(okOptions);
        } catch (NumberFormatException e) {
            logSend(getString(R.string.invalid_pulse_frequency));
        }
    }

    private void logSend(String log) {
        mSendLogAdapter.prepend(new LogBean(System.currentTimeMillis(), log));
    }

    private void logRece(String log) {
        mReceLogAdapter.prepend(new LogBean(System.currentTimeMillis(), log));
    }

    private String decodeSendable(ISendable data) {
        byte[] packet = data.parse();
        if (packet.length <= 4) {
            return "";
        }
        byte[] body = Arrays.copyOfRange(packet, 4, packet.length);
        return new String(body, StandardCharsets.UTF_8);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mManager != null) {
            mManager.disconnect();
            mManager.unRegisterReceiver(adapter);
        }
    }
}
