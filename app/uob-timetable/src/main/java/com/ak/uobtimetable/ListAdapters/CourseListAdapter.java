package com.ak.uobtimetable.ListAdapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ak.uobtimetable.R;

import java.util.List;

import com.ak.uobtimetable.API.Models;

public class CourseListAdapter extends BaseAdapter {

    private List<Models.Course> courses;
    private LayoutInflater liInflater;

    public CourseListAdapter(Activity activity, List<Models.Course> courses){
        this.courses = courses;
        this.liInflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return courses.size();
    }

    @Override
    public Object getItem(int position) {
        return courses.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null)
            convertView = liInflater.inflate(R.layout.row_course_list, null);

        Models.Course course = (Models.Course)getItem(position);

        TextView tvTitle = (TextView) convertView.findViewById(R.id.tvTitle);
        TextView tvSubtitle = (TextView) convertView.findViewById(R.id.tvSubtitle);

        tvTitle.setText(course.nameStart);
        tvSubtitle.setText(course.nameEnd);

        return convertView;
    }
}
