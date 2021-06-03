package com.example.wheresmybus;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    FusedLocationProviderClient mFusedLocationClient;
    Context context;
    //LatLng[] pino = null;
    MqttHelper mqtt;
    LatLng pino=null;

    private static final String[] areas= new String[]{"Aveiro","Coimbra"};
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button selecionar = findViewById(R.id.selecionar);
        ImageView autocarro=findViewById(R.id.autocarro_imagem);
        mqtt = new MqttHelper(getApplicationContext());


        Objects.requireNonNull(getSupportActionBar()).hide();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFrag != null;
        mapFrag.getMapAsync(this);

        selecionar.setOnClickListener(new View.OnClickListener() { // BUTAO PARA SELECIONAR O AUTOCARRO
            public void onClick(View v) {

                LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
                View promptView = layoutInflater.inflate(R.layout.selecionar_autocarro, null);
                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                alertDialogBuilder.setView(promptView);

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,
                        android.R.layout.simple_dropdown_item_1line, areas);
                AutoCompleteTextView textView = (AutoCompleteTextView)
                        promptView.findViewById(R.id.id_text_select);
                textView.setAdapter(adapter);
                alertDialogBuilder.setCancelable(true)
                        .setPositiveButton("OK", (dialog, id) -> {
                            String x=textView.getText().toString();
                            mqtt.publish("teste1",x,2);

                        });
                AlertDialog b = alertDialogBuilder.create();
                b.show();
                b.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.black));
            }
        });

        autocarro.setOnClickListener(new View.OnClickListener() { // BUTAO PARA FOCAR NO AUTOCARRO
            public void onClick(View v) {
                focar(pino,14);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(120000); // two minute interval
        mLocationRequest.setFastestInterval(120000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            //Location Permission already granted
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mGoogleMap.setMyLocationEnabled(true);
        } else {
            //Request Location Permission
            checkLocationPermission();
        }

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mqtt.subscribeToTopic("teste98",2);
                mqtt.mqttAndroidClient.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean b, String s) {
                        Log.w("connectado!!!!  ", s);
                    }
                    @Override
                    public void connectionLost(Throwable throwable) {
                    }
                    @Override
                    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                        String x=mqttMessage.toString();
                        String[] parts = x.split(",");
                        double part1 = Double.parseDouble(parts[0]); // 004
                        double part2 =Double.parseDouble(parts[1]);
                        pino= new LatLng (part1,part2);
                        googleMap.clear();
                        googleMap.addMarker(
                                new MarkerOptions()
                                        .draggable(false)
                                        .position(pino)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.autocarro)));

                    }
                    @Override
                    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                    }
                });
            }
        }, 2000);


    }

    private void focar(LatLng marker,int zoom){
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, zoom));
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }
                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                //move map camera
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
            }
        }
    };

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", (dialogInterface, i) -> {
                            //Prompt the user once explanation has been shown
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    MY_PERMISSIONS_REQUEST_LOCATION);
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay! Do the
                // location-related task you need to do.
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    mGoogleMap.setMyLocationEnabled(true);
                }
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

}