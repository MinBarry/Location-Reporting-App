package minna.location_reporting_app_android;

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
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class NewReportActivity extends AppCompatActivity {

    private String selectedType;
    private String selectedDescription;
    private Spinner spinner;
    private TextView descriptionView;
    private View formView;
    private TextView errorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_report);
        selectedType="";
        descriptionView = (TextView)  findViewById(R.id.description);
        formView = (View) findViewById(R.id.newReportForm);
        errorView = (TextView)findViewById(R.id.error);
        spinner = (Spinner) findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.planets_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setVisibility(View.INVISIBLE);
        //TODO: get auth_token
        //TODO: setup maps
        //TODO: set get current location
        //TODO: set qr code
        //TODO: get selected type
        //TODO: set add picture

        final Button submit = (Button) findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                formView.setVisibility(View.INVISIBLE);
                if(!submitReport()) {
                    formView.setVisibility(View.VISIBLE);
                }
            }
        });

    }
    // sets report data, checks if they are valid and submits report request
    private boolean submitReport(){
        String type = selectedType;
        if(selectedType.length()<1){
            errorView.setText("You must select a type");
            return false;
        }
        String description = descriptionView.getText().toString();
        if (description.length()<1 || description.length()>200) {
            errorView.setText("Description must be between 1 and 200 characters");
        }
        if(selectedType.equals(getString(R.string.option_emergency))){
                String newDesc = "Emergency Type: "+spinner.getSelectedItem().toString()+" "+description;
                description = newDesc;
        }
        //TODO: set lat and lng
        String lat = "";
        String lng="";
        //TODO: set address
        String address="";
        //TODO: set image data
        String image = "";
        //TODO: set userid
        String userid="";
        //TODO: set auth_token
        String auth_token = "";
        submitReportRequest(type, description,address,lat,lng,image,userid,auth_token);
        return true;
    }

    private void submitReportRequest(String type, String desc, String address, String lat, String lng, String image, String userid, String auth_token){
        RequestQueue queue = Volley.newRequestQueue(NewReportActivity.this);
        String url = R.string.host_url+"/Report";
        Map<String, String> jsonparams = new HashMap<String, String>();
        jsonparams.put("type", type);
        jsonparams.put("description",desc);
        jsonparams.put("address",address);
        jsonparams.put("lat",lat);
        jsonparams.put("lng",lng);
        jsonparams.put("image",image);
        jsonparams.put("userid", userid);
        JsonObjectRequest jsonObjectRequest = createReportRequest(url, jsonparams, auth_token);
        queue.add(jsonObjectRequest);
    }

    private JsonObjectRequest createReportRequest(String url, Map<String, String> jsonparams, final String auth_token) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, new JSONObject(jsonparams), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String token = response.getString("response");
                            if(token.length()>0){
                                // TODO: show success message
                                // TODO: move to next page
                                Intent mainIntent = new Intent(NewReportActivity.this, MainActivity.class);
                                startActivity(mainIntent);
                            }

                        } catch (JSONException e) {
                            //TODO: handle exception
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                        formView.setVisibility(View.VISIBLE);
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
        return jsonObjectRequest;
    }
    public void onRadioButtonClicked(View view){
        // Check which radio button was clicked
        spinner.setVisibility(View.INVISIBLE);
        boolean checked = ((RadioButton) view).isChecked();
        switch(view.getId()) {
            case R.id.optionRoutine:
                if (checked){
                    selectedType = getString(R.string.option_routine);
                }
                break;
            case R.id.optionEmergency:
                if (checked){
                    selectedType = getString(R.string.option_emergency);
                    spinner.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.optionSpecial:
                if (checked){
                    selectedType = getString(R.string.option_special);
                }
                break;
        }
        //TODO: remove
        TextView mtextView = findViewById(R.id.error);
        mtextView.setText("type = "+selectedType+"\n");
    }
}
