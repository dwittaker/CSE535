package com.example.DWittaker;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

public class ServerInteractionClass extends Service {
    private Binder binder = new LocalBinder();
    Thread uploadthread; //These two threads used to actually do the upload and download, as required by the API level
    Thread downloadthread; //These two threads used to actually do the upload and download, as required by the API level
    String DatabaseFileName;
    public String webstatus = "";

    public ServerInteractionClass() {
    }
    //Source: Binding parts learnt from https://www.worldbestlearningcenter.com/tips/Androd-bound-unbound-services-examples.htm

    public class LocalBinder extends Binder {
        ServerInteractionClass getService() {
            return ServerInteractionClass.this;
        }
    }
    @Override
    public void onCreate(){


        Toast.makeText(ServerInteractionClass.this, "Web Service created", Toast.LENGTH_SHORT).show();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //unneeded, as we are not starting it via this method
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //This is a bound service, but only returns the binder at this point
        try {

        } catch (Exception e){

            System.out.println(e.getMessage());
        }
        return binder;
    }

    @Override
    public void onDestroy() {
        //Unneeded, as we just maintain the service for the duration of the app
        //and there's really nothing to destroy
        Toast.makeText(this, "Upload Service Stopped", Toast.LENGTH_LONG).show();
    }


    /*   public String uploadMyDatabase(){
       recordmessage("Upload Service Started", Toast.LENGTH_SHORT);
       //This function is called from the main activity to upload the database
       //It runs in a thread, because this API requires certain network activities to run in a
       //separate thread. Even though it is in a bound service, that is essentially the same thread.
       uploadthread = new Thread(new Runnable(){
           @Override
           public void run() {
               try {
                   String rslt = "";
                   rslt = uploadfile(DatabaseFileName);
                   //recordmessage(rslt, Toast.LENGTH_SHORT);
                   webstatus = rslt;
                   System.out.println(rslt);
               }catch (IOException e){
                   System.out.println(e.getMessage());
               }
           }
       });
       try {
           uploadthread.start();
           recordmessage(webstatus, Toast.LENGTH_SHORT);
           return webstatus;

       }
       catch(Exception e)
       { System.out.println(e.getMessage());}
       return "Error?";
   }*/

    /*public interface RunnableListener
    {
        void onResult(String[] result);
    }

    // a field in your class
    private RunnableListener runnableListener;*/
    //Important to note https://www.sqlite.org/howtocorrupt.html
    public String UploadOrDownloadMyDatabase(String action){
        //This function is used to create an Asynctask that does the upload or download
        //It then gets the result, waiting 30 seconds at most
        try {
            String finalresult = "";
            WebTask webtaskDB = new WebTask();
            webtaskDB.execute(action);
            finalresult = webtaskDB.get(30, TimeUnit.SECONDS);

            return finalresult;
        }
        catch (InterruptedException ie){
            System.out.println(ie.getMessage());
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
        return null;
    }

    /*    public String uploadMyDatabase(){
        WebTask webtaskDB = new WebTask();
        webtaskDB.execute("upload");
        return "";
    }*/


    /*public String downloadMyDatabase(String oldvar){
        recordmessage("Download Service Started", Toast.LENGTH_SHORT);
        //This function is called from the main activity to download the database
        //It runs in a thread, because this API requires certain network activities to run in a
        //separate thread. Even though it is in a bound service, that is essentially the same thread.
        final String result = "";
        downloadthread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    String rslt = "";
                    rslt = downloadfile(DatabaseFileName);
                    System.out.println(rslt);
                    webstatus = rslt;
                }catch (Exception e){
                    System.out.println(e.getMessage());
                }
            }

        });
        try {
            downloadthread.start();
            recordmessage(webstatus, Toast.LENGTH_SHORT);
            return webstatus;
        }
        catch(Exception e)
        { System.out.println(e.getMessage());}
        return "Error?";
    }*/

    private File getDatabaseFile(String DBFilename){
        //This checks if the storage is mounted then gets the Database File for Upload
        try {
            File outFile;
            String envstate = Environment.getExternalStorageState();
            if (envstate.equals("mounted")) {
                String outFileName = DBFilename; //"CSE535_ASSIGNMENT2";
                outFile = new File(this.getExternalFilesDir(null), outFileName);

                if (outFile.exists() && !outFile.isDirectory()) {
                    System.out.println("File Exists");
                }
                return outFile;
            }
            else{
                System.out.println("Sorry, External Storage not found");

            }

        }catch ( Exception e   ){
            //Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            System.out.println(e.getMessage());

        }
        return null;
    }

    private void finalizeDownloadedDatabase(String OrigDBFilename, String TempDBFilename){
        //This script is used to simply rename the original file if it exists
        //and give the temp file that name.
        try {
            File origFile;
            File tempFile;
            File newFile;
            File RenamedFile2 = null;
            boolean rslt;
            String envstate = Environment.getExternalStorageState();
            if (envstate.equals("mounted")) {
                //String origFileName = OrigDBFilename;//"CSE535_ASSIGNMENT2";
                origFile = new File(this.getExternalFilesDir(null), OrigDBFilename);
                tempFile = new File(this.getExternalFilesDir(null), TempDBFilename);

                if (tempFile.exists() && !tempFile.isDirectory()) {
                    if (origFile.exists()){
                        //File RenamedFile = new File(this.getExternalFilesDir(null),  OrigDBFilename + "-" + getDateTime());
                        //rslt = origFile.renameTo(RenamedFile);
                        /*File JrnalFile = new File(this.getExternalFilesDir(null),  OrigDBFilename + "-journal");
                        if (JrnalFile.exists())
                            JrnalFile.delete();
                        */rslt = origFile.delete();

                    }
                    else{
                        rslt = true;
                    }

                    if (rslt) {
                        importDatabase(this.getExternalFilesDir(null) + "/" + TempDBFilename, this.getExternalFilesDir(null) + "/" + OrigDBFilename);
//                        RenamedFile2 = new File(this.getExternalFilesDir(null),  OrigDBFilename);
                        boolean rslt2 = tempFile.delete();
                        System.out.println("Temporary File has replaced the original");
//                        if (rslt2) {
//                            newFile = new File(this.getExternalFilesDir(null), RenamedFile2);
//                            System.out.println("File Exists and was renamed to " + RenamedFile.getAbsolutePath());
//                        }
//                        else{
//                            System.out.println("Sorry, Newly Established Database File Not Found");
//                        }
                    }
                }
                else{
                    System.out.println("Sorry, Downloaded File Not Found");
                }


                //return RenamedFile2.getAbsolutePath();
            }
            else{
                System.out.println("Sorry, External Storage not found");
            }

        }catch ( Exception e   ){
            //Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            System.out.println(e.getMessage());

        }
        //return null;
    }

    private String prepDatabaseFileDownload(String DBFilename){
        //This checks if the storage is mounted, then does some of the functions required
        //to download the file including removing temporary files and journals
        //might not be used in the end
        try {
            File tempFile;
            File origFile;
            String envstate = Environment.getExternalStorageState();
            if (envstate.equals("mounted")) {
                String tempFileName = DBFilename + "_TEMP";//"CSE535_ASSIGNMENT2";
                tempFile = new File(this.getExternalFilesDir(null), tempFileName);
                origFile = new File(this.getExternalFilesDir(null), DBFilename+ "_DOWN");
                if (tempFile.exists() ) {
                    //File RenamedTempFile = new File(this.getExternalFilesDir(null),  tempFileName + "-" + getDateTime());
                    //Boolean rslt = tempFile.renameTo(RenamedTempFile);
                    tempFile.delete();
                }
                if (origFile.exists()){
                    origFile.delete();
                }
                File JrnalFile = new File(this.getExternalFilesDir(null),  DBFilename+ "_DOWN" + "-journal");
                if (JrnalFile.exists())
                    JrnalFile.delete();
                    //File RenamedFile = new File(this.getExternalFilesDir(null),  outFileName + "-" + getDateTime());
                    //Boolean rslt = outFile.renameTo(RenamedFile);
                 //   if (rslt) {
                 //       outFile = new File(this.getExternalFilesDir(null), outFileName);
                 //       System.out.println("File Exists and was renamed to " + RenamedFile.getAbsolutePath());
                 //   }
                //}
                return tempFile.getAbsolutePath();
            }
            else{
                System.out.println("Sorry, External Storage not found");
            }

        }catch ( Exception e   ){
            //Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            System.out.println(e.getMessage());

        }
        return null;
    }

    /*public String downloadfile(String DBFile){
        try {
            String rslt = downloadFileSync("http://10.0.2.2:5000/upload/" + DBFile, this.getExternalFilesDir(null) + "/" + DBFile + "_DOWN");
            return rslt;
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
        return null;
    }*/

    public String downloadfile(String DBFile){
        //Source: Professor's sample code. Modified as needed
        //https removed as unneeded in local environment
        //requirement for https removed via the use of a network security config xml and an entry in the android manifest
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            //10.0.2.2 is the IP the emulator uses to point to the local machine
            URL url = new URL("http://10.0.2.2:5000/upload/"+DBFile);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage();
            }
            //Progress not being shown
            int fileLength = connection.getContentLength();

            input = connection.getInputStream();
            output = new FileOutputStream(prepDatabaseFileDownload(DBFile));
            byte data[] = new byte[4096];
            long total = 0;
            int count;
            //this writes the data to the file as long as there is data remaining
            try {
                while ((count = input.read(data)) != -1) {
                    if (count != 0) {
                        total += count;
                        output.write(data, 0, count);
                    }
                }
            }catch(ProtocolException pe) {System.out.println(pe.getMessage());}

            //Finalize the download by replacing the original in a two step process
            finalizeDownloadedDatabase(DBFile+"_DOWN", DBFile+"_TEMP");
            return "File Download was Sucessful";
        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }
    }

    private boolean importDatabase(String inputFileName, String outputFileName) throws IOException
    {   //This is no longer being used to copy the file from temp to permanent, since we are downloading directly
        InputStream mInput = new FileInputStream(inputFileName);
        String outFileName = outputFileName;
        OutputStream mOutput = new FileOutputStream(outFileName);
        byte[] mBuffer = new byte[1024];
        int mLength;
        while ((mLength = mInput.read(mBuffer))>0)
        {
            mOutput.write(mBuffer, 0, mLength);
        }
        mOutput.flush();
        mOutput.close();
        mInput.close();

        return true;
    }

    /* an attempt to use a different function download that is more resilient
    public String downloadFileSync(String downloadUrl, String outputPath) throws Exception {

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Failed to download file: " + response);
        }
        FileOutputStream fos = new FileOutputStream(outputPath);
        fos.write(response.body().bytes());
        fos.close();

        return "File Download Successful with okhttp";
    }*/

    public String uploadfile(String DBFile) throws IOException {
        //This function is called to upload a file in multipart format using POST to a web location
        MultipartUtility upfile = new MultipartUtility();

        upfile.MultipartUtilityV2("http://10.0.2.2:5000/upload");
        upfile.addFormField("file","");
        upfile.addFilePart("file", getDatabaseFile(DBFile));
        String status = upfile.finish();
        System.out.println(status);
        return status;

    }


    private class MultipartUtility {
        //Source: https://blog.morizyun.com/blog/android-httpurlconnection-post-multipart/index.html
        private HttpURLConnection httpConn;
        private DataOutputStream request;
        private final String boundary =  "*****";
        private final String crlf = "\r\n";
        private final String twoHyphens = "--";

        /**
         * This constructor initializes a new HTTP POST request with content type
         * is set to multipart/form-data
         *
         * @param requestURL
         * @throws IOException
         */
        public void MultipartUtilityV2(String requestURL)
                throws IOException {

            // creates a unique boundary based on time stamp
            URL url = new URL(requestURL);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setUseCaches(false);


            httpConn.setRequestMethod("POST");

            httpConn.setDoOutput(true); // indicates POST method
            httpConn.setDoInput(true);
            httpConn.setRequestProperty("Connection", "Keep-Alive");
            httpConn.setRequestProperty("Cache-Control", "no-cache");
            httpConn.setRequestProperty(
                    "Content-Type", "multipart/form-data;boundary=" + this.boundary);

            request =  new DataOutputStream(httpConn.getOutputStream());
        }

        /**
         * Adds a form field to the request
         *
         * @param name  field name
         * @param value field value
         */
        public void addFormField(String name, String value)throws IOException  {
            request.writeBytes(this.twoHyphens + this.boundary + this.crlf);
            //request.writeBytes("Content-Disposition: form-data; name=\"" + name + "\""+ this.crlf);
            request.writeBytes("Content-Disposition: form-data; name=\"" + name + "\""+ this.crlf);
            request.writeBytes("Content-Type: text/plain; charset=UTF-8" + this.crlf);
            request.writeBytes(this.crlf);
            request.writeBytes(value+ this.crlf);
            request.flush();
        }

        /**
         * Adds a upload file section to the request
         *
         * @param fieldName  name attribute in
         * @param uploadFile a File to be uploaded
         * @throws IOException
         */
        public void addFilePart(String fieldName, File uploadFile)
                throws IOException {
            String fileName = uploadFile.getName();
            request.writeBytes(this.twoHyphens + this.boundary + this.crlf);
            request.writeBytes("Content-Disposition: form-data; name=\"" +
                    fieldName + "\";filename=\"" +
                    fileName + "\"" + this.crlf);
            request.writeBytes(this.crlf);

            byte[] bytes = Files.readAllBytes(uploadFile.toPath());
            request.write(bytes);
        }

        /**
         * Completes the request and receives response from the server.
         *
         * @return a list of Strings as response in case the server returned
         * status OK, otherwise an exception is thrown.
         * @throws IOException
         */
        public String finish() throws IOException {
            String response ="";

            request.writeBytes(this.crlf);
            request.writeBytes(this.twoHyphens + this.boundary +
                    this.twoHyphens + this.crlf);

            request.flush();
            request.close();

            // checks server's status code first
            int status = httpConn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                InputStream responseStream = new
                        BufferedInputStream(httpConn.getInputStream());

                BufferedReader responseStreamReader =
                        new BufferedReader(new InputStreamReader(responseStream));

                String line = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((line = responseStreamReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                responseStreamReader.close();

                response = stringBuilder.toString();
                httpConn.disconnect();

                return "File Upload was successful";
            } else {
                throw new IOException("Server returned non-OK status: " + status);

            }

            //return response;
        }
    }

    private void recordmessage(String err, int ErrLen){
        Toast.makeText(this, err, ErrLen).show();
        System.out.println(err);
    }


    private String getDateTime() {
        //Source: https://www.caretit.com/blog/odoo-mobile-apps-caret-3/post/how-insert-datetime-value-in-sqlite-database-36
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyyMMdd-HHmmss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    private class WebTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute(){
            System.out.println("Prepping for Web Activity");
        }
        @Override
        protected String doInBackground(String... params) {
            //This just uploads or downloads. Sleep is put after download
            //as I was unable to implement a proper handler in time
            try {
                String rslt = "";
                if (params[0] == "upload") {
                    rslt = uploadfile(DatabaseFileName);
                }else{
                    rslt = downloadfile(DatabaseFileName);
                    Thread.sleep(500);
                }
                System.out.println(rslt);
                webstatus = rslt;
                return rslt;
            }
         catch (Exception e){//InterruptedException e) {
            //e.printStackTrace();
             System.out.println(e.getMessage());
        }
            return null;
        }
    @Override
    protected void onPostExecute(String result){
        webstatus = result;
        System.out.println("Web Activity Completed: " + result);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        //Runs on stopping the animation via Stop button or Switch
        //The Async Task is basically killed, as it cannot be restarted
        //It is re-initialized later using start or restart button
        recordmessage("Web Task cancelled.",Toast.LENGTH_SHORT);
    }
}
}
