package com.ak.uobtimetable;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.ak.uobtimetable.API.Models;
import com.ak.uobtimetable.API.Service;
import com.ak.uobtimetable.Exceptions.HTTPException;
import com.ak.uobtimetable.ListAdapters.CourseListAdapter;
import com.ak.uobtimetable.Utilities.AndroidUtilities;
import com.ak.uobtimetable.Utilities.Logging.Logger;
import com.ak.uobtimetable.Utilities.SettingsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity which allows the user to select their course.
 */
public class CourseListActivity extends AppCompatActivity {

    public Spinner spDepartments;
    public Spinner spLevels;
    public ListView lvCourses;
    public TextView tvListEmpty;
    public ProgressBar pbDownload;
    public LinearLayout llCoursesLayout;

    public List<View> enableControls;

    public List<Models.Department> departments = new ArrayList<>();
    public List<Models.Course> courses = new ArrayList<>();
    public List<Models.Level> levels = new ArrayList<>();
    public List<Models.Course> coursesFiltered = new ArrayList<>();

    private int selectedDepartmentIndex = -1;
    private int selectedLevelIndex = -1;

    public enum Args {
        departmentIndex,
        levelIndex,
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_list);

        Logger.getInstance().debug("CourseListActivity", "onCreate");

        // Show back button in actionbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get references to UI elements
        lvCourses = (ListView)findViewById(R.id.lvCourses);
        tvListEmpty = (TextView)findViewById(R.id.tvListEmpty);
        spDepartments = (Spinner)findViewById(R.id.spDepartments);
        spLevels = (Spinner)findViewById(R.id.spLevels);
        pbDownload = (ProgressBar)findViewById(R.id.pbDownload);
        llCoursesLayout = (LinearLayout)findViewById(R.id.llCoursesLayout);

        pbDownload.setVisibility(View.INVISIBLE);
        lvCourses.setEmptyView(tvListEmpty);

        // Hide certain controls until we have data to populate them
        enableControls = new ArrayList<>();
        enableControls.add(spDepartments);
        enableControls.add(spLevels);
        enableControls.add(llCoursesLayout);

        for (View control : enableControls)
            control.setVisibility(View.GONE);

        // Copy the selected department index from previous instance state
        if (savedInstanceState != null) {
            selectedDepartmentIndex = savedInstanceState.getInt(Args.departmentIndex.name());
            selectedLevelIndex = savedInstanceState.getInt(Args.levelIndex.name());
        }

        // Download the course list
        pbDownload.setVisibility(View.VISIBLE);
        new DownloadCoursesTask(this).execute();

        // Set department and level select handlers
        AdapterView.OnItemSelectedListener spListener = new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                updateCourseList(spDepartments.getSelectedItemPosition(), spLevels.getSelectedItemPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }
        };

        spDepartments.setOnItemSelectedListener(spListener);
        spLevels.setOnItemSelectedListener(spListener);

        // Set course select handler
        lvCourses.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Get selected course
                final Models.Course selectedCourse = coursesFiltered.get(position);

                // Ask user to confirm
                String msg = String.format("Do you want to set <i>%s</i> as your course?", selectedCourse.name);
                AlertDialog.Builder builder = new AlertDialog.Builder(CourseListActivity.this)
                    .setTitle("Confirm")
                    .setMessage(AndroidUtilities.fromHtml(msg));
                builder
                    .setPositiveButton(
                        R.string.dialog_positive,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();

                                // Save course
                                SettingsManager.getInstance(CourseListActivity.this).setCourse(selectedCourse);

                                // Launch main activity and remove this from stack. Force MainActivity to refresh
                                // sessions as the course may have changed.
                                Intent i = new Intent(CourseListActivity.this, MainActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    .putExtra(MainActivity.Args.forceRefreshSessions.name(), true);
                                startActivity(i);
                            }
                        }
                    )
                    .setNegativeButton(R.string.dialog_negative, null)
                    .show();
            }
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        Logger.getInstance().debug("CourseListActivity", "Saving state");

        // Store department and level indexes. The course list JSON cannot be stored as we would
        // likely hit the 1mb bundle limit and throw an exception on 7.0+.
        outState.putInt(Args.departmentIndex.name(), spDepartments.getSelectedItemPosition());
        outState.putInt(Args.levelIndex.name(), spLevels.getSelectedItemPosition());

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home){
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateCourseList(int departmentIndex, int levelIndex){

        if (departmentIndex < 0 || levelIndex < 0)
            return;

        // Get selected department and level
        Models.Department selectedDepartment = departments.get(departmentIndex);
        Models.Level selectedLevel = levels.get(levelIndex);

        // Get relevant courses for department
        coursesFiltered.clear();
        for (Models.Course c : courses){
            if (c.department != null && c.department.id.equals(selectedDepartment.id) && c.level.equals(selectedLevel.name))
                coursesFiltered.add(c);
        }

        // Set courses
        CourseListAdapter courseAdapter = new CourseListAdapter(CourseListActivity.this, coursesFiltered);
        lvCourses.setAdapter(courseAdapter);
    }

    private void setUpUi(){

        // Add departments to spinner
        List<String> departmentNamesList = new ArrayList<>();
        for (Models.Department dept : departments)
            departmentNamesList.add(dept.name + " (" + dept.courseCount + ")");
        String[] departmentNamesArr = departmentNamesList.toArray(new String[0]);

        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, departmentNamesArr);
        spDepartments.setAdapter(departmentAdapter);

        // Add levels to spinner
        List<String> levelNamesList = new ArrayList<>();
        for (Models.Level level : levels)
            levelNamesList.add(level.name);
        String[] levelNamesArr = levelNamesList.toArray(new String[0]);

        ArrayAdapter<String> levelsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, levelNamesArr);
        spLevels.setAdapter(levelsAdapter);

        // Set spinner to previously selected department and level, if set
        try {
            spDepartments.setSelection(selectedDepartmentIndex, false);
            spLevels.setSelection(selectedLevelIndex, false);
            updateCourseList(selectedDepartmentIndex, selectedLevelIndex);
        } catch (Exception e) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("department_index", Integer.valueOf(selectedDepartmentIndex).toString());
            metadata.put("level_index", Integer.valueOf(selectedLevelIndex).toString());
            e = new Exception("Failed to select department or level at index", e);
            Logger.getInstance().error("CourseListActivity", e, metadata);
        }
    }

    private class DownloadCoursesTask extends AsyncTask<Void, Integer, Models.CourseResponse> {

        private AppCompatActivity activity = null;
        private Exception fetchException;

        DownloadCoursesTask(AppCompatActivity activity){
            this.activity = activity;
        }

        protected Models.CourseResponse doInBackground(Void... params) {

            // Make API call
            Models.CourseResponse response = null;
            try {
                Service service = new Service(getApplicationContext());
                response = service.getCourses();
            } catch (Exception e) {
                fetchException = e;
            } finally {
                // Log Exception or response error here
                if (fetchException != null || response.error){

                    // Get previous exception or make from response
                    Exception cause = fetchException;
                    if (cause == null)
                        cause = new Exception(response.errorStr);

                    Exception e = new Exception("Failed to download courses.", cause);

                    HashMap<String, String> metadata = new HashMap<>();

                    // Add HTTP exception data
                    if (cause instanceof HTTPException)
                        metadata.putAll(((HTTPException) cause).toMap());

                    // Add response error
                    if (response != null) {
                        metadata.put("error", response.errorStr);
                        metadata.put("error_id", response.errorId);
                    }

                    Logger.getInstance().error("Course download", e, metadata);
                }
            }

            return response;
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(Models.CourseResponse response) {

            // Hide progress bar
            pbDownload.setVisibility(View.INVISIBLE);

            // Error handling
            if (fetchException != null){
                AlertDialog d = new AlertDialog.Builder(activity)
                    .setPositiveButton(R.string.dialog_dismiss, null)
                    .setTitle(R.string.warning_course_download_error)
                    .setMessage(fetchException.getMessage())
                    .create();
                d.show();
                return;
            } else if (response.error) {
                AlertDialog d = new AlertDialog.Builder(activity)
                    .setPositiveButton(R.string.dialog_dismiss, null)
                    .setTitle(R.string.warning_course_download_error)
                    .setMessage(R.string.warning_server_error)
                    .create();
                d.show();
                return;
            }

            // Show controls
            for (View control : enableControls)
                control.setVisibility(View.VISIBLE);

            // Copy contents
            courses = response.courses;
            departments = response.departments;
            levels = response.levels;

            // If we don't have selected department and level from previous saved state,
            // set the department and level for current course
            Models.Course course = new SettingsManager(activity).getCourse();

            if (selectedDepartmentIndex == -1 && course != null) {
                selectedDepartmentIndex = 0;
                for (int i = 0; i < departments.size(); i++){
                    if (departments.get(i).id.equals(course.department.id))
                        selectedDepartmentIndex = i;
                }
            }

            if (selectedLevelIndex == -1 && course != null) {
                selectedLevelIndex = 0;
                for (int i = 0; i < levels.size(); i++){
                    if (levels.get(i).name.equals(course.level))
                        selectedLevelIndex = i;
                }
            }

            // Populate UI
            setUpUi();
        }
    }
}