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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import youten.redo.ble.util.BleUtil;
import youten.redo.ble.util.BleUuid;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * BLEデバイスへのconnect・Service
 * Discoveryを実施し、Characteristicsのread/writeをハンドリングするActivity
 */
public class DeviceActivity extends Activity implements View.OnClickListener {
	private static final String TAG = "BLEDevice";

	public static final String EXTRA_BLUETOOTH_DEVICE = "BT_DEVICE";
	private BluetoothAdapter mBTAdapter;
	private BluetoothDevice mDevice;
	private BluetoothGatt mConnGatt;
	private int mStatus;

	private Button mReadManufacturerNameButton;
	private Button mReadSerialNumberButton;
	private Button mWriteAlertLevelButton;
	private EditText mEtApName;
	private EditText mEtApPassword;
	private TextView mTvNotify;

	private final BluetoothGattCallback mGattcallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			Log.d(TAG, "onConnectionStateChange: " + newState);
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				mStatus = newState;
				mConnGatt.discoverServices();
				Log.d(TAG, "STATE_CONNECTED");
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				mStatus = newState;
				runOnUiThread(new Runnable() {
					public void run() {
						mReadManufacturerNameButton.setEnabled(false);
						mReadSerialNumberButton.setEnabled(false);
						mWriteAlertLevelButton.setEnabled(false);
					};
				});
			}
		};

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			Log.d(TAG, "onServicesDiscovered: "+status);
			for (BluetoothGattService service : gatt.getServices()) {
				if ((service == null) || (service.getUuid() == null)) {
					continue;
				}
				Log.d(TAG, service.getUuid().toString());

				if (BleUuid.SERVICE_DEVICE_INFORMATION.equalsIgnoreCase(service.getUuid().toString())) {
					BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING));
					mReadManufacturerNameButton.setTag(characteristic);
					mReadSerialNumberButton.setTag(characteristic);
					boolean ret = mConnGatt.setCharacteristicNotification(characteristic,true);
					Log.d(TAG, "set Notification: "+ret);
					if(ret){
						List<BluetoothGattDescriptor> descriptorList = characteristic.getDescriptors();
						if (descriptorList != null && descriptorList.size() > 0) {
							for (BluetoothGattDescriptor descriptor : descriptorList) {
								descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
								mConnGatt.writeDescriptor(descriptor);
							}
						}
					}
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {	e.printStackTrace();}
						Log.d(TAG, "requestMtu(200) ");
						mConnGatt.requestMtu(200);
					}
					runOnUiThread(new Runnable() {
						public void run() {
							mReadManufacturerNameButton.setEnabled(true);
							mReadSerialNumberButton.setEnabled(true);
						};
					});
				}
			}

			runOnUiThread(new Runnable() {
				public void run() {
					setProgressBarIndeterminateVisibility(false);
				};
			});
		};

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			Log.d(TAG, "onCharacteristicRead: "+status);
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (BleUuid.CHAR_MANUFACTURER_NAME_STRING
						.equalsIgnoreCase(characteristic.getUuid().toString())) {
					final String name = characteristic.getStringValue(0);
					Log.d(TAG, "== onCharacteristicRead: "+name+":"+characteristic.getUuid().toString());

					runOnUiThread(new Runnable() {
						public void run() {
							mReadManufacturerNameButton.setText(name);
							setProgressBarIndeterminateVisibility(false);
						};
					});
				}

			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {

			runOnUiThread(new Runnable() {
				public void run() {
					setProgressBarIndeterminateVisibility(false);
				};
			});
		};

		@Override	//CharacteristicNotification
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicChanged(gatt, characteristic);
			//Log.d(TAG, "onCharacteristicChanged: ");
			byte[] mValue = characteristic.getValue();
//			String str = "\n"+new String(mValue);
			String newMsg = new String(mValue);
			if(newMsg.contains("save ap config")){
				//Log.d(TAG, "==set ap success!: ");
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(DeviceActivity.this, "set ap success!", Toast.LENGTH_LONG).show();
						mReadSerialNumberButton.setBackgroundColor(Color.parseColor("#0080C0"));
						mReadSerialNumberButton.setText("set ap success");

						SharedPreferences sp = getSharedPreferences("ap",MODE_PRIVATE);
						SharedPreferences.Editor editor = sp.edit();
						editor.putString("name",mEtApName.getText().toString());
						editor.putString("password",mEtApPassword.getText().toString());
						editor.commit();
					}
				});
			}
			String str =newMsg + "\n" + mTvNotify.getText().toString() ;
			mTvNotify.setText(str);
			//mTvNotify.append(str);

//			Log.d(TAG, str);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_device);

		// state
		mStatus = BluetoothProfile.STATE_DISCONNECTED;
		mReadManufacturerNameButton = (Button) findViewById(R.id.read_manufacturer_name_button);
		mReadManufacturerNameButton.setOnClickListener(this);
		mReadSerialNumberButton = (Button) findViewById(R.id.read_serial_number_button);
		mReadSerialNumberButton.setOnClickListener(this);
		mWriteAlertLevelButton = (Button) findViewById(R.id.write_alert_level_button);
		mWriteAlertLevelButton.setOnClickListener(this);
		mEtApName = (EditText)findViewById(R.id.etApName);
		mEtApPassword = (EditText)findViewById(R.id.etApPassword);
		mTvNotify = (TextView) findViewById(R.id.tvNotify);
		mTvNotify.setMovementMethod(ScrollingMovementMethod.getInstance());
		mReadManufacturerNameButton.requestFocus();

		SharedPreferences sp = getSharedPreferences("ap",MODE_PRIVATE);
		mEtApName.setText(sp.getString("name",""));
		mEtApPassword.setText(sp.getString("password",""));
	}

	@Override
	protected void onResume() {
		super.onResume();

		init();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mConnGatt != null) {
			if ((mStatus != BluetoothProfile.STATE_DISCONNECTING)
					&& (mStatus != BluetoothProfile.STATE_DISCONNECTED)) {
				mConnGatt.disconnect();
			}
			mConnGatt.close();
			mConnGatt = null;
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.read_manufacturer_name_button) {
			if ((v.getTag() != null)
					&& (v.getTag() instanceof BluetoothGattCharacteristic)) {
				BluetoothGattCharacteristic ch = (BluetoothGattCharacteristic) v.getTag();
				{
					ch.setValue("info");
					Log.d(TAG, "onClick: write "+ ch.getUuid());
					if (mConnGatt.writeCharacteristic(ch)) {
//				if (mConnGatt.readCharacteristic(ch)) {
						setProgressBarIndeterminateVisibility(true);
					}
				}
			}
		} else if (v.getId() == R.id.read_serial_number_button) {
			if ((v.getTag() != null) && (v.getTag() instanceof BluetoothGattCharacteristic)) {
				((Button)v).setText("set ap");
				((Button)v).setBackgroundColor(Color.parseColor("#D6D6D6"));
				BluetoothGattCharacteristic ch = (BluetoothGattCharacteristic) mReadManufacturerNameButton.getTag();
				if(mEtApName.length() < 1 || mEtApPassword.length() < 1){
					Toast.makeText(this, "AP name  & password cannot null", Toast.LENGTH_SHORT).show();
					return;
				}
				if(mEtApName.getText().toString().contains(":") || mEtApName.getText().toString().contains(":")){
					Toast.makeText(this, "Cannot contain the ':' ", Toast.LENGTH_SHORT).show();
					return;
				}
				ch.setValue("AP:"+mEtApName.getText().toString().trim()+":" + mEtApPassword.getText().toString().trim());
				//ch.setValue("AP:123:456");
				if (mConnGatt.writeCharacteristic(ch)) {
					setProgressBarIndeterminateVisibility(true);
				}
			}

		} else if (v.getId() == R.id.write_alert_level_button) {
			if ((v.getTag() != null)
					&& (v.getTag() instanceof BluetoothGattCharacteristic)) {
				BluetoothGattCharacteristic ch = (BluetoothGattCharacteristic) v
						.getTag();
				ch.setValue(new byte[] { (byte) 0x03 });
				if (mConnGatt.writeCharacteristic(ch)) {
					setProgressBarIndeterminateVisibility(true);
				}
			}
		}
	}

	private void init() {
		// BLE check
		if (!BleUtil.isBLESupported(this)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT)
					.show();
			finish();
			return;
		}

		// BT check
		BluetoothManager manager = BleUtil.getManager(this);
		if (manager != null) {
			mBTAdapter = manager.getAdapter();
		}
		if (mBTAdapter == null) {
			Toast.makeText(this, R.string.bt_unavailable, Toast.LENGTH_SHORT)
					.show();
			finish();
			return;
		}

		// check BluetoothDevice
		if (mDevice == null) {
			mDevice = getBTDeviceExtra();
			Log.d(TAG, mDevice.getName() + ":" + mDevice.getAddress());
			if (mDevice == null) {
				finish();
				return;
			}
		}

		// button disable
		mReadManufacturerNameButton.setEnabled(false);
		mReadSerialNumberButton.setEnabled(false);
		mWriteAlertLevelButton.setEnabled(false);

		// connect to Gatt
		if ((mConnGatt == null)
				&& (mStatus == BluetoothProfile.STATE_DISCONNECTED)) {
			// try to connect
			mConnGatt = mDevice.connectGatt(this, false, mGattcallback);
			mStatus = BluetoothProfile.STATE_CONNECTING;
		} else {
			if (mConnGatt != null) {
				// re-connect and re-discover Services
				mConnGatt.connect();
				mConnGatt.discoverServices();
			} else {
				Log.e(TAG, "state error");
				finish();
				return;
			}
		}
		setProgressBarIndeterminateVisibility(true);
	}

	private BluetoothDevice getBTDeviceExtra() {
		Intent intent = getIntent();
		if (intent == null) {
			return null;
		}

		Bundle extras = intent.getExtras();
		if (extras == null) {
			return null;
		}

		return extras.getParcelable(EXTRA_BLUETOOTH_DEVICE);
	}

}
