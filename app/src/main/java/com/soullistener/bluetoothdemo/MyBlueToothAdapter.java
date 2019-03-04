package com.soullistener.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class MyBlueToothAdapter extends RecyclerView.Adapter<MyBlueToothAdapter.BlueToothViewHolder> {
    private Context context;
    private ArrayList<BluetoothDevice> mBluelist;

    public MyBlueToothAdapter(Context context, ArrayList arrayList) {
        this.mBluelist = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public BlueToothViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.listitem_devicescan, null);
        BlueToothViewHolder blueToothViewHolder = new BlueToothViewHolder(view);

        return blueToothViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull BlueToothViewHolder viewHolder, int position) {
        BluetoothDevice blueDevice = mBluelist.get(position);
        //设备名称
        String deviceName = blueDevice.getName();

        //设备的蓝牙地（地址为17位，都为大写字母-该项貌似不可能为空）
        viewHolder.deviceName.setText(TextUtils.isEmpty(deviceName) ? "未知设备" : deviceName);
        String deviceAddress = blueDevice.getAddress();
        viewHolder.deviceAddress.setText(deviceAddress);

        //设备的蓝牙设备类型（DEVICE_TYPE_CLASSIC 传统蓝牙 常量值：1, DEVICE_TYPE_LE  低功耗蓝牙 常量值：2
        // DEVICE_TYPE_DUAL 双模蓝牙 常量值：3. DEVICE_TYPE_UNKNOWN：未知 常量值：0）
        int deviceType = blueDevice.getType();
        if (deviceType == 0) {
            viewHolder.deviceType.setText("未知类型");
        } else if (deviceType == 1) {
            viewHolder.deviceType.setText("传统蓝牙");
        } else if (deviceType == 2) {
            viewHolder.deviceType.setText("低功耗蓝牙");
        } else if (deviceType == 3) {
            viewHolder.deviceType.setText("双模蓝牙");
        }
        //设备的状态（BOND_BONDED：已绑定 常量值：12, BOND_BONDING：绑定中 常量值：11, BOND_NONE：未匹配 常量值：10）
        int deviceState = blueDevice.getBondState();
        if (deviceState == 10) {
            viewHolder.deviceState.setText("未匹配");
        } else if (deviceState == 11) {
            viewHolder.deviceState.setText("绑定中");
        } else if (deviceState == 12) {
            viewHolder.deviceState.setText("已绑定");
        }

        viewHolder.bluetooth.setOnClickListener(view -> {
            itemClickListener.onItemClick(view, position);
        });
    }

    @Override
    public int getItemCount() {
        return mBluelist == null ? 0 : mBluelist.size();
    }

    private ItemClickListener itemClickListener;

    public void setOnItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    public class BlueToothViewHolder extends RecyclerView.ViewHolder {
        LinearLayout bluetooth;
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceType;
        TextView deviceState;

        public BlueToothViewHolder(View itemView) {
            super(itemView);
            bluetooth = itemView.findViewById(R.id.ll_bluthtooth);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceAddress = itemView.findViewById(R.id.device_address);
            deviceType = itemView.findViewById(R.id.device_type);
            deviceState = itemView.findViewById(R.id.device_state);
        }
    }
}
    
