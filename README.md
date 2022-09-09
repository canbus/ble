# 实现的功能
1.实现 ble 扫描
2.连接特定的Characteristic
3.自动使能notify,并修改mtu为200
4.写Characteristic

# BLE 操作步骤
1. 获得蓝牙适配器
```
  BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
  BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
```
2. 判断是否支持BLE
``` java
context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
```
3. 判断蓝牙是否打开
```
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            binding.button.setEnabled(false);
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
```
4. 请求定位权限,android12 改成:BLUETOOTH_SCAN、BLUETOOTH_ADVERTISE 和 BLUETOOTH_CONNECT 
```
    if(checkCallingOrSelfPermission(permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{permission.ACCESS_FINE_LOCATION},110);//请求的结果在onRequestPermissionsResult()
            return;
        }
```
5. 扫描BLE设备
```
//1. Android 4.3以上，Android 5.0以下
mBluetoothAdapter.startLeScan(BluetoothAdapter.LeScanCallback LeScanCallback)
//2. Android 5.0以上，扫描的结果在mScanCallback中进行处理
mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
mBluetoothLeScanner.startScan(ScanCallback mScanCallback);
```
5. 获得扫描结果
```
     ScanCallback mScanCallback = new ScanCallback() {
         @Override
         public void onScanResult(int callbackType, ScanResult result) {

             Log.d(TAG, "onScanResult: " + result.getDevice().getName());
             super.onScanResult(callbackType, result);
         }
```
5. 对目标设备进行连接
```
result.getDevice().connectGatt(MainActivity.this, false, mGattCallback);
```
传入的BluetoothGattCallback对象中对连接结果做处理，以及通过GATT进行通信的绝大多数的操作都在这个对象中

5. 发现服务
```
   BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED){
                mConnGatt.discoverServices();
            }
            super.onConnectionStateChange(gatt, status, newState);
        }
```
6. 然后处理mGattCallback中的onServicesDiscovered回调,
   查找服务,再根据服务查找characteristic,拿到characteristic就可以通讯了
   mConnGatt.writeCharacteristic(mCharacteristic);//写值
```
@Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            for (BluetoothGattService service : gatt.getServices()) {
                if ((service == null) || (service.getUuid() == null)) { continue; }
                Log.d(TAG, service.getUuid().toString());
                //找到指定Service
                if(service.getUuid().toString().contains("000000ff-0000-1000-8000-00805f9b34fb")){
                    for (BluetoothGattCharacteristic characteristic:service.getCharacteristics()) {
                        Log.d(TAG,characteristic.getUuid().toString());
                        //找到指定characteristic
                        if(characteristic.getUuid().toString().contains("0000ff01-0000-1000-8000-00805f9b34fb")){
                            mCharacteristic = characteristic;
                            mCharacteristic.setValue("info"); //写入值
                            mConnGatt.writeCharacteristic(mCharacteristic);
                        }
                    }
                }
            }
        }
```   

7. 完整的demo代码
```
package com.bao.bledemo;

import android.Manifest;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.bao.bledemo.databinding.ActivityMain2Binding;
import com.bao.bledemo.databinding.ContentMainBinding;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bao.bledemo.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.security.Permission;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 100;
    private static final int REQUEST_ENABLE_BT_SCAN = 101;
    private static final String TAG = "BLEDemo";

    private AppBarConfiguration appBarConfiguration;
    //    private ActivityMainBinding binding;
    private ActivityMain2Binding binding;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothGatt mConnGatt;
    private BluetoothGattCharacteristic mCharacteristic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //binding = ActivityMainBinding.inflate(getLayoutInflater());
        binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            binding.button.setEnabled(false);
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if(checkCallingOrSelfPermission(permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED ||
                checkCallingOrSelfPermission(permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED ){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{permission.ACCESS_FINE_LOCATION,permission.ACCESS_COARSE_LOCATION},110);
            Log.d(TAG, "onCreate: requestPermissions");
            return;
        }


        binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ScanBle();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: "+requestCode);

        switch (requestCode) {
            case 110:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] ==PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted 授予权限
                    //处理授权之后逻辑
                    Log.d(TAG, "onRequestPermissionsResult: 授予权限");
                } else {
                    // Permission Denied 权限被拒绝
                    Log.d(TAG, "onRequestPermissionsResult: 权限被拒绝");
                    //ToastUtils.showShort(getActivity(),"权限被禁用");
                }

                break;
            default:
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }
    private void ScanBle() {
        Toast.makeText(MainActivity.this, "ScanBle", Toast.LENGTH_SHORT).show();
        
        //mBluetoothAdapter.startLeScan(leScanCallback); //Android 5.0以下
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeScanner.startScan(mScanCallback);
        
    }
    BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED){
                mConnGatt.discoverServices();
                Log.d(TAG,"discoverServices");
            }
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            for (BluetoothGattService service : gatt.getServices()) {
                if ((service == null) || (service.getUuid() == null)) { continue; }
                Log.d(TAG, service.getUuid().toString());
                //找到指定Service
                if(service.getUuid().toString().contains("000000ff-0000-1000-8000-00805f9b34fb")){
                    for (BluetoothGattCharacteristic characteristic:service.getCharacteristics()) {
                        Log.d(TAG,characteristic.getUuid().toString());
                        //找到指定characteristic
                        if(characteristic.getUuid().toString().contains("0000ff01-0000-1000-8000-00805f9b34fb")){
                            mCharacteristic = characteristic;
                            mCharacteristic.setValue("info"); //写入值
                            mConnGatt.writeCharacteristic(mCharacteristic);
                        }
                    }
                }
            }
        }
    };
    ScanCallback mScanCallback = new ScanCallback() {
         @Override
         public void onScanResult(int callbackType, ScanResult result) {
             String devName = result.getDevice().getName();
            if(devName != null && devName.length() > 0) {
                Log.d(TAG, "onScanResult: " + result.getDevice().getName());
                if (result.getDevice().getName().contains("E05_000")) {
                    mBluetoothLeScanner.stopScan(mScanCallback);
                    Log.d(TAG, "connectGatt");
                    mConnGatt = result.getDevice().connectGatt(MainActivity.this, false, mGattCallback);
                    Log.d(TAG, "mConnGatt: " + mConnGatt.connect());
                }
            }
             super.onScanResult(callbackType, result);
         }

         @Override
         public void onBatchScanResults(List<ScanResult> results) {
             super.onBatchScanResults(results);
         }

         @Override
         public void onScanFailed(int errorCode) {
             super.onScanFailed(errorCode);
         }
     };
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if( requestCode == REQUEST_ENABLE_BT && resultCode == -1){
            binding.button.setEnabled(true);
        }
    }
}
```   
#
移植自:https://github.com/youten/BLERW

