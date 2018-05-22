package minna.location_reporting_app_android;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

//TODO: add onlocationChange()
public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Location mLastKnownLocation;

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    private FusedLocationProviderClient mFusedLocationProviderClient;

    private final LatLng mDefaultLocation = new LatLng(0, 0);
    private static final int DEFAULT_ZOOM = 15;
    private static final String TAG = MapsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        Button msetLocation = (Button) findViewById(R.id.setLocation);
        msetLocation.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("lat", mLastKnownLocation.getLatitude());
                intent.putExtra("lng", mLastKnownLocation.getLongitude());
                setResult(MapsActivity.RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        updateLocationUI();
        getDeviceLocation();
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
             mMap.getUiSettings().setMyLocationButtonEnabled(true);

        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        try {
            Task locationResult = mFusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        mLastKnownLocation = (Location)task.getResult();
                        mMap.addMarker(new MarkerOptions().position(new LatLng(mLastKnownLocation.getLatitude(),mLastKnownLocation.getLongitude())).title("Current Location"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(mLastKnownLocation.getLatitude(),
                                        mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        Log.d(TAG, String.format(" " + mLastKnownLocation.getLongitude()+" "+mLastKnownLocation.getLatitude() ));
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.");
                        Log.e(TAG, "Exception: %s", task.getException());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                        mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    }
                    }
                });

        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }
}