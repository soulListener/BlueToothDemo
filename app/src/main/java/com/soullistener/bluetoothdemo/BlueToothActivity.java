package com.soullistener.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

/**
 * @author kuan
 * Created on 2019/3/1.
 * @description
 */
public class BlueToothActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArrayList<BluetoothDevice> mBluelist;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final int SEARCH_CODE = 0;
    private BlueToothPairtReceiver pairDeviceReceiver;
    private MyBlueToothAdapter myBlueToothAdapter;
    private BluetoothScanReceiver scanReceiver;
    private BluetoothGatt bluetoothGatt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devicescan);

        findViewById(R.id.btn_startscan).setOnClickListener(view ->{
            initBlueTooth();
        });

        findViewById(R.id.btn_stopscan).setOnClickListener(view ->{
            //配对之前把扫描关闭
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
                initRecycleView();
            }
        });

        recyclerView = findViewById(R.id.rv_bluetooth);
        initRecycleView();
    }

    /**
     * 初始化蓝牙设备
     */
    private void initBlueTooth() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
        }
        // 判断是否打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            // 弹出对话框提示用户是后打开
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            Toast.makeText(this, "正在搜索", Toast.LENGTH_SHORT).show();
            startActivityForResult(intent, SEARCH_CODE);
        } else {
            // 打开蓝牙
            Toast.makeText(this, "正在打开连接", Toast.LENGTH_SHORT).show();
            mBluetoothAdapter.enable();
        }
        startDiscovery();

    }


    private void startDiscovery() {

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }


        //开始扫描
        mBluetoothAdapter.startDiscovery();
        scanReceiver = new BluetoothScanReceiver();

//        mBluetoothAdapter.getBluetoothLeScanner().startScan(new ScanCallback() {
//            @Override
//            public void onScanResult(int callbackType, ScanResult result) {
//                mBluelist.add(result.getDevice());
//                myBlueToothAdapter.notifyDataSetChanged();
//            }
//        });

        IntentFilter startFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(scanReceiver, startFilter);
        //扫描到蓝牙
        IntentFilter foundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(scanReceiver, foundFilter);
        //蓝牙扫描结束广播
        IntentFilter finishFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(scanReceiver, finishFilter);

        //配对请求广播
        IntentFilter pairingRequestFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(pairDeviceReceiver, pairingRequestFilter);
        //配对状态广播
        IntentFilter stateChangeFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(pairDeviceReceiver, stateChangeFilter);
    }

    public void initRecycleView() {
        mBluelist = new ArrayList<>();
        myBlueToothAdapter = new MyBlueToothAdapter(BlueToothActivity.this, mBluelist);
        myBlueToothAdapter.setOnItemClickListener((view, position) -> {
            pairDeviceReceiver = new BlueToothPairtReceiver();
            connectBlueToothDevice(mBluelist.get(position));
        });
        recyclerView.addItemDecoration(new DividerItemDecoration(BlueToothActivity.this, DividerItemDecoration.VERTICAL));
        recyclerView.setLayoutManager(new LinearLayoutManager(BlueToothActivity.this));
        recyclerView.setAdapter(myBlueToothAdapter);
    }


    /**
     * 配对（配对成功与失败通过广播返回）
     *
     * @param device
     */
    public void connectBlueToothDevice(BluetoothDevice device) {
        if (device == null) {
            Log.e("connectBlueToothDevice", "bond device null");
            return;
        }
        //配对之前把扫描关闭
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        //判断设备是否配对，没有配对在配，配对了就不需要配了
        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            Log.d("connectBlueToothDevice", "attemp to bond:" + device.getName());
            try {
                Method createBondMethod = device.getClass().getMethod("createBond");
                Boolean returnValue = (Boolean) createBondMethod.invoke(device);
                returnValue.booleanValue();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("connectBlueToothDevice", "attemp to bond fail!");
            }
        } else {
            startConnection(device);
        }
    }

    /**
     * 开始通讯
     */
    private void startConnection(BluetoothDevice device) {
        Log.e("startConnection","开始连接");
        bluetoothGatt = device.connectGatt(this, false,bluetoothGattCallback);
    }


    /**
     * 蓝牙连接广播
     */
    public class BluetoothScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mBluelist.add(device);
                    myBlueToothAdapter.notifyDataSetChanged();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.e("BluetoothScanReceiver", "结束扫描");
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.e("BluetoothScanReceiver", "开始扫描");
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 蓝牙配对广播
     */
    public class BlueToothPairtReceiver extends BroadcastReceiver {

        private String pin = "0000";

        public BlueToothPairtReceiver(String pin) {
            this.pin = pin;
        }

        public BlueToothPairtReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("onReceive", "action:" + action);
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            switch (action) {
                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    try {
                        //1.确认配对
                        Method setPairingConfirmation = device.getClass().getDeclaredMethod("setPairingConfirmation", boolean.class);
                        setPairingConfirmation.invoke(device, true);
                        //2.终止有序广播
                        Log.d("order...", "isOrderedBroadcast:" + ",isInitialStickyBroadcast:" + isInitialStickyBroadcast());
                        abortBroadcast();//如果没有将广播终止，则会出现一个一闪而过的配对框。
                        //3.调用setPin方法进行配对
//                        Method removeBondMethod = device.getClass().getDeclaredMethod("setPin", new Class[]{byte[].class});
//                        Boolean returnValue = (Boolean) removeBondMethod.invoke(device, new Object[]{pin.getBytes()});
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    switch (device.getBondState()) {
                        case BluetoothDevice.BOND_NONE:
                            Log.d("getBondState", "取消配对");
                            break;
                        case BluetoothDevice.BOND_BONDING:
                            Log.d("getBondState", "配对中");
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            Log.d("getBondState", "配对成功");
                            startConnection(device);
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanReceiver != null) {
            //取消注册,防止内存泄露
            unregisterReceiver(scanReceiver);
        }
    }

    /**
     * 低功耗蓝牙连接回调
     */
    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "onConnectionStateChange  status=" + status + "  newState=" + newState);
            if (status == 0) {
                if (newState == 2) {
                    Log.i("onConnectionStateChange", "Connected to GATT server.");
                    Log.i("onConnectionStateChange", "Attempting to start service discovery:" + bluetoothGatt.discoverServices());
                } else if (newState == 0) {
                    Log.i("onConnectionStateChange", "Disconnected from GATT server.");
                }
            } else {

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == 0){
                Log.e("onServicesDiscovered","连接成功");
            }
            BluetoothGattService bgs = gatt.getService(UUID.fromString("0000fee9-0000-1000-8000-00000000000"));
            BluetoothGattCharacteristic mBleWriter = bgs.getCharacteristic(UUID.fromString("d44bc400-abfd-45a2-b575-000000000000"));
            BluetoothGattCharacteristic mBleReader = bgs.getCharacteristic(UUID.fromString("d44bc400-abfd-45a2-b575-000000000001"));
            Log.d("mBleWriter", "mBleWriter: type " + mBleWriter.getWriteType() + ", " + mBleWriter.getDescriptors().size());
            Log.d("mBleReader", "mBleReader: type " + mBleReader.getWriteType() + ", " + mBleReader.getDescriptors().size());

            mBleWriter.setValue("1111".getBytes());
            bluetoothGatt.writeCharacteristic(mBleWriter);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d("BluetoothGattCallback", characteristic.getStringValue(0));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d("BluetoothGattCallback", "onCharacteristicWrite");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d("BluetoothGattCallback", "onCharacteristicChanged");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d("onDescriptorRead", "onDescriptorRead received: " + status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d("onDescriptorWrite", "onDescriptorWrite received: " + status);
        }
    };

    /**
     * 取消配对（取消配对成功与失败通过广播返回 也就是配对失败）
     *
     * @param device
     */
    public void cancelConnectBlueTooth(BluetoothDevice device) {
        if (device == null) {
            Log.d("cancelConnectBlueTooth", "cancel bond device null");
            return;
        }

        //判断设备是否配对，没有配对就不用取消了
        if (device.getBondState() != BluetoothDevice.BOND_NONE) {
            Log.d("cancelConnectBlueTooth", "attemp to cancel bond:" + device.getName());
            try {
                Method removeBondMethod = device.getClass().getMethod("removeBond");
                Boolean returnValue = (Boolean) removeBondMethod.invoke(device);
                returnValue.booleanValue();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("cancelConnectBlueTooth", "attemp to cancel bond fail!");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SEARCH_CODE) {
            startDiscovery();
        }
    }
}
