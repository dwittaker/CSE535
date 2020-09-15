package com.example.DWittaker;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.text.format.DateUtils;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static java.lang.Thread.sleep;


public class MainActivity extends AppCompatActivity {
    final int PERMISSIONS_REQUEST_CODE = 1;
    //BASED ON INDICATIONS FROM THE INSTRUCTOR AND THE CLASS, I WAS UNSURE WHICH TYPE OF ANIMATION
    //WAS NEEDED, SO CREATED ONE THAT MORE RESEMBLES A HEART RATE MONITOR (CALLED 'LIVE').
    //THE OTHER ANIMATION RESEMBLES A REPLAY OR TICKER DATA AND IS CALLED ('REPLAY').

    //public int POINTS_TO_GENERATE = 100;
    public  int DLOAD_DISPLAY_POINT = 10;
    public String DatabaseFileName = "CSE535_ASSIGNMENT2";
    public String DatabaseFileName_dload = "CSE535_ASSIGNMENT2_DOWN";
    //public String DatabaseFileName_dloadattach = "CSE535_ASSIGNMENT2_DOWN";
    public static final String PROPERTY_X = "PROPERTY_X";
    public static final String PROPERTY_Y = "PROPERTY_Y";
    private GraphView gView;
    private Button btn_run;
    private Button btn_stop;
    SQLiteDatabase dbactive;
    SQLiteDatabase dbdload;
    EditText Etxt_ID;
    EditText Etxt_Age;
    EditText Etxt_Name;
    RadioGroup Rgrp_Sex;
    RadioButton Rbtn_Male;
    RadioButton Rbtn_Female;
    String tbl_name;

    private int datacnt;
    public ValueAnimator graphAnimator;
    public AnimatorSet animatorSet;

    List<Animator> animatorListX;
    List<Animator> animatorListY;
    List<Animator> animatorListZ;

    //AddDataTask dataAdder;
    public Switch switchmode;
    public boolean switchchecked;

    private boolean dbactive_exists = false;
    private boolean dbdload_exists= false;
    private String db_tablenm = "";
    private boolean db_tableexists = false;
    Intent startSenseService;
    Bundle bundle_activepatient;

    ServerInteractionClass myWebService;
    boolean isBound=false;
    ServiceConnection sConn;
    Intent webserviceIntent;
    BroadcastReceiver mMessageReceiver;
    public long individualanimatortime = 120;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestAppPermissions();
        //In the next few lines we tie our variables in code to the objects on the activity
        setContentView(R.layout.activity_main);
        gView = (GraphView) findViewById(R.id.gView);
        btn_run = (Button) findViewById(R.id.btn_run);
        btn_stop = (Button) findViewById(R.id.btn_stop);
        switchmode = (Switch) findViewById(R.id.switchmode);
        Etxt_ID = (EditText) findViewById(R.id.editText_ID);
        Etxt_Age = (EditText) findViewById(R.id.editText_Age);
        Etxt_Name = (EditText) findViewById(R.id.editText_Name);
        Rgrp_Sex = (RadioGroup) findViewById(R.id.RadioGroup_Sex);
        Rbtn_Male = (RadioButton) findViewById(R.id.radioButton_Male);
        Rbtn_Female = (RadioButton) findViewById(R.id.radioButton_Female);

        datacnt = gView.getdatacnt(); //We get the total amount of data points which were
        // generated when the graphview class was instantiated on the form. We could have used the
        // async task to do the initial, but that's just how it was initially designed, to make sure
        // there was some data there.
        gView.showmode = switchmode.isChecked(); //two switch check variables included for checking by threads
        switchchecked = switchmode.isChecked();


        //Creation of Live Feed Animator using AnimatorSet.
        //List of container Value Animators created after and set to sequential play
        //This plays each of them one by one upon animationupdate, created inside createanimatorlist
        //It animates the individuals line by line, then animate the most current line across the
        //length of the line for the appearance of live line movement
        //Data now added under broadcast receiver for sensor data
        animatorSet = new AnimatorSet();
        animatorSet.setStartDelay(400);
        animatorListX = new ArrayList<Animator>();
        animatorListY = new ArrayList<Animator>();
        animatorListZ = new ArrayList<Animator>();


        //Creation of Replay Animator using 1 ValueAnimator.
        //This draws the entire graph from data[0] to data[19] then redraws
        //with data[1] to data[20]. The index is generated based on the animatedvalue received.
        initializegraphanimator(datacnt);

        //These lines initiate collection of data in form fields
        bundle_activepatient = new Bundle();
        bundle_activepatient.putString("PatientName", "");
        bundle_activepatient.putString("PatientID", "");
        bundle_activepatient.putString("PatientAge", "");
        bundle_activepatient.putString("PatientSex", "");
        bundle_activepatient.putString("PatientTableName", "");
        bundle_activepatient.putString("PatientDatabase", DatabaseFileName);

        //This initiates the database so we can pull the data later
        initiateDatabase("active");
        initiateDatabase("dload");
        //This starts the sensor service
        startSenseService = new Intent(MainActivity.this, sensorHandlerClass.class);
        //This sets up communication with the sensor service for receiving updates
        setupBroadcastReceiver();
        //This starts the upload/download service
        startWebConnectService();

    }
    //////////////////////////////////////////////////////////////////////////
    /////////// UPLOAD/DOWNLOAD RELATED CODE

    private void startWebConnectService(){
        //Source: This snippet of code re-used from code provided by Professor
        //and https://developer.android.com/guide/components/bound-services
        webserviceIntent = new Intent(MainActivity.this, ServerInteractionClass.class);
        sConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ServerInteractionClass.LocalBinder binder;
                binder = (ServerInteractionClass.LocalBinder) service;
                myWebService=binder.getService();
                myWebService.DatabaseFileName = DatabaseFileName;
                isBound=true;
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                isBound=false;
            }
        };
        //This creates the binding with the upload service
        bindService(webserviceIntent,sConn, Context.BIND_AUTO_CREATE);
    }

    public void uploadDatabase(View view){
        //This stops the animation and sensor collection then uploads the already received data
        try {
            if (animatorSet.isRunning())
                stopanimation(view);
            isBound = true;
            String rslt = myWebService.UploadOrDownloadMyDatabase("upload");
            recordmessage(rslt, Toast.LENGTH_SHORT);
        }
        catch (Exception e){
            print(e.getMessage());
        }
    }

    public void downloadDatabase(View view){

        //This stops the animation and sensor collection then downloads data for display.
        //It checks if the database was downloaded properly, then finds the point in the animation to show the data.
        try {

            stopanimation(view);
            gView.showmode = false;
            isBound = true;
            int cnt = 0;
            //This detaches the active database before downloading the new one
            if (dbdload_exists) {
                connectDatabasefile("detach");
            }
            Thread.sleep(500);
            String rslt = myWebService.UploadOrDownloadMyDatabase("download");

            if (!rslt.contains("404")) { //This makes sure a database is on the webserver

                recordmessage(rslt, Toast.LENGTH_SHORT);


                if (!dbdload_exists) {
                    initiateDatabase("dload");
                }else{
                    //This reattaches the database
                    connectDatabasefile("attach");
                }

                String PatientSex = Rgrp_Sex.getCheckedRadioButtonId() == Rbtn_Male.getId() ? Rbtn_Male.getText().toString() : Rbtn_Female.getText().toString();
                tbl_name = Etxt_Name.getText().toString() + "_" + Etxt_ID.getText().toString() + "_" + Etxt_Age.getText().toString() + "_" + PatientSex;
                tbl_name = tbl_name.toUpperCase();

                //This gets the data from the downloaded database, updates the arrays and
                // shows the graph of the last 10 points as required
                int lastX = getDBDisplayPoint(tbl_name, DLOAD_DISPLAY_POINT);
                recordmessage("Showing last " + DLOAD_DISPLAY_POINT + " points", Toast.LENGTH_SHORT);
                gView.updatelabels();

                //We use the alternate styling of animation to show the graph
                initializegraphanimator(gView.datalst.datasetdLoad.get(0).size());
                displayanimationfrompoint(lastX);

            }
            else{
                recordmessage("Database Not Found on Web Server", Toast.LENGTH_SHORT);
            }
        }catch (Exception e){
            print(e.getMessage());
        }
    }



    //////////////////////////////////////////////////////////////////////////
    /////////// ANIMATION RELATED CODE

    private void setupBroadcastReceiver(){
        //https://stackoverflow.com/questions/18125241/how-to-get-data-from-service-to-activity
        //We listen for broadcasts from the sensor service with updated data
        //We use a global broadcast, but really should use a local broadcast, so that other apps
        //cannot listen to our data
        mMessageReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                // Get extras data included in the Intent

                Bundle e = intent.getExtras();
                //Array lists not being used since we want the data closer to real time
                //ArrayList<String> Tstamp = intent.getStringArrayListExtra("newTstamp");
                //ArrayList<Float> newdata_X = (ArrayList<Float>) e.get("NewData_X");
                //ArrayList<Float> newdata_Y = (ArrayList<Float>) e.get("NewData_Y");
                //ArrayList<Float> newdata_Z = (ArrayList<Float>) e.get("NewData_Z");
                String Tstamp = (String) e.get("newTstamp");
                float newdata_X = (float) e.get("NewData_X");
                float newdata_Y = (float) e.get("NewData_Y");
                float newdata_Z = (float) e.get("NewData_Z");

                //We call a newly created function in the graphdata class to add this new data
                gView.datalst.addsensordata("active", Tstamp, newdata_X, newdata_Y, newdata_Z);
                //System.out.println("Data Size: "+gView.datalst.sensordata.get(0).size());
                if (switchmode.isChecked()) { //It will always be checked, as the switch is now hidden.
                    //if (gView.Animposition[0] > 20){
                    //    if((gView.datalst.sensordata.get(0).size() - 30 > gView.Animposition[0]) ) {
                            gView.updatelabels(); //This updates the labels
                            addAnimatortoList("running"); //This adds the new data to an animator list for the play sequential part of the animator set
//                        }else{
//                            print("Freezing add to list until needed");
//                        }
//                    }else{
//                        gView.updatelabels(); //This updates the labels
//                        addAnimatortoList("running"); //This adds the new data to an animator list for the play sequential part of the animator set
//                    }
                } else {
                    //This adds the new data to the graphanimator which is a valueanimator
                    PropertyValuesHolder graphval = PropertyValuesHolder.ofInt("", 0, gView.datalst.sensordata.get(0).size());
                    Long currpos = graphAnimator.getCurrentPlayTime();
                    //int currval = graphAnimator.getAnimatedValue();
                    graphAnimator.setValues(graphval);
                    graphAnimator.setDuration(gView.datalst.sensordata.get(0).size() * 200); //this sets the duration to the number of points x 50ms.
                    // so that 400pts would require 20 seconds to complete
                    graphAnimator.setCurrentPlayTime(currpos);


                    if (graphAnimator.isPaused())
                        graphAnimator.resume();
                    else if (!graphAnimator.isStarted())
                        graphAnimator.start();
                }
            }
        };
        //This registers the broadcast receiver for sensor updates
        registerReceiver(
                mMessageReceiver, new IntentFilter("SensorDataUpdate"));
    }

    /*
    public void setgraphMode(View view){

        //This is used to switch mode between live and replay feeds.
        //Once the switch is modified, we cancel the current animator, reset variables and redraw
        //the view
        gView.showmode = switchmode.isChecked();
        switchchecked = switchmode.isChecked();
        if (switchmode.isChecked()) {
            graphAnimator.cancel();
            gView.reset();
            gView.showdata = false;
            gView.invalidate();
            gView.requestLayout();

            graphAnimator.setCurrentPlayTime(0);
        }
        else {
            //SwitchBackPosition = getRunningAnim() + 20; //To be used if user switches back
            animatorSet.cancel();
            gView.reset();
            gView.showdata = false;
            gView.invalidate();
            gView.requestLayout();

            animatorSet.setCurrentPlayTime(0);
        }

        String txtaction = "Switching to " + (switchchecked? "Live Feed": "Replay") + ". Click RUN";
        Toast.makeText(getApplicationContext(),txtaction,Toast.LENGTH_SHORT).show();
    }
    */

    public void runanimation(View view){
        gView.showdata = true; //This turns on the line in the graphview class

        gView.showmode = true;
        //Based on the mode in use (live or replay), we check the state of the required
        //animator, then act on it. ie. to resume or start.
        //We also initiate or re-initiate the async task here for the creation of data in the
        //background. We deliberately cancel the async task while the animation is stopped
        //to save overhead

        //Bundle b = new Bundle();
        //b.putString("phone", phoneNum);
        if(!Environment.getExternalStorageState().toString().equals("mounted")) {
            recordmessage("Unable to store accelerometer data. The database is not mounted", Toast.LENGTH_LONG);
        }
        else {
            if (checkifDifferentPatient()) {
                registerPatient(view);
            }

            registerReceiver(
                    mMessageReceiver, new IntentFilter("SensorDataUpdate"));
            startSenseService.putExtras(bundle_activepatient);
            startService(startSenseService);


            if (switchmode.isChecked()) {
                if (animatorSet.isPaused()) {
                    animatorSet.resume();
//                dataAdder = new AddDataTask();
//                dataAdder.execute();
                } else {
                    if (!animatorSet.isStarted()) {
                        animatorSet.start();
//                    dataAdder = new AddDataTask();
//                    dataAdder.execute();
                    }
                }
            } else {
                if (graphAnimator.isPaused()) {
                    graphAnimator.resume();
//                dataAdder = new AddDataTask();
//                dataAdder.execute();
                } else {
                    //We set the duration of the Replay Feed's ValueAnimator here
                    //based on the number of data points multiplied by 50ms
                    graphAnimator.setDuration(gView.datalst.sensordata.get(0).size() * 200); //gView.datalst.dataset.size() * 50);
                    if (!graphAnimator.isStarted()) {
                        graphAnimator.start();
//                    dataAdder = new AddDataTask();
//                    dataAdder.execute();
                    }

                }
            }

        }
    }

    public void stopanimation(View view){
        gView.showmode = true;
        //This occurs after the stop button is pressed. It STOPS the service and stops and hides the animation.
        //This just determines which animation style is in operation.
        if (switchmode.isChecked()) { //checked by default since the switch is hidden for space
            //if (animatorSet.isStarted() || CheckifRunningAnim()) {
            //if mMessageReceiver.
            try {
                unregisterReceiver(
                        mMessageReceiver);
            } catch(IllegalArgumentException e) {
                //e.printStackTrace();
            }

                animatorSet.pause();
                //animatorSet.cancel();

                gView.showdata = false;
                gView.invalidate();
                gView.requestLayout();

                stopService(startSenseService);

            //}
        }
        else {
            if (graphAnimator.isStarted()) {
                graphAnimator.pause();
                gView.showdata = false;
                gView.invalidate();
                gView.requestLayout();

            }
        }

    }

    /* public void restartanimation(View view){
        //NO Longer used as button is removed
        //This function restarts the animation based on which one is active.
        //It cancels the specific animator object, redraws and sets the playing time back to 0
        //It then starts, whether it was running before or now.
        if (switchmode.isChecked()) {
            animatorSet.cancel();
            gView.invalidate();
            gView.requestLayout();
            gView.reset();
            gView.resetposition();
            //gView.Animposition[linenum] = 0;
            animatorSet.setCurrentPlayTime(0);
            if (!animatorSet.isStarted()) {
                animatorSet.start();

            }
        }
        else {
            graphAnimator.cancel();
            gView.invalidate();
            gView.requestLayout();
            gView.reset();
            //gView.Animposition = 0;
            gView.resetposition();
            graphAnimator.setCurrentPlayTime(0);
            if (!graphAnimator.isStarted()) {
                graphAnimator.start();
            }
        }

    }
*/



    public void displayanimationfrompoint(int newposition){

        //This function restarts the animation based on which one is active.
        //It cancels the specific animator object, redraws and sets the playing time back to 0
        //It then starts, whether it was running before or now.
        /*if (switchmode.isChecked()) {
            animatorSet.cancel();
            gView.invalidate();
            gView.requestLayout();


            gView.reset();
            gView.resetposition(newposition);
            //gView.Animposition[linenum] = 0;
            newposition = newposition < 1? 0 : newposition;
            for (int i = 0; i < 3; i++) {
                gView.activeX[i] = newposition;
                gView.activeY[i] = gView.datalst.sensordata.get(i).get(newposition);
            }
            animatorSet.setCurrentPlayTime(newposition * individualanimatortime);
            if (!animatorSet.isStarted()) {
                animatorSet.start();

            }
        }
        else {*/
            graphAnimator.cancel();
            gView.invalidate();
            gView.requestLayout();
            gView.reset(newposition);
            //gView.Animposition = 0;
            gView.resetposition();
            graphAnimator.setDuration(gView.datalst.datasetdLoad.get(0).size() * individualanimatortime);
            graphAnimator.setCurrentPlayTime(newposition * individualanimatortime);
            if (!graphAnimator.isStarted()) {
                graphAnimator.start();
            }
        //}

    }


    private void initializegraphanimator(int datacntr){
        //This version of graph being used for download part of assignment 2
        graphAnimator = ValueAnimator.ofInt(0, datacntr); //Animated between 0 and datacnt value
        graphAnimator.setInterpolator(null); //No specific animator needed
        graphAnimator.setStartDelay(200);
        graphAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatedValue = (int) animation.getAnimatedValue();
                System.out.println("Anim: "  + animatedValue);
                if (animatedValue + 1 < gView.datalst.datasetdLoad.get(0).size())
                {
                    graphAnimator.pause();
                    animatedValue -= 1;
                }
                gView.firstdatapointondisplay = animatedValue;
                //These are used to redraw the view
                gView.invalidate();
                gView.requestLayout();
            }
        });
    }

    /*private List<Animator> createAnimatorList(){
        //Simple function build the list of animators for the Live Feed
        //It feeds the function with the data from the datapoints that were generated.
        List<Animator> animatorListX = new ArrayList<Animator>();
        List<Animator> animatorListY = new ArrayList<Animator>();
        List<Animator> animatorListZ = new ArrayList<Animator>();
        for (int x = 0; x < gView.datalst.sensordata.get(0).size()-1; x++){
            animatorListX.add(createAnimator(x, x + 1, gView.datalst.sensordata.get(0).get(x), gView.datalst.sensordata.get(0).get(x + 1)));
            animatorListY.add(createAnimator(x, x + 1, gView.datalst.sensordata.get(1).get(x), gView.datalst.sensordata.get(1).get(x + 1)));
            animatorListZ.add(createAnimator(x, x + 1, gView.datalst.sensordata.get(2).get(x), gView.datalst.sensordata.get(2).get(x + 1)));
        }
        return animatorList;
    }*/

    private void addAnimatortoList(String state){
        //Source: https://proandroiddev.com/android-bring-life-to-your-custom-view-8604ab3967b3
        //This updates the Live Feed AnimatorSet list of ValueAnimators on the fly, based on the Async Task's activities.
        //We get the data for the 3 sensor values and set in animator lists
        if (gView.datalst.sensordata.get(0).size() > 1) {

            for (int x = animatorListX.size(); x < gView.datalst.sensordata.get(0).size() - 1; x++) {
                int currlen = animatorListX.size();
                animatorListX.add(createAnimator("LINE-0"+"~NUM:"+(currlen), x, x + 1, gView.datalst.sensordata.get(0).get(x), gView.datalst.sensordata.get(0).get(x + 1)));
                animatorListY.add(createAnimator("LINE-1"+"~NUM:"+(currlen), x, x + 1, gView.datalst.sensordata.get(1).get(x), gView.datalst.sensordata.get(1).get(x + 1)));
                animatorListZ.add(createAnimator("LINE-2"+"~NUM:"+(currlen), x, x + 1, gView.datalst.sensordata.get(2).get(x), gView.datalst.sensordata.get(2).get(x + 1)));
            }
            //This just takes note of the current position and sets it again. Used because we have to
            //set the sequential play again.
            //3 different lists are sent to the animator set
            Long currtime = animatorSet.getCurrentPlayTime();
            animatorSet.cancel();
            animatorSet.playSequentially(animatorListX);
            animatorSet.playSequentially(animatorListY);
            animatorSet.playSequentially(animatorListZ);
            //animatorSet.play((android.animation.Animator) animatorListX).with((android.animation.Animator) animatorListY).with((android.animation.Animator) animatorListZ);
            if (state == "running") {
                if (animatorSet.getTotalDuration() < currtime) {
                    animatorSet.setCurrentPlayTime(0);
                } else {
                    animatorSet.setCurrentPlayTime(currtime);
                }

                animatorSet.start();
            }

            /*animatorListX.get(animatorListX.size()-1).addListener(new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                    System.out.println("Last animation started and animset status: " + animatorSet.isRunning());
                }
                @Override
                public void onAnimationRepeat(Animator animation) {
                    // ...
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    System.out.println("Last animation finished and animset status: " + animatorSet.isRunning());
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    // ...
                }
            });*/
        }
    }

    private ValueAnimator createAnimator(String AnimSet, float startX, float stopX, float startY, float stopY) {
        //Source: https://proandroiddev.com/android-bring-life-to-your-custom-view-8604ab3967b3

        //This creates and animator and sets an updatelistener for it
        //PropertyValuesHolder takes account of all items that need to be animated between
        //The first property values holder is used to self identify the valueanimator at run time
        //using the name AnimSet
        PropertyValuesHolder propertyAnimSet = PropertyValuesHolder.ofInt(AnimSet, 0, 1);
        PropertyValuesHolder propertyX = PropertyValuesHolder.ofFloat(PROPERTY_X, startX, stopX);
        PropertyValuesHolder propertyY = PropertyValuesHolder.ofFloat(PROPERTY_Y, startY, stopY);

        ValueAnimator animator = new ValueAnimator();

        animator.setValues(propertyAnimSet, propertyX, propertyY);
        animator.setDuration(individualanimatortime); //This sets the duration of each of the lines being animated
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                //These get the fractional distance between the start and end of the line, for animation
                //valueAnimator.getValues();
                int LineNum = 999;
                int AnimNum = 999;
                //we use this to determine the currently running anim, should be better than the previous function
                //We had set the identifier as one of the property value holders, but only for that purpose
                for (int anim = 0; anim < valueAnimator.getValues().length; anim++){
                    String AnimName = (String) valueAnimator.getValues()[0].getPropertyName();
                    if (AnimName.contains("LINE-")) {
                        int separatorindex = AnimName.indexOf("~");
                        //AnimSet = (String) valueAnimator.getValues()[0].getPropertyName().replace("ANIM-", "");
                        LineNum = Integer.parseInt(AnimName.substring(0, separatorindex).replace("LINE-",""));
                        AnimNum = Integer.parseInt(AnimName.substring(separatorindex + 5));// - 1; removed to try and fix anim
                        break;
                    }
                }

                float x = (float) valueAnimator.getAnimatedValue(PROPERTY_X);
                float y = (float) valueAnimator.getAnimatedValue(PROPERTY_Y);
                //System.out.println("Anim Value: " + x);

                //This determines the currently running ValueAnimator in the list of animators.
                gView.Animposition[LineNum] = AnimNum;

                //This basically keeps track of the current and last datapoint's X and Y.
                gView.activeX_1[LineNum] = gView.activeX[LineNum];
                gView.activeX[LineNum] = x;
                gView.activeY_1[LineNum] = gView.activeY[LineNum];
                gView.activeY[LineNum] = y;

                //This forces a redraw of the graph
                gView.invalidate();
                gView.requestLayout();



            }
        });

        return animator;
    }

    /*
//No longer used as having an issue identifying if the animatorset itself and children are running, from the isrunning() function
    private int getRunningAnim(){
    //Source: https://proandroiddev.com/android-bring-life-to-your-custom-view-8604ab3967b3
    //This just loops through all the animators in the AnimatorSet's list and finds the active one
    ArrayList<Animator> childAnimations = animatorSet.getChildAnimations();
    for (int i = 0; i < childAnimations.size(); i++) {
        ValueAnimator animator = ((ValueAnimator) childAnimations.get(i));

        //PropertyValuesHolder propX = animator.getValues()[0];
        PropertyValuesHolder propX = animator.getValues()[0];
        if (propX.getPropertyName() == "LINE-0"){
            if (animator.isRunning()){
                return i;
            }
        }

if (childAnimations.get(i).getValues()[0].getPropertyName() == "ANIM-0") {
                if (animator.isRunning()) {
                    return i;
                }
            }

        }
        return -1;
    }

*/

    /*private boolean CheckifRunningAnim(){
        //No longer used, for the same reason as the one above
        //Source: https://proandroiddev.com/android-bring-life-to-your-custom-view-8604ab3967b3
        //This just loops through all the animators in the AnimatorSet's list and finds the active one
        //Not actively used, since animators are creatively labelled with their own identifiers
        ArrayList<Animator> childAnimations = animatorSet.getChildAnimations();
        boolean rslt = false;
        for (int i = 0; i < childAnimations.size(); i++) {
            ValueAnimator animator = ((ValueAnimator) childAnimations.get(i));
                if (animator.isRunning()) {
                    rslt = true;
                    break;
                }
        }
        return rslt;
    }*/

    /*/////////////////////////////////////////////////////////////////////////////
    //This async task section no longer used to gather random data
    // Basic code sampled from instructor's sample code file
    private class AddDataTask extends AsyncTask<String, Long, Void> {
        @Override
        protected void onPreExecute(){
            //Sends an alert upon commencement of data generation
            Toast.makeText(getApplicationContext(),"Data Generation started.",Toast.LENGTH_SHORT).show();
            print("------------- AsyncTask Started");
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                    while (true) {
                        //This function is set to run forever until the async task is stopped (cancelled)
                        //In each loop it sets up new data (100 new pts). Running anim position is sent in case

                        //This checks for the running position based on which animation is running
                        //Then creates new data points (100) if we are within a 1000pt threshold (from running position to total points)
                        gView.datalst.createdataforarray(POINTS_TO_GENERATE, switchchecked? getRunningAnim(): (int) graphAnimator.getAnimatedValue());
                        publishProgress(); //This publishes the data to the main thread
                        Thread.sleep(5000); //Sleep for 5 seconds before creating more data

                }
            } catch (InterruptedException e) {
                //e.printStackTrace();
                print("------------- AsyncTask Cancelled");
            }


            return null;
        }
        @Override
        protected void onProgressUpdate(Long... value){
            boolean freezeupdate = false;
            freezeupdate = switchmode.isChecked()? gView.datalst.dataset.size() - 1000 > getRunningAnim() :gView.datalst.dataset.size() - 1000 > (int) graphAnimator.getAnimatedValue();
            // This is used to check the data being added is within a threshold of 1000 points.
            // It will ensure we do not load too much data too early and use too much memory.
            // In reality, we could check if the newdata is empty, to avoid the extra calls,
            // but that's not the only time we could overtake the threshold (1000)

            if (!freezeupdate) {
                //gView.datalst.adddatatoarray(); //This function adds the prepared data to the dataset
                gView.updatelabels(); //This updates the labels

                if (switchmode.isChecked()) {
                    addAnimatortoList(); //This adds the new data to an animator list for the play sequential part of the animator set
                } else {
                    //This adds the new data to the graphanimator which is a valueanimator
                    PropertyValuesHolder graphval = PropertyValuesHolder.ofInt("", 0, gView.datalst.dataset.size());
                    Long currpos = graphAnimator.getCurrentPlayTime();
                    //int currval = graphAnimator.getAnimatedValue();
                    graphAnimator.setValues(graphval);
                    graphAnimator.setDuration(gView.datalst.dataset.size() * 50); //this sets the duration to the number of points x 50ms.
                    // so that 400pts would require 20 seconds to complete
                    graphAnimator.setCurrentPlayTime(currpos);

                }
            }
            else{
                print("Too much data. Holding until needed.");
            }


            //print("Data Added--------------------------");
        }
        @Override
        protected void onPostExecute(final Void unused){
            //Unused
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            //Runs on stopping the animation via Stop button or Switch
            //The Async Task is basically killed, as it cannot be restarted
            //It is re-initialized later using start or restart button
            Toast.makeText(getApplicationContext(),"Data Generation cancelled.",Toast.LENGTH_SHORT).show();
        }
    }
*/

    ////////////////////////////////////////////////////////////////
    //// PATIENT RELATED CODE

    private boolean checkifDBexists(){
        //This function is used to check if the database was actually replaced
        try {
            final long ONE_MINUTE_IN_MILLIS = 60000;//millisecs
            String envstate = Environment.getExternalStorageState();
            if (envstate.equals("mounted")) {

                    String outFileName = DatabaseFileName; //"CSE535_ASSIGNMENT2";
                    File outFile = new File(this.getExternalFilesDir(null), outFileName);

                    if (outFile.exists() && !outFile.isDirectory()) {
                        Date lastModified = new Date(outFile.lastModified());
                        Date checkDate = new Date(Calendar.getInstance().getTimeInMillis() + (-2*ONE_MINUTE_IN_MILLIS));
                        if (lastModified.after(checkDate)) {
                            print("Recently downloaded database found at: "+outFile.getAbsolutePath());
                            dbdload_exists = true;
                            return true;
                        }
                        else{
                            recordmessage("Outdated database found", Toast.LENGTH_SHORT);
                        }
                    }



            }
            else{
                //Toast.makeText(MainActivity.this, "Sorry, External Storage not found", Toast.LENGTH_LONG).show();
                recordmessage("Sorry, External Storage not found", Toast.LENGTH_LONG);
            }

        }catch ( Exception e   ){
            //Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            recordmessage(e.getMessage(), Toast.LENGTH_SHORT);
            dbdload_exists = false;
            return false;
        }
        return false;
    }

    private void initiateDatabase(String version){
        //Initializes the database with a pointer, if the storage is mounted and the file exists
        try {
            Thread.sleep(500);
            String envstate = Environment.getExternalStorageState();
            if (envstate.equals("mounted")) {
                if (!(version == "active" ? dbactive_exists : dbdload_exists)) {
                    String outFileName = version == "active" ? DatabaseFileName : DatabaseFileName_dload; //"CSE535_ASSIGNMENT2";
                    File outFile = new File(this.getExternalFilesDir(null), outFileName);




                        if (version == "active") {
                            outFile.setWritable(true);
                            dbactive = SQLiteDatabase.openOrCreateDatabase(outFile.getAbsolutePath(), null);
                            dbactive_exists = true;
                        } else {
                            if (outFile.exists() && !outFile.isDirectory()) {
                                print("File Exists");
                                dbdload = SQLiteDatabase.openDatabase(outFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY, null);
                                dbdload_exists = true;
                            }
                            else{

                            }
                        }

                    print("Database stored at: "+outFile.getAbsolutePath());

                }
            }
            else{
                //Toast.makeText(MainActivity.this, "Sorry, External Storage not found", Toast.LENGTH_LONG).show();
                recordmessage("Sorry, External Storage not found", Toast.LENGTH_LONG);
            }

        }catch ( Exception e   ){
            //Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            recordmessage(e.getMessage(), Toast.LENGTH_LONG);
            if (version == "active") {
                dbactive_exists = false;
            } else
                dbdload_exists = false;
        }

    }

    private boolean checkifTableExists(String TableNm){
    //This function checks if the table exists for the patient
        boolean result = false;

        try {

            String query = "select distinct tbl_name from sqlite_master where tbl_name = '"+TableNm+"'" ;
            Cursor chkdb = dbactive.rawQuery(query, null);
            if (chkdb != null) {
                if (chkdb.getCount() > 0) {
                    result = true;
                }
            }
            chkdb.close();
        }
        catch (SQLiteException e) {
            recordmessage(e.getMessage(), Toast.LENGTH_LONG);
        }
        return result;
    }

    private boolean checkifFieldsFilled(){

    if(!Etxt_Name.getText().toString().equals("") &&
        !Etxt_ID.getText().toString().equals("") &&
        !Etxt_Age.getText().toString().equals(""))
    {
        return true;
    }
    else{
        recordmessage("One or more fields need to be filled", Toast.LENGTH_SHORT);
        return false;
    }

    }

    private boolean checkifDifferentPatient(){
        //This just checks if the patient details in the form are the same as registered
        //If not, we will register the patient in another function
        String PatientSex = Rgrp_Sex.getCheckedRadioButtonId() == Rbtn_Male.getId() ? Rbtn_Male.getText().toString() : Rbtn_Female.getText().toString();
        if (
                (!Etxt_Name.getText().toString().equals(bundle_activepatient.getString("PatientName")) ||
                !Etxt_ID.getText().toString().equals(bundle_activepatient.getString("PatientID")) ||
                !Etxt_Age.getText().toString().equals(bundle_activepatient.getString("PatientAge")) ||
                !PatientSex.equals(bundle_activepatient.getString("PatientSex"))
        ) || (Objects.equals(bundle_activepatient.getString("PatientName"), "") &&
                        Objects.equals(bundle_activepatient.getString("PatientID"), "") &&
                        Objects.equals(bundle_activepatient.getString("PatientAge"), "") &&
                        Objects.equals(bundle_activepatient.getString("PatientSex"), "")
        )
        ) {
            return true;
        } else {
            return false;
        }
    }


    public void registerPatient(View view){
        //This registers the patient based on the data in the fields provided.
        //If the user fails to click the register button, it is automatically
        //called when they click the run button
        try {
            if(checkifFieldsFilled()) {

                boolean tbl_found = false;
                if (!Environment.getExternalStorageState().toString().equals("mounted")) {
                    recordmessage("Patient Registration is unavailable at the moment. The database is not mounted", Toast.LENGTH_LONG);
                } else {

                    stopanimation(view);

                    String PatientSex = Rgrp_Sex.getCheckedRadioButtonId() == Rbtn_Male.getId() ? Rbtn_Male.getText().toString() : Rbtn_Female.getText().toString();
                    tbl_name = Etxt_Name.getText().toString() + "_" + Etxt_ID.getText().toString() + "_" + Etxt_Age.getText().toString() + "_" + PatientSex;
                    tbl_name = tbl_name.toUpperCase();
                    if (checkifDifferentPatient()) {
                        //This checks if it is a different/new patient being recorded and if the table exists with that name
                        //If needed, it creates the table and sets the db_tablenm variable to that new patient.
                        if (!checkifTableExists(tbl_name)) {
                            createPatientinDatabase(view, tbl_name);
                            recordmessage("New Patient Registered in DB: " + tbl_name, Toast.LENGTH_SHORT);
                        } else
                            recordmessage("Patient Activated: " + Etxt_Name.getText().toString(), Toast.LENGTH_SHORT);

//                    gView.invalidate();
//                    gView.requestLayout();


                        //animatorSet.setStartDelay(200);

                        //gView.datalst = new GraphData();
                        gView.resetposition();

                        animatorSet = new AnimatorSet();
                        //gView.Animposition[linenum] = 0;
                        animatorSet.setCurrentPlayTime(0);

                        gView.datalst.resetdatalive();

                        gView.updatelabels();
                        gView.showdata = true;
                    }

                    bundle_activepatient.putString("PatientName", Etxt_Name.getText().toString());
                    bundle_activepatient.putString("PatientID", Etxt_ID.getText().toString());
                    bundle_activepatient.putString("PatientAge", Etxt_Age.getText().toString());
                    bundle_activepatient.putString("PatientSex", PatientSex);
                    bundle_activepatient.putString("PatientTableName", tbl_name);
                    bundle_activepatient.putString("PatientDatabase", DatabaseFileName);
                }
            }
        }catch ( Exception e   ){
            recordmessage(e.getMessage(), Toast.LENGTH_LONG);
            //Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();

        }

    }

    public void createPatientinDatabase(View view, String tbl_name){
        //Source:Provided by Instructor in sample code. Modified as needed.
        if (Etxt_Age.getText().toString() == "" || Etxt_ID.getText().toString() == "" || Etxt_Name.getText().toString() == "") {
            Toast.makeText(MainActivity.this, "Please enter values in all fields", Toast.LENGTH_SHORT).show();
        } else {
            try {
                dbactive.beginTransaction();
                try {

                    dbactive.execSQL("create table " + tbl_name.toUpperCase() + " ("
                            //+ " Timestamp timestamp  autoincrement, "
                            + " Timestamp DATETIME PRIMARY KEY, "//" CURRENT_TIMESTAMP, "
                            + " X_value Decimal, "
                            + " Y_value Decimal, "
                            + " Z_value Decimal ); ");

                    dbactive.setTransactionSuccessful(); //commit your changes
                } catch (SQLiteException e) {
                    //report problem
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();

                } finally {
                    dbactive.endTransaction();
                }


                //createRecordinDatabase(0.1F, 0.2F, 0.3F);
            }catch(SQLException e){

                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void connectDatabasefile(String action){
                try {
                    Thread.sleep(500);
                    if (action == "attach"){
                        String exec = "ATTACH DATABASE '"+this.getExternalFilesDir(null) +"/"+ DatabaseFileName_dload+"' as "+DatabaseFileName_dload+" ";
                        //String exec = "ATTACH DATABASE '"+this.getExternalFilesDir(null) +"/"+ DatabaseFileName_dload+".sqlite' as "+DatabaseFileName_dload+" ";
                        dbdload.execSQL(exec);
                        print("Database Attached");
                    }
                    else if (action == "detach") {
                        String exec = "DETACH '"+ DatabaseFileName_dload+"'";
                        dbdload.execSQL(exec);
                        print("Database Detached");
                    }
                } catch (Exception e) {
                print(e.getMessage());
                }
    }

    private void getTableList(){
        //This is just a function to check the tables that exist when the database is downloaded
        //but only for debugging
        try {
            String listoftables = "";
            String query = "select distinct tbl_name from sqlite_master" ;
            Cursor chkdb = dbdload.rawQuery(query, null);
            if (chkdb != null) {
                if (chkdb.getCount() > 0) {
                    chkdb.moveToFirst();
                    for(int i = 0; i < chkdb.getCount(); i++){
                        listoftables = listoftables + ", " + chkdb.getString(0);
                        chkdb.moveToNext();
                    }
                }
            }
            System.out.println("~~~~~~~~~~~~~~~~~~~~~ "+ listoftables);
        }
        catch (SQLiteException e) {
            recordmessage(e.getMessage(), Toast.LENGTH_SHORT);
        }
        catch (Exception e){
            recordmessage(e.getMessage(), Toast.LENGTH_SHORT);
        }

    }

    private int getDBDisplayPoint(String TableNm, int lastX){
        //This function gets the data for the current patient from the database and records it in
        //the dataset array list
        int pointToStart = 0;
        //Cursor pullrecdb = null;
        try {
            getTableList();

            String query = "select * from "+TableNm+" " ;
            //String query = "select distinct tbl_name from sqlite_master";
            Cursor pullrecdb = dbdload.rawQuery(query, null);
            //print("x");
            if (pullrecdb != null) {
                gView.datalst.resetdata();
                if (pullrecdb.getCount() > 0) {
                    pullrecdb.moveToFirst();
                    for(int i = 0; i < pullrecdb.getCount(); i++){

                        gView.datalst.datasetdLoad_Tstmp.add(pullrecdb.getString(pullrecdb.getColumnIndex("Timestamp")));
                        gView.datalst.datasetdLoad_XVal.add(pullrecdb.getFloat(pullrecdb.getColumnIndex("X_value")));
                        gView.datalst.datasetdLoad_YVal.add(pullrecdb.getFloat(pullrecdb.getColumnIndex("Y_value")));
                        gView.datalst.datasetdLoad_ZVal.add(pullrecdb.getFloat(pullrecdb.getColumnIndex("Z_value")));
                        pullrecdb.moveToNext();
                    }
                    gView.datalst.addsensordata("dload", gView.datalst.datasetdLoad_Tstmp, gView.datalst.datasetdLoad_XVal, gView.datalst.datasetdLoad_YVal, gView.datalst.datasetdLoad_ZVal);

                    pointToStart = gView.datalst.datasetdLoad.get(0).size() - lastX + 1;
                    pointToStart = pointToStart < 0 ? 0 : pointToStart;
                }

            }
            pullrecdb.close();

        }
        catch (SQLiteException e) {
            recordmessage(e.getMessage(), Toast.LENGTH_SHORT);
        }
        catch (Exception e){
            recordmessage(e.getMessage(), Toast.LENGTH_SHORT);
        }
        finally {

            return pointToStart;
        }


    }
    ////////////////////////////////////////////////////////////////

    /*private void sendMessageToService(String action) {
        //This is no longer used for the Sensor class. Bundle used instead.
        //Sensor Service is created and destroyed upon run and stop, taking a new bundle each time
        Intent intent = new Intent("ServiceAdvisor");
        // You can also include some extra data.
        intent.putExtra("Action", action);
        sendBroadcast(intent);
    }*/

    private void requestAppPermissions(){
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
    }

    @Override
    protected void onStop() {
        //Called on stopping the activity, to unbind the web service finally
        //unregisterReceiver(mMessageReceiver);
        //dbactive.close();
        //dbdload.close();
        //stopService(startSenseService);
        /*if (isBound) {
            unbindService(sConn);
            isBound = false;
        }*/

        super.onStop();
    }

    private void recordmessage(String err, int ErrLen){
        Toast.makeText(MainActivity.this, err, ErrLen).show();
        print(err);
    }

    public void print(String msg){
        //Just included for code appearance and less typing for debugging :-D
        System.out.println(msg);
    }
}

