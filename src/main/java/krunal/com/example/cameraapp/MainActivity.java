package krunal.com.example.cameraapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

import static android.widget.Toast.LENGTH_LONG;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 1;

    public static final String EXTRA_MESSAGE ="" ;
    public static TextView jsonData;

    private AppExecutor mAppExcutor;
    private ImageView mImageView;

    private Button mStartCamera;

    private String mTempPhotoPath;

    private Bitmap mResultsBitmap;

    private FloatingActionButton mClear,mSave,mShare;
    public static String key;
    public JSONObject jsonObject;
    String data = "";
    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAppExcutor = new AppExecutor();

        mImageView = findViewById(R.id.imageView);
        mClear = findViewById(R.id.clear);
        mSave = findViewById(R.id.Save);
        mShare = findViewById(R.id.Share);
        mStartCamera = findViewById(R.id.startCamera);
        jsonData = findViewById(R.id.jsonData);

        mImageView.setVisibility(View.GONE);
        mShare.setVisibility(View.GONE);
        mSave.setVisibility(View.GONE);
        mClear.setVisibility(View.GONE);


        mStartCamera.setOnClickListener(v -> {
            // Check for the external storage permission
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // If you do not have permission, request it
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            } else {
                // Launch the camera if the permission exists
                launchCamera();
            }
        });

        mSave.setOnClickListener((View v) -> {
            mAppExcutor.diskIO().execute(() -> {
                // Delete the temporary image file
                BitmapUtils.deleteImageFile(this, mTempPhotoPath);

                // Save the image
                BitmapUtils.saveImage(this, mResultsBitmap);

            });

            Toast.makeText(this,"Image Save", LENGTH_LONG).show();
            change_screen();


        });

        mClear.setOnClickListener(v -> {
            // Clear the image and toggle the view visibility
            mImageView.setImageResource(0);
            mStartCamera.setVisibility(View.VISIBLE);
            mSave.setVisibility(View.GONE);
            mShare.setVisibility(View.GONE);
            mClear.setVisibility(View.GONE);

            mAppExcutor.diskIO().execute(() -> {
                // Delete the temporary image file
                BitmapUtils.deleteImageFile(this, mTempPhotoPath);
            });

        });

        mShare.setOnClickListener((View v) -> {

            mAppExcutor.diskIO().execute(() ->{
                // Delete the temporary image file
                BitmapUtils.deleteImageFile(this, mTempPhotoPath);

                // Save the image
                BitmapUtils.saveImage(this, mResultsBitmap);

            });

            // Share the image
            BitmapUtils.shareImage(this, mTempPhotoPath);

        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Called when you request permission to read and write to external storage
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If you get permission, launch the camera
                    launchCamera();
                } else {
                    // If you do not get permission, show a Toast
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the image capture activity was called and was successful
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Process the image and set it to the TextView
            processAndSetImage();
        } else {

            // Otherwise, delete the temporary image file
            BitmapUtils.deleteImageFile(this, mTempPhotoPath);
        }
    }

    /**
     * Creates a temporary image file and captures a picture to store in it.
     */
    private void launchCamera() {

        // Create the capture image intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the temporary File where the photo should go
            File photoFile = null;
            try {
                photoFile = BitmapUtils.createTempImageFile(this);
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                // Get the path of the temporary file
                mTempPhotoPath = photoFile.getAbsolutePath();

                // Get the content URI for the image file
                /*Uri photoURI = FileProvider.getUriForFile(this,
                        FILE_PROVIDER_AUTHORITY,
                        photoFile);
                        */
                Uri photoURI = FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()),
                        BuildConfig.APPLICATION_ID + ".provider", photoFile);


                // Add the URI so the camera can store the image
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // Launch the camera activity
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }


    /**
     * Method for processing the captured image and setting it to the TextView.
     */
    @SuppressLint("RestrictedApi")
    private void processAndSetImage() {

        // Toggle Visibility of the views
        mStartCamera.setVisibility(View.GONE);
        mSave.setVisibility(View.VISIBLE);
        mShare.setVisibility(View.VISIBLE);
        mClear.setVisibility(View.VISIBLE);
        mImageView.setVisibility(View.VISIBLE);

        // Resample the saved image to fit the ImageView
        mResultsBitmap = BitmapUtils.resamplePic(this, mTempPhotoPath);


        // Set the new bitmap to the ImageView
        mImageView.setImageBitmap(mResultsBitmap);
    }

    public void change_screen()
    {
        String TAG="LOG";
        getApplicationContext().startService(new Intent(getApplicationContext(), TransferService.class));
        // Initialize the AWSMobileClient if not initialized
        AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails userStateDetails) {
                Log.i(TAG, "AWSMobileClient initialized. User State is " + userStateDetails.getUserState());
                uploadWithTransferUtility();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Initialization error.", e);
            }
        });
        Intent intent = new Intent(this, s3_save_activity.class);
        String message = "Logging check";
        intent.putExtra(EXTRA_MESSAGE, message);

    }
    public void uploadWithTransferUtility() {
        String TAG="LOG";
        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance()))
                        .build();

        File file = new File(getApplicationContext().getFilesDir(), "sample.txt");
        String hashkey = UUID.randomUUID().toString();
        try {

            file = new File(getApplicationContext().getFilesDir(), "android_camera_"+hashkey+".png");
            FileOutputStream fOut = new FileOutputStream(file);
            mResultsBitmap.compress(Bitmap.CompressFormat.JPEG, 50, fOut);
            fOut.flush();
            fOut.close();

        }
        catch(Exception e) {
            Log.e(TAG, e.getMessage());
        }
        key="public/android_camera_"+hashkey+".png";
        TransferObserver uploadObserver =
                transferUtility.upload(
                        key,
                        new File(getApplicationContext().getFilesDir(),"android_camera_"+hashkey+".png"));

        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle a completed upload.
                    FetchAPIData process = new FetchAPIData();
                    //fetchAPIdata weatherProcess = new fetchAPIdata();
                    process.execute("https://8vywkld5b1.execute-api.us-east-1.amazonaws.com/invokesagetest/predictdisease","simplecameraapp231703-dev",key);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int)percentDonef;

                Log.d(TAG, "ID:" + id + " bytesCurrent: " + bytesCurrent
                        + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                // Handle errors
            }


        });

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == uploadObserver.getState()) {
            // Handle a completed upload.
        }

        Log.d(TAG, "Bytes Transferred: " + uploadObserver.getBytesTransferred());
        Log.d(TAG, "Bytes Total: " + uploadObserver.getBytesTotal());
    }


    public class FetchAPIData extends AsyncTask<String,Void,Void> {
        private static final String EXTRA_MESSAGE = "";

        String displayText = "";

        @Override
        protected Void doInBackground(String... params) {
            try {
                Log.i("fetch Json-Method", params[1]);
                URL url = new URL(params[0]);

                //Setting up a HTML Object
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                httpURLConnection.setRequestProperty("Accept","application/json");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("bucket", params[1]);
                jsonParam.put("key", params[2]);

                Log.i("fetch Json-Method", jsonParam.toString());
                try(OutputStream dos = httpURLConnection.getOutputStream()) {

                    byte[] input = jsonParam.toString().getBytes("utf-8");

                    dos.write(input, 0, input.length);

                }

                Log.i("STATUS", String.valueOf(httpURLConnection.getResponseCode()));
                Log.i("MSG" , httpURLConnection.getResponseMessage());

                // Fetching data from the endpoint.
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = "";
                data = "";
                while (line != null){
                    line = bufferedReader.readLine();
                    data = data + line;
                }

                Log.i("Data : " , data);
                jsonObject = new JSONObject(data.trim());
                Iterator<String> keys = jsonObject.keys();

                Log.i("Keys : ", keys.toString());
                for(int i = 0; i<jsonObject.names().length(); i++){
                    Log.v("Object Info", "key = " + jsonObject.names().getString(i) + " value = " + jsonObject.get(jsonObject.names().getString(i)));
                    displayText = displayText + jsonObject.names().getString(i) + " : " + jsonObject.get(jsonObject.names().getString(i)) +"\n";
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (jsonObject.has("Disease")) {
                Toast.makeText(MainActivity.this, "Match Found", LENGTH_LONG).show();
                Intent intent = new Intent(MainActivity.this, s3_save_activity.class);
                intent.putExtra(EXTRA_MESSAGE, data);
                startActivity(intent);
            }
            else{
                Toast.makeText(MainActivity.this, "No Match Found", LENGTH_LONG).show();
            }
        }
    }
}
