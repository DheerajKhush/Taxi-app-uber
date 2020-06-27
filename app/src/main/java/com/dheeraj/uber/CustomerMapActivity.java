package com.dheeraj.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

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
    private  LatLng pickUpLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private Button logoutBut, requestButton,settingsButton;
    private  boolean alreadyRemoved=false;
    private boolean requestBool=false;
    private int zoomLevel= 15;
    private Marker pickupMarker;
    private int radius=1;
    private boolean driverFound=false;
    private  String driverFoundID;

    private GeoQuery geoQuery;
    @Override
    protected void onRestart() {
        onMapReady(mMap);
        super.onRestart();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_customer_map);
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //
        logoutBut=findViewById(R.id.logoutbutt);
        logoutBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!alreadyRemoved){
                    removeLocationDb();
                }
                alreadyRemoved=true;
                FirebaseAuth.getInstance().signOut();
                Intent intent= new Intent(CustomerMapActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        settingsButton=findViewById(R.id.settingsBut);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(CustomerMapActivity.this,CustomerUserSettings.class);
                startActivity(intent);

            }
        });
        //
        requestButton=findViewById(R.id.requestbutton);
        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestBool){
                    requestBool=false;
                    geoQuery.removeAllListeners();
                    if(driverLocationRef != null)
                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    if(driverFoundID!=null) {
                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                        driverRef.setValue(true);
                        driverFoundID = null;
                    }
                    driverFound=false;
                    radius=1;
                    if (!alreadyRemoved) {
                        removeLocationDb();
                    }
                    alreadyRemoved=true;

                    if(pickupMarker!=null){
                        pickupMarker.remove();
                    }
                    if(mDriverMarker!=null){
                        mDriverMarker.remove();
                    }
                    requestButton.setText("Call Uber");


                }
                else{
                    requestBool=true;
                    String userId= FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref= FirebaseDatabase.getInstance().getReference("CustomerRequest");
                    GeoFire geoFire= new GeoFire(ref);
                    geoFire.setLocation(userId,new GeoLocation(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude()));
                    pickUpLocation=new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
                    pickupMarker=  mMap.addMarker(new MarkerOptions().position(pickUpLocation).title("Your PickUp Location"));
                    requestButton.setText("Getting your Driver..");

                    getClosestDriver();
                }

//                  if(requestButton.getText().equals("Driver Found"))
//                  {
//                      requestButton.setClickable(false);
//                  }
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
//
//                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//                        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
//                        GeoFire geoFire = new GeoFire(dbref);
//                        geoFire.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                        lastKnownLocation=location;
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
    }

    private void getClosestDriver(){
        DatabaseReference driverLocationRef =FirebaseDatabase.getInstance().getReference().child("DriverAvailable");
        GeoFire geoFire= new GeoFire(driverLocationRef);
         geoQuery=geoFire.queryAtLocation(new GeoLocation(pickUpLocation.latitude,pickUpLocation.longitude),radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound&& requestBool){
                driverFound=true;
                driverFoundID=key;
                DatabaseReference driverRef= FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                String customerId= FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map= new HashMap();
                    map.put("customerRideID",customerId);
                    driverRef.updateChildren(map);
                    getDriverLocation();
                    requestButton.setText("Looking for Driver Location..");


                }
            }


            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!driverFound){
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
    private Marker mDriverMarker;
    private  DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;
    private void getDriverLocation(){

        driverLocationRef= FirebaseDatabase.getInstance().getReference().child("DriverWorking").child(driverFoundID).child("l");
         driverLocationRefListener=driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&& requestBool){
                    List<Object> map= (List<Object>) dataSnapshot.getValue();
                    double locationLat= 0;
                    double locationLng=0;
                    requestButton.setText("Driver Found");
                    if(map.get(0)!=null){
                        locationLat=Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(0)!=null){
                        locationLng=Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLang= new LatLng(locationLat,locationLng);
                    if(mDriverMarker!=null){
                        mDriverMarker.remove();
                    }
                    Location loc1=new Location("");
                    loc1.setLatitude(pickUpLocation.latitude);
                    loc1.setLongitude(pickUpLocation.longitude);

                    Location loc2=new Location("");
                    loc1.setLatitude(driverLatLang.latitude);
                    loc1.setLongitude(driverLatLang.longitude);

                    float distance =loc1.distanceTo(loc2);
                    if(distance<100){

                        requestButton.setText("Driver is Here ");
                    }else{
                    requestButton.setText("Driver found " + (distance));
                    }
                    mDriverMarker=mMap.addMarker(new MarkerOptions().position(driverLatLang).title("Your driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.caricon)));




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

//                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//                                DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
//                                GeoFire geoFire = new GeoFire(dbref);
//                                geoFire.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                                lastKnownLocation=location;
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
    protected void onStop() {
        super.onStop();
//        if(!alreadyRemoved)
//       removeLocationDb();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!alreadyRemoved)
            removeLocationDb();

    }
        public void removeLocationDb()
    {
         String userId= FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref= FirebaseDatabase.getInstance().getReference("CustomerRequest");
                    GeoFire geoFire= new GeoFire(ref);
                    geoFire.removeLocation(userId);

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
