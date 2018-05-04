package minna.location_reporting_app_android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
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
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
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
import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity{

    public static final int GOOGLE_SIGNIN_CODE = 1;
    public static final int FACEBOOK_SIGNIN_CODE = 2;
    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    private RequestQueue mQueue;
    private UserSession mSession;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //init facebook sdk
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        //check if user is logged in and move to main activity
        mSession = new UserSession(this);
        if(mSession.isUserLoggedIn()){
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mEmailView = (EditText) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        SignInButton googleSignIn = (SignInButton) findViewById(R.id.googleSignIn);

        mGoogleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().requestIdToken(getString(R.string.server_client_id))
                .build());
        mQueue = Singleton.getInstance(this.getApplicationContext()).getRequestQueue();

        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        // App code

                        showProgress(true);
                        AccessToken token = AccessToken.getCurrentAccessToken();
                        if(token != null && !token.isExpired()){
                            Log.w("FACEBOOK", token.getPermissions().toString());
                            String url = getString(R.string.host_url)+"/google-login";
                            Map<String,String> params = new HashMap<String, String>();
                            params.put("token", token.getToken());
                            JsonObjectRequest request = createLoginRequest(url, params);
                            mQueue.add(request);
                        }
                    }

                    @Override
                    public void onCancel() {
                        // App code
                        Log.w("FACEBOOK", "cancel");
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // App code
                        Log.w("FACEBOOK", "error");
                    }
                });

        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showProgress(true);
                String email = mEmailView.getText().toString();
                String password = mPasswordView.getText().toString();
                if(isEmailValid(email)&& isPasswordValid(password)){
                    String url = getString(R.string.host_url)+"/login";
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

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

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

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            showProgress(true);
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String token = account.getIdToken();
            Log.w("LOG IN", "google token = "+ token);
            //send server request
            String url = getString(R.string.host_url)+"/google-login";
            Map<String,String> params = new HashMap<String, String>();
            params.put("token", token);
            JsonObjectRequest request = createLoginRequest(url, params);
            mQueue.add(request);
        } catch (ApiException e) {
            showProgress(false);
            Log.w("LOG IN", "signInResult:failed code=" + e.getStatusCode());
        }
    }

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
                            Log.w("LOG IN", "Response: " + response.toString());
                        }
                        showProgress(false);
                        Log.w("LOG IN", "Response: " + response.toString());
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error google login error
                        showProgress(false);
                        try {
                            JSONObject responseData = new JSONObject(new String(error.networkResponse.data,"UTF-8"));
                            if(responseData.has("response") && responseData.getJSONObject("response").has("errors")) {
                                if (responseData.getJSONObject("response").getJSONObject("errors").has("email")) {
                                    mEmailView.setError(responseData.getJSONObject("response").getJSONObject("errors").getString("email"));

                                } else if (responseData.getJSONObject("response").getJSONObject("errors").has("password")) {
                                    mPasswordView.setError(responseData.getJSONObject("response").getJSONObject("errors").getString("password"));
                                }
                                Log.w("LOG IN", new String(error.networkResponse.data, "UTF-8"));
                            }
                        } catch (UnsupportedEncodingException e) {
                            Log.w("LOG IN", "Something went wrong");
                        } catch (JSONException e) {
                            Log.w("LOG IN", "Something went wrong with json");
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

    private boolean isEmailValid(String email) {
        if(email.length()<1 || !email.contains("@") ){
            mEmailView.setError(getString(R.string.error_invalid_email));
            return false;
        }
        return true;
    }

    private boolean isPasswordValid(String password) {
        if(password.length()<5){
            mPasswordView.setError(getString(R.string.error_invalid_password));
            return false;
        }
        return true;
    }

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

}

