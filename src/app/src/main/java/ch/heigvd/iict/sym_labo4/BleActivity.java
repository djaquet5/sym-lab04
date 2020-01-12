package ch.heigvd.iict.sym_labo4;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import ch.heigvd.iict.sym_labo4.abstractactivies.BaseTemplateActivity;
import ch.heigvd.iict.sym_labo4.adapters.ResultsAdapter;
import ch.heigvd.iict.sym_labo4.utils.UUIDConstant;
import ch.heigvd.iict.sym_labo4.viewmodels.BleOperationsViewModel;

/**
 * Project: Labo4
 * Created by fabien.dutoit on 09.08.2019
 * (C) 2019 - HEIG-VD, IICT
 */

public class BleActivity extends BaseTemplateActivity {

    private static final String TAG = BleActivity.class.getSimpleName();

    //system services
    private BluetoothAdapter bluetoothAdapter = null;

    //view model
    private BleOperationsViewModel bleViewModel = null;

    //gui elements
    private View operationPanel = null;
    private View scanPanel = null;

    private ListView scanResults = null;
    private TextView emptyScanResults = null;

    //menu elements
    private MenuItem scanMenuBtn = null;
    private MenuItem disconnectMenuBtn = null;

    //adapters
    private ResultsAdapter scanResultsAdapter = null;

    //states
    private Handler handler = null;
    private boolean isScanning = false;

    private Button temperature;
    private TextView tempresult;

    private TextView nbButtonPressedValue;

    private EditText valueToSend;
    private Button sendValue;

    private TextView currentDeviceTime;

    private Button setDeviceTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        this.handler = new Handler();

        //enable and start bluetooth - initialize bluetooth adapter
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();

        //link GUI
        this.operationPanel = findViewById(R.id.ble_operation);
        this.scanPanel = findViewById(R.id.ble_scan);
        this.scanResults = findViewById(R.id.ble_scanresults);
        this.emptyScanResults = findViewById(R.id.ble_scanresults_empty);

        this.temperature = findViewById(R.id.temperature);
        this.tempresult = findViewById(R.id.tempresult);

        this.nbButtonPressedValue = findViewById(R.id.nbButtonPressedValue);

        this.valueToSend = findViewById(R.id.valueToSend);
        this.sendValue = findViewById(R.id.sendValue);

        this.currentDeviceTime = findViewById(R.id.currentDeviceTime);

        this.setDeviceTime = findViewById(R.id.setDeviceTime);

        //manage scanned item
        this.scanResultsAdapter = new ResultsAdapter(this);
        this.scanResults.setAdapter(this.scanResultsAdapter);
        this.scanResults.setEmptyView(this.emptyScanResults);

        //connect to view model
        this.bleViewModel = ViewModelProviders.of(this).get(BleOperationsViewModel.class);

        updateGui();

        //events
        this.scanResults.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            runOnUiThread(() -> {
                //we stop scanning
                scanLeDevice(false);
                //we connect to the clicked device
                bleViewModel.connect(((ScanResult)scanResultsAdapter.getItem(position)).getDevice());
            });
        });

        temperature.setOnClickListener(v -> bleViewModel.readTemperature());

        sendValue.setOnClickListener(v -> {
            // We do not use the method Integer.parseUnsignedInt because it requires the API level 26
            // and we currently are in API level 21 (according to Android Studio)
            try {
                bleViewModel.sendValue(Integer.parseInt(valueToSend.getText().toString()));
            } catch (NumberFormatException e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            }
        });

        setDeviceTime.setOnClickListener(v -> bleViewModel.setTime());

        //ble events
        this.bleViewModel.isConnected().observe(this, (isConnected) -> {
            updateGui();
        });

        this.bleViewModel.getDeviceTemp().observe(this, deviceTemp ->
            this.tempresult.setText(deviceTemp + "°C")
        );

        this.bleViewModel.getNbButtonClicked().observe(this, nbButtonClicked ->
            this.nbButtonPressedValue.setText(nbButtonClicked.toString())
        );

        this.bleViewModel.getCurrentTime().observe(this, currentTime ->
            this.currentDeviceTime.setText(currentTime)
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ble_menu, menu);
        //we link the two menu items
        this.scanMenuBtn = menu.findItem(R.id.menu_ble_search);
        this.disconnectMenuBtn = menu.findItem(R.id.menu_ble_disconnect);
        //we update the gui
        updateGui();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_ble_search) {
            if(isScanning)
                scanLeDevice(false);
            else
                scanLeDevice(true);
            return true;
        }
        else if (id == R.id.menu_ble_disconnect) {
            bleViewModel.disconnect();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(this.isScanning)
            scanLeDevice(false);
        if(isFinishing())
            this.bleViewModel.disconnect();
    }

    /*
     * Method used to update the GUI according to BLE status:
     * - connected: display operation panel (BLE control panel)
     * - not connected: display scan result
     */
    private void updateGui() {
        Boolean isConnected = this.bleViewModel.isConnected().getValue();
        if(isConnected != null && isConnected) {
            this.scanPanel.setVisibility(View.GONE);
            this.operationPanel.setVisibility(View.VISIBLE);

            if(this.scanMenuBtn != null && this.disconnectMenuBtn != null) {
                this.scanMenuBtn.setVisible(false);
                this.disconnectMenuBtn.setVisible(true);
            }
        } else {
            this.operationPanel.setVisibility(View.GONE);
            this.scanPanel.setVisibility(View.VISIBLE);

            if(this.scanMenuBtn != null && this.disconnectMenuBtn != null) {
                this.disconnectMenuBtn.setVisible(false);
                this.scanMenuBtn.setVisible(true);
            }
        }
    }

    //this method need user granted localisation permission, our demo app is requesting it on MainActivity
    private void scanLeDevice(final boolean enable) {
        final BluetoothLeScanner bluetoothScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (enable) {

            //config
            ScanSettings.Builder builderScanSettings = new ScanSettings.Builder();
            builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            builderScanSettings.setReportDelay(0);

            // Filter to get only the devices with SYM services
            // Source : https://www.programcreek.com/java-api-examples/index.php?api=android.bluetooth.le.ScanFilter
            List<ScanFilter> filterList = new ArrayList<>();
            ScanFilter filter = new ScanFilter.Builder()
                                              .setServiceUuid(new ParcelUuid(UUIDConstant.SYM_CUSTOM))
                                              .build();
            filterList.add(filter);

            //reset display
            scanResultsAdapter.clear();

            bluetoothScanner.startScan(filterList, builderScanSettings.build(), leScanCallback);
            Log.d(TAG,"Start scanning...");
            isScanning = true;

            //we scan only for 15 seconds
            handler.postDelayed(() -> {
                scanLeDevice(false);
            }, 15*1000L);

        } else {
            bluetoothScanner.stopScan(leScanCallback);
            isScanning = false;
            Log.d(TAG,"Stop scanning (manual)");
        }
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            runOnUiThread(() -> {
                scanResultsAdapter.addDevice(result);
            });
        }
    };

}
