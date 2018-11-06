package edu.self.blindgolf;

import android.content.Intent;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements SensorEventListener,TextToSpeech.OnInitListener {

    TextView explainTextView, objTextView, matchText;
    private SensorManager manager;
    //目的方位
    double obj;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 初期画面
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 部品をグローバルに保持
        explainTextView = findViewById(R.id.textView);
        objTextView = findViewById(R.id.textView2);
        matchText = findViewById(R.id.textView3);//ペルー-77.1278654,-12.0266034台東区139.7684694, 35.7132094 ゴルフクラブ139.4876672, 35.62112
        obj = getDirection(139.7814074,35.6182819,139.487667,35.62112);
        objTextView.setText(""+obj);

        // TextToSpeechオブジェクトの生成 ★初期化★
        this.tts = new TextToSpeech(this, this);

        // 加速度センサー
        manager = (SensorManager)getSystemService(SENSOR_SERVICE);

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // 回転行列
    private static final int MATRIX_SIZE = 16;
    float[] inR = new float[MATRIX_SIZE];
    float[] outR = new float[MATRIX_SIZE];
    float[] I = new float[MATRIX_SIZE];
    // センサー値
    private static final int AXIS_NUM = 3;
    float[] gravity = new float[AXIS_NUM];
    float[] geomagnetic = new float[AXIS_NUM];
    float[] orientation = new float[AXIS_NUM];
    float[] attitude = new float[AXIS_NUM];
    // 許容角度
    double permit = 7.5;
    double nearby = 45.0;
    double more = 90.0;

    // 描画頻度
    int displayCount = 0;
    // 発話頻度
    int speechCount = 0;
    // 撮影するための「今です」数
    int nowCount = 0;



    @Override
    protected void onStop(){
        super.onStop();
        manager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        // Listenerの登録
        manager.registerListener(this,
                manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
        manager.registerListener(this,
                manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);
        manager.registerListener(this,
                manager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    @Override
    public void onSensorChanged(SensorEvent event) {


        String str = "";

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravity = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomagnetic = event.values.clone();
                break;
            case Sensor.TYPE_ORIENTATION:
                orientation = event.values.clone();
                break;
        }
        if (gravity != null && geomagnetic != null && orientation != null) {
            // 回転行列を計算
            SensorManager.getRotationMatrix(inR, I, gravity, geomagnetic);
            // 端末の画面設定に合わせる(以下は, 縦表示で画面を上にした場合)
            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);
            // 方位角/傾きを取得
            SensorManager.getOrientation(outR, attitude);

            addSample(attitude);

            if (displayCount >= 20) {
                // デバック用表示
                str = "1) 地磁気+加速度センサー\n"
                        + " 方位角： " + String.format("%3.1f", Math.toDegrees(mParam[0]))
                        + " 傾斜角： " + String.format("%3.1f", Math.toDegrees(mParam[1]))
                        + " 回転角： " + String.format("%3.1f", Math.toDegrees(mParam[2]));
                explainTextView.setText(str);

                // 方向アシスト値
                double diff = 0;
                double direction = Math.toDegrees(mParam[0]);
                if (direction > obj - permit & direction < obj + permit) {
                    matchText.setText("今です");
                } else if (obj < 0) {

                    nowCount = 0;

                    if (direction >= obj & direction <= obj+180){
                        diff = direction - obj;
                        if(diff <= nearby){
                            matchText.setText("もう少し左です");
                        } else if(diff <= more){
                            matchText.setText("左です");
                        } else {
                            matchText.setText("もっと左です");
                        }
                    } else {
                        if(direction > obj+180){
                            direction = direction - 360;
                        }

                        diff = obj - direction;
                        if(diff <= nearby){
                            matchText.setText("もう少し右です");
                        } else if(diff <= more){
                            matchText.setText("右です");
                        } else {
                            matchText.setText("もっと右です");
                        }
                    }

                } else if (obj >= 0) {

                    nowCount = 0;

                    if (direction <= obj & direction >= obj-180){
                        diff = obj - direction;
                        if(diff <= nearby){
                            matchText.setText("もう少し右です");
                        } else if(diff <= more){
                            matchText.setText("右です");
                        } else {
                            matchText.setText("もっと右です");
                        }
                    } else {
                        if(direction < obj-180){
                            direction = direction + 360;
                        }

                        diff = direction - obj;
                        if(diff <= nearby){
                            matchText.setText("もう少し左です");
                        } else if(diff <= more){
                            matchText.setText("左です");
                        } else {
                            matchText.setText("もっと左です");
                        }
                    }

                }
                displayCount = 0;
            }
            displayCount++;

            if (speechCount >= 300) {
                speechText(matchText.getText().toString());
                speechCount = 0;

                nowCount++;
                if(nowCount >= 3){
                    Intent intent = new Intent(getApplication(), SubActivity.class);
                    startActivity(intent);
                }

            }
            speechCount++;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private ArrayList<Float> mFirst     = new ArrayList<Float>();
    private ArrayList<Float> mSecond    = new ArrayList<Float>();
    private ArrayList<Float> mThrad     = new ArrayList<Float>();

    public int sampleCount=45;//サンプリング数
    public int sampleNum = 25;//サンプリングした値の使用値のインデックス

    private float[] mParam = new float[3];//フィルタをかけた後の値

    private boolean mSampleEnable=false;//規定のサンプリング数に達したか

    void addSample(float[] sample)
    {
        //サンプリング数の追加
        mFirst.add(sample[0]);
        mSecond.add(sample[1]);
        mThrad.add(sample[2]);

        //必要なサンプリング数に達したら
        if(mFirst.size() == sampleCount)
        {
            //メディアンフィルタ(サンプリング数をソートして中央値を使用)かけて値を取得
            //その値にさらにローパスフィルタをかける

            ArrayList<Float> lst = (ArrayList<Float>) mFirst.clone();
            Collections.sort(lst);
            mParam[0] =(mParam[0]*0.9f) + lst.get(sampleNum)*0.1f;

            lst = (ArrayList<Float>) mSecond.clone();
            Collections.sort(lst);
            mParam[1] = (mParam[1]*0.9f) +lst.get(sampleNum)*0.1f;

            lst = (ArrayList<Float>) mThrad.clone();
            Collections.sort(lst);
            mParam[2] = (mParam[2]*0.9f) +lst.get(sampleNum)*0.1f;

            mSampleEnable = true;

            //一番最初の値を削除
            mFirst.remove(0);
            mSecond.remove(0);
            mThrad.remove(0);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static double getDirection(double latitude1, double longitude1, double latitude2, double longitude2) {
        /*
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        double lng1 = Math.toRadians(longitude1);
        double lng2 = Math.toRadians(longitude2);
        double Y = Math.sin(lng2 - lng1) * Math.cos(lat2);
        double X = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lng2 - lng1);
        double deg = Math.toDegrees(Math.atan2(Y, X));
        double angle = (deg + 360) % 360;
        return (Math.abs(angle) + (1 / 7200));
        */
        double Y = Math.cos(longitude2 * Math.PI / 180) * Math.sin(latitude2 * Math.PI / 180 - latitude1 * Math.PI / 180);
        double X = Math.cos(longitude1 * Math.PI / 180) * Math.sin(longitude2 * Math.PI / 180) - Math.sin(longitude1 * Math.PI / 180) * Math.cos(longitude2 * Math.PI / 180) * Math.cos(latitude2 * Math.PI / 180 - latitude1 * Math.PI / 180);
        double dirE0 = 180 * Math.atan2(Y, X) / Math.PI; // 東向きが０度の方向
        return dirE0 % 360; //(dirE0 + 90) % 360;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////

    private TextToSpeech tts = null;
    private static final String TAG = "TestTTS";

    @Override
    public void onInit(int status) {
        // TTS初期化
        if (TextToSpeech.SUCCESS == status) {
            Log.d(TAG, "initialized");
        } else {
            Log.e(TAG, "failed to initialize");
        }
    }

    private void shutDown(){
        if (null != tts) {
            // to release the resource of TextToSpeech
            tts.shutdown();
        }
    }

    // 読み上げのスピード
    private void setSpeechRate(float rate){
        if (null != tts) {
            tts.setSpeechRate(rate);
        }
    }

    // 読み上げのピッチ
    private void setSpeechPitch(float pitch){
        if (null != tts) {
            tts.setPitch(pitch);
        }
    }

    private void speechText(String string) {

        // string変数にアシストメッセージを入れる予定
        //String string = "目の前に木があります";

        if (0 < string.length()) {
            if (tts.isSpeaking()) {
                tts.stop();
                Log.d(TAG,"tts stop ");
                return;
            }
            setSpeechRate(1.0f);
            setSpeechPitch(1.0f);

            if (Build.VERSION.SDK_INT >= 21){
                // SDK 21 以上
                tts.speak(string, TextToSpeech.QUEUE_FLUSH, null, "messageID");
                Log.d(TAG,"tts speak1 ");
            }
            else{
                // tts.speak(text, TextToSpeech.QUEUE_FLUSH, null) に
                // KEY_PARAM_UTTERANCE_ID を HasMap で設定
                HashMap<String, String> map = new HashMap<String, String>();
                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"messageID");
                tts.speak(string, TextToSpeech.QUEUE_FLUSH, map);
                Log.d(TAG,"tts speak2 ");
            }

            setTtsListener();
        }
    }

    // 読み上げの始まりと終わりを取得
    private void setTtsListener(){
        // android version more than 15th
        if (Build.VERSION.SDK_INT >= 15){
            int listenerResult =
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onDone(String utteranceId) {
                            Log.d(TAG,"progress on Done " + utteranceId);
                        }

                        @Override
                        public void onError(String utteranceId) {
                            Log.d(TAG,"progress on Error " + utteranceId);
                        }

                        @Override
                        public void onStart(String utteranceId) {
                            Log.d(TAG,"progress on Start " + utteranceId);
                        }
                    });

            if (listenerResult != TextToSpeech.SUCCESS) {
                Log.e(TAG, "failed to add utterance progress listener");
            }
        }
        else {
            Log.e(TAG, "Build VERSION is less than API 15");
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        shutDown();
    }


}
