package com.xuhao.didi.oksocket.data;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class LogBean {
    public final String mTime;
    public final String mLog;
    public String mWho;

    public LogBean(long time, String log) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.US);
        mTime = format.format(time);
        mLog = log;
    }
}
