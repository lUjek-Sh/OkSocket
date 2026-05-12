package com.xuhao.didi.oksocket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xuhao.didi.core.iocore.interfaces.IPulseSendable;
import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.core.pojo.OriginalData;
import com.xuhao.didi.oksocket.adapter.LogAdapter;
import com.xuhao.didi.oksocket.data.HandShakeBean;
import com.xuhao.didi.oksocket.data.LogBean;
import com.xuhao.didi.oksocket.data.MsgDataBean;
import com.xuhao.didi.socket.client.impl.client.action.ActionDispatcher;
import com.xuhao.didi.socket.client.sdk.OkSocket;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;
import com.xuhao.didi.socket.client.sdk.client.connection.NoneReconnect;

import java.nio.charset.StandardCharsets;

public class SimpleDemoActivity extends AppCompatActivity {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final LogAdapter mSendLogAdapter = new LogAdapter();
    private final LogAdapter mReceLogAdapter = new LogAdapter();
    private final SocketActionAdapter adapter = new SocketActionAdapter() {
        @Override
        public void onSocketConnectionSuccess(ConnectionInfo info, String action) {
            mManager.send(new HandShakeBean());
            mConnect.setText(R.string.disconnect);
            mIPET.setEnabled(false);
            mPortET.setEnabled(false);
        }

        @Override
        public void onSocketDisconnection(ConnectionInfo info, String action, Exception e) {
            if (e != null) {
                logSend("Disconnected with exception: " + e.getMessage());
            } else {
                logSend("Disconnected manually");
            }
            mConnect.setText(R.string.connect);
            mIPET.setEnabled(true);
            mPortET.setEnabled(true);
        }

        @Override
        public void onSocketConnectionFailed(ConnectionInfo info, String action, Exception e) {
            logSend("Connection failed");
            mConnect.setText(R.string.connect);
            mIPET.setEnabled(true);
            mPortET.setEnabled(true);
        }

        @Override
        public void onSocketReadResponse(ConnectionInfo info, String action, OriginalData data) {
            logRece(new String(data.getBodyBytes(), StandardCharsets.UTF_8));
        }

        @Override
        public void onSocketWriteResponse(ConnectionInfo info, String action, ISendable data) {
            logSend(decodeSendable(data));
        }

        @Override
        public void onPulseSend(ConnectionInfo info, IPulseSendable data) {
            logSend(decodeSendable(data));
        }
    };

    private ConnectionInfo mInfo;
    private Button mConnect;
    private EditText mIPET;
    private EditText mPortET;
    private IConnectionManager mManager;
    private EditText mSendET;
    private OkSocketOptions mOkOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple);
        findViews();
        initData();
        setListener();
    }

    private void findViews() {
        RecyclerView mSendList = findViewById(R.id.send_list);
        RecyclerView mReceList = findViewById(R.id.rece_list);
        mIPET = findViewById(R.id.ip);
        mPortET = findViewById(R.id.port);
        Button mClearLog = findViewById(R.id.clear_log);
        mConnect = findViewById(R.id.connect);
        mSendET = findViewById(R.id.send_et);
        Button mSendBtn = findViewById(R.id.send_btn);

        mSendList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mSendList.setAdapter(mSendLogAdapter);
        mReceList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mReceList.setAdapter(mReceLogAdapter);

        mClearLog.setOnClickListener(v -> {
            mReceLogAdapter.clearLogs();
            mSendLogAdapter.clearLogs();
        });
        mSendBtn.setOnClickListener(v -> sendMessage());
    }

    private void initData() {
        initManager();
    }

    private void initManager() {
        Handler handler = new Handler(Looper.getMainLooper());
        mInfo = new ConnectionInfo(mIPET.getText().toString(), Integer.parseInt(mPortET.getText().toString()));
        mOkOptions = new OkSocketOptions.Builder()
                .setReconnectionManager(new NoneReconnect())
                .setConnectTimeoutSecond(10)
                .setCallbackThreadModeToken(new OkSocketOptions.ThreadModeToken() {
                    @Override
                    public void handleCallbackEvent(ActionDispatcher.ActionRunnable runnable) {
                        handler.post(runnable);
                    }
                })
                .build();
        mManager = OkSocket.open(mInfo).option(mOkOptions);
        mManager.registerReceiver(adapter);
    }

    private void setListener() {
        mConnect.setOnClickListener(v -> {
            if (mManager == null) {
                return;
            }
            if (!mManager.isConnect()) {
                initManager();
                mManager.connect();
                mIPET.setEnabled(false);
                mPortET.setEnabled(false);
            } else {
                mConnect.setText(R.string.disconnecting);
                mManager.disconnect();
            }
        });
    }

    private void sendMessage() {
        if (mManager == null) {
            return;
        }
        if (!mManager.isConnect()) {
            Toast.makeText(this, R.string.unconnected, Toast.LENGTH_SHORT).show();
            return;
        }
        String msg = mSendET.getText().toString();
        if (TextUtils.isEmpty(msg.trim())) {
            return;
        }
        mManager.send(new MsgDataBean(msg));
        mSendET.setText("");
    }

    private void logSend(final String log) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mSendLogAdapter.prepend(new LogBean(System.currentTimeMillis(), log));
        } else {
            String threadName = Thread.currentThread().getName();
            mainHandler.post(() -> logSend(threadName + " (background): " + log));
        }
    }

    private void logRece(final String log) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mReceLogAdapter.prepend(new LogBean(System.currentTimeMillis(), log));
        } else {
            String threadName = Thread.currentThread().getName();
            mainHandler.post(() -> logRece(threadName + " (background): " + log));
        }
    }

    private String decodeSendable(ISendable data) {
        byte[] packet = data.parse();
        if (packet.length <= 4) {
            return "";
        }
        return new String(packet, 4, packet.length - 4, StandardCharsets.UTF_8);
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
