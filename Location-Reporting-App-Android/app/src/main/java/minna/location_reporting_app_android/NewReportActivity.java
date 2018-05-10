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

public class NewReportActivity extends AppCompatActivity {
    public final static int CURRENT_LOCATION_REQUEST_CODE = 1;
    public final static int QR_CODE_REQUEST_CODE = 4;
    public final static int TAKE_PICTURE_REQUEST_CODE = 2;
    public final static int CHOOSE_PICTURE_REQUEST_CODE = 3;
    public final static int DESCRIPTION_MIN_LENGTH = 1;
    public final static int DESCRIPTION_MAX_LENGTH = 200;

    private String mSelectedType;
    private String mCurrentPhotoPath;
    private String mSelectedAddress;
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

    //TODO: fix - selected type gets reset but radio button is still selected
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_report);
        // TODO: check if token exists and is valid
        mSession = new UserSession(this);
        if(!mSession.isUserLoggedIn()){
           startActivity(new Intent(NewReportActivity.this, LoginActivity.class));
        }

        mQueue = Singleton.getInstance(this.getApplicationContext()).getRequestQueue();
        mSelectedType="";
        mLatLngAddress="";
        mQrAddress="";
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

        mErrorView.setText(user_id+" "+auth_token);

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
                                // The 'which' argument contains the index position
                                // of the selected item
                                switch (which){
                                    case 0:
                                        //TODO: check camera permission
                                        if(checkPermission(TAKE_PICTURE_REQUEST_CODE, Manifest.permission.CAMERA)){
                                            mErrorView.setText("Camera permission granted");
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
                //TODO: check storage permission
                if(checkPermission(CHOOSE_PICTURE_REQUEST_CODE, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    mErrorView.setText("storage permission granted");
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
                    mErrorView.setText("qr Camera permission granted");
                    startActivityForResult(new Intent(NewReportActivity.this, QrReader.class), 3);
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
    // sets report data, checks if they are valid and submits report request
    private boolean submitReport(){
        String type = mSelectedType;
        if(mSelectedType.length()<1){
            mErrorView.setText("You must select a type");
            return false;
        }
        String description = mDescriptionView.getText().toString();
        if (description.length() < DESCRIPTION_MIN_LENGTH || description.length() > DESCRIPTION_MAX_LENGTH) {
            mErrorView.setText("Description must be between "+DESCRIPTION_MIN_LENGTH+" and "+DESCRIPTION_MAX_LENGTH+" characters");
            return false;
        }
        if(mSelectedType.equals(getString(R.string.option_emergency))){
                String newDesc = "Emergency Type: "+mTypeDropDown.getSelectedItem().toString()+".\n"+description;
                description = newDesc;
        }
        if(lat == 0 || lng == 0){
            mErrorView.setText("You must specify your location");
            return false;
        }
        mErrorView.setText(lat+" "+lng);
        String latString = ""+lat;
        String lngString = ""+lng;

        String address = mQrAddress+"\n"+mLatLngAddress;
        String image = "";
        if(mSelectedImage != null){
            int quality = getQuality();
            image = compressImage(mSelectedImage, quality);
        }

        if (auth_token.length()==0 || user_id.length()==0) {
            mErrorView.setText("not logged in properly");
            return false;
        }
        submitReportRequest(type, description,address,latString,lngString,image,user_id,auth_token);
        return true;
    }

    private void submitReportRequest(String type, String desc, String address, String lat, String lng, String image, String userid, String auth_token){
        String url = getString(R.string.host_url)+"/api/reports";
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

    private JsonObjectRequest createReportRequest(String url, Map<String, String> jsonparams, final String auth_token) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, new JSONObject(jsonparams), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //String token = response.getString("response");
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(NewReportActivity.this);
                        alertDialogBuilder.setMessage("Your report has been submitted");
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
                        // TODO: Handle timeout error
                        if(error instanceof AuthFailureError){
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(NewReportActivity.this);
                            alertDialogBuilder.setMessage("Your session has ended, please log in again.");
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
                        mErrorView.setText(error.toString());
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

    // Result from MapsActivity, picture picker and qr scanner
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CURRENT_LOCATION_REQUEST_CODE) {
            if(resultCode == NewReportActivity.RESULT_OK){
                lat = data.getDoubleExtra("lat", 0);
                lng = data.getDoubleExtra("lng", 0);
                mLatLngAddress = getCompleteAddressString(lat,lng);

            }

            if (resultCode == NewReportActivity.RESULT_CANCELED) {
                mErrorView.setText("There was problem with getting your current location");
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
                mErrorView.setText("There was problem with setting the picture");
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
                mErrorView.setText("There was problem with setting the picture");
            }
        } else if (requestCode == QR_CODE_REQUEST_CODE){
            if(resultCode == RESULT_OK){
                mQrAddress = data.getStringExtra("address");
                TextView addressView = findViewById(R.id.qrTextView);
                addressView.setText(mQrAddress);
                addressView.setVisibility(View.VISIBLE);
            } else{
                mErrorView.setText("QR code was not detected");
            }
        }
    }

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(imageFileName, ".jpeg", storageDir);
            mCurrentPhotoPath = image.getAbsolutePath();
            return image;
        } catch (IOException e){
            //TODO: handle io exception
        }
        return null;
    }

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
            //TODO: handle no camera available
        }
    }

    public String compressImage(Bitmap bitmap, int quality) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        byte[] imageBytes = bos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

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

    private String getCompleteAddressString(double LATITUDE, double LONGITUDE) {
        String strAdd = "";
        TextView addressView = findViewById(R.id.latlngTextView);
        addressView.setText("Setting addres..");
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
            e.printStackTrace();
            strAdd = "Latitude: "+LATITUDE+"\nLongtitude: "+LONGITUDE;
        }
        //show address to user
        addressView.setText(strAdd);
        addressView.setVisibility(View.VISIBLE);
        return strAdd;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
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
        } else {
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
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
                                           String permissions[], int[] grantResults) {
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

}
