package edu.self.blindgolf;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.widget.TextView;
import android.view.View;


public class SensorMonitorExample implements SensorEventListener {


    private Context context;
    private SensorManager mSensorManager;


    private static final float alpha = 0.8f;
    private static final int MATRIX_SIZE = 16;
    private static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST;


    private float[] rawacc = new float[3];
    private float[] acc  = new float[3];
    private float[] grav = new float[3];
    private float[] mag  = new float[3];
    private float[] ori  = new float[3];


    public SensorMonitorExample(Context context, int interval) {
        this.context = context;
        this.mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }




    public void start() {
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),  SENSOR_DELAY);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SENSOR_DELAY);
    }


    public void stop() {
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}


    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                rawacc = event.values.clone();
                lowpassFilter(grav, rawacc);
                break;


            case Sensor.TYPE_MAGNETIC_FIELD:
                lowpassFilter(mag, event.values.clone());
                break;


            default:
                return;
        }


        if (rawacc != null && mag != null) {
            float[] R  = new float[MATRIX_SIZE];
            float[] I  = new float[MATRIX_SIZE];
            float[] rR = new float[MATRIX_SIZE];
            float[] oriRad = new float[3];
            SensorManager.getRotationMatrix(R, I, grav, mag);
            SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, rR);
            SensorManager.getOrientation(rR, oriRad);


            ori = rad2deg(oriRad);


            float[] accNoGrav = new float[4];
            for (int i=0; i<3; i++) accNoGrav[i] = rawacc[i] - grav[i];


            float[] invertR = new float[16];
            Matrix.invertM(invertR, 0, R, 0);


            float[] acc4 = new float[4];
            Matrix.multiplyMV(acc4, 0, invertR, 0, accNoGrav, 0);


            for (int i=0; i<3; i++) acc[i] = acc4[i];
        }
    }


    private void lowpassFilter(float[] vecPrev, float[] vecNew) {
        for (int i=0; i<vecNew.length; i++) {
            vecPrev[i] = alpha * vecPrev[i] + (1-alpha) * vecNew[i];
        }
    }


    private float[] rad2deg(float[] vec) {
        int VEC_SIZE = vec.length;
        float[] retvec = new float[VEC_SIZE];
        for (int i=0; i<VEC_SIZE; i++) {
            retvec[i] = vec[i]/(float)Math.PI*180;
        }
        return retvec;
    }
}
