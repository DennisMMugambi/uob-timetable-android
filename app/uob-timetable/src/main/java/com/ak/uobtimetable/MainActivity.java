package com.ak.uobtimetable;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.ak.uobtimetable.API.Models;
import com.ak.uobtimetable.Fragments.SessionListsFragment;
import com.ak.uobtimetable.Fragments.TermDatesFragment;
import com.ak.uobtimetable.Notifications.SessionReminderNotifier;
import com.ak.uobtimetable.Utilities.AndroidUtilities;
import com.ak.uobtimetable.Utilities.Logging.Logger;
import com.ak.uobtimetable.Utilities.SettingsManager;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout dlDrawer;
    private NavigationView navigationView;
    private AppBarLayout abAppBar;
    private CoordinatorLayout clCoordinatorLayout;
    private Menu meMain;

    private FragmentManager fragmentManager;
    private SessionListsFragment frSessions;
    private TermDatesFragment frTermDates;
    private Fragment currentFragment;

    private SettingsManager settings;

    private int devButtonClicks = 0;
    private boolean showMenu = true;

    private final String TITLE_DEFAULT = "UoB timetable";
    private final String TITLE_TERM_DATES = "Term dates";

    public enum Args {
        shouldRestore,
        fragment,
        currentIndex,
        forceRefreshSessions,
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Logger.getInstance().debug("MainActivity", "onCreate");

        if (getIntent() != null){
            Intent intent = getIntent();
            String session_hash_key = "notification_session_hash";
            if (intent.hasExtra(session_hash_key)) {
                String session_hash = intent.getStringExtra(session_hash_key);
                Logger.getInstance().debug("MainActivity", "Started from session reminder notification: " + session_hash);
            }
        }

        settings = SettingsManager.getInstance(this);

        // Log saved details if we have any. This allows us to view some data which is useful for
        // debugging but not displayed in the UI.
        StringBuilder logSessionsInfo = new StringBuilder();
        if (settings.hasSessions()){
            logSessionsInfo.append(settings.getSessions().size() + " saved sessions");
            if (settings.hasSessionsCourseName())
                logSessionsInfo.append(" - " + settings.getSessionsCourseName());
            if (settings.hasSessionsDateRange())
                logSessionsInfo.append(" - " + settings.getSessionsDateRange());
        } else {
            logSessionsInfo.append("No saved sessions");
        }
        Logger.getInstance().info("Settings", logSessionsInfo.toString());

        Toolbar toolbar = (Toolbar)findViewById(R.id.tbToolbar);
        setSupportActionBar(toolbar);

        // Get UI references
        dlDrawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        abAppBar = (AppBarLayout)findViewById(R.id.abAppBar);
        clCoordinatorLayout = (CoordinatorLayout)abAppBar.getParent();
        meMain = toolbar.getMenu();

        // Init drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, dlDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        dlDrawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Change the icon shown in the task switcher to a white icon,
        // otherwise the icon is red on red. Only needed up to Pie, which
        // uses the adaptive icon.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {

            Bitmap whiteIcon = BitmapFactory.decodeResource(getResources(), R.drawable.uob_logo);
            int appColour = ContextCompat.getColor(this, R.color.colorPrimary);

            //noinspection deprecation
            ActivityManager.TaskDescription description = new ActivityManager.TaskDescription(null, whiteIcon, appColour);
            setTaskDescription(description);
        }

        // If no course, load the welcome activity and clear activity stack
        if (settings.hasCourse() == false) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }

        // Should update sessions
        SessionListsFragment.InitialLoadMode sessionListsLoadArg;
        if (savedInstanceState != null && savedInstanceState.getBoolean(Args.shouldRestore.name(), false))
            sessionListsLoadArg = SessionListsFragment.InitialLoadMode.loadSessionsWithoutSnackbar;
        else if (shouldUpdate())
            sessionListsLoadArg = SessionListsFragment.InitialLoadMode.updateSessions;
        else
            sessionListsLoadArg = SessionListsFragment.InitialLoadMode.loadSessionsWithSnackbar;

        Logger.getInstance().debug("MainActivity", "sessionListFragmentArg: " + sessionListsLoadArg.name());

        // Check whether we should show term dates due to saved state
        boolean showTermDatesConfigChange = savedInstanceState != null &&
            savedInstanceState.getString(Args.fragment.name(), "").equals("TermDatesFragment");

        // Check whether we should show term dates due to app shortcut. We need to make sure
        // this only happens on launch, not every config change, so check for null saved state.
        boolean showTermDatesShortcut = savedInstanceState == null &&
            getIntent().getAction() != null
            && getIntent().getAction().equals("shortcutTermDates");

        // Check for internet connection if we're showing term dates from shortcut, otherwise
        // the webview error message will remain until configuration change
        if (showTermDatesShortcut && AndroidUtilities.hasNetwork(this) == false) {

            // Stay on sessions instead
            showTermDatesShortcut = false;

            // Show warning
            showNoInternetConnectionSnackbar();
        }

        Logger.getInstance()
            .debug("MainActivity", "showTermDatesConfigChange: " + showTermDatesConfigChange)
            .debug("MainActivity", "showTermDatesShortcut: " + showTermDatesShortcut);

        boolean showTermDates = showTermDatesConfigChange || showTermDatesShortcut;

        // Init sessions fragment
        int initialIndex = -1;
        if (savedInstanceState != null && savedInstanceState.containsKey(Args.currentIndex.name()))
            initialIndex = savedInstanceState.getInt(Args.currentIndex.name());
        frSessions = SessionListsFragment.newInstance(sessionListsLoadArg, initialIndex);

        // Init term dates fragment
        boolean loadTermDates = showTermDates || AndroidUtilities.getNetwork(this) == AndroidUtilities.NetworkType.Infrastructure;
        frTermDates = TermDatesFragment.newInstance(loadTermDates);

        // Set initial fragment

        // If sessions
        String title = TITLE_DEFAULT;
        int navDrawerSelected = 0;
        Fragment hideFragment = frTermDates;
        currentFragment = frSessions;
        boolean menuVisible = true;
        // If term dates
        if (showTermDates){
            title = TITLE_TERM_DATES;
            navDrawerSelected = 1;
            hideFragment = frSessions;
            currentFragment = frTermDates;
            menuVisible = false;
        }

        Logger.getInstance().debug("MainActivity", "Setting initial fragment: " + currentFragment.getClass().getSimpleName());

        // Commit fragment changes
        setTitle(title);
        navigationView.getMenu().getItem(navDrawerSelected).setChecked(true);
        setMenuVisible(menuVisible);

        fragmentManager = getSupportFragmentManager();

        if (fragmentManager.getFragments() != null) {
            for (Fragment fragment : fragmentManager.getFragments())
                fragmentManager.beginTransaction().remove(fragment).commit();
        }

        fragmentManager.beginTransaction()
            .add(R.id.flContent, frSessions, frSessions.getClass().getSimpleName())
            .add(R.id.flContent, frTermDates, frTermDates.getClass().getSimpleName())
            .show(currentFragment)
            .hide(hideFragment)
            .commit();

        fragmentManager.executePendingTransactions();

        // Show rate dialog on X launch
        if (settings.getLaunchCount() > 7 &&
            settings.getShownRateDialog() == false &&
            AndroidUtilities.hasNetwork(this)){

            AlertDialog d = new AlertDialog.Builder(this)
                .setNegativeButton("No thanks", null)
                .setPositiveButton("Sure", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AndroidUtilities.openPlayStorePage(MainActivity.this);
                    }
                })
                .setTitle("Rate in Play Store")
                .setMessage("Please rate this application if you find it useful. We wont bother you again.")
                .create();
            d.show();
            settings.setShownRateDialog();
        }

        // Open dev activity when the app bar is clicked repeatedly
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                devButtonClicks++;

                if (devButtonClicks >= 7) {
                    devButtonClicks = 0;

                    Logger.getInstance().debug("MainActivity", "Triggered developer activity");

                    startActivity(new Intent(getBaseContext(), DeveloperActivity.class));
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);

        // Set the visible state for the menu.
        meMain.setGroupVisible(R.id.grMainGroup, showMenu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_refresh) {

            // Warn about lack of network availability
            if (AndroidUtilities.hasNetwork(this) == false){
                showNoInternetConnectionSnackbar();
            } else {
                frSessions.updateSessions(settings.getCourse());
            }

            return true;
        } else if (id == R.id.action_toggle_hidden) {

            toggleShowHideSessions();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Logger.getInstance().debug("MainActivity", "NavigationItemSelected: " + item.getTitle());

        // Whether or not to set the selected item as selected
        boolean changedSelectedItem = true;

        // Whether to show the menu in the toolbar. 3 states (no change, show, hide).
        Boolean showMenu = null;

        String title = null;

        // Set sessions fragment
        if (id == R.id.nav_sessions) {
            title = TITLE_DEFAULT;
            currentFragment = frSessions;
            fragmentManager.beginTransaction().hide(frTermDates).show(frSessions).commit();
            AndroidUtilities.trySetElevation(abAppBar, 0);
            showMenu = Boolean.TRUE;
        }
        // Set term dates fragment
        else if (id == R.id.nav_termdates){

            // Warn about lack of network availability
            if (AndroidUtilities.hasNetwork(this) == false){
                showNoInternetConnectionSnackbar();
                changedSelectedItem = false;
            } else {
                title = TITLE_TERM_DATES;
                showMenu = Boolean.FALSE;
                currentFragment = frTermDates;
                fragmentManager.beginTransaction().hide(frSessions).show(frTermDates).commit();
                AndroidUtilities.trySetElevation(abAppBar, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));
                frTermDates.tryLoad();
            }
        }
        // Open course activity, don't change nav button
        else if (id == R.id.nav_course) {

            changedSelectedItem = false;

            // Warn about lack of network availability
            if (AndroidUtilities.hasNetwork(this) == false){
                showNoInternetConnectionSnackbar();
            } else {
                startActivity(new Intent(this, CourseListActivity.class));
            }
        }
        // Open settings activity, don't change nav button
        else if (id == R.id.nav_settings) {
            changedSelectedItem = false;
            startActivity(new Intent(this, SettingsActivity.class));
        }
        else if (id == R.id.nav_rate){

            changedSelectedItem = false;

            // Warn about lack of network availability
            if (AndroidUtilities.hasNetwork(this) == false)
                showNoInternetConnectionSnackbar();
            else
                AndroidUtilities.openPlayStorePage(this);
        }
        // Open about dialog, don't change nav button
        else if (id == R.id.nav_about) {

            changedSelectedItem = false;
            showAbout();
        }

        if (changedSelectedItem) {

            // Set title
            if (title != null)
                setTitle(title);

            // Set menu visibility
            setMenuVisible(showMenu);
        }

        // Close drawer, change active nav button, if changed
        dlDrawer.closeDrawer(GravityCompat.START);
        return changedSelectedItem;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){

        Logger.getInstance().debug("MainActivity", "Saving state");
        savedInstanceState.putBoolean(Args.shouldRestore.name(), true);

        // Save the current fragment
        if (currentFragment != null) {
            String currentFragmentClass = currentFragment.getClass().getSimpleName();
            Logger.getInstance().debug("MainActivity", "Saving current fragment: " + currentFragmentClass);
            savedInstanceState.putString(Args.fragment.name(), currentFragmentClass);
        }

        // Save current index in the SessionListsFragment ViewPager
        if (frSessions != null) {
            int currentIndex = frSessions.getSelectedIndex();
            savedInstanceState.putInt(Args.currentIndex.name(), currentIndex);
            Logger.getInstance().debug("MainActivity", "Saving current index: " + currentIndex);
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onResume() {

        super.onResume();

        Logger.getInstance().debug("MainActivity", "OnResume");

        // Start the session list redraw timer, because we are back to the foreground
        if (frSessions != null)
            frSessions.startRedrawUpdateTimer();
    }

    @Override
    public void onPause() {

        super.onPause();

        Logger.getInstance().debug("MainActivity", "OnPause");

        // Stop session list redraw timer, because the app is in the background
        if (frSessions != null)
            frSessions.stopRedrawUpdateTimer();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        Logger.getInstance().debug("MainActivity", "OnDestroy");
    }

    private void setMenuVisible(Boolean visible){

        if (visible == null)
            return;

        // As well as changing visibility, store the visible state. We'll need this later in case
        // we attempt to set menu visibility before the menu has been created.
        showMenu = visible;

        meMain.setGroupVisible(R.id.grMainGroup, showMenu);
    }

    public void updateNavDrawerLabels(Models.Course course, List<Models.DisplaySession> sessions){

        boolean showingHiddenSessions = settings.getShowHiddenSessions();

        View navHeaderView = navigationView.getHeaderView(0);
        TextView tvCourseName = (TextView)navHeaderView.findViewById(R.id.tvCourseName);
        TextView tvCourseDetails = (TextView)navHeaderView.findViewById(R.id.tvCourseDetails);
        TextView tvSessions = (TextView)navHeaderView.findViewById(R.id.tvSessionCount);

        tvCourseName.setText(course.nameStart);
        tvCourseDetails.setText(course.nameEnd);

        int sessionCount = 0;
        float hours = 0;
        for (Models.DisplaySession s : sessions) {
            if (s.visible == true || showingHiddenSessions == true) {
                hours += s.length;
                sessionCount++;
            }
        }

        String hoursStr = String.format("%.1f", hours);
        if ((int)hours == hours)
            hoursStr = (int)hours + "";

        tvSessions.setText(sessionCount + " sessions, " + hoursStr + " hours total");
    }

    private void toggleShowHideSessions(){

        // Can't toggle when we're in edit mode, warn user
        if (frSessions.getEditMode() == true) {
            AlertDialog d = new AlertDialog.Builder(this)
                .setPositiveButton(R.string.dialog_dismiss, null)
                .setTitle(R.string.warning_cant_toggle_show_hide)
                .setMessage(R.string.text_cant_toggle_show_hide)
                .create();
            d.show();
            return;
        }

        List<Models.DisplaySession> sessions = settings.getSessions();

        // Toggle value
        boolean showingHidden = settings.toggleShowHiddenSessions();
        Logger.getInstance().debug("MainActivity", "toggleShowHideSessions - Showing hidden: " + showingHidden);

        // Show message
        showSessionSnackbar(sessions, showingHidden, false);

        // Update fragment
        frSessions.refreshLists();

        // Update notification alarms, as some sessions may now be visible
        if (settings.getNotificationSessionRemindersEnabled()) {
            SessionReminderNotifier notifier = new SessionReminderNotifier(this);
            notifier.setAlarms(settings.getSessions(), settings.getNotificationSessionRemindersMinutes());
        }
    }

    public void showSessionSnackbar(List<Models.DisplaySession> sessions, boolean showingHidden,
                                    boolean showSavedTime){

        // Count sessions
        int sessionCount = sessions.size();

        // Count visible sessions
        int visibleSessions = 0;
        for (Models.DisplaySession session : sessions) {
            if (session.visible == true)
                visibleSessions++;
        }

        // Build string
        StringBuilder sb = new StringBuilder();

        // Add session count
        if (showingHidden == false)
            sb.append("Showing " + visibleSessions + " of " + sessionCount + " sessions");
        else
            sb.append("Showing all " + sessionCount + " sessions");

        // Add datetime when the sessions were cached, if applicable
        if (showSavedTime)
            sb.append(", last updated " + settings.getSessionsUpdatedTimeAgo());

        // Show snackbar
        Snackbar.make(clCoordinatorLayout, sb.toString(), Snackbar.LENGTH_LONG).show();
    }

    public void showNoInternetConnectionSnackbar(){

        CharSequence message = getText(R.string.net_required);
        Snackbar.make(clCoordinatorLayout, message, Snackbar.LENGTH_LONG).show();
    }

    private boolean shouldUpdate(){

        AndroidUtilities.NetworkType network =  AndroidUtilities.getNetwork(this);

        // Check if we received a forceRefreshSessions message in the intent (eg changed course)
        boolean forceSessionRefresh = getIntent().getBooleanExtra(Args.forceRefreshSessions.name(), false);

        if (forceSessionRefresh && network != AndroidUtilities.NetworkType.None)
            return true;
        else if (settings.getRefreshCellular() && network == AndroidUtilities.NetworkType.Cellular)
            return true;
        else if (settings.getRefreshWiFi() && network == AndroidUtilities.NetworkType.Infrastructure)
            return true;

        return false;
    }

    private void showAbout(){

        String version = AndroidUtilities.buildVersionName(this);
        String title = String.format("About (version %s)", version);
        String body = "Developed by <a href=\"https://adriankeenan.co.uk\">Adrian Keenan</a>.<br><br>" +
            "<a href=\"https://github.com/adriankeenan/uob-timetable-android\">Source code</a> " +
            "and <a href=\"https://github.com/adriankeenan/uob-timetable-android/releases\">changelog</a> available on GitHub.<br><br>" +
            "Third-party libraries:<br>" +
            "<a href=\"http://square.github.io/okhttp/\">square/okhttp</a> (Apache 2.0)<br>" +
            "<a href=\"https://github.com/google/gson\">google/gson</a> (Apache 2.0)<br>" +
            "<a href=\"https://commons.apache.org/proper/commons-lang/\">apache/commons-lang</a> (Apache 2.0)<br>" +
            "<a href=\"https://github.com/JakeWharton/ThreeTenABP\">JakeWharton/ThreeTenABP</a> (Apache 2.0)<br>" +
            "<a href=\"https://github.com/h6ah4i/android-tablayouthelper\">h6ah4i/android-tablayouthelper</a> (Apache 2.0)<br>" +
            "<a href=\"https://github.com/bugsnag/bugsnag-android\">bugsnag/bugsnag-android</a> (MIT)<br>" +
            "<br>" +
            getString(R.string.text_disclaimer) +
            "<br><br>" +
            "To find out about analytics data captured by this application, please read the " +
            "<a href=\"https://adriankeenan.co.uk/uobtimetable/privacypolicy\">Privacy Policy</a>.";

        AlertDialog d = new AlertDialog.Builder(this)
            .setPositiveButton(R.string.dialog_dismiss, null)
            .setTitle(title)
            .setMessage(AndroidUtilities.fromHtml(body))
            .create();
        d.show();

        // Make the TextView clickable. Must be called after show()
        ((TextView)d.findViewById(android.R.id.message))
            .setMovementMethod(LinkMovementMethod.getInstance());
    }
}
