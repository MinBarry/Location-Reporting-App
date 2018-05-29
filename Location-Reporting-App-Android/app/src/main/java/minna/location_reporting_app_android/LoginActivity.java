package minna.location_reporting_app_android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.ClientError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * Login activity that offers login via  google, facebook or
 * email/password.
 */
public class LoginActivity extends AppCompatActivity{

    public static final int GOOGLE_SIGNIN_CODE = 1;

    private EditText mEmailView;
    private EditText mPasswordView;
    private TextView mErrorView;
    private View mProgressView;
    private View mLoginFormView;

    private RequestQueue mQueue;
    private UserSession mSession;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //check if user is logged in and move to main activity
        mSession = new UserSession(this);
        if(mSession.isUserLoggedIn()){
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }
        setContentView(R.layout.activity_login);

        //init facebook sdk
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);

        // Set up the login form.
        mEmailView = (EditText) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mErrorView = (TextView) findViewById(R.id.loginErrorView);

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        SignInButton googleSignIn = (SignInButton) findViewById(R.id.googleSignIn);

        mGoogleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().requestIdToken(getString(R.string.server_client_id))
                .build());
        mQueue = Singleton.getInstance(this.getApplicationContext()).getRequestQueue();

        // setup facebook sign in
        LoginButton authButton = (LoginButton)findViewById(R.id.facebookSigin);
        authButton.setReadPermissions(Arrays.asList("email"));
        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        showProgress(true);
                        AccessToken token = AccessToken.getCurrentAccessToken();
                        if(token != null && !token.isExpired()){
                            String url = getString(R.string.host_url)+getString(R.string.route_facebook);
                            Map<String,String> params = new HashMap<String, String>();
                            params.put("token", token.getToken());
                            JsonObjectRequest request = createLoginRequest(url, params);
                            mQueue.add(request);
                        }
                    }
                    @Override
                    public void onCancel() {
                        mErrorView.setText(getString(R.string.cancel_login));
                    }
                    @Override
                    public void onError(FacebookException exception) {
                        mErrorView.setText(getString(R.string.error_login));
                    }
                });

        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showProgress(true);
                String email = mEmailView.getText().toString();
                String password = mPasswordView.getText().toString();
                if(mSession.isEmailValid(email, mEmailView)&& mSession.isPasswordValid(password, mPasswordView)){
                    String url = getString(R.string.host_url)+getString(R.string.route_login);
                    Map<String, String> jsonparams = new HashMap<String, String>();
                    jsonparams.put("email", email);
                    jsonparams.put("password",password);
                    JsonObjectRequest jsonObjectRequest = createLoginRequest(url, jsonparams);
                    mQueue.add(jsonObjectRequest);
                }else{
                    showProgress(false);
                }
            }
        });

        googleSignIn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgress(true);
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, GOOGLE_SIGNIN_CODE);
            }
        });

        TextView registerButton = (TextView) findViewById(R.id.registerButton);
        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        TextView resetButton = (TextView) findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new  OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, EditAccountActivity.class));
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }
    /**
     * Handles google login result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        showProgress(false);
        if (requestCode == GOOGLE_SIGNIN_CODE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        } else {
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }
    /**
     * Helper that logs user in using google
     */
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            showProgress(true);
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String token = account.getIdToken();
            //send server request
            String url = getString(R.string.host_url)+getString(R.string.route_google);
            Map<String,String> params = new HashMap<String, String>();
            params.put("token", token);
            JsonObjectRequest request = createLoginRequest(url, params);
            mQueue.add(request);
        } catch (ApiException e) {
            showProgress(false);
            mErrorView.setText(getString(R.string.error_login));
        }
    }
    /**
     * Sends a login request and starts main activity if login was successful
     */
    private JsonObjectRequest createLoginRequest(String url, Map<String,String> jsonparams){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, new JSONObject(jsonparams), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String token = response.getJSONObject("response").getJSONObject("user").getString("authentication_token");
                            String userid = response.getJSONObject("response").getJSONObject("user").getString("id");
                            if(token.length() > 0 && userid.length() > 0){
                                mSession.logUserIn(token, userid);
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            }
                        } catch (JSONException e) {
                            showProgress(false);
                            mErrorView.setText(getString(R.string.error_try_again));
                        }
                        showProgress(false);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error instanceof TimeoutError) {
                            mErrorView.setText(getString(R.string.error_timeout));
                        } else if (error instanceof ClientError){
                            try {
                                JSONObject responseData = new JSONObject(new String(error.networkResponse.data, "UTF-8"));
                                if (responseData.has("response") && responseData.getJSONObject("response").has("errors")) {
                                    if (responseData.getJSONObject("response").getJSONObject("errors").has("email")) {
                                        String errorMsg = responseData.getJSONObject("response").getJSONObject("errors").getJSONArray("email").getString(0);
                                        mEmailView.setError(errorMsg);
                                        if(errorMsg.equals("Email requires confirmation.")){
                                            JsonObjectRequest confirmRequest = mSession.confirmationRequest( mEmailView.getText().toString());
                                            mQueue.add(confirmRequest);
                                        }

                                    } else if (responseData.getJSONObject("response").getJSONObject("errors").has("password")) {
                                        mPasswordView.setError(responseData.getJSONObject("response").getJSONObject("errors").getJSONArray("password").getString(0));
                                    }
                                }
                            } catch (UnsupportedEncodingException e) {
                                mErrorView.setText(getString(R.string.error_try_again));
                            } catch (JSONException e) {
                                mErrorView.setText(getString(R.string.error_try_again));
                            }
                        }
                        showProgress(false);
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
    /**
     * Display progress bar
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {

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
    }

}

