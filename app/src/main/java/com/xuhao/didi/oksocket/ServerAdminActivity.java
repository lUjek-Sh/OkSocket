package com.xuhao.didi.oksocket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xuhao.didi.core.pojo.OriginalData;
import com.xuhao.didi.oksocket.adapter.LogAdapter;
import com.xuhao.didi.oksocket.data.AdminHandShakeBean;
import com.xuhao.didi.oksocket.data.AdminKickOfflineBean;
import com.xuhao.didi.oksocket.data.LogBean;
import com.xuhao.didi.oksocket.data.RestartBean;
import com.xuhao.didi.socket.client.impl.client.action.ActionDispatcher;
import com.xuhao.didi.socket.client.sdk.OkSocket;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;

import java.nio.charset.StandardCharsets;

public class ServerAdminActivity extends AppCompatActivity {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final LogAdapter mReceLogAdapter = new LogAdapter();
    private final SocketActionAdapter adapter = new SocketActionAdapter() {
        @Override
        public void onSocketConnectionSuccess(ConnectionInfo info, String action) {
            mManager.send(new AdminHandShakeBean(mPass));
            mConnect.setText(R.string.disconnect);
            log("Connection successful");
            mPortEt.setEnabled(false);
            mIPEt.setEnabled(false);
        }

        @Override
        public void onSocketDisconnection(ConnectionInfo info, String action, Exception e) {
            if (e != null) {
                log("Disconnected with exception: " + e.getMessage());
            } else {
                log("Disconnected manually");
            }
            mPortEt.setEnabled(true);
            mIPEt.setEnabled(true);
            mConnect.setText(R.string.connect);
        }

        @Override
        public void onSocketConnectionFailed(ConnectionInfo info, String action, Exception e) {
            log("Connection failed");
            mConnect.setText(R.string.connect);
            mPortEt.setEnabled(true);
            mIPEt.setEnabled(true);
        }

        @Override
        public void onSocketReadResponse(ConnectionInfo info, String action, OriginalData data) {
            log(new String(data.getBodyBytes(), StandardCharsets.UTF_8));
        }
    };

    private ConnectionInfo mInfo;
    private EditText mIPEt;
    private EditText mPortEt;
    private IConnectionManager mManager;
    private OkSocketOptions mOkOptions;
    private Button mConnect;
    private String mPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        findViews();
        initData();
        setListener();
    }

    private void findViews() {
        RecyclerView mOpsList = findViewById(R.id.ops_list);
        mIPEt = findViewById(R.id.ip);
        mPortEt = findViewById(R.id.port);
        Button mClearLog = findViewById(R.id.clear_log);
        mConnect = findViewById(R.id.connect);
        Button mRestart = findViewById(R.id.restart);
        Button mKickOffLine = findViewById(R.id.kick_people_offline);

        mOpsList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mOpsList.setAdapter(mReceLogAdapter);

        mClearLog.setOnClickListener(v -> mReceLogAdapter.clearLogs());
        mRestart.setOnClickListener(v -> restartServer());
        mKickOffLine.setOnClickListener(v -> showKickOfflineDialog());
    }

    private void initData() {
        initManager();
    }

    private void initManager() {
        Handler handler = new Handler(Looper.getMainLooper());
        mInfo = new ConnectionInfo(mIPEt.getText().toString(), Integer.parseInt(mPortEt.getText().toString()));
        mOkOptions = new OkSocketOptions.Builder()
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
                showAdminLoginDialog();
            } else {
                mConnect.setText(R.string.disconnecting);
                mManager.disconnect();
            }
        });
    }

    private void showAdminLoginDialog() {
        final View view = LayoutInflater.from(this).inflate(R.layout.alert_admin_login_layout, null);
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_login_title)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.login, (dialog, which) -> {
                    mPass = ((EditText) view.findViewById(R.id.pass)).getText().toString();
                    mPortEt.setEnabled(false);
                    mIPEt.setEnabled(false);
                    mManager.connect();
                })
                .show();
    }

    private void restartServer() {
        if (mManager == null) {
            return;
        }
        if (!mManager.isConnect()) {
            Toast.makeText(this, R.string.connect_first, Toast.LENGTH_SHORT).show();
            return;
        }
        mManager.send(new RestartBean());
    }

    private void showKickOfflineDialog() {
        if (mManager == null) {
            return;
        }
        if (!mManager.isConnect()) {
            Toast.makeText(this, R.string.connect_first, Toast.LENGTH_SHORT).show();
            return;
        }
        final View view = LayoutInflater.from(this).inflate(R.layout.alert_kickoffline_layout, null);
        new AlertDialog.Builder(this)
                .setTitle(R.string.kick_offline_title)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm_action, (dialog, which) -> {
                    String who = ((EditText) view.findViewById(R.id.who)).getText().toString();
                    mManager.send(new AdminKickOfflineBean(who));
                })
                .show();
    }

    private void log(final String log) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            LogBean logBean = new LogBean(System.currentTimeMillis(), log);
            int separatorIndex = log.indexOf('@');
            if (separatorIndex > 0) {
                logBean.mWho = log.substring(0, separatorIndex);
            }
            mReceLogAdapter.prepend(logBean);
        } else {
            String threadName = Thread.currentThread().getName();
            mainHandler.post(() -> log(threadName + " (background): " + log));
        }
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
