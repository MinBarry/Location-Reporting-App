package minna.location_reporting_app_android;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class QrReader extends AppCompatActivity {
    SurfaceView mCameraView;
    TextView qrCodeText;
    BarcodeDetector mDetector;
    CameraSource mCameraSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_reader);

        mCameraView = (SurfaceView)findViewById(R.id.surfaceView);
        qrCodeText = (TextView)findViewById(R.id.qrCodeText);

        mDetector = new BarcodeDetector.Builder(this) .setBarcodeFormats(Barcode.QR_CODE) .build();
        mCameraSource = new CameraSource.Builder(this, mDetector) .setRequestedPreviewSize(640, 480) .build();

        mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder holder) {
                try {
                    mCameraSource.start(mCameraView.getHolder());
                } catch (IOException ie) {
                    Log.e("CAMERA SOURCE", ie.getMessage());
                } catch (SecurityException se){
                    //TODO: handle no permission
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
                    qrCodeText.setText(barcodes.valueAt(0).toString());
                    Intent intent = new Intent();
                    intent.putExtra("address", barcodes.valueAt(0).toString());
                    setResult(QrReader.RESULT_OK, intent);
                    finish();
                }
            }
        });
    }
}
