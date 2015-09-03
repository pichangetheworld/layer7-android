package layer7.layer7;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Eminent Domain AS
 * Author: pchan
 * Date: 03/09/15
 */
public class MessageListAdapter extends ArrayAdapter<Layer7Message> {

    public MessageListAdapter(Context context, int resource, List<Layer7Message> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.list_message, null);
        }

        Layer7Message p = getItem(position);

        if (p != null) {
            TextView authorView = (TextView) v.findViewById(R.id.author);
            TextView messageView = (TextView) v.findViewById(R.id.message);
            TextView timePostedView = (TextView) v.findViewById(R.id.time_posted);

//            authorView.setText(p.author); // TODO
            messageView.setText(p.message);
            timePostedView.setText(DateUtils.getRelativeTimeSpanString(p.timePosted));
        }

        return v;
    }
}
