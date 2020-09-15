package edu.asu.cse535.team18;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VideoRecordActivity extends AppCompatActivity {

    final private String URL_VIDEO_UPLOAD = "http://" + BuildConfig.FOG_SERVER_HOST + ":" + BuildConfig.FOG_SERVER_PORT + "/" + BuildConfig.FOG_SERVER_END_POINT;

    final private MediaType MEDIATYPE_VIDEO = MediaType.parse("video/mp4");

    final int VIDEO_CAPTURE_REQUEST = 2;

    private Uri videoUri;
    private VideoView aslRecordView;
    private int position = 0;
    private MediaController mediaControls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_record);

        dispatchTakeVideoIntent();

        final Button button = findViewById(R.id.upload_button);
        button.setOnClickListener(v -> sendVideo());
    }

    private void dispatchTakeVideoIntent(){
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                .putExtra(MediaStore.EXTRA_DURATION_LIMIT,5)
                .putExtra(MediaStore.EXTRA_VIDEO_QUALITY,1);

        if(takeVideoIntent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(takeVideoIntent, VIDEO_CAPTURE_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String gestureName = getIntent().getStringExtra(getString(R.string.asl_video_to_record));
        String practiceSuffix = "";
        practiceSuffix = "-" + gestureName; //comment this line if you prefer to retain current practiceNumber style ie. 1-60
        String practiceKey = "practiceNumber" + practiceSuffix;
        int practiceNumber = prefs.getInt(practiceKey, 0);
        prefs.edit().putInt(practiceKey, ++practiceNumber).commit();


        TextView practiceTextView= findViewById(R.id.practiceTextView);
        practiceTextView.setText("Practice # " + practiceNumber + " | Gesture: " + gestureName);

        videoUri = data.getData();
        aslRecordView = findViewById(R.id.asl_record_view);
        aslRecordView.setVideoURI(videoUri);

        aslRecordView.setOnPreparedListener(mp -> {
            aslRecordView.seekTo(position);
            if (position == 0){
                aslRecordView.start();
            }
            else{
                aslRecordView.pause();
            }
        });

        if(mediaControls == null){
            mediaControls = new MediaController(VideoRecordActivity.this);
        }

        aslRecordView.setMediaController(mediaControls);
    }

    private void sendVideo() {
        OkHttpClient client = new OkHttpClient();

        byte[] videoArray = new byte[0];

        videoArray = convertUriToByteArray(videoUri);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file",getVideoName(),
                        RequestBody.create(MEDIATYPE_VIDEO,videoArray))
                .build();

        Request request = new Request.Builder()
                .url(URL_VIDEO_UPLOAD)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                threadToast(getApplicationContext(), "Sorry, an error occurred. Please try again.", Toast.LENGTH_LONG);
                Log.i("http_request",e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                threadToast(getApplicationContext(), "Your practice video was uploaded successfully.", Toast.LENGTH_SHORT);

                Intent videoViewingIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(videoViewingIntent);
            }
        });
    }

    private byte[] convertUriToByteArray(Uri uri){
        byte[] resourceAsByteArray = new byte[0];

        try{

            byte[] buffer;

            InputStream is = getContentResolver().openInputStream(uri);
            buffer = new byte[4096];
            int read;

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            while ((read = is.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, read);

                resourceAsByteArray = byteArrayOutputStream.toByteArray();
            }

        } catch (IOException e){
            Toast.makeText(getApplicationContext(), "An error has occurred", Toast.LENGTH_LONG).show();
            Log.e("uri_to_byte",e.getMessage());
        }

        return resourceAsByteArray;
    }

    private String getVideoName(){
        //video names should be of format GESTURE_PRACTICE_(practice number)_USERLASTNAME.mp4
        Intent videoRecordingIntent = getIntent();
        String gestureName = videoRecordingIntent.getStringExtra(getString(R.string.asl_video_to_record));
        String practiceSuffix = "";
        practiceSuffix = "-" + gestureName; //comment this line if you prefer to retain current practiceNumber style ie. 1-60
        String practiceKey = "practiceNumber" + practiceSuffix;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int practiceNumber = prefs.getInt(practiceKey, 0);




        final String videoUploadPrefix = gestureName.toUpperCase() + "_PRACTICE";
        final String videoUploadFileExtension = ".mp4";

        String userName = videoRecordingIntent.getStringExtra(getString(R.string.user_name));

        String videoName = videoUploadPrefix + "_" + practiceNumber + "_" + userName + videoUploadFileExtension;

        return videoName;
    }

    public static void threadToast(final Context context, final String msg, final int length) {
        if (context != null && msg != null) {
            new Handler(Looper.getMainLooper()).post(
                    () -> Toast.makeText(context, msg, length).show()
            );
        }
    }
}
