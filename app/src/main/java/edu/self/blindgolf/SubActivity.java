package edu.self.blindgolf;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import java.io.InputStream;


import com.ibm.watson.developer_cloud.http.ServiceCallback;
import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifiedImages;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SubActivity extends AppCompatActivity implements Runnable,TextToSpeech.OnInitListener{

    private final static int RESULT_CAMERA = 1001;
    private final static int REQUEST_PERMISSION = 1002;

    private ImageView imageView;
    private Uri cameraUri;
    private String filePath;

    VisualRecognition visualRecognition;

    IamOptions options;

    ClassifyOptions classifyOptions;

    InputStream inputStream;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

//        imageView = findViewById(R.id.image_view);

        // TextToSpeechオブジェクトの生成 ★初期化★
        this.tts = new TextToSpeech(this, this);

        options = new IamOptions.Builder()
                .apiKey("ROXRHwsFzc0HmxVvfAIBYvZWwVLFyU9PaksM2vR535Bl")
                .build();
        visualRecognition = new VisualRecognition("2018-03-19", options);

//        Button cameraButton = findViewById(R.id.camera_button);
//        cameraButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Android 6, API 23以上でパーミッシンの確認
//                if (Build.VERSION.SDK_INT >= 23) {
//                    checkPermission();
//                }
//                else {
//                    cameraIntent();
//                }
//            }
//        });
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermission();
        }
        else {
            cameraIntent();
        }
    }

    private void cameraIntent(){
        Log.d("debug","cameraIntent()");


        // 保存先のフォルダーをカメラに指定した場合
        File cameraFolder = new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM),"Camera");


        // 保存ファイル名
        String fileName = new SimpleDateFormat(
                "ddHHmmss", Locale.US).format(new Date());
        filePath = String.format("%s/%s.jpg", cameraFolder.getPath(),fileName);
        Log.d("debug","filePath:"+filePath);

        // capture画像のファイルパス
        File cameraFile = new File(filePath);

        cameraUri = FileProvider.getUriForFile(
                SubActivity.this,
                getApplicationContext().getPackageName() + ".fileprovider",
                cameraFile);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        startActivityForResult(intent, RESULT_CAMERA);

        Log.d("debug","startActivityForResult()");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d("code", String.valueOf(resultCode));
        if (requestCode == RESULT_CAMERA) {

            if(cameraUri != null){
//                imageView.setImageURI(cameraUri);

                try {
                    // capture画像のファイルパス
                    File cameraFile = new File(filePath);
                    inputStream = new FileInputStream(cameraFile);

                    //ここでPOSTする内容を設定　"image/jpg"の部分は送りたいファイルの形式に合わせて変更する
                    classifyOptions = new ClassifyOptions.Builder()
                            .imagesFile(inputStream)
                            .imagesFilename("sample.jpg")
                            .threshold((float) 0.3)
                            .build();

//                    System.out.println(visualRecognition.getEndPoint());

                    Thread thread = new Thread(this);
                    thread.start();
                    // inputStream.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else{
                Log.d("debug","cameraUri == null");
            }
        }

    }

    private void checkPermission(){
        // 既に許可している
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED){
            cameraIntent();
        }
        // 拒否していた場合
        else{
            requestPermission();
        }
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(SubActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);

        } else {
            Toast toast = Toast.makeText(this,
                    "許可されないとアプリが実行できません",
                    Toast.LENGTH_SHORT);
            toast.show();

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,},
                    REQUEST_PERMISSION);

        }
    }

    public void run() {
        System.out.println("thread");
        visualRecognition.classify(classifyOptions).enqueue(new ServiceCallback<ClassifiedImages>() {
            @Override public void onResponse(ClassifiedImages response) {
                //人間が存在するかどうか
                try {
                    JSONObject json = new JSONObject(response.toString());
                    JSONArray check = json.getJSONArray("images").getJSONObject(0).getJSONArray("classifiers");
                    JSONArray datas = check.getJSONObject(0).getJSONArray("classes");
                    for (int i = 0; i < datas.length(); i++) {
                        JSONObject data = datas.getJSONObject(i);
                        System.out.println(data.getString("class"));
                        // 人間オブジェクトが存在するかチェック
                        try {
                            if(data.getString("class").equals("person")) {
                                // 人間がいるとアナウンスする
                                speechText("障害物があります。少し右に打ってください。");
                                // for文を抜けてよい
                                break;
                            }

                            if (i == datas.length()-1){
                                speechText("視界は良好です。そのまま打ってください。");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
            @Override public void onFailure(Exception e) {
                e.printStackTrace();
            }
        });
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

