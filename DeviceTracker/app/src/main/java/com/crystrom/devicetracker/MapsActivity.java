package com.crystrom.devicetracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.HashMap;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private GoogleMap mMap;
    private boolean mapReady,datasetInitialized = false;
    DatabaseReference databaseReference;
    FirebaseDatabase firebaseDatabase;
    boolean activityStart = true;
    TextView deviceNameTxt, deviceId, battery,lastFix;
    ImageView geoFence,left,right,deviceInfo, batteryStatus, fixZoom;
    ArrayList<String> deviceIds = new ArrayList<String>();

    //Holds the latest version in the database. Assign it in your OnDataChange and work with this data.
    UserNode currentUser = null;
    float zoomLevel;

    //intializing currentDevice as 0, add and substract when the user presses the navigation buttons.
    int currentDevice,previousDevice = 0;

    //Collection of Markers
    private HashMap<String,Marker> markers = new HashMap<String,Marker>();


    /**
     * Contains common operation to be performed on maps, but the data depends on asynchronous access.
     */
    public interface MapOperations{
        void onLocationChangeUpdater(Object dataset);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map1);
        mapFragment.getMapAsync(this);

        //device name textview
        deviceNameTxt = (TextView)findViewById(R.id.textView);
        deviceNameTxt.setVisibility(View.INVISIBLE);

        //device id
        deviceId = (TextView) findViewById(R.id.device_name);

        //devicelastfix
        lastFix = (TextView)findViewById(R.id.last_fix);

        //battery status
        battery = (TextView)findViewById(R.id.battery);

        firebaseDatabase = FirebaseDatabase.getInstance();
        //Reference string will be the root node and the username. Under this
        //all the devices and password and other information will be stored.
        databaseReference = firebaseDatabase.getReference("Accounts/vdl-235353");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot != null) {

                    onLocationChangeUpdater(dataSnapshot.getValue(UserNode.class));
                }else{
                    Toast.makeText(getApplicationContext(),"Username/Password Invalid",Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getApplicationContext(),"Something fishy happened! Sorry for the inconvenience!",Toast.LENGTH_SHORT).show();
            }
        });


        //geo fence button
        geoFence = (ImageView) findViewById(R.id.geo_fence);
        geoFence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



            }
        });

        fixZoom = (ImageView)findViewById(R.id.freeze_zoom_level);
        fixZoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mapReady == true){
                    // toggle the color of the icon.
                    zoomLevel = mMap.getCameraPosition().zoom;
                    SharedPreferences shared = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor  edit = shared.edit();
                    edit.putFloat("ZOOM_LEVEL", zoomLevel);
                    edit.commit();


                }
            }
        });

        left = (ImageView)findViewById(R.id.navigate_left);
        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //navigate to left marker
                //the currentDevice variable is used to iterate thru the devices stored in the deviceIDs. use the
                //deviceIds to get the marker and set the cameraposition to it. and also get the details populated
                //in the bottom sliding drawer.
                int size = deviceIds.size();
                if(size > 0 ) {
                    previousDevice = currentDevice;
                    if (currentDevice == 0) {
                        currentDevice = size - 1;
                    } else {
                        currentDevice--;
                    }
                    Log.d("MapsActivity", currentDevice + "");
                    updateCameraUI(currentDevice,previousDevice);
                }
                else{
                    Toast.makeText(getApplicationContext(),"No device to track!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        right = (ImageView)findViewById(R.id.navigate_Right);
        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //navigate to right marker
                //the currentDevice variable is used to iterate thru the devices stored in the deviceIDs. use the
                //deviceIds to get the marker and set the cameraposition to it. and also get the details populated
                //in the bottom sliding drawer.
                int size = deviceIds.size();
                if(size > 0) {
                    previousDevice= currentDevice;
                if((currentDevice == deviceIds.size()-1)){
                    currentDevice = 0;
                }else{
                    currentDevice++;
                }
                Log.d("MapsActivity",currentDevice +"");


                    //update the camera to center at the current marker
                    updateCameraUI(currentDevice,previousDevice);
                }else{
                    Toast.makeText(getApplicationContext(),"No device to track!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        deviceInfo = (ImageView) findViewById(R.id.details);
        deviceInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //display the bottom drawer and contents of the marker.
            }
        });

        //This is not interactable. but shows battery as color red/ green as status
        batteryStatus = (ImageView) findViewById(R.id.battery_status);

    }


    public void onLocationChangeUpdater(Object dataset){
        //Only manipulate maps when it is ready else will throw exception
        if(mapReady == true){

            //Check if current user is null, if it is null then the user might exist in the server.
            if(dataset != null){
                //Cast the dataset to Device node and get the details
                currentUser = (UserNode) dataset;

                //Check if any new device has been added, if it has been added, then do a fresh intialization.
                if(currentUser.getDevices().size() != markers.size()){
                    activityStart = true;
                }

                        if (activityStart == true) {
                            //For the first time the activity started for the session, add the markers, then set activityStart to false
                            //remove all the old markers then add the new dataset
                            markers = new HashMap<String,Marker>();

                            //remove all device ids before adding
                            deviceIds = new ArrayList<>();

                            for (Device device : currentUser.getDevices()) {
                                LatLng latLng = new LatLng(device.getLatitude(), device.getLongitude());
                                Marker mark = mMap.addMarker(new MarkerOptions().position(latLng).title(device.getDeviceName()));
                                mark.setTag(device);

                                //add the devices in to the markers hashmap.
                                markers.put(device.getDeviceId(), mark);

                                //Add the device ID so we can navigate, show data about different devices later.
                                deviceIds.add(device.getDeviceId());

                                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                                    @Override
                                    public boolean onMarkerClick(Marker marker) {
                                        //sliding bottom drawer should come up and display the marker contents
                                        deviceNameTxt.setText(marker.getTitle());

                                        return true;
                                    }
                                });
                            }


                            deviceNameTxt.setVisibility(View.VISIBLE);
                            datasetInitialized = true;
                            activityStart = false;
                        }else{
                            //Since markers has been already added, we just update their location and info
                            for (Device device : currentUser.getDevices()) {

                                LatLng latLng = new LatLng((double)device.getLatitude(), (double)device.getLongitude());
                                Marker mark = markers.get(device.getDeviceId());
                                //before setting to new position, do a runnable to smoothly move the marker from previous
                                //point to current point using linear interpolation
                                //refer this stack overflow question for more information
                                //http://stackoverflow.com/questions/15905359/how-to-change-the-position-of-a-marker-on-a-android-map-v2
                                mark.setPosition(latLng);
                                mark.setTag(device);
                                if (mark.getTitle().equals(deviceIds.get(currentDevice))){
                                    //if this is current. we need to animate this while transistion from former location to current location


                                }
                            }
                        }


            }else{

                Toast.makeText(getApplicationContext(), "User does not exists, or  no data found on server", Toast.LENGTH_SHORT).show();

            }
        }else{
            Toast.makeText(getApplicationContext(),"Map is not ready yet. Please wait for sometime and try again!",Toast.LENGTH_LONG).show();
        }

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mapReady = true;

 /*       // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }


    /**
     * Initializes the node with our dataset to give consistent approach.
     * remove during production
     */

    private void initialize(){

        ArrayList<Device> deviceList = new ArrayList<Device>();
       /* deviceList.add(new Device("x1,y1,x2,y2,x3,y3,x4,y4","April 23,2017","26.2324234234","26.23423424","Alpha1","Abcd1234","90","300"));
        deviceList.add(new Device("x1,y1,x2,y2,x3,y3,x4,y4","April 23,2017","26.2324234234","26.23423424","Alpha2","Abcd1444","90","300"));
        deviceList.add(new Device("x1,y1,x2,y2,x3,y3,x4,y4","April 23,2017","26.2324234234","26.23423424","Beta1","Abcd1233","90","300"));
        deviceList.add(new Device("x1,y1,x2,y2,x3,y3,x4,y4","April 23,2017","26.2324234234","26.23423424","Beta2","Abcd1224","90","300"));
        deviceList.add(new Device("x1,y1,x2,y2,x3,y3,x4,y4","April 23,2017","26.2324234234","26.23423424","Charlie1","Abcd1134","90","300"));
        deviceList.add(new Device("x1,y1,x2,y2,x3,y3,x4,y4","April 23,2017","26.2324234234","26.23423424","Charlie2","Abcd2224","90","300"));
*/
        UserNode user1 = new UserNode("vinayaka","vdl1244",deviceList);

        databaseReference.setValue(user1);

    }


    /**
     * This method handles the Updation of the UI based on the currentDevice
     * use the current Device IDs to fetch the marker and get their latlng
     * update the camera position to the latlng obtained from the current marker
     * @param currentDeviceIds
     */

    private void updateCameraUI(int currentDeviceIds, int previousDeviceIds){
        if(mapReady == false || datasetInitialized == false){
            Toast.makeText(this, "Map not ready yet. Please try again!", Toast.LENGTH_SHORT).show();
        }else{

            String lDeviceId = deviceIds.get(currentDeviceIds);
            Log.d("updateCameraUI", lDeviceId);
            //get the current Marker
            Marker m = markers.get(lDeviceId);

            Log.d("updateCameraUI",m.getTitle());
            //assign the current position
            LatLng latLng = m.getPosition();

            //update the textview with device name
            deviceNameTxt.setText(m.getTitle());

            deviceId.setText("Device Id: " +((Device)m.getTag()).getDeviceId());

            battery.setText("Battery: " + ((Device)m.getTag()).getBatteryStatus());

            lastFix.setText("Last fix: " + ((Device)m.getTag()).getLastUpdated());

            //update the camera positions to the marker selected.
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

            //change the marker icon to show it is selected
            m.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_my_location_black_24dp));
            markers.get(deviceIds.get(previousDeviceIds)).setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_location_searching_black_24dp));
        }
    }

}
