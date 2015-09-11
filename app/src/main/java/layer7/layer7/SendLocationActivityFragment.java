package layer7.layer7;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONObject;

/**
 * A placeholder fragment containing a simple view.
 */
public class SendLocationActivityFragment extends Fragment {

    // EditText for posting new messages
    EditText newMessageInput;
    SendLocationActivity sendLocationActivity;
    private CameraPosition cameraPosition;

    public SendLocationActivityFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View myViewFrag = inflater.inflate(R.layout.fragment_send_location, container, false);
        newMessageInput = (EditText) myViewFrag.findViewById(R.id.new_message_input);
        newMessageInput.requestFocus();
        Button newMessageSend = (Button) myViewFrag.findViewById(R.id.new_message_send);

        newMessageSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = newMessageInput.getText().toString();
                if (!message.isEmpty()) {
                    // TODO post to the /post endpoint
                    Log.d("PostMessage", "Posting message " + message);

                    LatLng loc = cameraPosition.target;
                    double lat = loc.latitude,
                            lng = loc.longitude;

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
                            sendLocationActivity.supportFinishAfterTransition();
                        }
                    });

                    newMessageInput.setText("");
                }
            }
        });
        return myViewFrag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            sendLocationActivity = (SendLocationActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString());
        }
        cameraPosition = sendLocationActivity.getIntent().getParcelableExtra(getString(R.string.intent_parceble_camera_position));
    }
}
