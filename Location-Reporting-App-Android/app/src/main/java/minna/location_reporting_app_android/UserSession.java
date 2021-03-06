package minna.location_reporting_app_android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.login.LoginManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles user login, logout and confirmation
 */
public class UserSession{

    private Context context;
    private SharedPreferences mSharedPreferences;

    public  UserSession(Context context){
        this.context = context;
        this.mSharedPreferences = context.getSharedPreferences(context.getString(R.string.pref_name), context.MODE_PRIVATE);
    }

    /**
     * Logs user in
     */
    public void logUserIn(String token, String id){
        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
        mEditor.putString(context.getString(R.string.token_key), token);
        mEditor.putString(context.getString(R.string.user_key), id);
        mEditor.apply();
    }

    /**
     * Logs user out
     */
    public void logUserOut(){
        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
        mEditor.remove(context.getString(R.string.token_key));
        mEditor.remove(context.getString(R.string.user_key));
        mEditor.apply();
        LoginManager.getInstance().logOut();
    }

    /**
     * Check if the given user is logged in
     */
    public boolean isUserLoggedIn(){
        return (mSharedPreferences.contains(context.getString(R.string.token_key)));
    }

    /**
     * Returns logged in user token
     */
    public String getToken() {
        return mSharedPreferences.getString(context.getString(R.string.token_key),"");
    }

    /**
     * Returns logged in user id
     */
    public String getUserId(){
        return mSharedPreferences.getString(context.getString(R.string.user_key),"");
    }

    /**
     * Checks if given email is the right format
     */
    public boolean isEmailValid(String email, TextView emailView) {
        if(email.length()<1 || !email.contains("@") ){
            emailView.setError(context.getString(R.string.error_invalid_email));
            return false;
        }
        return true;
    }
    /**
     * Checks if given passwords match
     */
    public boolean isPasswordConfirmValid(String password, String confirmPass, TextView passwordView) {
        if(password.length()<1){
            passwordView.setError(context.getString(R.string.error_invalid_password));
            return false;
        }
        return true;
    }
    /**
     * Checks if given password is the right format
     */
    public boolean isPasswordValid(String password, TextView passwordView) {
        if(password.length()<5){
            passwordView.setError(context.getString(R.string.error_invalid_password));
            return false;
        }
        return true;
    }
    /**
     * Send a confirmation request and notify the user
     */
    public JsonObjectRequest confirmationRequest(String email){
        String url = context.getString(R.string.host_url)+context.getString(R.string.route_confirm);
        Map<String, String> jsonparams = new HashMap<String, String>();
        jsonparams.put("email", email);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, new JSONObject(jsonparams), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                        alertDialogBuilder.setMessage(context.getString(R.string.notice_confirmation));
                        alertDialogBuilder.setPositiveButton("Ok",
                                new DialogInterface.OnClickListener(){
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(50 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return jsonObjectRequest;
    }
    /**
     * Send a token validation request and logs user out if it expired
     */
    public JsonObjectRequest validationRequest(){
        String url = context.getString(R.string.host_url)+context.getString(R.string.route_validate);
        Map<String, String> jsonparams = new HashMap<String, String>();
        jsonparams.put("id", getUserId());
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, new JSONObject(jsonparams), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                    }
                    }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if(error instanceof AuthFailureError){
                            BuildSessionEndDialog();
                        }
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("Authentication-Token", getToken());
                return headers;
            }
        };
        return jsonObjectRequest;
    }
    /**
     * Builds and displays a session end dialog
     */
    public void BuildSessionEndDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setMessage(context.getString(R.string.notice_session_end));
        alertDialogBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        context.startActivity(new Intent(context, LoginActivity.class));
                    }
                });
        logUserOut();
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}
