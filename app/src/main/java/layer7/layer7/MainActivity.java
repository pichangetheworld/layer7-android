package layer7.layer7;

import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.common.collect.Lists;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    // GoogleAPIClient to interface with the Location API
    private GoogleApiClient mGoogleApiClient;
    Location mLastLocation;

    private GoogleMap mMap;

    // ListView and Adapter for messages
    List<String> messageList = Lists.newArrayList();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the map
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Set up the GoogleAPIClient
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        // Set up the posts list
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                messageList);

        ListView listView = (ListView) findViewById(R.id.message_list);
        listView.setAdapter(adapter);

    }

    // Setup the GoogleAPIClient
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Callback triggers when the map is ready to use
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("Layer6debug", "Map finished loading.");
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);

        LatLng target = mMap.getCameraPosition().target;

        updateMessages(target);
    }

    private void updateMessages(LatLng target) {
        Log.d("Layer6debug", "Looking at " + target.latitude + "/" + target.longitude);
        ServerRestClient.get("listen/" + target.latitude + "/" + target.longitude + "/1", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                // If the response is JSONObject instead of expected JSONArray
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray responses) {
                // Pull out the first event on the public timeline
                for (int i = 0; i < responses.length(); i++) {
                    try {
                        JSONObject message = (JSONObject) responses.get(i);
                        messageList.add(message.getString("body"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                adapter.notifyDataSetChanged();

            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            double lat = mLastLocation.getLatitude(),
                    lng = mLastLocation.getLongitude();
            Log.d("Layer6debug", "Lat:" + lat + ", Long:" + lng);
            if (mMap != null) {
                // Move map to user's location
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 17.0f));
                updateMessages(mMap.getCameraPosition().target);
            }
        } else {
            Log.d("Layer6debug", "No last location found");
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("Layer6debug", "CONNECTION FAILED");
    }
}
