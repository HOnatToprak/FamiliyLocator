package com.example.merv.familylocator;

import android.content.Context;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private GoogleMap mMap;
    private LocationManager locationManager;
    Marker myLocMarker;
    boolean firstLocationDataArrived = false;
    DatabaseReference activeUsers;
    DatabaseReference currentUser;
    HashMap<String,Marker> userMarkers;
    FirebaseDatabase firebaseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        activeUsers = FirebaseDatabase.getInstance().getReference("ActiveUsers");
        firebaseDatabase = FirebaseDatabase.getInstance();


    }



    @Override
    protected void onResume() {
        super.onResume();
        becomeActive();
    }

    @Override
    protected void onPause() {
        super.onPause();
        becomePassive();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        becomePassive();
        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        initializeUserMarkers();
        //EventBus.getDefault().register(this);
        /*LatLng latLng = new LatLng(-122.084, 37.422);
        if (myLocMarker == null)
            myLocMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("This"));
        myLocMarker.setPosition(latLng);
        if (firstLocationDataArrived == false) {
            firstLocationDataArrived = true;
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocMarker.getPosition(), 14));

        }*/
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLocationUpdate (LocationUpdateEvent locationUpdateEvent){
        Log.d(" on MapsActivity", "onLocationUpdate: worked");
            LatLng latLng = new LatLng(locationUpdateEvent.location.getLatitude(),locationUpdateEvent.location.getLongitude());
            if(myLocMarker == null)
                myLocMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("This"));
            else
                myLocMarker.setPosition(latLng);
            if(firstLocationDataArrived == false) {
                firstLocationDataArrived = true;
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocMarker.getPosition(),14));
            }
    }
    void becomeActive () {
        Log.d("On Become Active","start");
        currentUser = activeUsers.push();
        String currentuserexists = currentUser != null ? "current user not null" : "current user null";
        Log.d("On Become Active", currentuserexists);
        String uId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("On Become Active", "User ID = " + uId);
        currentUser.setValue(new FireActiveUserModel(uId));
    }
    void becomePassive () {
        currentUser.removeValue();
    }
    void initializeUserMarkers () {
        if(userMarkers==null)
            userMarkers = new HashMap<>();

        /*firebaseDatabase.getReference("Locations").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot x:dataSnapshot.getChildren()){
                    LatLng latLng = new LatLng((double)x.child("latitude").getValue(),(double)x.child("longtitude").getValue());
                    Marker marker = mMap.addMarker(new MarkerOptions().position(latLng));
                    userMarkers.put(dataSnapshot.getKey(),marker);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });*/

        firebaseDatabase.getReference("Locations").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot x, String s) {
                LatLng latLng = new LatLng((Double)x.child("latitude").getValue(),(Double)x.child("longtitude").getValue());
                Marker marker = mMap.addMarker(new MarkerOptions().position(latLng));
                userMarkers.put(x.getKey(),marker);
            }

            @Override
            public void onChildChanged(DataSnapshot x, String s) {
                LatLng latLng = new LatLng((Double)x.child("latitude").getValue(),(Double) x.child("longtitude").getValue());
                userMarkers.get(x.getKey()).setPosition(latLng);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                userMarkers.remove(dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

}
