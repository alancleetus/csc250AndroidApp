package com.example.rsalogin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class ScanActivity extends AppCompatActivity {

    private String TAG = "MY-LOG";

    SurfaceView surfaceView;
    BarcodeDetector barcodeDetector;
    CameraSource cameraSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        surfaceView = findViewById(R.id.surfaceView);
        barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();
        cameraSource = new CameraSource.Builder(this, barcodeDetector).setRequestedPreviewSize(400, 400).setAutoFocusEnabled(true).build();

        getPermissions();
        startCamera();
        scanForBarcode();
    }


    private void getPermissions() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Camera Permissions Denied");
            int MY_CAMERA_REQUEST_CODE = 200;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_CAMERA_REQUEST_CODE);
        }

        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Vibrate Permissions Denied");
            int MY_REQUEST_CODE = 200;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.VIBRATE},
                    MY_REQUEST_CODE);
        }
    }

    private void startCamera() {
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    cameraSource.start(holder);
                    Log.d(TAG, "startCamera: Starting Camera");
                }
                catch(IOException e){ e.printStackTrace(); }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) { cameraSource.stop(); }
        });
    }

    private void scanForBarcode() {

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {

            boolean found = false;

            @Override
            public void release() { }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {

                final SparseArray<Barcode> qrCodes = detections.getDetectedItems();

                if(qrCodes.size()!=0 && !found)
                {
                    found=true;

                    Log.d(TAG, "scanForBarcode: QR Value = " + qrCodes.valueAt(0).displayValue.toString());

                    //send qr code value back to main activity
                    Intent intent = new Intent();
                    intent.putExtra("qrCodeVal", String.valueOf(qrCodes.valueAt(0).displayValue.toString()));
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
    }
}

