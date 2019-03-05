package com.example.mappics;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.location.Location;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    public static final int REQUEST_ACCESS_onConnected = 1;

    Location mLocation;
    LocationRequest mLocationRequest;
    ArrayList<Pair<Marker, Bitmap>> PictureRoll;

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;

    String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PictureRoll = new ArrayList<>();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);




        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 0);
            }
        });

        createLocationRequest();
        getLastLocation();

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.setOnMarkerClickListener(this);
    }

    protected void createLocationRequest() {
        Log.i(TAG, "This gets called a lot");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Gets location for app whenever needed
     **/
    public void getLastLocation() {
        //first check to see if I have permissions (marshmallow) if I don't then ask, otherwise start up the demo.
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            //I'm on not explaining why, just asking for permission.
            Log.v(TAG, "asking for permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    MainActivity.REQUEST_ACCESS_onConnected);
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location == null) {
                            Log.w(TAG, "onSuccess:null");
                            return;
                        }
                        mLocation = location;
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), 15));
                        Log.v(TAG, "getLastLocation");
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "getLastLocation:onFailure", e);
                    }
                });

    }

    /**
     * Just close if we're not getting the permissions we need
     **/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.v(TAG, "onRequest result called.");
        boolean coarse = false, fine = false;

        //received result for GPS access
        for (int i = 0; i < grantResults.length; i++) {
            if ((permissions[i].compareTo(Manifest.permission.ACCESS_COARSE_LOCATION) == 0) &&
                    (grantResults[i] == PackageManager.PERMISSION_GRANTED))
                coarse = true;
            else if ((permissions[i].compareTo(Manifest.permission.ACCESS_FINE_LOCATION) == 0) &&
                    (grantResults[i] == PackageManager.PERMISSION_GRANTED))
                fine = true;
        }

        if (coarse && fine) {
            Log.v(TAG, permissions[0] + " permission has now been granted.");
        } else {
            // permission denied.
            Log.v(TAG, "GPS permission was NOT granted.");
            finish();
        }


    }

    /**
     *Deal with result of camera
     **/
    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final Bundle extras = data.getExtras();

        if (extras != null) {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    LatLng tmpLL = new LatLng(location.getLatitude(), location.getLongitude());
                    Marker tmpMarker = mMap.addMarker(new MarkerOptions().position(tmpLL));
                    Bitmap bp = (Bitmap) extras.get("data");
                    Pair tempPair = new Pair(tmpMarker, bp);
                    PictureRoll.add(tempPair);

                }
            });
        } else {
        }
    }

    /**
     * Find which marker was clicked on
     **/
    @Override
    public boolean onMarkerClick(final Marker marker) {
        for(int i = 0; i < PictureRoll.size(); i++)
        {
            if (marker.equals(PictureRoll.get(i).first))
            {
                //Build dialog
                showImage(PictureRoll.get(i).second);
                return true;
            }
        }
        Log.i(TAG, "we never find it");
        return false;
    }

    public void showImage(Bitmap bp) {
        Dialog builder = new Dialog(this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT));
        builder.getWindow().setLayout(1000,1000);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                //nothing;
            }
        });

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bp);
        builder.addContentView(imageView, new RelativeLayout.LayoutParams(1000,1000));
        builder.show();


    }


}
