package minna.location_reporting_app_android;

import android.content.Context;
import android.content.SharedPreferences;

import com.facebook.login.LoginManager;


public class UserSession{

    private Context context;
    private SharedPreferences mSharedPreferences;

    public  UserSession(Context context){
        this.context = context;
        this.mSharedPreferences = context.getSharedPreferences(context.getString(R.string.pref_name), context.MODE_PRIVATE);
    }
    public void logUserIn(String token, String id){
        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
        mEditor.putString(context.getString(R.string.token_key), token);
        mEditor.putString(context.getString(R.string.user_key), id);
        mEditor.apply();
    }
    public void logUserOut(){
        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
        mEditor.remove(context.getString(R.string.token_key));
        mEditor.remove(context.getString(R.string.user_key));
        mEditor.apply();
        LoginManager.getInstance().logOut();
    }
    public boolean isUserLoggedIn(){
        return (mSharedPreferences.contains(context.getString(R.string.token_key)));
    }

    public String getToken() {
        return mSharedPreferences.getString(context.getString(R.string.token_key),"");
    }

    public String getUserId(){
        return mSharedPreferences.getString(context.getString(R.string.user_key),"");
    }
}
