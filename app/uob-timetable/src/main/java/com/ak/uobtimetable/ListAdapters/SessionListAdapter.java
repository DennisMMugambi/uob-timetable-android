package com.ak.uobtimetable.ListAdapters;

import android.app.Activity;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.ak.uobtimetable.Fragments.SessionListFragment;
import com.ak.uobtimetable.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ak.uobtimetable.API.Models;

public class SessionListAdapter extends BaseAdapter {

    private List<Models.DisplaySession> sessions;
    private LayoutInflater liInflater;
    private SessionListFragment fragment;

    public SessionListAdapter(Activity activity, List<Models.DisplaySession> sessions,
                              SessionListFragment fragment){

        if (sessions == null)
            sessions = new ArrayList<>();
        this.sessions = sessions;

        this.fragment = fragment;

        this.liInflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return sessions.size();
    }

    @Override
    public Object getItem(int position) {
        return sessions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null)
            convertView = liInflater.inflate(R.layout.row_session_list, null);

        final Models.DisplaySession session = (Models.DisplaySession)getItem(position);

        TextView tvTime = (TextView)convertView.findViewById(R.id.tvTime);
        TextView tvTitle = (TextView)convertView.findViewById(R.id.tvTitle);
        TextView tvSubtitle = (TextView)convertView.findViewById(R.id.tvSubtitle);
        final CheckBox cbShown = (CheckBox)convertView.findViewById(R.id.cbShown);

        // Set checkbox state
        cbShown.setOnCheckedChangeListener(null);
        cbShown.setChecked(session.visible);
        cbShown.setVisibility(View.GONE);
        if (fragment.getEditMode() == true)
            cbShown.setVisibility(View.VISIBLE);

        // Set text
        tvTime.setText(session.start);
        tvTitle.setText(session.getTitle());
        tvSubtitle.setText(session.getSubtitle());

        // Set text colour
        Context context = fragment.getContext();
        HashMap<Models.TimeState, Integer> colourMap = new HashMap<>();
        colourMap.put(Models.TimeState.Elapsed, ContextCompat.getColor(context, R.color.colorTextDisabled));
        colourMap.put(Models.TimeState.Ongoing, ContextCompat.getColor(context, R.color.colorPrimary));
        colourMap.put(Models.TimeState.Future, ContextCompat.getColor(context, R.color.colorTextBlack));

        for (TextView tv : new TextView[] { tvTime, tvTitle, tvSubtitle })
            tv.setTextColor(colourMap.get(session.getState()));

        // Add event for checkbox clicked
        cbShown.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                // Update this session
                session.visible = cbShown.isChecked();

                // Notify fragment that this session has been updated
                fragment.updateSession(session);
            }
        });

        return convertView;
    }
}
