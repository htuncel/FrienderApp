package com.example.frienderapp;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.LocationResult;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.content.Context.NOTIFICATION_SERVICE;

public class Utils {

    public static final long UPDATE_INTERVAL = 60 * 1000;
    public static final float SMALLEST_DISPLACEMENT = 1.0F;
    public static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;
    public static final long MAX_WAIT_TIME = UPDATE_INTERVAL * 2;
    final static String KEY_LOCATION_UPDATES_RESULT = "location-update-result";
    private static final String USERUID = "USERUID";
    private static final String DATE = "DATE";
    private static final String LOCATION = "LOCATION";
    private static final String TAG = "UtilsClass";
    public static float accuracy;
    static String addressFragments = "";
    static List<Address> addresses = null;

    static void setLocationUpdatesResult(Context context, String value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_LOCATION_UPDATES_RESULT, value)
                .apply();
    }

    @SuppressLint("MissingPermission")
    public static void getLocationUpdates(final Context context, final Intent intent, String broadcastevent) {

        LocationResult result = LocationResult.extractResult(intent);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String uid = currentUser.getUid();

        if (result != null) {

            Map<String, Object> newLocation = new HashMap<>();
            newLocation.put(USERUID, uid);
            newLocation.put(DATE, Timestamp.now());
            newLocation.put(LOCATION, new GeoPoint(result.getLastLocation().getLatitude(), result.getLastLocation().getLongitude()));

            CollectionReference locationsRef = db.collection("Locations");
            locationsRef.document().set(newLocation);
            Log.i("update", "new location added for user : " + uid + " time : " + Timestamp.now().toDate().toString()
                    + "\n at location lat,lon format :" + result.getLastLocation().getLatitude() + "," + result.getLastLocation().getLongitude());

            Date today = Calendar.getInstance().getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
            String nowDate = formatter.format(today);


            List<Location> locations = result.getLocations();
            Location firstLocation = locations.get(0);

            getAddress(firstLocation, context);
            //firstLocation.getAccuracy();
            //firstLocation.getLatitude();
            //firstLocation.getLongitude();
            //firstLocation.getAccuracy();
            //firstLocation.getSpeed();
            //firstLocation.getBearing();
            LocationRequestHelper.getInstance(context).setValue("locationTextInApp", "You are at " + getAddress(firstLocation, context) + "(" + nowDate + ") with accuracy " + firstLocation.getAccuracy() + " Latitude:" + firstLocation.getLatitude() + " Longitude:" + firstLocation.getLongitude() + " Speed:" + firstLocation.getSpeed() + " Bearing:" + firstLocation.getBearing());
            showNotificationOngoing(context, broadcastevent, "");
        }
    }

    public static String getAddress(Location location, Context context) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        // Address found using the Geocoder.
        addresses = null;
        Address address = null;
        addressFragments = "";
        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);
            address = addresses.get(0);
        } catch (IOException ioException) {
            Log.e(TAG, "error", ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            Log.e(TAG, "Latitude = " + location.getLatitude() +
                    ", Longitude = " + location.getLongitude(), illegalArgumentException);
        }

        if (addresses == null || addresses.size() == 0) {
            Log.i(TAG, "ERORR");
            addressFragments = "NO ADDRESS FOUND";
        } else {
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressFragments = addressFragments + String.valueOf(address.getAddressLine(i));
            }
        }
        LocationRequestHelper.getInstance(context).setValue("addressFragments", addressFragments);
        return addressFragments;
    }

    public static void showNotificationOngoing(Context context, String broadcastevent, String title) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, LocationServiceActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder notificationBuilder = new Notification.Builder(context)
                .setContentTitle(title + DateFormat.getDateTimeInstance().format(new Date()) + ":" + accuracy)
                .setContentText(addressFragments.toString())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setStyle(new Notification.BigTextStyle().bigText(addressFragments.toString()))
                .setAutoCancel(true);
        notificationManager.notify(3, notificationBuilder.build());

    }

    public static void removeNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }
}
