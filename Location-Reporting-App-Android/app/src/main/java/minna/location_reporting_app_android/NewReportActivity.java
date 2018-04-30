package minna.location_reporting_app_android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NewReportActivity extends AppCompatActivity {

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
                startActivityForResult(intent, 1);
            }
        });

        //TODO: set qr code
        //TODO: set add picture

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
        //TODO: set image data
        String image = "";

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
        jsonparams.put("image",image);
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
                        //TODO: alert success
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
                            //TODO: set alert
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

    // Result from MapsActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == NewReportActivity.RESULT_OK){
                lat = data.getDoubleExtra("lat", 0);
                lng = data.getDoubleExtra("lng", 0);
            }

            if (resultCode == NewReportActivity.RESULT_CANCELED) {
                //TODO: handle
            }
        }
    }
}
