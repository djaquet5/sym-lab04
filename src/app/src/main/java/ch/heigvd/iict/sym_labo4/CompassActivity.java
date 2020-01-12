package ch.heigvd.iict.sym_labo4;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ch.heigvd.iict.sym_labo4.gl.OpenGLRenderer;

public class CompassActivity extends AppCompatActivity implements SensorEventListener {

    //opengl
    private OpenGLRenderer  opglr           = null;
    private GLSurfaceView   m3DView         = null;

    // Sensors
    private SensorManager sensorManager;
    private Sensor magnetic;
    private Sensor accelerometer;

    private float [] geomagnetics;
    private float [] gravity;
    private float [] rotMatrix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        geomagnetics = new float[3];
        gravity = new float[3];
        rotMatrix = new float[16];

        // we need fullscreen
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // we initiate the view
        setContentView(R.layout.activity_compass);

        //we create the renderer
        this.opglr = new OpenGLRenderer(getApplicationContext());

        // link to GUI
        this.m3DView = findViewById(R.id.compass_opengl);

        //init opengl surface view
        this.m3DView.setRenderer(this.opglr);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (sensorManager == null) {
            Toast.makeText(CompassActivity.this, "Sensor manager is null", Toast.LENGTH_LONG).show();
            finish();
        }

        if ((magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) == null){
            Toast.makeText(CompassActivity.this, "Magnetic sensor is not available", Toast.LENGTH_LONG).show();
            finish();
        }

        if ((accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) == null) {
            Toast.makeText(CompassActivity.this, "Accelerometer sensor is not available", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Behavior when the Activity is resumed
     */
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Behavior when the Activity is on pause
     */
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    /**
     * Source : https://www.programcreek.com/java-api-examples/?class=android.hardware.SensorManager&method=getRotationMatrix
     * @param sensorEvent event that detects if the sensors have moved or not
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravity = sensorEvent.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomagnetics = sensorEvent.values.clone();
                break;
        }

        if (gravity != null || geomagnetics != null) {
            SensorManager.getRotationMatrix(opglr.swapRotMatrix(rotMatrix), null, gravity, geomagnetics);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }



}
