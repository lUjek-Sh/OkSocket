package com.xuhao.didi.oksocket.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.xuhao.didi.oksocket.R;
import com.xuhao.didi.oksocket.data.LogBean;

import java.util.ArrayList;
import java.util.List;

public final class LogAdapter extends RecyclerView.Adapter<LogAdapter.ItemHolder> {

    private final List<LogBean> mDataList = new ArrayList<>();

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.log_item, parent, false);
        return new ItemHolder(view);
    }

    @Override
    public void onBindViewHolder(final ItemHolder holder, int position) {
        LogBean bean = mDataList.get(position);
        holder.mTime.setText(bean.mTime);
        holder.mLog.setText(bean.mLog);
        holder.itemView.setOnLongClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return false;
            }
            LogBean log = mDataList.get(adapterPosition);
            String copiedValue = (log.mWho == null || log.mWho.isEmpty()) ? log.mLog : log.mWho;
            ClipboardManager clipboardManager =
                    (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboardManager != null) {
                clipboardManager.setPrimaryClip(ClipData.newPlainText(null, copiedValue));
                Toast.makeText(v.getContext(), R.string.copied_to_clipboard, Toast.LENGTH_LONG).show();
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    public void prepend(LogBean logBean) {
        mDataList.add(0, logBean);
        notifyItemInserted(0);
    }

    public void clearLogs() {
        int itemCount = mDataList.size();
        if (itemCount == 0) {
            return;
        }
        mDataList.clear();
        notifyItemRangeRemoved(0, itemCount);
    }

    public static final class ItemHolder extends RecyclerView.ViewHolder {
        private final TextView mTime;
        private final TextView mLog;

        ItemHolder(View itemView) {
            super(itemView);
            mTime = itemView.findViewById(R.id.time);
            mLog = itemView.findViewById(R.id.logtext);
        }
    }
}
