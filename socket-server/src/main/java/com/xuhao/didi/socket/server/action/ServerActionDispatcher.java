package com.xuhao.didi.socket.server.action;

import com.xuhao.didi.core.iocore.interfaces.IStateSender;
import com.xuhao.didi.core.utils.SLog;
import com.xuhao.didi.socket.common.interfaces.basic.AbsLoopThread;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.dispatcher.IRegister;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClient;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClientPool;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IServerActionListener;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IServerManager;
import com.xuhao.didi.socket.server.impl.OkServerOptions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static com.xuhao.didi.socket.server.action.IAction.Server.ACTION_CLIENT_CONNECTED;
import static com.xuhao.didi.socket.server.action.IAction.Server.ACTION_CLIENT_DISCONNECTED;
import static com.xuhao.didi.socket.server.action.IAction.Server.ACTION_SERVER_ALLREADY_SHUTDOWN;
import static com.xuhao.didi.socket.server.action.IAction.Server.ACTION_SERVER_LISTENING;
import static com.xuhao.didi.socket.server.action.IAction.Server.ACTION_SERVER_LISTEN_FAILED;
import static com.xuhao.didi.socket.server.action.IAction.Server.ACTION_SERVER_WILL_BE_SHUTDOWN;

/**
 * 服务器状态机
 * Created by didi on 2018/4/19.
 */
public class ServerActionDispatcher implements IRegister<IServerActionListener, IServerManager>, IStateSender {
    private static final int DEFAULT_ACTION_QUEUE_CAPACITY = 1024;
    private static final DispatchThread HANDLE_THREAD = new DispatchThread();
    private static final LinkedBlockingQueue<ActionBean> ACTION_QUEUE =
            new LinkedBlockingQueue<>(DEFAULT_ACTION_QUEUE_CAPACITY);

    static {
        HANDLE_THREAD.start();
    }

    private volatile List<IServerActionListener> mResponseHandlerList = new ArrayList<>();
    private volatile int mServerPort;
    private volatile IClientPool<IClient, String> mClientPool;
    private volatile IServerManager<OkServerOptions> mServerManager;

    public ServerActionDispatcher(IServerManager<OkServerOptions> manager) {
        this.mServerManager = manager;
    }

    public void setServerPort(int localPort) {
        mServerPort = localPort;
    }

    public void setClientPool(IClientPool<IClient, String> clientPool) {
        mClientPool = clientPool;
    }

    @Override
    public IServerManager<OkServerOptions> registerReceiver(final IServerActionListener socketResponseHandler) {
        if (socketResponseHandler != null) {
            synchronized (mResponseHandlerList) {
                if (!mResponseHandlerList.contains(socketResponseHandler)) {
                    mResponseHandlerList.add(socketResponseHandler);
                }
            }
        }
        return mServerManager;
    }

    @Override
    public IServerManager<OkServerOptions> unRegisterReceiver(IServerActionListener socketResponseHandler) {
        synchronized (mResponseHandlerList) {
            mResponseHandlerList.remove(socketResponseHandler);
        }
        return mServerManager;
    }

    private void dispatchActionToListener(String action, Object arg, IServerActionListener responseHandler) {
        switch (action) {
            case ACTION_SERVER_LISTENING: {
                try {
                    responseHandler.onServerListening(mServerPort);
                } catch (Exception e) {
                    SLog.e("Server listening callback failed", e);
                }
                break;
            }
            case ACTION_SERVER_LISTEN_FAILED: {
                try {
                    responseHandler.onServerListenFailed(mServerPort, (Throwable) arg);
                } catch (Exception e) {
                    SLog.e("Server listen failed callback failed", e);
                }
                break;
            }
            case ACTION_CLIENT_CONNECTED: {
                try {
                    IClient client = (IClient) arg;
                    responseHandler.onClientConnected(client, mServerPort, mClientPool);
                } catch (Exception e) {
                    SLog.e("Client connected callback failed", e);
                }
                break;
            }
            case ACTION_CLIENT_DISCONNECTED: {
                try {
                    IClient client = (IClient) arg;
                    responseHandler.onClientDisconnected(client, mServerPort, mClientPool);
                } catch (Exception e) {
                    SLog.e("Client disconnected callback failed", e);
                }
                break;
            }
            case ACTION_SERVER_WILL_BE_SHUTDOWN: {
                try {
                    Throwable throwable = (Throwable) arg;
                    responseHandler.onServerWillBeShutdown(mServerPort, mServerManager, mClientPool, throwable);
                } catch (Exception e) {
                    SLog.e("Server shutdown callback failed", e);
                }
                break;
            }
            case ACTION_SERVER_ALLREADY_SHUTDOWN: {
                try {
                    responseHandler.onServerAlreadyShutdown(mServerPort);
                } catch (Exception e) {
                    SLog.e("Server already shutdown callback failed", e);
                }
                break;
            }
        }
    }

    @Override
    public void sendBroadcast(String action, Serializable serializable) {
        enqueueAction(new ActionBean(action, serializable, this));
    }

    @Override
    public void sendBroadcast(String action) {
        sendBroadcast(action, null);
    }

    private static void enqueueAction(ActionBean bean) {
        try {
            ACTION_QUEUE.put(bean);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (bean != null && bean.mDispatcher != null) {
                bean.mDispatcher.dispatch(bean);
            }
        }
    }

    private void dispatch(ActionBean actionBean) {
        synchronized (mResponseHandlerList) {
            List<IServerActionListener> list = new ArrayList<>(mResponseHandlerList);
            Iterator<IServerActionListener> it = list.iterator();
            while (it.hasNext()) {
                IServerActionListener listener = it.next();
                dispatchActionToListener(actionBean.mAction, actionBean.arg, listener);
            }
        }
    }

    protected static class ActionBean {
        ActionBean(String action, Serializable arg, ServerActionDispatcher dispatcher) {
            mAction = action;
            this.arg = arg;
            mDispatcher = dispatcher;
        }

        String mAction = "";
        Serializable arg;
        ServerActionDispatcher mDispatcher;
    }

    private static class DispatchThread extends AbsLoopThread {
        DispatchThread() {
            super("server_action_dispatch_thread");
        }

        @Override
        protected void runInLoopThread() throws Exception {
            ActionBean actionBean = ACTION_QUEUE.take();
            if (actionBean != null && actionBean.mDispatcher != null) {
                actionBean.mDispatcher.dispatch(actionBean);
            }
        }

        @Override
        protected void loopFinish(Exception e) {
        }
    }
}
