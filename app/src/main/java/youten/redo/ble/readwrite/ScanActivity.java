/*
 * Copyright (C) 2013 youten
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package youten.redo.ble.readwrite;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;
import youten.redo.ble.util.BleUtil;
import youten.redo.ble.util.ScannedDevice;
/**
 * BLEデバイスをスキャンし、一覧に表示するActivity。
 */
@RuntimePermissions
public class ScanActivity extends Activity implements BluetoothAdapter.LeScanCallback {
    private BluetoothAdapter mBTAdapter;
    private DeviceAdapter mDeviceAdapter;
    private boolean mIsScanning;
    private final String TAG = "BLEMsg";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_scan);

        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopScan();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mIsScanning) {
            menu.findItem(R.id.action_scan).setVisible(false);
            menu.findItem(R.id.action_stop).setVisible(true);
        } else {
            menu.findItem(R.id.action_scan).setVisible(true);
            menu.findItem(R.id.action_stop).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // ignore
            return true;
        } else if (itemId == R.id.action_scan) {
            ScanActivityPermissionsDispatcher.startScanWithPermissionCheck(this);

            return true;
        } else if (itemId == R.id.action_stop) {
            stopScan();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLeScan(final BluetoothDevice newDeivce, final int newRssi, final byte[] newScanRecord) {
        if(newDeivce.getName() != null && newDeivce.getName().contains("E05_")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDeviceAdapter.update(newDeivce, newRssi, newScanRecord);
                }
            });
        }
    }

    private void init() {
        // BLE check
        if (!BleUtil.isBLESupported(this)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // BT check
        BluetoothManager manager = BleUtil.getManager(this);
        if (manager != null) {
            mBTAdapter = manager.getAdapter();
        }
        if (mBTAdapter == null) {
            Toast.makeText(this, R.string.bt_unavailable, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!mBTAdapter.isEnabled()) {
            Toast.makeText(this, R.string.bt_disabled, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // init listview
        ListView deviceListView = (ListView) findViewById(R.id.list);
        mDeviceAdapter = new DeviceAdapter(this, R.layout.listitem_device,
                new ArrayList<ScannedDevice>());
        deviceListView.setAdapter(mDeviceAdapter);
        deviceListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {
                ScannedDevice item = mDeviceAdapter.getItem(position);
                if (item != null) {
                    stopScan();
                    Intent intent = new Intent(view.getContext(), DeviceActivity.class);
                    BluetoothDevice selectedDevice = item.getDevice();
                    Log.d(TAG, selectedDevice.getName() + ":" + selectedDevice.getAddress());
                    intent.putExtra(DeviceActivity.EXTRA_BLUETOOTH_DEVICE, selectedDevice);
                    startActivity(intent);

                    // stop before change Activity
                    stopScan();
                }
            }
        });

        stopScan();
    }

    @NeedsPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    void startScan() {
        if ((mBTAdapter != null) && (!mIsScanning)) {
            if(mBTAdapter.isDiscovering()){
                mBTAdapter.stopLeScan(this);
            }
            mBTAdapter.startLeScan(this);
            //扫描到后,会调用onLeScan()回调
            mIsScanning = true;
            setProgressBarIndeterminateVisibility(true);
            invalidateOptionsMenu();
        }
    }

    private void stopScan() {
        if (mBTAdapter != null) {
            mBTAdapter.stopLeScan(this);
        }
        mIsScanning = false;
        setProgressBarIndeterminateVisibility(false);
        invalidateOptionsMenu();
    }

}
