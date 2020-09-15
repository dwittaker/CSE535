package com.example.DWittaker;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.view.View;
import android.util.AttributeSet;

import java.util.Arrays;
import java.util.Locale;

/**
 * Source: Provided by instructor and modified as needed
 * GraphView creates a scaled line or bar graph with x and y axis labels.
 * @author Arno den Hond
 *
 */
public class GraphView extends View {

    GraphData datalst = new GraphData(); //This object creates the data
    public static boolean LINE = true;
    private int datacnt = 20; //This is the total number of data points initially created.
    private int maxvert = 13;
    private int minvert = -13;
    private Paint paint;
    private String[] horlabels;
    private String[] verlabels;
    private String title;
    public int firstdatapointondisplay = 0; //The graph should start with this data point
    private int maxdatapointondisplay = 20; //The graph should only show 20 data points
    //This is used in the draw functions to limit the number of data points on the chart
    public int totaldatapointondisplay = maxdatapointondisplay;
    //This is critical for the live feed as it is used to do mod calculations for the labels
    private int datapointlimit = maxdatapointondisplay - 1;
    public float lasth = 0; //For replay feed (we track the last Y value). This was based on instructor provided code.
    public float[] lastharr = new float[3]; //For replay feed (we track the last Y value). This was based on instructor provided code.
    public boolean showdata = true;
    //public int Animposition = -1;
    //We track the graph locations of the previous/next and current datapoints for drawing
//    public float activeX_1 = 0;
//    public float activeY_1 = 0;
//    public float activeX = 0;
//    public float activeY = 0;
    public int[] Animposition = new int[3];


    public float[] activeX_1 = new float[3];
    public float[] activeY_1 = new float[3];
    public float[] activeX = new float[3];
    public float[] activeY = new float[3];
    public boolean showmode = true; //This is a local tracker for monitoring the switch state

    //public GraphView(){}

    public GraphView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        resetposition();
        //Initial set of data created upon instantiation of the app
        //Label data also pulled from the variables created in the graphdata object.
        //datalst.getvaluearray(datacnt);
        updatelabels();
        title = "Graph - Duane Wittaker Heart Monitor UI";
        paint = new Paint();
    }
    //This is not used
    public GraphView(Context context, float[] values, String title, String[] horlabels, String[] verlabels, boolean type)
    {
        super(context);
        resetposition();
        if (title == null)
            title = "";
        else
            this.title = title;
        if (horlabels == null)
            this.horlabels = new String[0];
        else
            this.horlabels = horlabels;
        if (verlabels == null)
            this.verlabels = new String[0];
        else
            this.verlabels = verlabels;
        paint = new Paint();
    }

    public void reset(int... position){
        //Used to reset certain variables used for drawing, upon stop/restart of the animation
        int newpos = position.length > 0 ? position[0] : 0;
        totaldatapointondisplay = maxdatapointondisplay;
        firstdatapointondisplay = newpos;
        lasth = 0;
        showdata = true;
    }

    public void resetposition(int... position){
        int newpos = position.length > 0 ? position[0] : -1;
        for (int y = 0; y < Animposition.length; y++)
            Animposition[y] = newpos;
    }

    public void updatelabels(){
        //Used to update the labels in this object (used for animation with those that were done in
        //the graph data object.
        this.horlabels = datalst.horlbl;
        this.verlabels = datalst.verlbl;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //This function is drawn upon initiation of the activity form
        //and every time we invalidate and request layout
        //i.e. upon animator update.
        if (showmode) {
            //This draws the live feed version of the animation. The drawgraphbox is called inside
            //drawlive(canvas);
            drawaccel(canvas);
        }
        else
        {
            //This draws the replay version of the animation. The drawgraphbox draws the graph
            //grid and labels
            drawgraphbox(canvas,firstdatapointondisplay);
            //This is a tweaked version of the function provided by the instructor
            drawreplay(canvas, firstdatapointondisplay, true);
        }
    }

    public void drawreplay(Canvas canvas, Integer startframe, boolean showline){
        //Original source: Above. Provided by instructor and modified as needed

        if (datalst.datasetdLoad.get(0).size() >= startframe + 1){

            float border = 60;
            float horstart = border * 2;
            float height = getHeight();
            float width = getWidth() - 1;
            if (totaldatapointondisplay > horlabels.length)
                totaldatapointondisplay = horlabels.length;

            float max = getMax();
            float min = getMin();
            float diff = max - min;
            float graphheight = height - (2 * border);
            float graphwidth = width - (2 * border);

            float datalength = totaldatapointondisplay;
            float colwidth = (width - (2 * border)) / datalength;
            float halfcol = colwidth;
            int hors = totaldatapointondisplay - 1;

            //Based on the totaldatapointondisplay variable, it loops and draws all the lines within
            //range of the startframe, therefore filling out the graph.
            //On the next iteration of the Valueanimator (called 'graphanimator' in mainactivity)
            //the startframe value is 1 higher, therefore giving the impression of animation
            for (int i = 0; i < datalst.datasetdLoad.get(0).size() - startframe; i++) {
                //paint.setColor(Color.DKGRAY);

                //This calculates the position of the data at the index i, to be drawn based on size of the graph etc.
                float x = ((graphwidth / hors) * i) + horstart;
                float x_1 = horstart;
                if (i > 0)
                    x_1 = ((graphwidth / hors) * (i-1)) + horstart;


                if (max != min){

                    //This cycles through the dataset and draws the 3 accelerometer lines after download
                    float[] val = new float[3];
                    for (int a = 0; a < val.length; a++) {
                        paint.setColor(setPaint(a));

                        //This basically checks the data conditions and then determines the best value to use
                        if (!(startframe <= horlabels.length - 1 && i <= (horlabels.length - 1 - startframe))) {
                            val[a] = 0 - min;
                        } else {
                            if ((startframe + i) >= (horlabels.length - 1)) {
                                val[a] = 0 - min;
                            } else {
                                val[a] = datalst.datasetdLoad.get(a).get(startframe + i) - min;//datalst.dataset.get(startframe + i) - min;
                            }
                        }

                        //This calculates the Y value based on the height etc. of the view etc.
                        float rat = val[a] / diff;
                        float h = graphheight * rat;

                        //paint.setColor(Color.WHITE);
                        paint.setStrokeWidth(6.0f);

                        //if (showdata) //uncomment these 2 lines to show the data values on the graph
                        //canvas.drawText(String.format(Locale.getDefault(), "%.0f", val + min ), x, (border - h) + graphheight, paint);

                        //If we should be showing the data, we draw the line of the graph
                        //from the start of the line until the end of the line.
                        //A line is formed between the X and Y of two sequential datapoints
                        //The X is derived from the index in the Arraylist and
                        //The Y is derived from the value of the data point in the Arraylist.
                        if (i > 0 && showdata && showline)
                            canvas.drawLine(x_1, (border - lastharr[a]) + graphheight, x, (border - h) + graphheight, paint);
                        lastharr[a] = h; //This is set for use in the next iteration of the loop
                    }
                }
            }
            firstdatapointondisplay = firstdatapointondisplay + 1;
        }
    }

    private float getMax() {
        //Source: As above. provided by instructor
        float largest = Integer.MIN_VALUE;
//        for (int i = 0; i < datalst.dataset.size(); i++)
//            if (datalst.dataset.get(i) > largest)
//                largest = datalst.dataset.get(i);
        //This function was being used to determine max value of the data points generated, but unused
        largest = maxvert;
        return largest;
    }

    private float getMin() {
        //Source: As above. provided by instructor
        //Used to check the smallest value in the dataset
        float smallest = Integer.MAX_VALUE;
//        for (int i = 0; i < datalst.dataset.size(); i++)
//            if (datalst.dataset.get(i) < smallest)
//                smallest = datalst.dataset.get(i);

        smallest = minvert;
        return smallest;
    }




    public void print(String msg){
        //Just a lazy man's function to print text for debugging
        System.out.println(msg);
    }

    public int getdatacnt(){
        //Just returning the number of data points
        //Was used before I got tired of doing so many functions
        return datacnt;
    }



    /*public void drawlive(Canvas canvas, int i, boolean b, int i1, int i2, int linenum){
        //This function is used to draw the animation in Live mode.

        paint.setStrokeWidth(6.0f);
        int runposition = Animposition % datapointlimit; //Was used to keep labels in datapointlimit
        // range


        drawgraphbox(canvas, Animposition); //Used to draw the box, grid and labels.

        //This draws all stationary lines already drawn at any frame in the animation.
        //It draws the entire set of lines before the current animposition. Animposition
        //is the currently active valueanimator in the animatorSet
        for (int i = (Animposition - Animposition % datapointlimit); i < Animposition; i++){
            drawlive(canvas, i, false, i);
        }

        //This then draws the line at Animposition (the current valueanimator), using the
        //valueanimator data in the next drawlive, to animate the drawing of the line
        if (Animposition > -1) {
            drawlive(canvas, Animposition, true, runposition);
        }
    }*/

    private int setPaint(int linenum){
        int rslt;
        switch(linenum) {
            case 0:
                rslt = Color.RED;
                break;
            case 1:
                rslt = Color.YELLOW;
                break;
            case 2:
                rslt = Color.GREEN;
                break;
            default:
                rslt = Color.WHITE;
            break;

        }
        return rslt;
    }

    public void drawaccel(Canvas canvas){
        //This function is used to draw the animation in Live mode.
        //It uses data original sourced from the accelerometer to draw the 3 lines

        paint.setStrokeWidth(6.0f);
        //paint.setStrokeWidth(6.0f);

        // range

        //This draws the background and other items before drawing the lines
        //System.out.println("Position: " + Animposition[0]);
        drawgraphbox(canvas, Animposition[0]); //Used to draw the box, grid and labels.

        for (int linenum = 0; linenum < 3; linenum++){
            int runposition = Animposition[linenum] % datapointlimit; //Was used to keep labels in datapointlimit
            //This draws all stationary lines already drawn at any frame in the animation.
            //It draws the entire set of lines before the current animposition. Animposition
            //is the currently active valueanimator in the animatorSet
            for (int i = (Animposition[linenum] - Animposition[linenum] % datapointlimit); i < Animposition[linenum]; i++){
            //for (int i = 0; i < (Animposition[linenum] ) % datapointlimit; i++){
                drawaccel(canvas, i, false, i, linenum);

            }

            //This then draws the line at Animposition (the current valueanimator), using the
            //valueanimator data in the next drawlive, to animate the drawing of the line
            //print("XXXXXXX Anim Position: " +Animposition[linenum]);
            if (Animposition[linenum] > -1) {
                drawaccel(canvas, Animposition[linenum], true, runposition, linenum);
            }
        }
    }

    public void drawaccel(Canvas canvas, int startframe, boolean anim, int chartposition, int linenum){
        //This version of the drawlive function determines if the content to be drawn should be
        //animated or not. In this mode, if the line is to be drawn for the 9th to 10th point
        //the 1st to 8th are drawn as static lines all at once via anim=false
        //the 9th to 10th line is animated via anim=true
        float datastartX = startframe;
        float datastartY = this.datalst.sensordata.get(linenum).get(startframe);

        CoordData drawcoords = new CoordData();
        paint.setColor(setPaint(linenum));
        if (anim){
            //paint.setColor(setPaint(linenum));
            //To animate the drawing of this most current line, we get the activeX and activeY values
            //from the activate valueanimator in the animatorSet, for the end points of the line.
            //The start points were already taken above.
            float datastopX = this.activeX[linenum];
            float datastopY = this.activeY[linenum];
            //System.out.println("-------- Animated Active X "+ datastartX + " - " + datastopX);

            //Work out the actual coordinates of the line on the view, given the position,
            //size etc. of the view on the activity
            drawcoords = determinegraphcoords(chartposition, datastartX, datastartY, datastopX, datastopY);
        }
        else
        {


            float datastopX = 0;
            float datastopY = 0;

            //To draw the previous lines (1-8), we just get the data from the dataset and use that
            //for the line end points. Start points were taken earlier.
            if (startframe < datalst.sensordata.get(linenum).size()) {
                datastopX = startframe + 1; //%10 + 1;
                datastopY = datalst.sensordata.get(linenum).get(startframe + 1);
            }
            else{
                //This only happens if we are at the last point in the dataset. Not sure if it is ever called
                datastopX = datastartX;
                datastopY = datastartY;
            }

            //System.out.println("-------- Static Active X "+ datastartX + " - " + datastopX);
            //Work out the actual coordinates of the line on the view, given the position,
            //size etc. of the view on the activity
            drawcoords = determinegraphcoords(chartposition, datastartX, datastartY, datastopX, datastopY);
        }

        //If we are to show data (meaning the chart is not stopped), we draw the actual line.
        if (this.showdata)
            drawaccel(canvas, drawcoords.coordStartX, drawcoords.coordStartY, drawcoords.coordStopX, drawcoords.coordStopY);

    }

    public void drawaccel(Canvas canvas, float drawstartX, float drawstartY, float drawstopX, float drawstopY){
        //This just draws the actual line.
        canvas.drawLine(drawstartX, drawstartY, drawstopX, drawstopY, paint);
    }

    private void drawgraphbox(Canvas canvas, int startFrame){
        //Original Source: Above. Provided by instructor and modified
        //This function draws the actual graph grid/box and labels.

        float border = 60;
        float horstart = border * 2;
        float height = getHeight();
        float width = getWidth() - 1;

        float max = getMax();
        float min = getMin();
        float diff = max - min;
        float graphheight = height - (2 * border);
        float graphwidth = width - (2 * border);
        float datalength = totaldatapointondisplay;
        float colwidth = (width - (2 * border)) / datalength;
        int hors = totaldatapointondisplay - 1;

        paint.setTextAlign(Align.LEFT);

        //Draws the vertical labels
        int vers = verlabels.length - 1;
        for (int i = vers; i >= 0; --i) {

            paint.setColor(Color.DKGRAY);
            float y = ((graphheight / vers) * i) + border;
            canvas.drawLine(horstart, y, width, y, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(30);
            canvas.drawText(verlabels[vers-i], 0 + 10, y, paint);
        }
        //Draws the horizontal labels
        for (int i = 0; i < totaldatapointondisplay; i++) {

            paint.setColor(Color.DKGRAY);
            float x = ((graphwidth / hors) * i) + horstart;
            canvas.drawLine(x, height - border, x, border, paint);
            paint.setTextAlign(Align.CENTER);
            if (i == totaldatapointondisplay - 1)
                paint.setTextAlign(Align.RIGHT);
            if (i == 0)
                paint.setTextAlign(Align.LEFT);
            paint.setColor(Color.WHITE);
            paint.setTextSize(30);
            int dataptlabel = 0;
            if (showmode) {
                dataptlabel = i + (startFrame - startFrame % datapointlimit);
            }
            else{
                dataptlabel = i + startFrame;
            }
            if (i%2 == 0) //This skips out every other label
               canvas.drawText(String.format(Locale.getDefault(), "%d", dataptlabel), x, height -5, paint);

        }
            //Title unneeded
            //paint.setTextAlign(Align.CENTER);
            //canvas.drawText(title, (graphwidth / 2) + horstart, border - 4, paint);

        }

    public CoordData determinegraphcoords(int chartposition, float datastartX, float datastartY, float datastopX, float datastopY){
        //Original Source: Above. Provided by instructor and modified
        //This function calculates the coordinates for the X and Y values received from the dataset.
        //It does this based on the position, size etc of the GraphView object etc.
        //Used for the live feed only
        CoordData newcoords = new CoordData();

        float border = 60;
        float horstart = border * 2;
        float height = getHeight();
        float width = getWidth() - 1;

        float max = getMax();
        float min = getMin();
        float diff = max - min;
        float graphheight = height - (2 * border);
        float graphwidth = width - (2 * border);
        float datalength = totaldatapointondisplay;
        float colwidth = (width - (2 * border)) / datalength;
        int hors = totaldatapointondisplay - 1;



        float val_1 = datastartY - min;
        float rat_1 = val_1 / diff;
        float h_1 = graphheight * rat_1;

        float val = datastopY - min;
        float rat = val / diff;
        float h = graphheight * rat;

        //Adjusted values created for live feed
        float adjustedStartX =  datastartX%datapointlimit;
        float adjustedStopX = datastopX%datapointlimit==0? datastartX%datapointlimit==0? 0: datapointlimit : datastopX%datapointlimit;

        newcoords.coordStartX = datastartX > 0 ? ((graphwidth / hors) * (adjustedStartX)) + horstart : horstart;
        newcoords.coordStartY = (border - h_1) + graphheight;
        newcoords.coordStopX = ((graphwidth / hors) * (adjustedStopX)) + horstart; //last thing was to put in chartposition
        newcoords.coordStopY = (border - h) + graphheight;

        return newcoords;
    }



}