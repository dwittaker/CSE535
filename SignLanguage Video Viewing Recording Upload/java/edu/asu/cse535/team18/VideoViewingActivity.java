package edu.asu.cse535.team18;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

public class VideoViewingActivity extends AppCompatActivity {

    private String videoToView;
    private String videoUrl;
    private String userName;

    private VideoView aslVideoView;
    private MediaController mediaControls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_viewing);

        aslVideoView = findViewById(R.id.asl_video_view);
        TextView reviewTextView= findViewById(R.id.reviewtextView);


        retrieveIntentExtras();
        reviewTextView.setText("Gesture: " + videoToView);

        new BackgroundAsyncTask().execute(videoUrl);

        initializeMediaControls();
        setupPracticeButton();
    }

    private void retrieveIntentExtras(){
        Intent videoViewingIntent;

        videoViewingIntent = getIntent();
        videoToView = videoViewingIntent.getStringExtra(getString(R.string.asl_video_to_view));
        videoUrl = videoViewingIntent.getStringExtra(getString(R.string.asl_training_video_url));
        userName = videoViewingIntent.getStringExtra(getString(R.string.user_name));
    }

    private void initializeMediaControls(){
        if(mediaControls == null){
            mediaControls = new MediaController(this);
        }

        mediaControls.setAnchorView(aslVideoView);
        aslVideoView.setMediaController(mediaControls);
    }

    private void setupPracticeButton(){
        final Button tryItNowButton = findViewById(R.id.try_it_now_button);

        tryItNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String intentVideoToRecordLabel = getString(R.string.asl_video_to_record);
                String intentUserNameLabel = getString(R.string.user_name);

                //https://developer.android.com/training/basics/firstapp/starting-activity#java
                Intent videoRecordingIntent = new Intent(getApplicationContext(),VideoRecordActivity.class);
                videoRecordingIntent.putExtra(intentVideoToRecordLabel,videoToView);
                videoRecordingIntent.putExtra(intentUserNameLabel,userName);

                startActivity(videoRecordingIntent);
            }
        });
    }

    //https://www.oodlestechnologies.com/blogs/Working-with-MediaController,--VideoView-in-Android/
    public class BackgroundAsyncTask extends AsyncTask<String, Uri, Void> {

        private ConstraintLayout constraintLayout = findViewById(R.id.layout_video_viewing);
        private ProgressBar progressBar;


        protected void onPreExecute() {

            initializeProgressBar();

            constraintLayout.addView(progressBar);
            aslVideoView.setVisibility(View.GONE);
        }

        private void initializeProgressBar(){

            constraintLayout = findViewById(R.id.layout_video_viewing);
            progressBar = new ProgressBar(VideoViewingActivity.this,null,android.R.attr.progressBarStyleLarge);
            progressBar.generateViewId();

            //https://stackoverflow.com/questions/45263159/constraintlayout-change-constraints-programmatically
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);
            constraintSet.connect(progressBar.getId(),ConstraintSet.RIGHT,constraintLayout.getId(),ConstraintSet.RIGHT,0);
            constraintSet.connect(progressBar.getId(),ConstraintSet.TOP,constraintLayout.getId(),ConstraintSet.TOP,0);
            constraintSet.connect(progressBar.getId(),ConstraintSet.LEFT,constraintLayout.getId(),ConstraintSet.LEFT,0);
            constraintSet.connect(progressBar.getId(),ConstraintSet.BOTTOM,constraintLayout.getId(),ConstraintSet.BOTTOM,0);
            constraintSet.applyTo(constraintLayout);
        }

        protected void onProgressUpdate(final Uri... uri) {

            try {
                aslVideoView.setVisibility(View.VISIBLE);
                constraintLayout.removeView(progressBar);
                aslVideoView.setVideoURI(uri[0]);
                aslVideoView.requestFocus();
                aslVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                    public void onPrepared(MediaPlayer arg0) {
                        aslVideoView.start();
                    }
                });
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                Uri uri = Uri.parse(params[0]);

                publishProgress(uri);
            } catch (Exception e) {
                e.printStackTrace();

            }
            return null;
        }
    }
}
