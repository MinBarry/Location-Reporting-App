package minna.location_reporting_app_android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button mSignout = (Button) findViewById(R.id.signout);
        final Button mEditAccount = (Button) findViewById(R.id.editAccount);
        Button mNewReport = (Button) findViewById(R.id.newReport);

        mSignout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences mSharedPreferences = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
                SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                mEditor.remove(getString(R.string.token_key));
                mEditor.apply();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }

        });

        mEditAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditAccountActivity.class);
                startActivity(intent);
            }

        });

        mNewReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, NewReportActivity.class);
                startActivity(intent);
            }

        });

    }

}
