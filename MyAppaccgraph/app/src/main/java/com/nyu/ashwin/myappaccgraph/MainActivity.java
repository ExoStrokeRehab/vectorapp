package com.nyu.ashwin.myappaccgraph;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.util.Random;
import static android.util.FloatMath.cos;
import static android.util.FloatMath.sin;
import static android.util.FloatMath.sqrt;


public class MainActivity extends ActionBarActivity {
    Sensor accelerometer, gyroscope, orientation;
    SensorManager sm;
    TextView display;
    float x, y, z;
    float X, Y, Z, X1, Y1, Z1;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    float gravity1,gravity2,gravity3;
    private float timestamp;
    float axisX, axisY, axisZ;
    private static final Random RANDOM = new Random();
    private int lastX=0;
    private LineGraphSeries<DataPoint> series;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sm=(SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope=sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        orientation=sm.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sm.registerListener(accelerationListener,accelerometer,SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
        sm.registerListener(gyroscopeListener,gyroscope,SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
        sm.registerListener(orientationListener, orientation, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
        display=(TextView)findViewById(R.id.display);
        GraphView graph = (GraphView)findViewById(R.id.graph);
        series = new LineGraphSeries<DataPoint>();
        graph.addSeries(series);
        Viewport viewport = graph.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setMaxY(360);
        viewport.setMinY(-360);
        viewport.setMinX(0);
        viewport.setMaxX(10);
        viewport.setScrollable(true);
    }
    private SensorEventListener accelerationListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            final float alpha = (float) 0.8;
            gravity1 = alpha * gravity1 + (1 - alpha) * event.values[0];
            gravity2 = alpha * gravity2 + (1 - alpha) * event.values[1];
            gravity3 = alpha * gravity3 + (1 - alpha) * event.values[2];
            x = event.values[0] - gravity1;
            y = event.values[1] - gravity2;
            z = event.values[2] - gravity3;
            refresh();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    private SensorEventListener orientationListener = new SensorEventListener() {


        @Override
        public void onSensorChanged(SensorEvent event) {
            Z = event.values[0];
            X = event.values[1];
            Y = event.values[2];
            refresh();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    private SensorEventListener gyroscopeListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // This timestep's delta rotation to be multiplied by the current rotation
            // after computing it from the gyro sample data.
            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                axisX = event.values[0];
                axisY = event.values[1];
                axisZ = event.values[2];

                // Calculate the angular speed of the sample
                float omegaMagnitude = sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

                // Normalize the rotation vector if it's big enough to get the axis
                if (omegaMagnitude > 0) {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                // Integrate around this axis with the angular speed by the timestep
                // in order to get a delta rotation from this sample over the timestep
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                float sinThetaOverTwo = sin(thetaOverTwo);
                float cosThetaOverTwo = cos(thetaOverTwo);
                deltaRotationVector[0] = sinThetaOverTwo * axisX;
                deltaRotationVector[1] = sinThetaOverTwo * axisY;
                deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                deltaRotationVector[3] = cosThetaOverTwo;
            }
            timestamp = event.timestamp;
            float[] deltaRotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
            // User code should concatenate the delta rotation we computed with the current rotation
            // in order to get the updated rotation.
            // rotationCurrent = rotationCurrent * deltaRotationMatrix;

            refresh();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private void refresh(){
        String Output = String.format("LIN_ACC X: %f \n LIN_ACC Y: %f \n LIN_ACC Z: %f \n YAW: %f\n PITCH: %f\n ROLL: %f\n X: %f\n Y: %f\n Z: %f\n W: %f\n",x,y,z,X,Y,Z,deltaRotationVector[0],deltaRotationVector[1],deltaRotationVector[2],deltaRotationVector[3]);
        display.setText(Output);
    }
    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable(){
            @Override
            public void run() {
                for(int i=0;i<10000;i++){
                    runOnUiThread(new Runnable(){
                        public void run(){
                            addEntry();
                        }
                    });
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                }

            }
        }).start();
    }
    private void addEntry() {
        series.appendData(new DataPoint(lastX++, X), true, 10);
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
}
