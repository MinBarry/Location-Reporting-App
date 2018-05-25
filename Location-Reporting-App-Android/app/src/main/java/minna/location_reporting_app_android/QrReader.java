package minna.location_reporting_app_android;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

/**
 * Displays a camera to scan qr codes
 */
public class QrReader extends AppCompatActivity {
    SurfaceView mCameraView;
    BarcodeDetector mDetector;
    CameraSource mCameraSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_reader);
        mCameraView = (SurfaceView)findViewById(R.id.surfaceView);
        mCameraView.setZOrderMediaOverlay(true);

        mDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE) .build();
        mCameraSource = new CameraSource.Builder(this, mDetector).setFacing(CameraSource.CAMERA_FACING_BACK).setAutoFocusEnabled(true).setRequestedPreviewSize(640, 480) .build();

        mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder holder) {
                try {
                    mCameraSource.start(mCameraView.getHolder());
                } catch (IOException ie) {
                    Intent intent = new Intent();
                    setResult(QrReader.RESULT_CANCELED, intent);
                    finish();
                } catch (SecurityException se){
                    Intent intent = new Intent();
                    setResult(QrReader.RESULT_CANCELED, intent);
                    finish();
                }
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCameraSource.stop();
            }
        });
        mDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if(barcodes.size()>0){
                    String value = barcodes.valueAt(0).rawValue;
                    Intent intent = new Intent();
                    intent.putExtra("address", value);
                    setResult(QrReader.RESULT_OK, intent);
                    finish();
                }
            }
        });
    }
}
