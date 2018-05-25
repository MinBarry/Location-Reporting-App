package minna.location_reporting_app_android;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.icu.text.DateFormat.getDateInstance;

public class NewReportActivity extends AppCompatActivity {
    public final static int CURRENT_LOCATION_REQUEST_CODE = 1;
    public final static int QR_CODE_REQUEST_CODE = 4;
    public final static int TAKE_PICTURE_REQUEST_CODE = 2;
    public final static int CHOOSE_PICTURE_REQUEST_CODE = 3;
    public final static int DESCRIPTION_MIN_LENGTH = 1;
    public final static int DESCRIPTION_MAX_LENGTH = 200;

    private String mSelectedType;
    private String mCurrentPhotoPath;
    private String mLatLngAddress;
    private String mQrAddress;
    private String auth_token;
    private String user_id;

    private Spinner mTypeDropDown;
    private Spinner mQualityDropDown;
    private TextView mDescriptionView;
    private TextView mErrorView;
    private View mFormView;
    private View mProgressView;
    private ImageView mImageView;

    private double lat;
    private double lng;

    private RequestQueue mQueue;
    private UserSession mSession;
    private Bitmap mSelectedImage;
    AlertDialog.Builder mPictureDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_report);
        //check if token exists and is valid
        mSession = new UserSession(this);
        if(!mSession.isUserLoggedIn()) {
            mSession.BuildSessionEndDialog();
        }else {
            JsonObjectRequest validateRequest = mSession.validationRequest();
            RequestQueue queue = Singleton.getInstance(this.getApplicationContext()).getRequestQueue();
            queue.add(validateRequest);
        }

        mQueue = Singleton.getInstance(this.getApplicationContext()).getRequestQueue();
        mSelectedType="";
        mLatLngAddress="";
        mQrAddress="";
        mCurrentPhotoPath = "";
        mDescriptionView = (TextView)  findViewById(R.id.description);
        mFormView = (View) findViewById(R.id.newReportForm);
        mProgressView = (View) findViewById(R.id.newReportProgress);
        mErrorView = (TextView)findViewById(R.id.error);
        mImageView = (ImageView) findViewById(R.id.imageView);
        mSelectedImage = null;
        mTypeDropDown = (Spinner) findViewById(R.id.spinner);
        mQualityDropDown = (Spinner)findViewById(R.id.spinner2);
        // Create emergency type drop down menu
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.types_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTypeDropDown.setAdapter(adapter);
        mTypeDropDown.setVisibility(View.GONE);
        // create image quality drop down
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.quality_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mQualityDropDown.setAdapter(adapter2);
        mQualityDropDown.setVisibility(View.GONE);

        auth_token = mSession.getToken();
        user_id = mSession.getUserId();

        // Setup get current location button
        Button getLocatoion = (Button) findViewById(R.id.currentLocation);
        getLocatoion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkPermission(CURRENT_LOCATION_REQUEST_CODE, Manifest.permission.ACCESS_FINE_LOCATION)){
                    Intent intent = new Intent(NewReportActivity.this, MapsActivity.class);
                    startActivityForResult(intent, CURRENT_LOCATION_REQUEST_CODE);
                }
            }
        });

        // setup add picture button
        Button imageSelect = (Button)findViewById(R.id.addPicture);
        imageSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPictureDialog = new AlertDialog.Builder(NewReportActivity.this);
                mPictureDialog.setTitle("Choose an option")
                        .setItems(R.array.image_picker, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which){
                                    case 0:
                                        if(checkPermission(TAKE_PICTURE_REQUEST_CODE, Manifest.permission.CAMERA)){
                                            dispatchTakePictureIntent();
                                        }
                                        break;
                                    case 1:
                                        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                        startActivityForResult(pickPhoto , CHOOSE_PICTURE_REQUEST_CODE);
                                        break;
                                }
                            }
                        });
                if(checkPermission(CHOOSE_PICTURE_REQUEST_CODE, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    mPictureDialog.create();
                    mPictureDialog.show();
                }
            }
        });
        // setup remove picture button
        Button removePicture = (Button) findViewById(R.id.removePicture);
        removePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
                mImageView.setVisibility(View.GONE);
                mQualityDropDown.setVisibility(View.GONE);
                findViewById(R.id.qualityLable).setVisibility(View.GONE);
                findViewById(R.id.addPicture).setVisibility(View.VISIBLE);
            }
        });
        // setup qr code button
        Button qrCode = (Button) findViewById(R.id.qrCode);
        qrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkPermission(QR_CODE_REQUEST_CODE, Manifest.permission.CAMERA)){
                    startActivityForResult(new Intent(NewReportActivity.this, QrReader.class), QR_CODE_REQUEST_CODE);
                }
            }
        });

        final Button submit = (Button) findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                showProgress(true);
                if(!submitReport()) {
                    showProgress(false);
                    mErrorView.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    /**
     * sets report data, checks if they are valid and submits report request
     */
    private boolean submitReport(){
        String type = mSelectedType;
        if(mSelectedType.length()<1){
            mErrorView.setText(getString(R.string.error_set_type));
            return false;
        }
        String description = mDescriptionView.getText().toString();
        if (description.length() < DESCRIPTION_MIN_LENGTH || description.length() > DESCRIPTION_MAX_LENGTH) {
            mErrorView.setText("Description must be between "+DESCRIPTION_MIN_LENGTH+" and "+DESCRIPTION_MAX_LENGTH+" characters");
            return false;
        }
        if(mSelectedType.equals(getString(R.string.option_emergency))){
                String newDesc = "Emergency Type: "+mTypeDropDown.getSelectedItem().toString()+". \n"+description;
                description = newDesc;
        }
        if((lat == 0 && lng == 0) && mQrAddress.equals("")){
            mErrorView.setText(getString(R.string.error_set_location));
            return false;
        }
        String latString = ""+lat;
        String lngString = ""+lng;

        String address = mQrAddress+"\n"+mLatLngAddress;
        String image = "";
        if(mSelectedImage != null){
            int quality = getQuality();
            image = compressImage(mSelectedImage, quality);
        }

        if (auth_token.length()==0 || user_id.length()==0) {
            return false;
        }
        submitReportRequest(type, description,address,latString,lngString,image,user_id,auth_token);
        return true;
    }

    /**
     * Sends a new report request
     */
    private void submitReportRequest(String type, String desc, String address, String lat, String lng, String image, String userid, String auth_token){
        String url = getString(R.string.host_url)+getString(R.string.route_report);
        Map<String, String> jsonparams = new HashMap<String, String>();
        jsonparams.put("type", type);
        jsonparams.put("description",desc);
        jsonparams.put("address",address);
        jsonparams.put("lat",lat);
        jsonparams.put("lng",lng);
        if(image!=null) {
            jsonparams.put("image", image);
        }
        jsonparams.put("user_id", userid);
        JsonObjectRequest jsonObjectRequest = createReportRequest(url, jsonparams, auth_token);
        mQueue.add(jsonObjectRequest);
    }

    /**
     * Create a new report request object
     */
    private JsonObjectRequest createReportRequest(String url, Map<String, String> jsonparams, final String auth_token) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, new JSONObject(jsonparams), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(NewReportActivity.this);
                        alertDialogBuilder.setMessage(getString(R.string.notice_submit_report));
                        alertDialogBuilder.setPositiveButton("Ok",
                                new DialogInterface.OnClickListener(){
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        startActivity(new Intent(NewReportActivity.this, MainActivity.class));
                                    }
                                });
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error instanceof TimeoutError){
                            mErrorView.setText(getString(R.string.error_timeout));
                        }
                        if(error instanceof AuthFailureError){
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(NewReportActivity.this);
                            alertDialogBuilder.setMessage(getString(R.string.notice_session_end));
                            alertDialogBuilder.setPositiveButton("Ok",
                                    new DialogInterface.OnClickListener(){
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            startActivity(new Intent(NewReportActivity.this, LoginActivity.class));
                                        }
                                    });
                            mSession.logUserOut();
                            AlertDialog alertDialog = alertDialogBuilder.create();
                            alertDialog.show();
                        }
                        mErrorView.setVisibility(View.VISIBLE);
                        showProgress(false);
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("Authentication-Token", auth_token);
                return headers;
            }
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(50 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return jsonObjectRequest;
    }

    /**
     * Specify the type from radio options
     */
    public void onRadioButtonClicked(View view){
        // Check which radio button was clicked
        mTypeDropDown.setVisibility(View.GONE);
        findViewById(R.id.typeLable2).setVisibility(View.GONE);
        boolean checked = ((RadioButton) view).isChecked();
        switch(view.getId()) {
            case R.id.optionRoutine:
                if (checked){
                    mSelectedType = getString(R.string.option_routine);
                }
                break;
            case R.id.optionEmergency:
                if (checked){
                    mSelectedType = getString(R.string.option_emergency);
                    mTypeDropDown.setVisibility(View.VISIBLE);
                    findViewById(R.id.typeLable2).setVisibility(View.VISIBLE);
                }
                break;
            case R.id.optionSpecial:
                if (checked){
                    mSelectedType = getString(R.string.option_special);
                }
                break;
        }
    }

    /**
     * Handles result from MapsActivity, picture picker and qr scanner
     **/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CURRENT_LOCATION_REQUEST_CODE) {
            if(resultCode == NewReportActivity.RESULT_OK){
                lat = data.getDoubleExtra("lat", 0);
                lng = data.getDoubleExtra("lng", 0);
                mLatLngAddress = getCompleteAddressString(lat,lng);
                if(lat == 0 && lng ==0){
                    mErrorView.setText(getString(R.string.error_location));
                }

            }

            if (resultCode == NewReportActivity.RESULT_CANCELED) {
                mErrorView.setText(getString(R.string.error_location));
            }
        } else if(requestCode == TAKE_PICTURE_REQUEST_CODE){
            if(resultCode == RESULT_OK){
                File imgFile = new  File(mCurrentPhotoPath);
                if(imgFile.exists()) {
                    mSelectedImage = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    //mImageData = compressImage(mSelectedImage);
                    mImageView.setImageBitmap(mSelectedImage);
                    mImageView.setVisibility(View.VISIBLE);
                    mQualityDropDown.setVisibility(View.VISIBLE);
                    findViewById(R.id.qualityLable).setVisibility(View.VISIBLE);
                    findViewById(R.id.removePicture).setVisibility(View.VISIBLE);
                    findViewById(R.id.addPicture).setVisibility(View.GONE);
                }
            } else{
                mErrorView.setText(getString(R.string.error_set_picture));
            }
        } else if(requestCode == CHOOSE_PICTURE_REQUEST_CODE){
            if(resultCode == RESULT_OK) {
                Uri selectedImageuri = data.getData();
                try {
                    mSelectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageuri);
                    //mImageData = compressImage(mSelectedImage);
                    mImageView.setImageURI(selectedImageuri);
                    mImageView.setVisibility(View.VISIBLE);
                    mQualityDropDown.setVisibility(View.VISIBLE);
                    findViewById(R.id.qualityLable).setVisibility(View.VISIBLE);
                    findViewById(R.id.removePicture).setVisibility(View.VISIBLE);
                    findViewById(R.id.addPicture).setVisibility(View.GONE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else{
                mErrorView.setText(getString(R.string.error_set_picture));
            }
        } else if (requestCode == QR_CODE_REQUEST_CODE){
            if(resultCode == RESULT_OK){
                mQrAddress = data.getStringExtra("address");
                TextView addressView = findViewById(R.id.qrTextView);
                addressView.setText(mQrAddress);
                addressView.setVisibility(View.VISIBLE);
            } else{
                mErrorView.setText(getString(R.string.error_camera));
            }
        }
    }

    /**
     * Creates a file to store an image in
     */
    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(imageFileName, ".jpeg", storageDir);
            mCurrentPhotoPath = image.getAbsolutePath();
            return image;
        } catch (IOException e){
            mErrorView.setText(getString(R.string.error_storage));
        }
        return null;
    }

    /**
     * Starts the camera to take pictures
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            photoFile = createImageFile();
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "minna.location_reporting_app_android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, TAKE_PICTURE_REQUEST_CODE);
            }
        }else{
            mErrorView.setText(getString(R.string.error_camera));
        }
    }

    /**
     * Compresses the given image to Base64
     */
    public String compressImage(Bitmap bitmap, int quality) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        byte[] imageBytes = bos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    /**
     * Helper to gets the selected quality
     */
    public int getQuality(){
        int quality = 10;
        switch (mQualityDropDown.getSelectedItem().toString()){
            case "Low":
                quality = 10;
                break;
            case "Normal":
                quality = 30;
                break;
            case "Best":
                quality = 80;
                break;
        }
        return quality;
    }

    /**
     * Generates the address of the given coordinates
     */
    private String getCompleteAddressString(double LATITUDE, double LONGITUDE) {
        String strAdd = "";
        TextView addressView = findViewById(R.id.latlngTextView);
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("");

                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                }
                strAdd = strReturnedAddress.toString();
            } else {
                strAdd = "Latitude: "+LATITUDE+"\nLongtitude: "+LONGITUDE;
            }
        } catch (Exception e) {
            strAdd = "Latitude: "+LATITUDE+"\nLongtitude: "+LONGITUDE;
        }
        //show address to user
        addressView.setText(strAdd);
        addressView.setVisibility(View.VISIBLE);
        return strAdd;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private boolean checkPermission(int code, String permission){
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission},code);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case CURRENT_LOCATION_REQUEST_CODE: {
                    Intent intent = new Intent(NewReportActivity.this, MapsActivity.class);
                    startActivityForResult(intent, CURRENT_LOCATION_REQUEST_CODE);
                    break;
                }
                case CHOOSE_PICTURE_REQUEST_CODE: {
                    mPictureDialog.create();
                    mPictureDialog.show();
                    break;
                }
                case TAKE_PICTURE_REQUEST_CODE: {
                    dispatchTakePictureIntent();
                    break;
                }
                case QR_CODE_REQUEST_CODE: {
                    startActivityForResult(new Intent(NewReportActivity.this, QrReader.class), 3);
                    break;
                }

            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null){
            mSelectedType = savedInstanceState.getString("type");
            mLatLngAddress = savedInstanceState.getString("latlngAddress");
            mQrAddress = savedInstanceState.getString("qrAddress");
            if(!mLatLngAddress.equals("")) {
                TextView view = findViewById(R.id.latlngTextView);
                view.setVisibility(View.VISIBLE);
                view.setText(mLatLngAddress);
            }
            if(!mQrAddress.equals("")) {
                TextView view = findViewById(R.id.qrTextView);
                view.setVisibility(View.VISIBLE);
                view.setText(mQrAddress);
            }
            lat = savedInstanceState.getDouble("lat");
            lng = savedInstanceState.getDouble("lng");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedType!= null)
            outState.putString("type", mSelectedType);
        if(mLatLngAddress!= null)
            outState.putString("latlngAddress", mLatLngAddress);
        if(mQrAddress!= null)
            outState.putString("qrAddress", mQrAddress);
        outState.putDouble("lat",lat);
        outState.putDouble("lng",lng);

    }

}
