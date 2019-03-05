package two.andr.pearson.com.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    public static String TABLE_NAME = "";
    LineGraphSeries<DataPoint> seriesX;
    LineGraphSeries<DataPoint> seriesY;
    LineGraphSeries<DataPoint> seriesZ;
    LineGraphSeries<DataPoint> seriesXD;
    LineGraphSeries<DataPoint> seriesYD;
    LineGraphSeries<DataPoint> seriesZD;
    ArrayList<String> downloadData;
    int sizeDownloadData = 0;
    private boolean killMe = false;
    double x = 0;
    double xd ;
    int j = 0;
    int count =0;
    GraphView graph;
    EditText editName, editAge, editPID;
    Spinner spinner;
    String mostRecentValid = "";
    String getMostRecentValid2 = "";
    boolean isPatientChanged = false;
    boolean isStarted = false;
    DataPoint[] d, d1;
    Sensor sensor;
    SensorManager sensorManager;
    public static double xVal = 0.0;
    public static double yVal = 0.0;
    public static double zVal = 0.0;
    boolean checkSensor = false;
    public static SQLiteDatabase db;
    ProgressDialog dialog = null;
    public static String upLoadServerUri = "http://impact.asu.edu/CSE535Spring18Folder/UploadToServer.php";
    static String imagePath = "";
    private String selectedFilePath;
    int min = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("ON CREATE", "run: " + "Inside On_Create");
        d = new DataPoint[]{new DataPoint(0d, 0d)};
        createDatabase();
        graph = (GraphView) findViewById(R.id.graphView);
        Button btnStart = (Button) findViewById(R.id.button1);
        Button btnStop = (Button) findViewById(R.id.button2);
        Button btnUpload = (Button) findViewById(R.id.btnUpload);
        Button btnDown = (Button) findViewById(R.id.btnDown);
        editName = (EditText) findViewById(R.id.editName);
        editAge = (EditText) findViewById(R.id.editAge);
        spinner = (Spinner) findViewById(R.id.spinner);
        editPID = (EditText) findViewById(R.id.editPID);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.array_sex, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        //Graph configuration
        Viewport viewport = graph.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(-5);
        viewport.setMaxY(17);
        viewport.setBackgroundColor(Color.LTGRAY);
        viewport.setScalable(true);
        viewport.setScrollable(true);
        graph.getGridLabelRenderer().setVerticalAxisTitle("Match Value");
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Time/s");
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(15);
        graph.getGridLabelRenderer().setNumHorizontalLabels(16);
        graph.getGridLabelRenderer().setNumVerticalLabels(20);

        //Start button click event listener
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count = 0;
                if ((!isStarted) && isRecordValid()) {
                    Log.i("INSIDE", "btnStart.setOnClickListener 1st if");
                    if (isPatientChanged) {
                        Log.i("INSIDE", "btnStart.setOnClickListener 2nd if");
                        TABLE_NAME = getName() + "_" + getAge() + "_" + getSex() + "_" + getPID();
                        if (db == null)
                            db = SQLiteDatabase.openDatabase(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/CSE535_ASSIGNMENT2" + "/Group12.db",
                                    null, SQLiteDatabase.CREATE_IF_NECESSARY);
                        db.execSQL("create table if not exists " + TABLE_NAME + " (timestamp DATETIME DEFAULT (STRFTIME('%d-%m-%Y   %H:%M:%f', 'NOW','localtime')), xValue real, yValue real, zValue real)");
                        //db.execSQL("create table if not exists " + TABLE_NAME + " (SlNo integer primary key autoincrement, xValue real, yValue real, zValue real, timestamp DATETIME DEFAULT (STRFTIME('%d-%m-%Y   %H:%M:%f', 'NOW','localtime')))");
                        seriesX = new LineGraphSeries<DataPoint>();
                        seriesX.setColor(Color.GREEN);
                        seriesY = new LineGraphSeries<DataPoint>();
                        seriesY.setColor(Color.RED);
                        seriesZ = new LineGraphSeries<DataPoint>();
                        seriesZ.setColor(Color.BLUE);

                        seriesX.resetData(d);
                        seriesY.resetData(d);
                        seriesZ.resetData(d);
                        graph.removeCallbacks(action);
                        graph.removeAllSeries();
                        x = 0;
                        j = 0;
                    }
                    graph.removeAllSeries();
                    graph.addSeries(seriesX);
                    graph.addSeries(seriesY);
                    graph.addSeries(seriesZ);
                    graph.post(action);
                    isStarted = true;
                }
            }
        });

        //Stop button click event listener
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStarted) {
                    Log.i("INSIDE", "btnStop.setOnClickListener if");
                    graph.removeCallbacks(action);
                    graph.removeAllSeries();
                    isStarted = false;
                }
            }
        });

        //Upload button click event listener
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);
                new Thread(new Runnable() {
                    public void run() {
                        Log.i("INSIDE", "btnUpload.setOnClickListener run");
                        imagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/CSE535_ASSIGNMENT2" + "/Group12.db";
                        uploadDB(imagePath);
                    }
                }).start();
            }
        });


        //Download button click event listener
        btnDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog = ProgressDialog.show(MainActivity.this, "", "Downloading file...", true);
                new Thread(new Runnable() {
                    public void run() {
                        try{
                            Log.i("INSIDE", "btnDown.setOnClickListener try");
                            downloadDB(new URL("http://impact.asu.edu/CSE535Spring18Folder/Group12.db"),"Group12.db");
                        }catch(MalformedURLException ex){
                            Toast.makeText(getApplicationContext(), "Cannot download", Toast.LENGTH_LONG).show();
                        }
                    }
                }).start();
            }
        });
    }

    //Runnable for graph not downloaded
    Runnable action = new Runnable() {
        @Override
        public void run() {
            Log.i("INSIDE", "action");
            coreRun();
            graph.postDelayed(this, 1000);
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        xVal = event.values[0];
        yVal = event.values[1];
        zVal = event.values[2];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

        @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            File folder = new File(Environment.getExternalStorageDirectory().toString() + "/Android/data/CSE535_ASSIGNMENT2");
            deleteRecursive(folder);
            folder.mkdirs();
            db = SQLiteDatabase.openDatabase(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/CSE535_ASSIGNMENT2" + "/Group12.db", null, SQLiteDatabase.CREATE_IF_NECESSARY);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK){
            Uri selectedFileUri = data.getData();
            selectedFilePath = imagePath;
            if(selectedFilePath != null && !selectedFilePath.equals("")){
            }else{
                Toast.makeText(this,"Cannot upload file to server",Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Method associated with action
    private void coreRun() {
        x += 1;
        double x1 = 0.0;
        double y1 = 0.0;
        double z1 = 0.0;
        DecimalFormat df = new DecimalFormat("#.####");
        x1 = (double) Math.round(xVal * 100000d) / 100000d;
        y1 = (double) Math.round(yVal * 100000d) / 100000d;
        z1 = (double) Math.round(zVal * 100000d) / 100000d;
        ContentValues initialValues = new ContentValues();
        initialValues.put("xValue", x1);
        initialValues.put("yValue", y1);
        initialValues.put("zValue", z1);
        db.insert(TABLE_NAME, null, initialValues);
        addEntry(x, x1, y1, z1);
    }

    //To add entry every second
    private void addEntry(double b, double d1, double d2, double d3) {
        seriesX.appendData(new DataPoint(b, d1), true, 20);
        seriesY.appendData(new DataPoint(b, d2), true, 20);
        seriesZ.appendData(new DataPoint(b, d3), true, 20);

    }

    //Getter for Name field
    private String getName() {
        String name = "";
        name = editName.getText().toString();
        return name;
    }

    //Getter for Age field
    private int getAge() {
        int age1 = 0;
        String age2 = editAge.getText().toString();
        age1 = (age2.trim().equals("")) ? 0 : Integer.parseInt(editAge.getText().toString());
        return age1;
    }

    //Getter for Sex field
    private String getSex() {
        String sex = "";
        sex = spinner.getSelectedItem().toString();
        return sex;
    }

    //Getter for Patient ID field
    private int getPID() {
        int pID1 = 0;
        String pID2 = editPID.getText().toString();
        pID1 = (pID2.trim().equals("")) ? 0 : Integer.parseInt(editPID.getText().toString());
        return pID1;
    }

    //Validating input record
    private boolean isRecordValid() {
        String name = getName();
        int age = getAge();
        String sex = getSex();
        int pID = getPID();
        boolean b = false;
        boolean isNameValid = (getName() != "");
        boolean isAgeValid = (getAge() != 0);
        boolean isPIDValid = (getPID() != 0);
        b = (isNameValid && isAgeValid && isPIDValid);
        if (b) {
            mostRecentValid = name + "_" + age + "_" + sex + "_" + pID;
            isPatientChanged = !mostRecentValid.equals(getMostRecentValid2);
            getMostRecentValid2 = mostRecentValid;
        }
        return b;
    }

    //Checking if SD card permission granted
    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v("WriteAccess", "Permission is granted");
                return true;
            } else {
                Log.v("WriteAccess", "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            //permission is automatically granted on sdk<23 upon installation
            Log.v("WriteAccess", "Permission is granted");
            return true;
        }
    }

    //Before the app starts, the app folder is cleared
    public void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.exists() && fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        fileOrDirectory.delete();
    }

    //Method to create database
    public void createDatabase() {
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)&&
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE)
                        == PackageManager.PERMISSION_GRANTED)&&
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                        == PackageManager.PERMISSION_GRANTED)
                ) {
            File folder = new File(Environment.getExternalStorageDirectory().toString() + "/Android/data/CSE535_ASSIGNMENT2");
            deleteRecursive(folder);
            folder.mkdirs();
            db = SQLiteDatabase.openDatabase(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/CSE535_ASSIGNMENT2" + "/Group12.db", null, SQLiteDatabase.CREATE_IF_NECESSARY);
        } else {
            // Permission is missing and must be requested.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET}, 1);
        }


    }

    //Method to upload DB in to ASU server
    public int uploadDB(final String selectedFilePath) {
        int serverResponseCode = 0;
        HttpURLConnection connection;
        DataOutputStream dataOutputStream;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File selectedFile = new File(selectedFilePath);

        String[] parts = selectedFilePath.split("/");
        final String fileName = parts[parts.length - 1];

        if (!selectedFile.isFile()) {
            dialog.dismiss();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "No file located", Toast.LENGTH_LONG).show();
                }
            });
            return 0;
        } else {
            try {
                FileInputStream fileInputStream = new FileInputStream(selectedFile);
                URL url = new URL(upLoadServerUri);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);//Allow Inputs
                connection.setDoOutput(true);//Allow Outputs
                connection.setUseCaches(false);//Don't use a cached Copy
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                connection.setRequestProperty("uploaded_file", selectedFilePath);

                //creating new dataoutputstream
                dataOutputStream = new DataOutputStream(connection.getOutputStream());

                //writing bytes to data outputstream
                dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
                dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + selectedFilePath + "\"" + lineEnd);

                dataOutputStream.writeBytes(lineEnd);

                //returns no. of bytes present in fileInputStream
                bytesAvailable = fileInputStream.available();
                //selecting the buffer size as minimum of available bytes or 1 MB
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                //setting the buffer as byte array of size of bufferSize
                buffer = new byte[bufferSize];

                //reads bytes from FileInputStream(from 0th index of buffer to buffersize)
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                //loop repeats till bytesRead = -1, i.e., no bytes are left to read
                while (bytesRead > 0) {
                    //write the bytes read from inputstream
                    dataOutputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                dataOutputStream.writeBytes(lineEnd);
                dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                serverResponseCode = connection.getResponseCode();
                String serverResponseMessage = connection.getResponseMessage();

                Log.i("SERVER RESPONSE", "Server Response is: " + serverResponseMessage + ": " + serverResponseCode);

                //response code of 200 indicates the server status OK
                if (serverResponseCode == 200) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Successfully Uploaded", Toast.LENGTH_LONG).show();
                            //tvFileName.setText("File Upload completed.\n\n You can see the uploaded file here: \n\n" + "http://coderefer.com/extras/uploads/"+ fileName);
                        }
                    });
                }
                //closing the input and output streams
                fileInputStream.close();
                dataOutputStream.flush();
                dataOutputStream.close();


            } catch (FileNotFoundException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "File Not Found", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "URL error!", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Cannot Read/Write File!", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
            return serverResponseCode;
        }


    }

    //Method to download DB to SD card and initiate display
    public void downloadDB(URL url, String filename){
        try{
            count = 0;
            File root = Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath()+"/Android/Data/CSE535_ASSIGNMENT2_DOWN");
            if(!dir.exists()) dir.mkdirs();
            File file = new File(dir, filename);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[5000];
            int current = 0;
            while((current = bis.read(data,0,data.length)) != -1){
                buffer.write(data,0,current);
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(buffer.toByteArray());
            if (conn.getResponseCode() ==200) {
                dialog.dismiss();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(getApplicationContext(), "File Downloaded", Toast.LENGTH_LONG).show();
                    }
                });
            }

            SQLiteDatabase dbDownloaded  = SQLiteDatabase.openDatabase(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/Android/data/CSE535_ASSIGNMENT2_DOWN" + "/Group12.db",null, SQLiteDatabase.OPEN_READONLY);

            ArrayList<String> arrTblNames = new ArrayList<String>();
            Cursor c1 = dbDownloaded.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

            if (c1.moveToFirst()) {
                while ( !c1.isAfterLast() ) {
                    arrTblNames.add( c1.getString( c1.getColumnIndex("name")) );
                    c1.moveToNext();
                }
            }
            boolean isTablePresent = arrTblNames.contains(TABLE_NAME);
            Log.i("SQLITE TABLE COUNT",""+arrTblNames.size()+"-"+TABLE_NAME+" - is table present? "+arrTblNames.contains(TABLE_NAME));
            if(isTablePresent) {
                String tableName = "SELECT * FROM "+TABLE_NAME+" order by timestamp desc LIMIT 10";
                String orderby ="timestamp ASC";
                String sql = "Select * from ("+tableName+") order by "+orderby;
                Cursor c = dbDownloaded.rawQuery(sql,null);

                graph.removeCallbacks(action);
                graph.removeAllSeries();
                isStarted = false;

                downloadData = new ArrayList<String>();
                while (c.moveToNext()) {
                    downloadData.add(c.getString(0) + "_" + c.getString(1) + "_" + c.getString(2) + "_" + c.getString(3));

                }
                min = Math.min(10, downloadData.size());
                fos.flush();
                fos.close();
                dbDownloaded.close();
                plotDownloadGraph();
            }else{
                displayDownloadError();
            }

        }catch (IOException ex){
            Log.i("INSIDE", "IO Exception");
        }catch (SQLiteException ex){
            Log.i("INSIDE", "downloadDB SQLITEException");
        }
    }

    //If error  occurs in display after downloading DB
    private void displayDownloadError() {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), "Unable to display graph data of "+TABLE_NAME+". Please upload data before attempting to download", Toast.LENGTH_LONG).show();
            }
        });
    }

    //Method to plot downloaded graph
    public void plotDownloadGraph(){
        killMe = false;
        count = 0;
        xd = 0;
        resetDownLoadData(false);
        graph.addSeries(seriesXD);
        graph.addSeries(seriesYD);
        graph.addSeries(seriesZD);
        graph.post(action2);

    }

    //Runnable for plotting downloaded graph
    Runnable action2 = new Runnable() {
        @Override
        public void run() {
            if(killMe)
                return;
            coreRun2();
            graph.postDelayed(action2, 200);
        }
    };

    //Core run method associated with action2
    private void coreRun2() {
        if(count<min) {
         String s = downloadData.get(count);

            double x1 = 0.0;
            double y1 = 0.0;
            double z1 = 0.0;
            count++;

            x1 = Double.parseDouble(s.split("_")[1]);
            y1 = Double.parseDouble(s.split("_")[2]);
            z1 = Double.parseDouble(s.split("_")[3]);
            addEntry2(xd, x1, y1, z1);
        }else{
            killMe = true;
        }
    }

    //Method to stop plotting of graph
    private void stopGraph2() {
        graph.removeCallbacks(action2);
    }

    //Method to reset graph After every download
    public void resetDownLoadData(boolean  b){
        if(!b){
            seriesXD = new LineGraphSeries<DataPoint>();
            seriesXD.setColor(Color.GREEN);
            seriesYD = new LineGraphSeries<DataPoint>();
            seriesYD.setColor(Color.RED);
            seriesZD = new LineGraphSeries<DataPoint>();
            seriesZD.setColor(Color.BLUE);

            String s = downloadData.get(0);
            double xx = Double.parseDouble(s.split("_")[1]);
            double yy = Double.parseDouble(s.split("_")[2]);
            double zz = Double.parseDouble(s.split("_")[3]);
            seriesXD.resetData(new DataPoint[]{new DataPoint(xd, xx)});
            seriesYD.resetData(new DataPoint[]{new DataPoint(xd, yy)});
            seriesZD.resetData(new DataPoint[]{new DataPoint(xd, zz)});
        }
    }

    //Method to plot points in graph from downloaded DB
    private void addEntry2(double b, double d1, double d2, double d3) {
        seriesXD.appendData(new DataPoint(b, d1), true, 20);
        seriesYD.appendData(new DataPoint(b, d2), true, 20);
        seriesZD.appendData(new DataPoint(b, d3), true, 20);
        xd++;
    }
}

