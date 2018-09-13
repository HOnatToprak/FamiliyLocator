package com.example.merv.familylocator;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.service.carrier.CarrierMessagingService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;

import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;

public class LocationService extends Service{
    private static final String TAG = "Location Service";
    private LocationRequest mLocationRequest;
    private static final long REQUEST_INTERVAL = 2000;
    private static final long FASTEST_REQUEST_INTERVAL = 1000;
    private static final int ID_SERVICE = 101;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private SettingsClient mSettingsClient;
    private LocationCallback mLocationCallback;
    private LocationSettingsRequest mLocationSettingsRequest;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth auth;
    private boolean activeUsersExist = false;
    private boolean updatingLocation = false;

    public LocationService (){}

    @Override
    public void onCreate() {
        initializeNotification();
       firebaseDatabase = FirebaseDatabase.getInstance();
       auth = FirebaseAuth.getInstance();
       initializeActiveUserListener();
       initializeLocationItems();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"OnStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void initializeLocationRequest() {
        //TODO setFastestInterval initialization is needed when app slow downs caused by FireBase sync
        if (mLocationRequest != null)
            return;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(REQUEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(FASTEST_REQUEST_INTERVAL);
    }
    private void initializeLocationCallback() {
        if(mLocationCallback != null)
            return;

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                LocationUpdateEvent event = new LocationUpdateEvent(locationResult.getLastLocation());
                Log.i(TAG, "Sending Location Info" + event.location.getLatitude() + " - " + event.location.getLongitude());
                EventBus.getDefault().post(event);
                sendLocationToDatabase(locationResult.getLastLocation());
            }
        };
    }
    private void initializeLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        mLocationSettingsRequest = builder.build();
        Task<LocationSettingsResponse> response = mSettingsClient.checkLocationSettings(mLocationSettingsRequest);
        response.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i(TAG, "Failed Resolution" + e.getMessage());
            }
        });
    }
    private void initializeLocationItems (){
        Log.i(TAG,"Initializing location items");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);
        initializeLocationRequest();
        initializeLocationCallback();
        initializeLocationSettingsRequest();
    }
    @SuppressLint("MissingPermission")
    private void startLocationUpdates () {
        //TODO onFailure should be implemented
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest).addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Log.i(TAG,"Location Requested");
                mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest,mLocationCallback,Looper.myLooper());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i(TAG,"Location didn't requested" + e.getMessage());
                ResolvableApiExceptionEvent event = new ResolvableApiExceptionEvent((ResolvableApiException) e);
                EventBus.getDefault().post(event);
                mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest,mLocationCallback,Looper.myLooper());
            }
        });
    }
    private void stopLocationUpdates(){
        Log.i(TAG,"Location Request Stopped");
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }
    private void logLocationSettingStates(LocationSettingsStates item){
        Log.d(TAG,"isBlePresent = "+item.isBlePresent());
        Log.d(TAG,"isBleUsable = "+item.isBleUsable());
        Log.d(TAG,"isGpsPresent = "+item.isGpsPresent());
        Log.d(TAG,"isGpsUsable = "+item.isGpsUsable());
        Log.d(TAG,"isLocationPresent = "+item.isLocationPresent());
        Log.d(TAG,"isLocationUsable = "+item.isLocationUsable());
        Log.d(TAG,"isNetworkLocationPresent = "+item.isNetworkLocationPresent());
        Log.d(TAG,"isNetworkLocationUsable = "+item.isNetworkLocationUsable());
    }

    private void initializeActiveUserListener () {
        firebaseDatabase.getReference().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild("ActiveUsers") && updatingLocation == false) {
                    startLocationUpdates();
                    updatingLocation = true;
                }
                else if(!dataSnapshot.hasChild("ActiveUsers") && updatingLocation == true) {
                    stopLocationUpdates();
                    updatingLocation = false;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void sendLocationToDatabase (Location location){
        String uId = auth.getUid();
        if(uId == null)
            return;
        FireLocationModel mdl = new FireLocationModel(location.getLatitude(),location.getLongitude());
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Locations/" + uId);
        ref.setValue(mdl);
    }

    private void initializeNotification(){
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        startForeground(ID_SERVICE, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        String channelId = "my_service_channelid";
        String channelName = "My Foreground Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }
}
