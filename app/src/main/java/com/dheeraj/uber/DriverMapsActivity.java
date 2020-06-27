package com.dheeraj.uber;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DriverMapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private LocationRequest mLocationRequest;
    private boolean locationPermissionGranted;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private CameraPosition cameraPosition;
    private LocationCallback locationCallback;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean isContinue = false;
    private Location lastKnownLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private Button logOutBut;
    private  boolean alreadyRemoved=false;
    private int zoomLevel= 15;
    private String customerId="";

    @Override
    protected void onRestart() {
            onMapReady(mMap);
        super.onRestart();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_driver_maps);
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //
        logOutBut=findViewById(R.id.logoutbutt);
        logOutBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeLocationDb();
                alreadyRemoved=true;
                FirebaseAuth.getInstance().signOut();
                Intent intent= new Intent(DriverMapsActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        //
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null && isContinue) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel));


                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference dbrefAvailable = FirebaseDatabase.getInstance().getReference("DriverAvailable");
                        DatabaseReference dbrefWorking = FirebaseDatabase.getInstance().getReference("DriverWorking");
                        GeoFire geoFireAvailable = new GeoFire(dbrefAvailable);
                        GeoFire geoFireWorking = new GeoFire(dbrefWorking);

                        if ("".equals(customerId)) {
                            geoFireWorking.removeLocation(userId);
                            geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                        } else {
                            geoFireAvailable.removeLocation(userId);
                            geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                        }
                        isContinue = true;
                        getDeviceLocation();



                    if (!isContinue && mFusedLocationClient != null) {
                            mFusedLocationClient.removeLocationUpdates(locationCallback);
                        }
                    }
                }

            }
        };
        // Construct a FusedLocationProviderClient.
//        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getAssignedCustomer();
    }

    private void getAssignedCustomer(){
        String driverId= FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef= FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRideID");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                        customerId=dataSnapshot.getValue().toString();
                        getAssignedCustomerPickupLocation();

                }
                else{
                    customerId="";

                    if (markerPickupLocation!=null)
                    markerPickupLocation.remove();
                    if(assignedCustomerPikupLocationRefListener!=null){
                        assignedCustomerPikupLocationRef.removeEventListener(assignedCustomerPikupLocationRefListener);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
    private DatabaseReference assignedCustomerPikupLocationRef;
    private ValueEventListener assignedCustomerPikupLocationRefListener;
    private Marker markerPickupLocation;
    private void getAssignedCustomerPickupLocation(){
         assignedCustomerPikupLocationRef= FirebaseDatabase.getInstance().getReference().child("CustomerRequest").child(customerId).child("l");


         assignedCustomerPikupLocationRefListener=assignedCustomerPikupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&& !customerId.equals("")){
                    List<Object> map=(List<Object>) dataSnapshot.getValue();
                    double locationLat= 0;
                    double locationLong=0;
                    if(map.get(0)!=null){
                        locationLat=Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(0)!=null){
                        locationLong=Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLang= new LatLng(locationLat,locationLong);

                   markerPickupLocation= mMap.addMarker(new MarkerOptions().position(driverLatLang).title("Pickup Location"));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getLocationPermission();

        mMap.setMyLocationEnabled(true);


        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();


    }


    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                getDeviceLocation();
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }


//
//
//        if (ContextCompat.checkSelfPermission(DriverMapsActivity.this,
//                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(DriverMapsActivity.this,
//                    Manifest.permission.ACCESS_FINE_LOCATION)) {
//                ActivityCompat.requestPermissions(DriverMapsActivity.this,
//                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//                locationPermissionGranted = true;
//            } else {
//                ActivityCompat.requestPermissions(DriverMapsActivity.this,
//                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//            }




    private void getDeviceLocation() {

        try {


            if (locationPermissionGranted) {
                if (isContinue) {
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);

                } else {
                    mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {

                                if (location != null) {
                                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                                    mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel));
                                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    DatabaseReference dbrefAvailable = FirebaseDatabase.getInstance().getReference("DriverAvailable");
                                    DatabaseReference dbrefWorking = FirebaseDatabase.getInstance().getReference("DriverWorking");
                                    GeoFire geoFireAvailable = new GeoFire(dbrefAvailable);
                                    GeoFire geoFireWorking = new GeoFire(dbrefWorking);

                                    if ("".equals(customerId)) {
                                        geoFireWorking.removeLocation(userId);
                                        geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                                    } else {
                                        geoFireAvailable.removeLocation(userId);
                                        geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                                    }
                                    isContinue = true;
                                    getDeviceLocation();
                                } else {
                                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);


                                }

                        }
                    });

                }
            }

        } catch (Exception e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {

        onStop();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(!alreadyRemoved)
            removeLocationDb();

    }

    public void removeLocationDb()
    {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference dbrefAvailable = FirebaseDatabase.getInstance().getReference("DriverAvailable");
        GeoFire geoFire = new GeoFire(dbrefAvailable);
        geoFire.removeLocation(userId);

        mFusedLocationClient.removeLocationUpdates(locationCallback);

    }





    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }



}








