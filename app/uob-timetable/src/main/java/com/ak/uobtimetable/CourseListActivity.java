package com.ak.uobtimetable;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import com.ak.uobtimetable.API.Service;
import com.ak.uobtimetable.API.Models;
import com.ak.uobtimetable.ListAdapters.CourseListAdapter;
import com.ak.uobtimetable.Utilities.AndroidUtilities;
import com.ak.uobtimetable.Utilities.Logging.Logger;
import com.ak.uobtimetable.Utilities.SettingsManager;
import com.google.gson.JsonParseException;

/**
 * Activity which allows the user to select their course.
 */
public class CourseListActivity extends AppCompatActivity {

    public Spinner spDepartments;
    public ListView lvCourses;
    public ProgressBar pbDownload;

    public List<Models.Department> departments = new ArrayList<>();
    public List<Models.Course> courses = new ArrayList<>();
    public List<Models.Course> coursesForSelectedDepartment = new ArrayList<>();
    private int selectedDepartmentIndex = -1;
    private String initialDepartmentId = null;

    public enum Args {
        departmentIndex,
        departmentId
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_list);

        // Get references to UI elements
        lvCourses = (ListView)findViewById(R.id.lvCourses);
        spDepartments = (Spinner)findViewById(R.id.spDepartments);
        pbDownload = (ProgressBar)findViewById(R.id.pbDownload);

        pbDownload.setVisibility(View.INVISIBLE);

        // Copy the selected department index from previous instance state
        if (savedInstanceState != null) {
            selectedDepartmentIndex = savedInstanceState.getInt(Args.departmentIndex.name());
        }
        // Or check if we were passed a course name or ID
        else if (getIntent().hasExtra(Args.departmentId.name())){
            initialDepartmentId = getIntent().getStringExtra(Args.departmentId.name());
        }

        // Download the course list
        pbDownload.setVisibility(View.VISIBLE);
        new DownloadCoursesTask(this).execute();

        // Set department select handler
        spDepartments.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                updateCourseList(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }
        });

        // Set course select handler
        lvCourses.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Get selected course
                final Models.Course selectedCourse = coursesForSelectedDepartment.get(position);

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

                                Intent i = new Intent(CourseListActivity.this, MainActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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

        // Store department index. The course list JSON cannot be stored as we would
        // likely hit the 1mb bundle limit and throw an exception on 7.0+.
        outState.putInt(Args.departmentIndex.name(), spDepartments.getSelectedItemPosition());

        super.onSaveInstanceState(outState);
    }

    private void updateCourseList(int departmentIndex){

        if (departmentIndex < 0)
            return;

        // Get selected department
        Models.Department selectedDepartment = departments.get(departmentIndex);

        // Get relevant courses for department
        coursesForSelectedDepartment.clear();
        for (Models.Course c : courses){
            if (c.department != null && c.department.id.equals(selectedDepartment.id))
                coursesForSelectedDepartment.add(c);
        }

        // Set courses
        CourseListAdapter courseAdapter = new CourseListAdapter(CourseListActivity.this, coursesForSelectedDepartment);
        lvCourses.setAdapter(courseAdapter);
    }

    private void setUpUi(List<Models.Department> departments, List<Models.Course> courses,
                         int selectedDepartmentIndex){

        // Add departments to list
        // There's no point using a custom ArrayAdaptor here
        List<String> departmentNamesList = new ArrayList<>();
        for (Models.Department dept : departments)
            departmentNamesList.add(dept.name + " (" + dept.courseCount + ")");
        String[] departmentNamesArr = departmentNamesList.toArray(new String[0]);

        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, departmentNamesArr);
        spDepartments.setAdapter(departmentAdapter);

        // Set selected department. If we have an ID, set the index based on that.
        // The index will then be saved for future onCreate calls.
        if (initialDepartmentId != null){
            for (int i = 0; i < departments.size(); i++){
                if (departments.get(i).id.equals(initialDepartmentId))
                    selectedDepartmentIndex = i;
            }
        }
        // Set course by index, if we have one.
        if (selectedDepartmentIndex >= 0) {
            try {
                spDepartments.setSelection(selectedDepartmentIndex, false);
                updateCourseList(selectedDepartmentIndex);
            } catch (Exception e) {
                e = new Exception("Failed to select department at index", e);
                Logger.getInstance().error("CourseListActivity", e);
            }
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
                // Wrap JSON parse exception
                if (e instanceof JsonParseException) {
                    e = new Exception("Failed to parse JSON", e);
                }

                fetchException = e;
                Logger.getInstance().error("Course download", e);
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
                Logger.getInstance().error("Course download", "Server returning error msg: " + response.errorStr);
                AlertDialog d = new AlertDialog.Builder(activity)
                    .setPositiveButton(R.string.dialog_dismiss, null)
                    .setTitle(R.string.warning_course_download_error)
                    .setMessage(R.string.warning_server_error)
                    .create();
                d.show();
                return;
            }

            // Copy contents
            courses = response.courses;
            departments = response.departments;

            // Populate UI
            setUpUi(departments, courses, selectedDepartmentIndex);
        }
    }
}