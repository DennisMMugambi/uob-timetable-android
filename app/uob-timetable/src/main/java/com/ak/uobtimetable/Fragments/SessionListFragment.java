package com.ak.uobtimetable.Fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import com.ak.uobtimetable.API.Models;
import com.ak.uobtimetable.ListAdapters.SessionListAdapter;
import com.ak.uobtimetable.MainActivity;
import com.ak.uobtimetable.R;
import com.ak.uobtimetable.Utilities.AndroidUtilities;
import com.ak.uobtimetable.Utilities.Logging.Logger;
import com.ak.uobtimetable.Utilities.SettingsManager;

/**
 * Fragment containing a list of sessions for a given day.
 */
public class SessionListFragment extends Fragment {

    private ListView lvSessions;
    private TextView tvListEmpty;

    public SessionListsFragment parentFragment;
    public List<Models.DisplaySession> todaysSessions;

    public SessionListFragment() {

        // Required empty public constructor
    }

    public static SessionListFragment newInstance(List<Models.DisplaySession> sessions,
                                                  SessionListsFragment parentFragment) {

        SessionListFragment fragment = new SessionListFragment();
        fragment.parentFragment = parentFragment;
        fragment.todaysSessions = sessions;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the preferences for this fragment
        View view =  inflater.inflate(R.layout.fragment_session_list, container, false);

        // Get UI references
        lvSessions = (ListView)view.findViewById(R.id.lvSessions);
        tvListEmpty = (TextView)view.findViewById(R.id.tvListEmpty);

        // Set empty view
        lvSessions.setEmptyView(tvListEmpty);

        // Populate session list
        SessionListAdapter sessionAdapter = new SessionListAdapter(getActivity(), todaysSessions, this);
        lvSessions.setAdapter(sessionAdapter);

        // Register session list click event
        lvSessions.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Get selected session
                Models.DisplaySession selectedSession = todaysSessions.get(position);

                // Build alert dialog. If session is invalid, add an extra button to open the
                // timetable website.
                final SettingsManager settings = SettingsManager.getInstance(getActivity());
                boolean sessionIsValid = selectedSession.isValid;

                String description = selectedSession.getDescription(settings.getLongRoomNames());
                if (sessionIsValid == false)
                    description += "\n" + getString(R.string.text_session_invalid);

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity())
                    .setTitle(selectedSession.getLongTitle())
                    .setMessage(description)
                    .setPositiveButton(R.string.dialog_dismiss, null);

                if (sessionIsValid == false && settings.hasTimetableUrl()){
                    dialogBuilder.setNeutralButton(R.string.dialog_open_timetable_site, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (AndroidUtilities.hasNetwork(getContext())) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(settings.getTimetableUrl()));
                                startActivity(browserIntent);
                            } else {
                                ((MainActivity)getActivity()).showNoInternetConnectionSnackbar();
                            }
                        }
                    });
                }

                // Show dialog
                dialogBuilder.create().show();
            }
        });

        // Register session list long click event
        lvSessions.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){

            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {

                // Message parent to toggle checkbox for all child fragments
                boolean editMode = parentFragment.toggleEditMode();

                Logger.getInstance().debug("SessionListFragment", "editMode: " + editMode);

                return true;
            }
        });

        return view;
    }

    public boolean getEditMode(){

        return parentFragment.getEditMode();
    }

    public void updateSession(Models.DisplaySession session){

        parentFragment.updateSession(session);
    }

    public void redrawSessionList(){

        if (lvSessions == null)
            return;

        SessionListAdapter adapter = (SessionListAdapter)lvSessions.getAdapter();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
    }

    @Override
    public void onDetach() {

        super.onDetach();
    }
}
