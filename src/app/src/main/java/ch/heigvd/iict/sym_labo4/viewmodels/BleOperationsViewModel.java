package ch.heigvd.iict.sym_labo4.viewmodels;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import ch.heigvd.iict.sym_labo4.utils.UUIDConstant;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.data.Data;

public class BleOperationsViewModel extends AndroidViewModel {

    private static final String TAG = BleOperationsViewModel.class.getSimpleName();

    private MySymBleManager ble = null;
    private BluetoothGatt mConnection = null;

    //live data - observer
    private final MutableLiveData<Boolean> mIsConnected = new MutableLiveData<>();
    public LiveData<Boolean> isConnected() {
        return mIsConnected;
    }

    //references to the UUIDConstant and Characteristics of the SYM Pixl
    private BluetoothGattService timeService = null, symService = null;
    private BluetoothGattCharacteristic currentTimeChar = null, integerChar = null, temperatureChar = null, buttonClickChar = null;

    private MutableLiveData<Float> deviceTemp = new MutableLiveData<>();
    private MutableLiveData<Integer> nbButtonClicked = new MutableLiveData<>();
    private MutableLiveData<String> currentTime = new MutableLiveData<>();

    public MutableLiveData<Float> getDeviceTemp() {
        return deviceTemp;
    }

    public MutableLiveData<Integer> getNbButtonClicked() {
        return nbButtonClicked;
    }

    public MutableLiveData<String> getCurrentTime() {
        return currentTime;
    }

    public BleOperationsViewModel(Application application) {
        super(application);
        this.mIsConnected.setValue(false); //to be sure that it's never null
        this.ble = new MySymBleManager();
        this.ble.setGattCallbacks(this.bleManagerCallbacks);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "onCleared");
        this.ble.disconnect();
    }

    public void connect(BluetoothDevice device) {
        Log.d(TAG, "User request connection to: " + device);
        if(!mIsConnected.getValue()) {
            this.ble.connect(device)
                    .retry(1, 100)
                    .useAutoConnect(false)
                    .enqueue();
        }
    }

    public void disconnect() {
        Log.d(TAG, "User request disconnection");
        this.ble.disconnect();
        if(mConnection != null) {
            mConnection.disconnect();
        }
    }

    public boolean readTemperature() {
        if(!isConnected().getValue() || temperatureChar == null) return false;

        return ble.readTemperature();
    }

    public boolean sendValue(int value) {
        if(!isConnected().getValue() || integerChar == null) return false;

        return ble.sendValue(value);
    }

    public boolean setTime() {
        if(!isConnected().getValue() || currentTimeChar == null) return false;

        return ble.setTime();
    }

    private BleManagerCallbacks bleManagerCallbacks = new BleManagerCallbacks() {
        @Override
        public void onDeviceConnecting(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceConnecting");
            mIsConnected.setValue(false);
        }

        @Override
        public void onDeviceConnected(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceConnected");
            mIsConnected.setValue(true);
        }

        @Override
        public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceDisconnecting");
            mIsConnected.setValue(false);
        }

        @Override
        public void onDeviceDisconnected(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceDisconnected");
            mIsConnected.setValue(false);
        }

        @Override
        public void onLinkLossOccurred(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onLinkLossOccurred");
        }

        @Override
        public void onServicesDiscovered(@NonNull BluetoothDevice device, boolean optionalServicesFound) {
            Log.d(TAG, "onServicesDiscovered");
        }

        @Override
        public void onDeviceReady(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceReady");
        }

        @Override
        public void onBondingRequired(@NonNull BluetoothDevice device) {
            Log.w(TAG, "onBondingRequired");
        }

        @Override
        public void onBonded(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onBonded");
        }

        @Override
        public void onBondingFailed(@NonNull BluetoothDevice device) {
            Log.e(TAG, "onBondingFailed");
        }

        @Override
        public void onError(@NonNull BluetoothDevice device, @NonNull String message, int errorCode) {
            Log.e(TAG, "onError:" + errorCode);
        }

        @Override
        public void onDeviceNotSupported(@NonNull BluetoothDevice device) {
            Log.e(TAG, "onDeviceNotSupported");
            Toast.makeText(getApplication(), "Device not supported", Toast.LENGTH_SHORT).show();
        }
    };

    /*
     *  This class is used to implement the protocol to communicate with the BLE device
     */
    private class MySymBleManager extends BleManager<BleManagerCallbacks> {

        private MySymBleManager() {
            super(getApplication());
        }

        @Override
        public BleManagerGattCallback getGattCallback() { return mGattCallback; }

        /**
         * BluetoothGatt callbacks object.
         */
        private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {

            @Override
            public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
                mConnection = gatt; //trick to force disconnection
                Log.d(TAG, "isRequiredServiceSupported - discovered services:");

                timeService = gatt.getService(UUIDConstant.CURRENT_TIME);
                symService = gatt.getService(UUIDConstant.SYM_CUSTOM);

                if (timeService == null || symService == null)
                    return false;

                currentTimeChar = timeService.getCharacteristic(UUIDConstant.CURRENT_TIME_CARAC);
                integerChar = symService.getCharacteristic(UUIDConstant.SEND_INT);
                temperatureChar = symService.getCharacteristic(UUIDConstant.GET_TEMPERATURE);
                buttonClickChar = symService.getCharacteristic(UUIDConstant.BTN);

                if (currentTimeChar == null || integerChar == null || temperatureChar == null || buttonClickChar == null)
                    return false;

                return true;
            }

            @Override
            protected void initialize() {
                setNotificationCallback(buttonClickChar).with((device, data) ->
                    nbButtonClicked.setValue(data.getIntValue(Data.FORMAT_UINT8, 0))
                );

                enableNotifications(buttonClickChar).enqueue();

                setNotificationCallback(currentTimeChar).with((device, data) -> {
                    Calendar calendar = Calendar.getInstance();

                    calendar.set(data.getIntValue(Data.FORMAT_UINT16, 0),
                                 data.getIntValue(Data.FORMAT_UINT8, 2) - 1,
                                 data.getIntValue(Data.FORMAT_UINT8, 3),
                                 data.getIntValue(Data.FORMAT_UINT8, 4),
                                 data.getIntValue(Data.FORMAT_UINT8, 5),
                                 data.getIntValue(Data.FORMAT_UINT8, 6));

                    // To have the same format as displayed on the device
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d yyyy - HH:mm:ss");
                    currentTime.setValue(dateFormat.format(calendar.getTime()));
                });

                enableNotifications(currentTimeChar).enqueue();
            }

            @Override
            protected void onDeviceDisconnected() {
                //we reset services and characteristics
                timeService = null;
                currentTimeChar = null;

                symService = null;
                integerChar = null;
                temperatureChar = null;
                buttonClickChar = null;
            }
        };

        public boolean readTemperature() {
            readCharacteristic(temperatureChar).with((device, data) ->
                deviceTemp.postValue(data.getIntValue(Data.FORMAT_SINT16, 0) / 10.F)
            ).enqueue();

            return true;
        }

        public boolean sendValue(int value) {
            writeCharacteristic(integerChar, ByteBuffer.allocate(4).putInt(value).array()).enqueue();
            return true;
        }

        public boolean setTime() {
            Calendar calendar = Calendar.getInstance();
            byte[] yearBuffer = ByteBuffer.allocate(2)
                                          .putShort((short)calendar.get(Calendar.YEAR))
                                          .array();

            byte[] currentTime = new byte[10];

            // The year is in Little Endian
            currentTime[0] = yearBuffer[1];
            currentTime[1] = yearBuffer[0];
            currentTime[2] = (byte) (calendar.get(Calendar.MONTH) + 1);
            currentTime[3] = (byte) calendar.get(Calendar.DAY_OF_MONTH);
            currentTime[4] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
            currentTime[5] = (byte) calendar.get(Calendar.MINUTE);
            currentTime[6] = (byte) calendar.get(Calendar.SECOND);
            currentTime[7] = (byte) calendar.get(Calendar.DAY_OF_WEEK);

            writeCharacteristic(currentTimeChar, currentTime).enqueue();

            return true;
        }
    }
}
