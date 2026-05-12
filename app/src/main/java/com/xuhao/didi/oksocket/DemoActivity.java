package com.xuhao.didi.oksocket;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.core.pojo.OriginalData;
import com.xuhao.didi.core.utils.SLog;
import com.xuhao.didi.oksocket.data.MsgDataBean;
import com.xuhao.didi.socket.client.sdk.OkSocket;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClient;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClientIOCallback;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClientPool;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IServerManager;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IServerShutdown;
import com.xuhao.didi.socket.server.action.ServerActionAdapter;
import com.xuhao.didi.socket.server.impl.OkServerOptions;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;

public class DemoActivity extends AppCompatActivity implements IClientIOCallback {
    private Button mServerBtn;
    private IServerManager mServerManager;
    private TextView mIPTv;
    private final int mPort = 8080;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
        Button mSimpleBtn = findViewById(R.id.btn1);
        Button mComplexBtn = findViewById(R.id.btn2);
        mServerBtn = findViewById(R.id.btn3);
        Button mAdminBtn = findViewById(R.id.admin);
        mIPTv = findViewById(R.id.ip);

        OkServerOptions.setIsDebug(true);
        OkSocketOptions.setIsDebug(true);
        SLog.setIsDebug(true);

        updateLocalIp();

        mSimpleBtn.setOnClickListener(v -> startActivity(new Intent(this, SimpleDemoActivity.class)));
        mComplexBtn.setOnClickListener(v -> startActivity(new Intent(this, ComplexDemoActivity.class)));
        mAdminBtn.setOnClickListener(v -> startActivity(new Intent(this, ServerAdminActivity.class)));

        mServerManager = OkSocket.server(mPort).registerReceiver(new ServerActionAdapter() {
            @Override
            public void onServerListening(int serverPort) {
                Log.i("ServerCallback", Thread.currentThread().getName() + " onServerListening, serverPort:" + serverPort);
                flushServerText();
            }

            @Override
            public void onClientConnected(IClient client, int serverPort, IClientPool clientPool) {
                Log.i("ServerCallback", Thread.currentThread().getName() + " onClientConnected, serverPort:" + serverPort + ", clients:" + clientPool.size() + ", clientTag:" + client.getUniqueTag());
                client.addIOCallback(DemoActivity.this);
            }

            @Override
            public void onClientDisconnected(IClient client, int serverPort, IClientPool clientPool) {
                Log.i("ServerCallback", Thread.currentThread().getName() + " onClientDisconnected, serverPort:" + serverPort + ", clients:" + clientPool.size() + ", clientTag:" + client.getUniqueTag());
                client.removeIOCallback(DemoActivity.this);
            }

            @Override
            public void onServerWillBeShutdown(int serverPort, IServerShutdown shutdown, IClientPool clientPool, Throwable throwable) {
                Log.i("ServerCallback", Thread.currentThread().getName() + " onServerWillBeShutdown, serverPort:" + serverPort + ", clients:" + clientPool.size());
                shutdown.shutdown();
            }

            @Override
            public void onServerAlreadyShutdown(int serverPort) {
                Log.i("ServerCallback", Thread.currentThread().getName() + " onServerAlreadyShutdown, serverPort:" + serverPort);
                flushServerText();
            }
        });

        mIPTv.setOnClickListener(v -> {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboardManager != null) {
                clipboardManager.setPrimaryClip(ClipData.newPlainText(getString(R.string.copy_ip), getIPAddress()));
                Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_LONG).show();
            }
        });

        mServerBtn.setOnClickListener(v -> {
            if (!mServerManager.isLive()) {
                mServerManager.listen();
            } else {
                mServerManager.shutdown();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        flushServerText();
        updateLocalIp();
    }

    @Override
    public void onClientRead(OriginalData originalData, IClient client, IClientPool<IClient, String> clientPool) {
        String str = new String(originalData.getBodyBytes(), StandardCharsets.UTF_8);
        try {
            JsonObject jsonObject = JsonParser.parseString(str).getAsJsonObject();
            int cmd = jsonObject.get("cmd").getAsInt();
            if (cmd == 54) {
                String handshake = jsonObject.get("handshake").getAsString();
                Log.i("onClientIOServer", Thread.currentThread().getName() + " received handshake from " + client.getHostIp() + ": " + handshake);
            } else if (cmd == 14) {
                Log.i("onClientIOServer", Thread.currentThread().getName() + " received heartbeat from " + client.getHostIp());
            } else {
                Log.i("onClientIOServer", Thread.currentThread().getName() + " received from " + client.getHostIp() + ": " + str);
            }
        } catch (Exception e) {
            Log.i("onClientIOServer", Thread.currentThread().getName() + " received from " + client.getHostIp() + ": " + str);
        }
        clientPool.sendToAll(new MsgDataBean(str));
    }

    @Override
    public void onClientWrite(ISendable sendable, IClient client, IClientPool<IClient, String> clientPool) {
        String str = decodeSendable(sendable);
        try {
            JsonObject jsonObject = JsonParser.parseString(str).getAsJsonObject();
            int cmd = jsonObject.get("cmd").getAsInt();
            if (cmd == 54) {
                String handshake = jsonObject.get("handshake").getAsString();
                Log.i("onClientIOServer", Thread.currentThread().getName() + " sent handshake to " + client.getHostIp() + ": " + handshake);
            } else {
                Log.i("onClientIOServer", Thread.currentThread().getName() + " sent to " + client.getHostIp() + ": " + str);
            }
        } catch (Exception e) {
            Log.i("onClientIOServer", Thread.currentThread().getName() + " sent to " + client.getHostIp() + ": " + str);
        }
    }

    private void updateLocalIp() {
        mIPTv.setText(getString(R.string.local_device_ip_prefix, getIPAddress()));
    }

    private void flushServerText() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            if (mServerManager != null && mServerManager.isLive()) {
                mServerBtn.setText(getString(R.string.server_stop, mPort));
            } else {
                mServerBtn.setText(getString(R.string.server_start, mPort));
            }
        });
    }

    private String decodeSendable(ISendable sendable) {
        byte[] packet = sendable.parse();
        if (packet.length <= 4) {
            return "";
        }
        byte[] body = Arrays.copyOfRange(packet, 4, packet.length);
        return new String(body, StandardCharsets.UTF_8);
    }

    public String getIPAddress() {
        try {
            for (Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                 networkInterfaces.hasMoreElements(); ) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                for (Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                     addresses.hasMoreElements(); ) {
                    InetAddress inetAddress = addresses.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            SLog.e("Failed to enumerate local IP addresses", e);
        }
        return getString(R.string.local_ip_unavailable);
    }
}
