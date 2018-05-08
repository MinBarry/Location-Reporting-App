package minna.location_reporting_app_android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private RequestQueue mQueue;
    private UserSession mSession;
    private TextView mEmailView;
    private TextView mPasswordView;
    private TextView mConfirmPassView;
    private TextView mUsernameView;
    private TextView mFirstnameView;
    private TextView mLastnameView;
    private TextView mPhoneView;
    private TextView mAddress1View;
    private TextView mAddress2View;
    private TextView mPostalcodeView;
    private TextView mProvinceView;
    private View mForm;
    private View mProgressView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mSession = new UserSession(this);
        mQueue = Singleton.getInstance(this.getApplicationContext()).getRequestQueue();

        mEmailView = findViewById(R.id.email);
        mPasswordView = findViewById(R.id.password);
        mConfirmPassView = findViewById(R.id.confirmPass);
        mUsernameView = findViewById(R.id.username);
        mFirstnameView = findViewById(R.id.firstName);
        mLastnameView = findViewById(R.id.lastName);
        mPhoneView = findViewById(R.id.phone);
        mAddress1View = findViewById(R.id.address1);
        mAddress2View = findViewById(R.id.address2);
        mPostalcodeView = findViewById(R.id.postalCode);
        mProvinceView = findViewById(R.id.province);
        mForm = (View) findViewById(R.id.rgisterForm);
        mProgressView = (View) findViewById(R.id.registerPrrogress);
        Button submit = (Button)findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgress(true);
                boolean valid = true;

                String email = mEmailView.getText().toString();
                String password = mPasswordView.getText().toString();
                String confirmPass = mConfirmPassView.getText().toString();
                String username = mUsernameView.getText().toString();
                String firstname = mFirstnameView.getText().toString();
                String lastname = mLastnameView.getText().toString();
                String address1 = mAddress1View.getText().toString();
                String address2 = mAddress2View.getText().toString();
                String phone = mPhoneView.getText().toString();
                String province = mProvinceView.getText().toString();
                String postalcode = mPostalcodeView.getText().toString();
                valid = valid && isEmailValid(email) && isPasswordValid(password, confirmPass) && isAddressValid(address1,address2,province,postalcode) && isPhoneValid(phone)
                        && isNotEmpty(mUsernameView) && isNotEmpty(mFirstnameView) && isNotEmpty(mLastnameView);
                if(valid){
                    submitReportRequest(email, password, username, firstname, lastname, phone, address1, address2, province, postalcode);
                } else {
                   showProgress(false);
                }

            }
        });
    }

    private void submitReportRequest(String email, String password, String username, String firstname, String lastname, String phone, String address1, String address2, String province, String postalcode){
        String url = getString(R.string.host_url)+"/register";
        Map<String, String> jsonparams = new HashMap<String, String>();
        jsonparams.put("email", email);
        jsonparams.put("password",password);
        jsonparams.put("username",username);
        jsonparams.put("firstname",firstname);
        jsonparams.put("lastname",lastname);
        jsonparams.put("phone",phone);
        jsonparams.put("address1", address1);
        jsonparams.put("address2", address2);
        jsonparams.put("province", province);
        jsonparams.put("postalcode", postalcode);
        JsonObjectRequest jsonObjectRequest = createReportRequest(url, jsonparams);
        mQueue.add(jsonObjectRequest);
    }

    private JsonObjectRequest createReportRequest(String url, Map<String, String> jsonparams) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, new JSONObject(jsonparams), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String token = response.getJSONObject("response").getJSONObject("user").getString("authentication_token");
                            String userid = response.getJSONObject("response").getJSONObject("user").getString("id");
                            if(token.length() > 0 && userid.length() > 0){
                                mSession.logUserIn(token, userid);
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            }
                        } catch (JSONException e) {
                            //showProgress(false);
                            showProgress(false);
                            //TODO: remove
                            TextView mtextView = findViewById(R.id.textView2);
                            mtextView.setText("Response: " + response.toString());
                        }
                        //showProgress(false);
                        showProgress(false);
                        //TODO: remove
                        TextView mtextView = findViewById(R.id.textView2);
                        mtextView.setText("Response: " + response.toString());
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                        //TODO: parse server error messages
                        //showProgress(false);
                        showProgress(false);
                        TextView mtextView = findViewById(R.id.textView2);
                        if (error instanceof TimeoutError){
                            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(RegisterActivity.this);
                            alertDialogBuilder.setMessage("There was a network problem, please try again later.");
                            alertDialogBuilder.setPositiveButton("Ok",
                                    new DialogInterface.OnClickListener(){
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            AlertDialog alertDialog = alertDialogBuilder.create();
                            alertDialog.show();
                        } else {
                            try {
                                JSONObject responseData = new JSONObject(new String(error.networkResponse.data,"UTF-8"));
                                if(responseData.has("response") && responseData.getJSONObject("response").has("errors")) {
                                    if (responseData.getJSONObject("response").getJSONObject("errors").has("email")) {
                                        mEmailView.setError(responseData.getJSONObject("response").getJSONObject("errors").getString("email"));

                                    } else if (responseData.getJSONObject("response").getJSONObject("errors").has("password")) {
                                        mPasswordView.setError(responseData.getJSONObject("response").getJSONObject("errors").getString("password"));
                                    } else if (responseData.getJSONObject("response").getJSONObject("errors").has("username")) {
                                        mUsernameView.setError(responseData.getJSONObject("response").getJSONObject("errors").getString("username"));
                                    }

                                    //TODO: remove
                                    mtextView.setText(new String(error.networkResponse.data, "UTF-8"));
                                }
                            } catch (UnsupportedEncodingException e) {
                                //TODO: remove
                                mtextView.setText("Something went wrong");
                            } catch (JSONException e) {
                                //TODO: remove
                                mtextView.setText("Something went wrong with json");
                            }
                        }
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return jsonObjectRequest;
    }

    private boolean isEmailValid(String email) {
        //TODO:
        if(email.length()<1 || !email.contains("@") ){
            mEmailView.setError(getString(R.string.error_invalid_email));
            return false;
        }
        return true;
    }

    private boolean isPasswordValid(String password, String confirmPass) {
        //TODO:
        if(password.length()<1){
            mPasswordView.setError(getString(R.string.error_invalid_password));
            return false;
        }
        if(!password.equals(confirmPass)){
            mConfirmPassView.setError("Passwords don't match");
            return false;
        }
        return true;
    }

    private boolean isAddressValid(String address1, String address2, String province, String postalCode){
        //TODO:
        return true;
    }

    private boolean isPhoneValid(String phone){
        //TODO:
        return true;
    }

    private boolean isNotEmpty(TextView view){
        if(view.getText().toString().length() <= 1){
            view.setError("This field is required");
            return false;
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mForm.setVisibility(show ? View.GONE : View.VISIBLE);
            mForm.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mForm.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mForm.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
