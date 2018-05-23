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
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class EditAccountActivity extends AppCompatActivity {

    TextView mEmailView;
    TextView mErrorView;
    View mResetForm;
    ProgressBar mProgressView;
    RequestQueue mQueue;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_account);
        mEmailView = (TextView) findViewById(R.id.resetEmail);
        mResetForm = (View) findViewById(R.id.resetPassForm);
        mErrorView = (TextView) findViewById(R.id.registerErrorView);
        mProgressView = (ProgressBar) findViewById(R.id.editAccount_progress);
        mQueue = Singleton.getInstance(EditAccountActivity.this).getRequestQueue();
        findViewById(R.id.resetButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmailView.getText().toString();
                if (isEmailValid(email)){
                    showProgress(true);
                    String url = getString(R.string.host_url)+"/reset";
                    Map<String,String> params = new HashMap<String, String>();
                    params.put("email", email);
                    JsonObjectRequest restRequest = resetPasswordRequest(url,params);
                    mQueue.add(restRequest);
                }
            }
        });
    }

    private boolean isEmailValid(String email) {
        if(email.length()<1 || !email.contains("@") ){
            mEmailView.setError(getString(R.string.error_invalid_email));
            return false;
        }
        return true;
    }

    private JsonObjectRequest resetPasswordRequest(String url, Map<String,String> jsonparams){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, new JSONObject(jsonparams), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(EditAccountActivity.this);
                        alertDialogBuilder.setMessage("An email has been sent with instructions to reset password.");
                        alertDialogBuilder.setPositiveButton("Ok",
                                new DialogInterface.OnClickListener(){
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        startActivity(new Intent(EditAccountActivity.this, LoginActivity.class));
                                    }
                                });
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showProgress(false);
                        if (error instanceof TimeoutError) {
                            mErrorView.setText(getString(R.string.error_timeout));
                        } else if (error instanceof ClientError) {
                            try {
                                JSONObject responseData = new JSONObject(new String(error.networkResponse.data, "UTF-8"));
                                if (responseData.has("response") && responseData.getJSONObject("response").has("errors")) {
                                    if (responseData.getJSONObject("response").getJSONObject("errors").has("email")) {
                                        String errorMsg = responseData.getJSONObject("response").getJSONObject("errors").getJSONArray("email").getString(0);
                                        mEmailView.setError(errorMsg);
                                        if (errorMsg.equals("Email requires confirmation.")) {
                                            UserSession session = new UserSession(EditAccountActivity.this);
                                            JsonObjectRequest confirmRequest = session.confirmationRequest(mEmailView.getText().toString());
                                            mQueue.add(confirmRequest);
                                        }
                                    }
                                }
                            } catch (UnsupportedEncodingException e) {
                                mErrorView.setText(getString(R.string.error_try_again));
                            } catch (JSONException e) {
                                mErrorView.setText(getString(R.string.error_try_again));
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
        return jsonObjectRequest;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mResetForm.setVisibility(show ? View.GONE : View.VISIBLE);
        mResetForm.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mResetForm.setVisibility(show ? View.GONE : View.VISIBLE);
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
