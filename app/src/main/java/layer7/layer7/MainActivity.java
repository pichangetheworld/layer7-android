package layer7.layer7;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.common.collect.Lists;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.joda.time.DateTime;
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
    List<Layer7Message> messageList = Lists.newArrayList();
    MessageListAdapter mAdapter;


    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ListView mListView;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
        }
        // Set up the map
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mapFragment.getMap().setPadding(0, getToolbarAndStatusBarPxHeight(), 0, 0);


        // Set up the GoogleAPIClient
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        // Set up the posts list
        mAdapter = new MessageListAdapter(this, R.layout.list_message, messageList);

        mListView = (ListView) findViewById(R.id.message_list);
        mListView.setAdapter(mAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.message_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateMessages(mMap.getCameraPosition());
            }
        });

        final FloatingActionButton myFab = (FloatingActionButton)  findViewById(R.id.fab_button);
        myFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent mSendMsgIntent = new Intent(MainActivity.this, SendLocationActivity.class);
                mSendMsgIntent.putExtra(getString(R.string.intent_parceble_camera_position), mMap.getCameraPosition());
                // TODO(mchinavan) make this work
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        MainActivity.this, findViewById(R.id.map_wrapper), getString(R.string.transition_map));
                ActivityCompat.startActivity(MainActivity.this, mSendMsgIntent, options.toBundle());
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
        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setTiltGesturesEnabled(false);
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                mSwipeRefreshLayout.setRefreshing(true);
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
                messageList.clear();

                // Pull out the first event on the public timeline
                for (int i = 0; i < responses.length(); i++) {
                    try {
                        JSONObject message = (JSONObject) responses.get(i);

                        Layer7Message m = new Layer7Message();
                        m.id = message.getInt("id");

                        JSONObject userObj = message.getJSONObject("user");
                        m.author = userObj.getString("first_name") + " " + userObj.getString("last_name");

                        m.message = message.getString("body");
                        String created_at = message.getString("created_at");
                        DateTime dateTime = new DateTime(created_at);
                        m.timePosted = dateTime.getMillis();

                        messageList.add(m);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                mAdapter.notifyDataSetChanged();
                mSwipeRefreshLayout.setRefreshing(false);

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
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            updateMessages(mMap.getCameraPosition());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("Layer6debug", "CONNECTION FAILED");
    }


    /* ================================== *
     * Helpers                            *
     * ================================== */


    private int getToolbarAndStatusBarPxHeight() {
        return (int) ((mToolbar.getHeight() + getStatusBarHeight()) * getResources().getDisplayMetrics().density);
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

}
