package minna.location_reporting_app_android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
//TODO: make singleton request queue
public class LoginActivity extends AppCompatActivity{


    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    SharedPreferences.Editor mEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //check if user is logged in and move to main activity
        SharedPreferences mSharedPreferences = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();
        if(mSharedPreferences.contains(getString(R.string.token_key))){
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mEmailView = (EditText) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);

        //TODO: remove
        mEditor.putString(getString(R.string.token_key),"token-token");
        mEditor.putString(getString(R.string.user_key), "22");
        mEditor.apply();
        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(mainIntent);

        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // Instantiate the RequestQueue.
                showProgress(true);
                String email = mEmailView.getText().toString();
                String password = mPasswordView.getText().toString();
                if(isEmailValid(email)&& isPasswordValid(password)){
                    //TOD: change to singleton queue
                    RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);
                    //TODO: use resource strring
                    String url = "http://10.0.2.2:58270/login";
                    Map<String, String> jsonparams = new HashMap<String, String>();
                    jsonparams.put("email", email);
                    jsonparams.put("password",password);
                    JsonObjectRequest jsonObjectRequest = createLoginRequest(url, jsonparams);
                    queue.add(jsonObjectRequest);
                }else{
                    showProgress(false);
                }

            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }


    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        if(email.length()<1 || !email.contains("@") ){
            mEmailView.setError(getString(R.string.error_invalid_email));
            return false;
        }
        return true;
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        if(password.length()<1){
            mPasswordView.setError(getString(R.string.error_invalid_password));
            return false;
        }
        return true;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private JsonObjectRequest createLoginRequest(String url, Map<String,String> jsonparams){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, new JSONObject(jsonparams), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String token = response.getJSONObject("response").getJSONObject("user").getString("authentication_token");
                            String userid = response.getJSONObject("response").getJSONObject("user").getString("user_id");
                            if(token.length() > 0 && userid.length() > 0){
                                mEditor.putString(getString(R.string.token_key), token);
                                mEditor.putString(getString(R.string.user_key), userid);
                                mEditor.apply();

                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            }


                        } catch (JSONException e) {
                            showProgress(false);
                            //TODO: remove
                            TextView mtextView = findViewById(R.id.textView);
                            mtextView.setText("Response: " + response.toString());
                        }
                        showProgress(false);
                        //TODO: remove
                        TextView mtextView = findViewById(R.id.textView);
                        mtextView.setText("Response: " + response.toString());
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                        showProgress(false);
                        TextView mtextView = findViewById(R.id.textView);
                        try {
                            JSONObject responseData = new JSONObject(new String(error.networkResponse.data,"UTF-8"));
                            if(responseData.has("response") && responseData.getJSONObject("response").has("errors")) {
                                if (responseData.getJSONObject("response").getJSONObject("errors").has("email")) {
                                    mEmailView.setError(responseData.getJSONObject("response").getJSONObject("errors").getString("email"));

                                } else if (responseData.getJSONObject("response").getJSONObject("errors").has("password")) {
                                    mPasswordView.setError(responseData.getJSONObject("response").getJSONObject("errors").getString("password"));
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
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };
        return jsonObjectRequest;
    }

}

