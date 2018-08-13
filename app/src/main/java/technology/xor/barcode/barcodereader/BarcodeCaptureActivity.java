/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package technology.xor.barcode.barcodereader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import technology.xor.barcode.general.NoSSLv3SocketFactory;
import technology.xor.barcode.R;
import technology.xor.barcode.barcodereader.ui.camera.CameraSource;
import technology.xor.barcode.barcodereader.ui.camera.CameraSourcePreview;
import technology.xor.barcode.barcodereader.ui.camera.GraphicOverlay;

/**
 * Activity for the multi-tracker app.  This app detects barcodes and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and ID of each barcode.
 */
public final class BarcodeCaptureActivity extends AppCompatActivity implements BarcodeGraphicTracker.BarcodeDetectorListener {
    private static final String TAG = "SWIFT-CAPTURE";

    // intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    // constants used to pass extra data in the intent
    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    public static final String AutoCapture = "AutoCapture";
    public static final String BarcodeObject = "Barcode";

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;

    private boolean autoFocus, useFlash, autoCapture;

    private String URL = null;
    private String codeName = null;

    private int counter = 0;

    // Location to store QRcode values. Prevent duplicate entries
    private Map<String, String> map = new HashMap<String, String>();

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.barcode_capture);

        mPreview = findViewById(R.id.preview);
        mGraphicOverlay = findViewById(R.id.graphicOverlay);

        // Load the shared preferences and extract url and code name
        SharedPreferences sharedPref = this.getSharedPreferences(
                this.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        URL = sharedPref.getString("domain_name", "https://www.duckduckgo.com");
        codeName = sharedPref.getString("code_name", "ALABASTER");

        // Check for default values. If they exist, go back to the main screen.
        boolean defaultsDetected = URL.equals(getString(R.string.default_url)) ||
                codeName.equals(getString(R.string.default_name));

        // Exit back to the main UI thread and return a value of cancelled to display the appropriate
        // snackbar message
        if (defaultsDetected) {
            Intent returnMainUi = new Intent();
            returnMainUi.putExtra("Result", "defaults");
            setResult(CommonStatusCodes.CANCELED, returnMainUi);
            finish();
        }

        // read parameters from the intent used to launch the activity.
        autoFocus = getIntent().getBooleanExtra(AutoFocus, true);
        useFlash = getIntent().getBooleanExtra(UseFlash, false);
        autoCapture = getIntent().getBooleanExtra(AutoCapture, false);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED)
            createCameraSource();
        else
            requestCameraPermission();

        MakeSnakckbar(getString(R.string.barcode_instruct), 1);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            if (hasFocus) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
    }

    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions(BarcodeCaptureActivity.this, permissions,
                                RC_HANDLE_CAMERA_PERM);
                    }
                })
                .show();
    }

    @SuppressLint("InlinedApi")
    private void createCameraSource() {
        Context context = getApplicationContext();

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();
        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay, autoCapture?this:null);
        barcodeDetector.setProcessor(
                new MultiProcessor.Builder<>(barcodeFactory).build());

        if (!barcodeDetector.isOperational()) {

            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                MakeSnakckbar(getString(R.string.low_storage_error), 1);
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext(), barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(metrics.heightPixels, metrics.widthPixels)
                .setRequestedFps(30.0f);

        // make sure that auto focus is an available option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            builder = builder.setFocusMode(
                    autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);

        mCameraSource = builder
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Barcode Scanner")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    @Override
    public void onObjectDetected(Barcode data) {

        // If the QRcode was already scanned, then show a toast message
        if (map.containsValue(data.displayValue)) {
            // If QR already scanned, move on
            MakeSnakckbar(getString(R.string.barcode_exists), 0);

        } else {
            // Set the number of QR codes to read. On the last one, send the message to the URL
            if (counter == 0) {
                map.put("1", data.displayValue);
            } else if (counter == 1) {
                map.put("2", data.displayValue);
            } else if (counter == 2) {
                map.put("3", data.displayValue);
            } else if (counter == 3) {
                map.put("4", data.displayValue);

                // Send a POST request to the server. Increment the counter for every qrcode you have
                // in the desktop application.
                new SendPostRequest().execute(map.get("1"), map.get("2"), map.get("3"), map.get("4"), URL, codeName);

                // Return to the main UI with a success code
                Intent returnMainUi = new Intent();
                returnMainUi.putExtra(BarcodeObject, data);
                setResult(CommonStatusCodes.SUCCESS, returnMainUi);
                finish();
            }

            // Increment counter and display results to the user
            counter=counter+1;
            MakeSnakckbar("QR" + counter + " Captured!", 0);
        }
    }

    private void MakeSnakckbar(String msg, int length) {
        if (length == 0) {
            Snackbar.make(mGraphicOverlay, msg,
                    Snackbar.LENGTH_SHORT)
                    .show();
        } else {
            Snackbar.make(mGraphicOverlay, msg,
                    Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    private static class SendPostRequest extends AsyncTask<String, Void, String> {

        protected void onPreExecute(){}

        protected String doInBackground(String... params) {
            // QR code values
            String data1 = params[0];
            String data2 = params[1];
            String data3 = params[2];
            String data4 = params[3];

            // Routing values
            String domain = params[4];
            String team = params[5];

            try {
                // PREVENT SSLv3 and force TLS1.2
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, null, null);
                SSLSocketFactory NoSSLv3Factory = new NoSSLv3SocketFactory(sslContext.getSocketFactory());

                // Create the URL
                URL url = new URL(domain);

                // Create a JSON object
                JSONObject postDataParams = new JSONObject();
                postDataParams.put("team", team);
                postDataParams.put("1", data1);
                postDataParams.put("2", data2);
                postDataParams.put("3", data3);
                postDataParams.put("4", data4);

                // Make the HTTPS connection
                HttpsURLConnection.setDefaultSSLSocketFactory(NoSSLv3Factory);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setReadTimeout(15000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept","application/json");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream());
                os.write(postDataParams.toString());

                os.flush();
                os.close();

                int responseCode=conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    Log.d(TAG, "RECEIVED OK FROM SERVER");

                    BufferedReader in=new BufferedReader(new
                            InputStreamReader(
                            conn.getInputStream()));

                    StringBuffer sb = new StringBuffer("");
                    String line="";

                    while((line = in.readLine()) != null) {
                        sb.append(line);
                        break;
                    }

                    in.close();
                    return sb.toString();

                } else {
                    return "false : " + responseCode;
                }
            }
            catch(Exception e){
                return "Exception: " + e.getMessage();
            }

        }

        @Override
        protected void onPostExecute(String result) {
            // Do nothing
        }
    }
}