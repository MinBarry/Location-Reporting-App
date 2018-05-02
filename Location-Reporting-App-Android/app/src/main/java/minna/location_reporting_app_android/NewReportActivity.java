package minna.location_reporting_app_android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
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
import java.util.Map;

public class NewReportActivity extends AppCompatActivity {
    public final static int CURRENT_LOCATION_REQUEST_CODE = 1;
    public final static int QR_CODE_REQUEST_CODE = 4;
    public final static int TAKE_PICTURE_REQUEST_CODE = 2;
    public final static int CHOOSE_PICTURE_REQUEST_CODE = 3;

    private String mSelectedType;
    private String mSelectedDescription;
    private Spinner mSpinner;
    private TextView mDescriptionView;
    private View mFormView;
    private TextView mErrorView;
    private double lat;
    private double lng;
    private String auth_token;
    private String user_id;
    private RequestQueue mQueue;
    private UserSession mSession;
    private ImageView mImageView;
    private String mCurrentPhotoPath;
    private String mImageData;

    //todo: ask for camera, file, and location permissions
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
        mDescriptionView = (TextView)  findViewById(R.id.description);
        mFormView = (View) findViewById(R.id.newReportForm);
        mErrorView = (TextView)findViewById(R.id.error);
        mSpinner = (Spinner) findViewById(R.id.spinner);
        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageData = null;
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.planets_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSpinner.setAdapter(adapter);
        mSpinner.setVisibility(View.INVISIBLE);

        auth_token = mSession.getToken();
        user_id = mSession.getUserId();

        mErrorView.setText(user_id+" "+auth_token);

        // Setup get current location button
        Button getLocatoion = (Button) findViewById(R.id.currentLocation);
        getLocatoion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NewReportActivity.this, MapsActivity.class);
                startActivityForResult(intent, CURRENT_LOCATION_REQUEST_CODE);
            }
        });

        //TODO: set qr code
        //TODO: set add picture
        Button imageSelect = (Button)findViewById(R.id.addPicture);
        imageSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(NewReportActivity.this);
                builder.setTitle("Choose an option")
                        .setItems(R.array.image_picker, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // The 'which' argument contains the index position
                                // of the selected item
                                switch (which){
                                    case 0:
                                        dispatchTakePictureIntent();
                                        break;
                                    case 1:
                                        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                        startActivityForResult(pickPhoto , CHOOSE_PICTURE_REQUEST_CODE);
                                        break;
                                }
                            }
                        });
                builder.create();
                builder.show();
            }
        });
        Button qrCode = (Button) findViewById(R.id.qrCode);
        qrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(NewReportActivity.this, QrReader.class), 3);
            }
        });

        final Button submit = (Button) findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                mFormView.setVisibility(View.INVISIBLE);
                if(!submitReport()) {
                    mFormView.setVisibility(View.VISIBLE);
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
        if (description.length()<1 || description.length()>200) {
            mErrorView.setText("Description must be between 1 and 200 characters");
        }
        if(mSelectedType.equals(getString(R.string.option_emergency))){
                String newDesc = "Emergency Type: "+mSpinner.getSelectedItem().toString()+" "+description;
                description = newDesc;
        }
        if(lat == 0 || lng == 0){
            mErrorView.setText("You must specify your location");
            return false;
        }
        mErrorView.setText(lat+" "+lng);
        String latString = ""+lat;
        String lngString = ""+lng;

        //TODO: set address
        String address="";
        String image = mImageData;

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
                        // TODO: Handle error
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
                            AlertDialog alertDialog = alertDialogBuilder.create();
                            alertDialog.show();
                            mSession.logUserOut();
                        }
                        mErrorView.setText(error.toString());
                        mFormView.setVisibility(View.VISIBLE);
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
        mSpinner.setVisibility(View.INVISIBLE);
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
                    mSpinner.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.optionSpecial:
                if (checked){
                    mSelectedType = getString(R.string.option_special);
                }
                break;
        }
        //TODO: remove
        TextView mtextView = findViewById(R.id.error);
        mtextView.setText("type = "+mSelectedType+"\n");
    }

    // Result from MapsActivity, and picture picker
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CURRENT_LOCATION_REQUEST_CODE) {
            if(resultCode == NewReportActivity.RESULT_OK){
                lat = data.getDoubleExtra("lat", 0);
                lng = data.getDoubleExtra("lng", 0);
            }

            if (resultCode == NewReportActivity.RESULT_CANCELED) {
                //TODO: handle
            }
        } else if(requestCode == TAKE_PICTURE_REQUEST_CODE){
            if(resultCode == RESULT_OK){
                File imgFile = new  File(mCurrentPhotoPath);
                if(imgFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    mImageData = compressImage(bitmap);
                    mImageView.setImageBitmap(bitmap);
                }
            } else{
                //TODO: handle
            }
        } else if(requestCode == CHOOSE_PICTURE_REQUEST_CODE){
            if(resultCode == RESULT_OK) {
                Uri selectedImage = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    mImageData = compressImage(bitmap);
                    mImageView.setImageURI(selectedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else{
                //TODO: handle
            }
        } else if (requestCode == QR_CODE_REQUEST_CODE){
            if(resultCode == RESULT_OK){
                //TODO: get qr code data
            } else{
                //TODO: handle
            }
        }
    }

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(imageFileName, ".png", storageDir);
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

    public String compressImage(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        //TODO: set quality
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] imageBytes = bos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);

    }

}
