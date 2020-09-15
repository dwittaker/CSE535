package com.example.DWittaker;

/**
 * Created by jlee375 on 2016-02-03.
 */
import android.animation.PropertyValuesHolder;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class sensorHandlerClass extends Service implements SensorEventListener{

    private SensorManager accelManage;
    private Sensor senseAccel;
    ArrayList<String>  accelTstamp = new ArrayList<String>();
    ArrayList<Float> accelValuesX = new ArrayList<Float>() ;
    ArrayList<Float> accelValuesY = new ArrayList<Float>() ;
    ArrayList<Float> accelValuesZ = new ArrayList<Float> ();
    int index = 0;
    int k=0;
    Bundle b;
    SQLiteDatabase db;
    private boolean db_exists = false;
    private Date lastsensordate = Calendar.getInstance().getTime();
    private int samplingPeriod = 100000; //frequency of accelerometer collection in microseconds
    private int insertDBInterval = 20; //amount of data collected (records) before batch insertion into database

    private String currentDate() {
        //Used to get the format for date timestamp stored in the database
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        //We monitor the sensor for interval changes. We send them immediately to MainActivity,
        //but wait a few intervals before inserting in the database
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if ((Calendar.getInstance().getTime().getTime() - lastsensordate.getTime())/100 >= 1) { //calculation initially used to moderate data receival.
                accelTstamp.add(currentDate());
                accelValuesX.add(sensorEvent.values[0]);
                accelValuesY.add(sensorEvent.values[1]);
                accelValuesZ.add(sensorEvent.values[2]);
                lastsensordate = Calendar.getInstance().getTime();

                //We immediately send the data to the main activity for showing on the UI
                sendMessageToActivity(currentDate(), sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
                //System.out.println("Accel rec " + sensorEvent.values[0] + " " + sensorEvent.values[1] + " " + sensorEvent.values[2]);
                //We gather the data and wait until a specified interval to insert the data into the database
                if (index == insertDBInterval) { //Insert frequency
                    index = 0;
                    accelManage.unregisterListener(this);
                    //If the patient data bundle still exists, we record the data in the database
                    //This is used in the event the accelerometer collection is triggered in the split second after the bundle is killed, upon stopping the service
                    if (b.getString("PatientTableName") != null && b.getString("PatientTableName") != "") {
                        recordAccelData(b.getString("PatientTableName"), accelTstamp, accelValuesX, accelValuesY, accelValuesZ);
                        //after recording in DB, we clear the array lists and start again
                        accelTstamp.clear();
                        accelValuesX.clear();
                        accelValuesY.clear();
                        accelValuesZ.clear();
                    }
                    else
                    {
                        System.out.println("Patient Table Bundle does not exist");
                    }
                        accelManage.registerListener(this, senseAccel, samplingPeriod);//SensorManager.SENSOR_DELAY_NORMAL);
                }
                else
                    index++;
            }
        }
    }

    private void initiateDatabase(){
        //We maintain a separation DB pointer for the service so it can independently interact with the DB
        try {
            String envstate = Environment.getExternalStorageState();
            if (envstate.equals("mounted")) {
                if (!db_exists) {
                    String outFileName = b.getString("PatientDatabase");//"CSE535_ASSIGNMENT2";
                    File outFile = new File(this.getExternalFilesDir(null), outFileName);

                    if (outFile.exists() && !outFile.isDirectory()) {
                        print("File Exists");
                    }
                    outFile.setWritable(true);
                    db = SQLiteDatabase.openOrCreateDatabase(outFile.getAbsolutePath(), null);
                    db_exists = true;
                }
            }
            else{
                recordmessage("Sorry, External Storage not found", Toast.LENGTH_LONG);
            }

        }catch ( Exception e   ){
            recordmessage(e.getMessage(), Toast.LENGTH_LONG);
            db_exists = false;
        }

    }

    private void recordAccelData(String tbl_name,  ArrayList<String> Tstamp, ArrayList<Float> X_Val, ArrayList<Float> Y_Val, ArrayList<Float> Z_Val){
                    try {
                        int numTrans = X_Val.size();
                        //Source: https://medium.com/@JasonWyatt/squeezing-performance-from-sqlite-insertions-971aff98eef2
                        //We formulate a DB ExecSQL query using valuesbuilder and insert the data in batch
                        if (db != null) {
                            db.beginTransaction();
                            Object[] values = new Object[numTrans];
                            StringBuilder valuesBuilder = new StringBuilder();
                            for (int i = 0; i < numTrans; i++) {
                                if (i != 0) {
                                    valuesBuilder.append(", ");
                                }
                                values[i] = "('" + Tstamp.get(i) + "'," + X_Val.get(i) + ", " + Y_Val.get(i) + "," + Z_Val.get(i) + ")";
                                valuesBuilder.append(values[i]);
                            }
                            db.execSQL(
                                    "INSERT INTO " + tbl_name + " VALUES " + valuesBuilder.toString()
                            );
                            db.setTransactionSuccessful();
                            //db.endTransaction();
                        }
                    }
                    catch (SQLiteException e) {
                        recordmessage(e.getMessage(), Toast.LENGTH_SHORT);

                    }
                    finally {
                        db.endTransaction();
                    }



    }

    //source: https://stackoverflow.com/questions/18125241/how-to-get-data-from-service-to-activity
    //This allows me to send data from sensor to the main activity
    private void sendMessageToActivity(String Tstamp, float X_Val, float Y_Val, float Z_Val) {
        Intent intent = new Intent("SensorDataUpdate");
        intent.putExtra("newTstamp", Tstamp);
        intent.putExtra("NewData_X", X_Val);
        intent.putExtra("NewData_Y", Y_Val);
        intent.putExtra("NewData_Z", Z_Val);
        sendBroadcast(intent);
        //We use the broadcast receiver inside the MainActivity to send data for the animation
    }

    //This version may not be used after all, since the individual floats are working sufficiently well
    private void sendMessageToActivity(ArrayList<String> Tstamp, ArrayList<Float> X_Val, ArrayList<Float> Y_Val, ArrayList<Float> Z_Val) {
        Intent intent = new Intent("SensorDataUpdate");
        intent.putExtra("newTstamp", Tstamp);
        intent.putExtra("NewData_X", X_Val);
        intent.putExtra("NewData_Y", Y_Val);
        intent.putExtra("NewData_Z", Z_Val);
        sendBroadcast(intent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCreate(){
        //This allows us to connect to the accelerometer
        accelManage = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senseAccel = accelManage.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onDestroy() {
        //We unregister the listener and close the database connection for the accelerometer
        //just before the service is destroyed
        accelManage.unregisterListener(this);
        //db.close();

        recordmessage("Accelerometer Collection Stopped.", Toast.LENGTH_SHORT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Upon start of the service, we pull certain fields from the main activity
        //including the patient details.
        b = intent.getExtras();
        //Service initialized in ONCreate and started immediately, after the database is initialized.
        initiateDatabase();
        //This allows us to connect to the accelerometer and gather data
        startit();
        Toast.makeText(sensorHandlerClass.this, "Accelerometer Collection Started", Toast.LENGTH_SHORT).show();
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //This is an unbound service so we do not need a binder
        // TODO Auto-generated method stub
        return null;
        //return mMessenger.getBinder();
    }

    private void startit(){
        accelManage.registerListener(this, senseAccel, samplingPeriod);
        //This actually enables collection of accelerometer data.
        //Sampling period is set to closely follow data collection period specified - 10 samples per second
    }

    public void print(String msg){
        //Just included for code appearance and less typing for debugging :-D
        System.out.println(msg);
    }

    private void recordmessage(String err, int ErrLen){
        Toast.makeText(this, err, ErrLen).show();
        print(err);
    }



}
