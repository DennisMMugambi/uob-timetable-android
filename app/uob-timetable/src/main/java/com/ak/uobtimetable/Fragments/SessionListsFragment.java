package com.ak.uobtimetable.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import com.ak.uobtimetable.API.Service;
import com.ak.uobtimetable.API.Models;
import com.ak.uobtimetable.CourseListActivity;
import com.ak.uobtimetable.Exceptions.HTTPException;
import com.ak.uobtimetable.MainActivity;
import com.ak.uobtimetable.R;
import com.ak.uobtimetable.Utilities.Logging.Logger;
import com.ak.uobtimetable.Notifications.SessionReminderNotifier;
import com.ak.uobtimetable.Utilities.SettingsManager;
import com.h6ah4i.android.tablayouthelper.TabLayoutHelper;

/**
 * Fragment containing a ViewPager with one SessionListFragment for each day.
 */
public class SessionListsFragment extends Fragment {

    private View view;
    private ProgressBar pbDownload;
    private ViewPager viewPager;

    private List<Models.DisplaySession> sessions;
    private Models.Course course;
    private List<SessionListFragment> sessionListFragments;
    private boolean editMode;
    private SettingsManager settings;
    private MainActivity activity;
    private Handler listsUpdateHandler;
    private AsyncTask sessionDownloadTask;

    public enum InitialLoadMode {
        loadSessionsWithSnackbar,
        loadSessionsWithoutSnackbar,
        updateSessions
    }

    public enum Args {
        load,
        save,
        initialIndex
    }

    public SessionListsFragment() {

        // Required empty public constructor
    }

    public static SessionListsFragment newInstance(InitialLoadMode loadMode, int tabIndex) {

        SessionListsFragment fragment = new SessionListsFragment();
        Bundle args = new Bundle();
        args.putString(Args.load.name(), loadMode.toString());
        args.putInt(Args.initialIndex.name(), tabIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Logger.getInstance().debug("SessionListsFragment", "onCreateView");

        // Inflate the preferences for this fragment
        view = inflater.inflate(R.layout.fragment_session_lists, container, false);

        viewPager = (ViewPager)view.findViewById(R.id.viewpager);

        pbDownload = (ProgressBar)view.findViewById(R.id.pbDownload);
        pbDownload.setVisibility(View.INVISIBLE);

        activity = (MainActivity)getActivity();

        settings = SettingsManager.getInstance(activity);
        course = settings.getCourse();
        sessions = settings.getSessions();

        sessionListFragments = new ArrayList<>();

        int initialIndex = getArguments().getInt(Args.initialIndex.name(), -1);
        InitialLoadMode mo = InitialLoadMode.valueOf(getArguments().getString(Args.load.name()));
        Logger.getInstance()
            .debug("SessionListsFragment", "loadArgument: " + mo.name())
            .debug("SessionListsFragment", "initialIndex: " + initialIndex);

        if (savedInstanceState != null && savedInstanceState.getBoolean(Args.save.name(), false)) {

        } else if (mo == InitialLoadMode.loadSessionsWithoutSnackbar && settings.hasSessions() && settings.hasCourse()){
            setSessions(course, sessions, initialIndex);
        }
        else if (mo == InitialLoadMode.loadSessionsWithSnackbar) {

            setSessions(course, sessions, initialIndex);
            activity.showSessionSnackbar(sessions, settings.getShowHiddenSessions(), true);
        }
        else {
            updateSessions(course);
        }

        return view;
    }

    /**
     * Downloads the sessions for the selected course and updates the session lists
     * @param course
     */
    public void updateSessions(Models.Course course){

        pbDownload.setVisibility(View.VISIBLE);
        sessionDownloadTask = new DownloadSessionsTask(this, course).execute();
    }

    /**
     * Re-initialises the session list views with session data stored in settings
     */
    public void refreshLists(){

        setSessions(settings.getCourse(), settings.getSessions(), getSelectedIndex());
    }

    /**
     * Update the UI for a given list of sessions
     * @param course
     * @param sessions
     * @param initialIndex
     */
    private void setSessions(Models.Course course, List<Models.DisplaySession> sessions,
                            int initialIndex){

        // Check if this fragment has been added before continuing
        // in order to prevent crash if the host activity is no longer
        // available (eg due to configuration change). Does prevent
        // the update of sessions but this is preferable to crashing.
        if (isAdded() == false) {
            Logger.getInstance().info("SesionListsFragment", "not isAdded in setSessions");
            return;
        }

        // Cache data
        this.sessions = sessions;
        this.course = course;

        // Show all sessions when hidden sessions visible or in edit mode
        boolean showingHiddenSessions = settings.getShowHiddenSessions() || editMode;

        // Init view pager
        ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager());

        // Add a fragment for each day
        sessionListFragments = new ArrayList<>();
        for (int day = 0; day <= 4; day++){

            // Get sessions for day
            List<Models.DisplaySession> thisDaysSessions = new ArrayList<>();
            for (Models.DisplaySession session : sessions){
                if (session.day == day && (session.visible || showingHiddenSessions))
                    thisDaysSessions.add(session);
            }

            // Create fragment
            SessionListFragment thisDayFragment = SessionListFragment.newInstance(thisDaysSessions, this);
            adapter.addFrag(thisDayFragment);
            sessionListFragments.add(thisDayFragment);

            Logger.getInstance().debug("SessionListsFragment", thisDaysSessions.size() + " sessions for day " + day);
        }

        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(5);

        TabLayout tabLayout = (TabLayout)view.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        // Initialise the helper which switches the tab mode between
        // scrollable and centre gravity
        TabLayoutHelper tabLayoutHelper = new TabLayoutHelper(tabLayout, viewPager);
        tabLayoutHelper.setAutoAdjustTabModeEnabled(true);

        // Switch to the current day or predefined index
        int index = initialIndex > -1 ? initialIndex : getCurrentTimetableDay();
        viewPager.setCurrentItem(index, true);

        Logger.getInstance().debug("SessionListsFragment", "Refreshed ViewPager");

        // Update the course text labels in navigation drawer
        activity.updateNavDrawerLabels(course, sessions);

        // Start the timer used to redraw the session lists every minute
        startRedrawUpdateTimer();

        // Show tutorial message boxes
        if (settings.getCompletedTutorial() == false){

            // First message
            AlertDialog d = new AlertDialog.Builder(activity)
                .setTitle("Just so you know...")
                .setMessage(getString(R.string.text_tutorial_disclaimer))
                .setPositiveButton(R.string.dialog_dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // Second message
                        AlertDialog d = new AlertDialog.Builder(activity)
                            .setTitle("One more thing...")
                            .setMessage(getString(R.string.text_tutorial_hide_sessions))
                            .setPositiveButton(R.string.dialog_dismiss, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    settings.setCompletedTutorial();
                                }
                            })
                            .create();
                        d.show();
                    }
                })
                .create();
            d.show();
        }
    }

    /**
     * Gets the initial day to select when displaying the timetable. This can either be the current
     * day the next day if all of todays sessions have finished, or monday if it's the weekend.
     * @return day - 0 is monday
     */
    private int getCurrentTimetableDay(){

        // Get today
        int currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2;

        // Return monday if weekend
        if (currentDayOfWeek < 0 || currentDayOfWeek > 4)
            return 0;

        // If there are sessions today and all elapsed, go to tomorrow
        boolean showingHiddenSessions = settings.getShowHiddenSessions();
        boolean sessionsToday = false;
        boolean sessionsTodayAllElapsed = true;
        for (Models.DisplaySession session : sessions) {
            // Filter today and visible
            if (session.day == currentDayOfWeek && (session.visible || showingHiddenSessions)) {
                sessionsToday = true;
                if (session.getState() != Models.TimeState.Elapsed)
                    sessionsTodayAllElapsed = false;
            }
        }
        if (sessionsToday && sessionsTodayAllElapsed){
            // Increment day of week, wrap around from friday to monday
            int selectedDay = currentDayOfWeek + 1;
            if (selectedDay > 4)
                selectedDay = 0;
            return selectedDay;
        }

        // Return current day
        return currentDayOfWeek;
    }

    /**
     * Gets the selected index (day) of the ViewPager holding the SessionListFragments
     * @return
     */
    public int getSelectedIndex(){

        if (viewPager == null)
            return -1;

        return viewPager.getCurrentItem();
    }

    public boolean toggleEditMode(){

        return setEditMode(!editMode);
    }

    public boolean getEditMode(){

        return editMode;
    }

    private boolean setEditMode(boolean editMode){

        // Toggle internal state
        this.editMode = editMode;

        Logger.getInstance().debug("SessionListsFragment", "Set edit mode: " + editMode);

        // If not editing, update our alarms, as we may have sessions where isVisible as changed
        // which changes whether or not we need to show a notification for that session
        if (editMode == false && settings.getNotificationSessionRemindersEnabled()) {
            SessionReminderNotifier notifier = new SessionReminderNotifier(getContext());
            notifier.setAlarms(settings.getSessions(), settings.getNotificationSessionRemindersMinutes());
        }

        // Log number of hidden sessions
        int hiddenSessions = 0;
        for (Models.DisplaySession s : sessions) {
            if (s.visible == false)
                hiddenSessions++;
        }
        Logger.getInstance().debug("SessionListsFragment", "Hidden session count: " + hiddenSessions);

        // Update sub-fragments
        refreshLists();

        return editMode;
    }

    /**
     * Updates the state of the given session in the saved session list.
     * @param session
     */
    public void updateSession(Models.DisplaySession session){

        // Find this session in the saved session list and update it with the new state
        int hiddenSessions = 0;
        int updatedSessions = 0;
        for (Models.DisplaySession s : sessions){
            if (s.equals(session)) {
                s.update(session);
                updatedSessions++;
                Logger.getInstance().debug("SessionListsFragment", "Updated session visible:" + s.visible);
            }
            if (s.visible == false)
                hiddenSessions++;
        }

        if (updatedSessions == 0)
            Logger.getInstance().error("SessionListsFragment", "Unable to update unticked session!");

        Logger.getInstance()
            .debug("sessionListsFragment", "Updated sessions: " + updatedSessions)
            .debug("SessionListsFragment", "Sessions hidden: " + hiddenSessions);

        settings.setSessions(sessions, false);
    }

    /**
     * Start the timer used to trigger a redraw of the session list, in case a session's timestate
     * has changed.
     */
    public void startRedrawUpdateTimer(){

        if (listsUpdateHandler != null)
            return;

        // Set a timer for re-rendering the session list every minute, in case a session timestate
        // has changed, as the text colour will change. We want to fire every minute, on the minute,
        // so first work out how many seconds until the next minute.
        int currentSeconds = Calendar.getInstance().get(Calendar.SECOND);
        int fireInSeconds = 60 - currentSeconds + 5;

        listsUpdateHandler = new Handler();
        listsUpdateHandler.postDelayed(new Runnable() {

            @Override
            public void run() {

                Logger.getInstance().debug("SessionListsFragment", "Fired refresh session list handler");

                // Update all 5 lists
                for (SessionListFragment sessionListFragment : sessionListFragments)
                    sessionListFragment.redrawSessionList();

                // Fire again in one minute
                listsUpdateHandler.postDelayed(this, 60 * 1000);

            }
        }, fireInSeconds * 1000 );
    }

    /**
     * Stop the timer used to trigger a redraw of the session list, in case a session's timestate
     * has changed.
     */
    public void stopRedrawUpdateTimer(){

        if (listsUpdateHandler == null)
            return;

        listsUpdateHandler.removeCallbacksAndMessages(null);
        listsUpdateHandler = null;
    }

    /**
     * Return whether the lists have been populated with sessions yet.
     */
    public boolean hasLoaded(){

        return sessionListFragments != null && sessionListFragments.size() > 0;
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
    }

    @Override
    public void onDetach() {

        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState){

        Logger.getInstance().debug("SessionListsFragment", "Saving state");
        outState.putBoolean(Args.save.name(), true);

        super.onSaveInstanceState(outState);
    }

    class ViewPagerAdapter extends FragmentStatePagerAdapter {

        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final String[] days = new String[] { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday" };

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return days.length;
        }

        public void addFrag(Fragment fragment) {
            mFragmentList.add(fragment);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return days[position].toUpperCase();
        }
    }

    private class DownloadSessionsTask extends AsyncTask<Void, Integer, Models.SessionResponse> {

        private SessionListsFragment fragment;
        private Models.Course course;
        private Exception fetchException;

        DownloadSessionsTask(SessionListsFragment fragment, Models.Course course){

            this.fragment = fragment;
            this.course = course;
        }

        protected Models.SessionResponse doInBackground(Void... params) {

            // Make API call
            Models.SessionResponse response = null;
            try {
                Service service = new Service(getContext());
                response = service.getSessions(course.sessionUrl);
            } catch (Exception e) {
                fetchException = e;
            } finally {
                // Log Exception or response error here
                if (fetchException != null || response.error){

                    // Get previous exception or make from response
                    Exception cause = fetchException;
                    if (cause == null)
                        cause = new Exception(response.errorStr);

                    Exception e = new Exception("Failed to download sessions.", cause);

                    HashMap<String, String> metadata = new HashMap<>();

                    // Add HTTP exception data
                    if (cause instanceof HTTPException)
                        metadata.putAll(((HTTPException) cause).toMap());

                    // Add response error
                    if (response != null) {
                        metadata.put("error", response.errorStr);
                        metadata.put("error_id", response.errorId);
                    }

                    // Add session info
                    metadata.put("session_url", course.sessionUrl);
                    metadata.put("course_id", course.id);
                    metadata.put("course_name", course.name);
                    metadata.put("course_department_id", course.department.id);
                    metadata.put("course_department_name", course.department.name);
                    metadata.put("course_level", course.level);
                    metadata.put("user_last_updated", settings.getSessionsUpdatedTimeAgo());

                    Logger.getInstance().error("Session download", e, metadata);
                }
            }

            return response;
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(Models.SessionResponse response) {

            // Hide progress bar
            fragment.pbDownload.setVisibility(View.INVISIBLE);

            // Check for cancelled before applying any changes
            if (isCancelled()) {
                Logger.getInstance().info("DownloadSessionsTask", "cancelled in onPostExecute ");
                return;
            }

            // Error handling
            if (fetchException != null || response.error){

                // Check whether we should load cached sessions
                boolean useCachedSessions = fragment.hasLoaded() == false && settings.hasSessions();

                // If we have previously saved sessions, we can show them
                DialogInterface.OnClickListener clickListener = null;

                if (useCachedSessions) {
                    clickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            List<Models.DisplaySession> sessions = settings.getSessions();

                            // Set sessions in child fragments
                            fragment.setSessions(course, sessions, -1);

                            // Show snackbar
                            fragment.activity.showSessionSnackbar(sessions, settings.getShowHiddenSessions(), true);
                        }
                    };
                }

                // Error message string
                String errorMsg = "";

                if (fetchException != null)
                    errorMsg = fetchException.getMessage();
                else
                    errorMsg = getString(R.string.warning_server_error);

                if (useCachedSessions)
                    errorMsg += "\n\n" + getString(R.string.text_loading_cached_sessions);

                // Build alert dialog. Show either a generic error dialog, or a custom
                // dialog for invalid course errors.

                // Check for invalid course
                boolean isCourseInvalidError =
                    response != null &&
                    response.error &&
                    Service.ERROR_COURSE_INVALID.equals(response.errorId);

                if (!isCourseInvalidError){
                    AlertDialog d = new AlertDialog.Builder(fragment.getActivity())
                        .setPositiveButton(android.R.string.ok, clickListener)
                        .setTitle(R.string.warning_session_download_error)
                        .setMessage(errorMsg)
                        .create();
                    d.show();
                } else {
                    AlertDialog d = new AlertDialog.Builder(fragment.getActivity())
                        .setPositiveButton(R.string.dialog_change_course, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(fragment.getActivity(), CourseListActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.dialog_close, clickListener)
                        .setTitle(R.string.warning_course_invalid)
                        .setMessage(R.string.text_course_invalid)
                        .create();
                    d.show();
                }

                return;
            }

            SettingsManager settings = SettingsManager.getInstance(getContext());

            // Restore the hidden value of the sessions from the previous saved list
            List<Models.DisplaySession> sessions = response.sessions;
            if (settings.hasSessions()) {
                Service service = new Service(getContext());
                sessions = service.syncSessionLists(sessions, settings.getSessions());
            }

            // Save to settings
            settings.setSessions(sessions, true);
            settings.setSessionsCourseName(response.courseName);
            settings.setSessionsDateRange(response.dateRange);
            settings.setTimetableUrl(response.timetableUrl);

            // Update notification alarms
            if (settings.getNotificationSessionRemindersEnabled()){
                SessionReminderNotifier notifier = new SessionReminderNotifier(getContext());
                notifier.setAlarms(sessions, settings.getNotificationSessionRemindersMinutes());
            }

            // Set sessions in child fragments
            fragment.setSessions(course, sessions, -1);

            // Show snackbar
            fragment.activity.showSessionSnackbar(sessions, settings.getShowHiddenSessions(), true);
        }
    }
}
