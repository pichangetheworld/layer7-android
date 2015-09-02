package layer7.layer7;

import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.common.collect.Lists;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

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

    // EditText for posting new messages
    EditText newMessageInput;

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

        newMessageInput = (EditText) findViewById(R.id.new_message_input);
        Button newMessageSend = (Button) findViewById(R.id.new_message_send);
        newMessageSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = newMessageInput.getText().toString();
                if (!message.isEmpty()) {
                    // TODO post to the /post endpoint
                    Log.d("PostMessage", "Posting message " + message);

                    double lat = mLastLocation.getLatitude(),
                            lng = mLastLocation.getLongitude();

                    long timestamp = System.currentTimeMillis();
                    RequestParams params = new RequestParams();
                    // TODO put the user_id
                    params.put("message", message);
                    params.put("timestamp", timestamp);

                    ServerRestClient.post("shout/" + lat + "/" + lng, params, new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            // response was successful!!
                            Log.d("PostMessage", "Response was successful! " + response.toString());
                            updateMessages(mMap.getCameraPosition());
                        }
                    });

                    newMessageInput.setText("");
                }
            }
        });
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
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                updateMessages(cameraPosition);
            }
        });
    }

    private void updateMessages(CameraPosition cameraPosition) {
        LatLng target = cameraPosition.target;

        // TODO: set correct circle radius to meters
        Log.d("Layer6debug", "Looking at " + target.latitude + "/" + target.longitude);
        ServerRestClient.get("listen/" + target.latitude + "/" + target.longitude + "/100", null, new JsonHttpResponseHandler() {
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

                        // TODO Only add this message if it hasn't already been added
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
