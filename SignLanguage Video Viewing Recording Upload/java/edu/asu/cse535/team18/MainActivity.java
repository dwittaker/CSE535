package edu.asu.cse535.team18;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    final int PERMISSIONS_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestAppPermissions();

        setContentView(R.layout.activity_main);
        createDropDown();

        // get userName from SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String userName = prefs.getString("userName", "");

        EditText userNameEditText = findViewById(R.id.user_name_edit_text);
        userNameEditText.setText(userName);

        // update userName in prefs whenever changed
        userNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                prefs.edit().putString("userName", s.toString()).commit();
            }
        });
    }

    //https://developer.android.com/guide/topics/ui/controls/spinner
    private void createDropDown(){
        Spinner spinner = findViewById(R.id.spinner_videoChoices);

        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, Gestures.values()));

        //https://stackoverflow.com/questions/16581536/setonitemselectedlistener-of-spinner-does-not-call
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position != 0){
                    //https://developer.android.com/training/basics/firstapp/starting-activity#java
                    Intent videoViewingIntent = new Intent(getApplicationContext(), VideoViewingActivity.class);
                    Gestures selectedGesture = (Gestures) parent.getItemAtPosition(position);

                    videoViewingIntent.putExtra(getString(R.string.asl_video_to_view),getString(selectedGesture.displayResId));

                    String userNameLabel = getString(R.string.user_name);
                    String userNameData = getUserName();

                    videoViewingIntent.putExtra(userNameLabel,userNameData);

                    String videoToViewUrlLabel = getString(R.string.asl_training_video_url);
                    String videoToViewUrlData = getVideoUrl(selectedGesture);
                    videoViewingIntent.putExtra(videoToViewUrlLabel,videoToViewUrlData);

                    startActivity(videoViewingIntent);
                }
            }

            private String getVideoUrl(Gestures gesture){
                return BuildConfig.SIGN_SAVVY_BASE_URL+gesture.urlPath;
            }

            private String getUserName(){
                EditText userNameEditText = findViewById(R.id.user_name_edit_text);
                return userNameEditText.getText().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(getApplicationContext(),"You must select an item to view the video",Toast.LENGTH_LONG).show();
            }
        });
    }

    //https://stackoverflow.com/questions/34342816/android-6-0-multiple-permissions
    private void requestAppPermissions(){
        String[] permissions = {
                Manifest.permission.CAMERA
        };
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
    }
    //implement https://developer.android.com/reference/android/support/v4/app/ActivityCompat.OnRequestPermissionsResultCallback.html?
    //good practice, but it's hmwk
}
