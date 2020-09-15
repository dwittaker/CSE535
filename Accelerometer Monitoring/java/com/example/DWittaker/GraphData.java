package com.example.DWittaker;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class GraphData {

    ArrayList<ArrayList<Float>> sensordata = new ArrayList<>(3);
    ArrayList<ArrayList<Float>> datasetdLoad = new ArrayList<>(3);

    ArrayList<String> sensordata_Tstmp = new ArrayList<>(0);
    ArrayList<Float> sensordata_XVal = new ArrayList<>(0);
    ArrayList<Float> sensordata_YVal = new ArrayList<>(0);
    ArrayList<Float> sensordata_ZVal = new ArrayList<>(0);

    ArrayList<String> datasetdLoad_Tstmp = new ArrayList<>(0);
    ArrayList<Float> datasetdLoad_XVal = new ArrayList<>(0);
    ArrayList<Float> datasetdLoad_YVal = new ArrayList<>(0);
    ArrayList<Float> datasetdLoad_ZVal = new ArrayList<>(0);
    ArrayList<Float> dataset; //ArrayList used since it allows 'add' and other simple functions
    //List of horizontal and vertical labels
    String[] horlbl;
    String[] verlbl;
    private int maxvert = 16;
    private int minvert = -16;
    //private int MAX_VERTLBL = 3000; //This is used for the maximum vertical label

    //This is used to smooth (via multiplier) the randomized data into something that looks like heart beats
    private float[] multiplier = {0.1F,0.1F,0.2F,1.0F,0.2F,0.1F,0.5F,0.2F,0.1F,0.1F};
    private int multcnt = 0;
    //These new variables are used to store the data generated in the async task before it is needed (e.g. 5 secs later)
    private ArrayList<Float> newdata;
    private String[] newhorlbl;
    private String[] newverlbl;


    GraphData() //Instantiate the class
    {
        dataset = new ArrayList<>();
        newdata = new ArrayList<>();
        newhorlbl = new String[]{};
        newverlbl = new String[]{};
        verlbl = createvlabels(minvert, maxvert);
        horlbl = createhlabels(20);

        for (int i=0; i < 3; i++) {
            sensordata.add(new ArrayList());
            datasetdLoad.add(new ArrayList());
        }
    }

    public void getvaluearray(int total){
        //Used to create the data points at  the start of the app
        multcnt = 0;
        for (int i=0; i< total; i++){
            if (multcnt == multiplier.length)
                multcnt = 0;
            if (i == 0)
                newdata.add(0F);
            else
                //Multiplier used to smooth the data to look like heart beat data
                //get1rand used to generate the random value
                newdata.add(get1rand()*multiplier[multcnt]);
            ++multcnt;
        }
        //Data is immediately added to the dataset, labels are created
        this.dataset.addAll(newdata);
        horlbl = createhlabels(dataset.size());

        this.newdata.clear();
        System.out.println("Total Data:" + dataset.size());
    }

    public void createdataforarray(int total, int pos){
        //Used to create the data points based on async task.
        //Data is stored but not used or cleared in this function.
        //Get1rand and multiplier function is the same as above
        //The starting condition checks if we need to generate new data
        //ie. whether the stored new data is less than the expected amount or if we have generated
        //more data than required (1000 points more than the current position).

        //It is called in the doInBackground function of the Async Task
        if (newdata.size() < total && dataset.size() - pos < 1000) {
            multcnt = 0;
            for (int i = 0; i < total; i++) {
                if (multcnt == multiplier.length)
                    multcnt = 0;
                float valadd = get1rand() * multiplier[multcnt];
                newdata.add(valadd);
                ++multcnt;
            }
            newhorlbl = createhlabels(dataset.size() + newdata.size());
            //newverlbl = createvlabels(MAX_VERTLBL); //getMaxValue()); //Max function no longer used, for the sake of performance
        }

    }

    /*public void adddatatoarray(){
        //This function actually pushes the stored new data into the datasets (data and labels)
        //that are being used live. It then clears them.
        //It is called in the onprogressupdate function of the async task.
        this.dataset.addAll(newdata);
        System.out.println("------------- Total Data Size now at:" + this.dataset.size());
        this.horlbl = this.newhorlbl;
        this.verlbl = this.newverlbl;

        newdata.clear();
        this.newhorlbl = new String[]{};
        this.newverlbl = new String[]{};
    }*/

    public void resetdata(){
        datasetdLoad_Tstmp.clear();
        datasetdLoad_XVal.clear();
        datasetdLoad_YVal.clear();
        datasetdLoad_ZVal.clear();

//        sensordata_Tstmp.clear();
//        sensordata_XVal.clear();
//        sensordata_YVal.clear();
//        sensordata_ZVal.clear();


//        sensordata.get(0).clear();
//        sensordata.get(1).clear();
//        sensordata.get(2).clear();
        datasetdLoad.get(0).clear();
        datasetdLoad.get(1).clear();
        datasetdLoad.get(2).clear();
    }

    public void resetdatalive(){

        sensordata_Tstmp.clear();
        sensordata_XVal.clear();
        sensordata_YVal.clear();
        sensordata_ZVal.clear();


        sensordata.get(0).clear();
        sensordata.get(1).clear();
        sensordata.get(2).clear();
    }



    //This is used to add data to the dataset
    public void addsensordata(String version, ArrayList<String> Tstamp, ArrayList<Float> X_Val, ArrayList<Float> Y_Val, ArrayList<Float> Z_Val){
        if (version == "active") {
            sensordata_Tstmp.addAll(Tstamp);
            sensordata.get(0).addAll(X_Val);
            sensordata.get(1).addAll(Y_Val);
            sensordata.get(2).addAll(Z_Val);
            horlbl = createhlabels(sensordata.get(0).size());//+ newdata.size());
        }
        else
        {
            //sensordata_Tstmp.addAll(Tstamp);
            datasetdLoad.get(0).addAll(X_Val);
            datasetdLoad.get(1).addAll(Y_Val);
            datasetdLoad.get(2).addAll(Z_Val);
            horlbl = createhlabels(datasetdLoad.get(0).size());//+ newdata.size());
        }
    }
    //This is used to add data to the dataset
    public void addsensordata(String version, String Tstamp, float X_Val, float Y_Val, float Z_Val){
        sensordata_Tstmp.add(Tstamp);
        sensordata.get(0).add( X_Val);
        sensordata.get(1).add( Y_Val);
        sensordata.get(2).add( Z_Val);
        horlbl = createhlabels(sensordata.get(0).size());//+ newdata.size());
    }


    private float get1rand(){
        //Very simple function to generate random numbers between min and max.
        //The numbers are rounded additionally for performance savings. In my mind at least.
        Random rand = new Random();
        int min = 0;
        int max = 3000;

        return (float) (Math.round(rand.nextFloat() * 100.0)/100.0) * max + min;

    }

    private String[] createhlabels(int valuelstlen){
        //source: https://proandroiddev.com/android-bring-life-to-your-custom-view-8604ab3967b3
        //This function generates the horizontal labels based on the length of the data list
        int interval = 1;
        int ticks = valuelstlen / interval;
        String[] hlist = new String[ticks+1];
        for(int i = 0; i<= ticks; i++){
            hlist[i] = String.format(Locale.getDefault(),"%d", i*interval);
        }
        return hlist;
    }

    private String[] createvlabels(int minVal, int maxVal){
        //source: https://proandroiddev.com/android-bring-life-to-your-custom-view-8604ab3967b3
        //This generates the vertical labels based on the maximum value provided.
        //We no longer use the max and min value of the data since we generate a specific range
        //of data
        int ticks = 4;
        String[] vlist = new String[ticks+1];
        //float highestnumber = maxVal; //maxVal //getMaxValue(valuelist);
        int interval = (int) (maxVal - minVal) / ticks;
        for(int i = 0; i<= ticks; i++){
            vlist[i] = String.format(Locale.getDefault(),"%d", minVal + i*interval);
        }
        return vlist;
    }

    private int getMaxValue() { //Unused for the sake of performance. Hard 3000 will be set
        //Just finds the highest value in the dataset
        float maxnum = -9999999;

        /*for(int x = 0; x < dataset.size() - 1; x++){
            if (maxnum < dataset.get(x)){
                maxnum = dataset.get(x);
            }
        }*/
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < sensordata.get(x).size(); y++) {
                if (maxnum < sensordata.get(x).get(y)) {
                    maxnum = sensordata.get(x).get(y);
                }
            }
        }
        return Math.round(maxnum);
    }


}
