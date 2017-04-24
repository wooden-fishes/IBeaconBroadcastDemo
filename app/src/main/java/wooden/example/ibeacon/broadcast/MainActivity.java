package wooden.example.ibeacon.broadcast;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private BluetoothAdapter mBTAdapter;
    private BluetoothLeAdvertiser mBTAdvertiser;
    private static final int REQUEST_ENABLE_BT = 1;
    private EditText etUUID;
    private EditText etMajor;
    private EditText etMinor;
    private Switch switchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        etUUID = (EditText) findViewById(R.id.et_uuid);
        etMajor = (EditText) findViewById(R.id.et_major);
        etMinor = (EditText) findViewById(R.id.et_minor);
        switchView = (Switch) findViewById(R.id.switch_view);
        switchView.setOnCheckedChangeListener(this);

        setupBLE();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void setupBLE() {
        if (!isBLESupported(this)) {
            Toast.makeText(this, "device not support ble", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        BluetoothManager manager = getManager(this);
        if (manager != null) {
            mBTAdapter = manager.getAdapter();
        }
        if ((mBTAdapter == null) || (!mBTAdapter.isEnabled())) {
            Toast.makeText(this, "bluetooth not open", Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAdvertise();
        switchView.setChecked(false);
    }

    private void startAdvertise(String uuid, int major, int minor) {
        if (mBTAdapter == null) {
            return;
        }
        if (mBTAdvertiser == null) {
            mBTAdvertiser = mBTAdapter.getBluetoothLeAdvertiser();
        }
        mBTAdapter.setName("Test");
        if (mBTAdvertiser != null) {
            mBTAdvertiser.startAdvertising(
                    createAdvertiseSettings(true, 0),
                    createAdvertiseData(
                            UUID.fromString(uuid),
                            major, minor, (byte) 0xc5),
                    mAdvCallback);
        }
    }


    private void stopAdvertise() {
        if (mBTAdvertiser != null) {
            mBTAdvertiser.stopAdvertising(mAdvCallback);
            mBTAdvertiser = null;
            switchView.setChecked(false);
        }
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                return;
            }
        }
        finish();
    }

    public static boolean isBLESupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static BluetoothManager getManager(Context context) {
        return (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    public AdvertiseSettings createAdvertiseSettings(boolean connectable, int timeoutMillis) {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        builder.setConnectable(connectable);
        builder.setTimeout(timeoutMillis);
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        return builder.build();
    }


    public AdvertiseData createAdvertiseData(UUID proximityUuid, int major,
                                             int minor, byte txPower) {
        if (proximityUuid == null) {
            throw new IllegalArgumentException("proximitiUuid null");
        }
        byte[] manufacturerData = new byte[23];
        ByteBuffer bb = ByteBuffer.wrap(manufacturerData);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 0x02);
        bb.put((byte) 0x15);
        bb.putLong(proximityUuid.getMostSignificantBits());
        bb.putLong(proximityUuid.getLeastSignificantBits());
        bb.putShort((short) major);
        bb.putShort((short) minor);
        bb.put(txPower);

        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        builder.addManufacturerData(0x004c, manufacturerData);
        AdvertiseData adv = builder.build();
        return adv;
    }

    private AdvertiseCallback mAdvCallback = new AdvertiseCallback() {
        public void onStartSuccess(android.bluetooth.le.AdvertiseSettings settingsInEffect) {
            if (settingsInEffect != null) {
                Log.d("debug", "onStartSuccess TxPowerLv="
                        + settingsInEffect.getTxPowerLevel()
                        + " mode=" + settingsInEffect.getMode()
                        + " timeout=" + settingsInEffect.getTimeout());
            } else {
                Log.d("debug", "onStartSuccess, settingInEffect is null");
            }
            switchView.setChecked(true);
            setProgressBarIndeterminateVisibility(false);
        }

        public void onStartFailure(int errorCode) {
            Log.d("debug", "onStartFailure errorCode=" + errorCode);
            switchView.setChecked(false);
        }
    };


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            String uuid = etUUID.getText().toString().trim();
            int major = 0;
            if (!TextUtils.isEmpty(etMajor.getText().toString())) {
                major = Integer.parseInt(etMajor.getText().toString());
            }

            int minor = 0;
            if (!TextUtils.isEmpty(etMinor.getText().toString())) {
                minor = Integer.parseInt(etMinor.getText().toString());
            }

            if (major < 0 || major > 65535) {
                Toast.makeText(this, "major ranges：0- 65535", Toast.LENGTH_LONG).show();
                switchView.setChecked(false);
                return;
            }

            if (minor < 0 || minor > 65535) {
                Toast.makeText(this, "major ranges：0- 65535", Toast.LENGTH_LONG).show();
                switchView.setChecked(false);
                return;
            }

            if (TextUtils.isEmpty(uuid) || major == 0 || minor == 0) {
                Toast.makeText(this, "please enter params", Toast.LENGTH_LONG).show();
                switchView.setChecked(false);
                return;
            }
            startAdvertise(uuid, major, minor);
        } else {
            stopAdvertise();
        }
    }
}
