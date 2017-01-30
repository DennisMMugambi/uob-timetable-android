package com.ak.uobtimetable.API;

import org.apache.commons.lang3.StringUtils;

import java.util.Calendar;
import java.util.List;

/**
 * Contains classes for data returned by the API
 */
public class Models {

    public enum TimeState { Elapsed, Ongoing, Future }

    public abstract class Response {
        public double responseTime;
        public boolean error;
        public String errorStr;
    }

    public class Department {
        public String id;
        public String name;
        public int courseCount;
    }

    public class Course {
        public String id;
        public String name;
        public String level;
        public Department department;
        public String sessionUrl;
        public String nameStart;
        public String nameEnd;
    }

    public class CourseResponse extends Response {
        public List<Course> courses;
        public List<Department> departments;
    }

    public class Session {
        public String moduleCode;
        public String moduleName;
        public int day;
        public String start;
        public String end;
        public float length;
        public String lengthStr;
        public String type;
        public List<String> rooms;
        public List<String> roomsShort;
        public List<String> staff;
        public String hash;
        public boolean isValid;
    }

    public class SessionResponse extends Response {
        public List<DisplaySession> sessions;
    }

    public class DisplaySession extends Session {

        public boolean visible;

        public DisplaySession(){

            visible = true;
        }

        public String getDay(){
            return new String[] { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday" }[day];
        }

        public String getTitle(){
            return moduleName + " (" + moduleCode + ") " + type;
        }

        public String getSubtitle(){

            StringBuilder sb = new StringBuilder();

            sb.append(type);
            sb.append(" / ");
            sb.append(lengthStr);
            sb.append(" / ");

            if (roomsShort.size() == 0)
                sb.append("No rooms");
            else
                sb.append(StringUtils.join(roomsShort, ", ") + "\n");

            return sb.toString();
        }

        public String getDescription(boolean longRoomNames){

            StringBuilder sb = new StringBuilder();

            sb.append("On " + getDay() + "\n");
            sb.append("At " + start + "\n");
            sb.append("For " + lengthStr + "\n");

            List<String> roomList = longRoomNames ? rooms : roomsShort;
            if (roomList.size() == 0)
                sb.append("No rooms listed\n");
            else
                sb.append("In " + StringUtils.join(roomList, ", ") + "\n");

            if (staff.size() == 0)
                sb.append("No staff listed");
            else
                sb.append("With " + StringUtils.join(staff, ", "));

            return sb.toString();
        }

        public TimeState getState(){

            Calendar calendar = Calendar.getInstance();

            // Get current day of week
            int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 2;

            // Set timestate based on day
            // If after Friday, count sessions as in the future
            if (currentDayOfWeek >= 5)
                return TimeState.Future;
            else if (currentDayOfWeek < day)
                return TimeState.Future;
            else if (currentDayOfWeek > day)
                return TimeState.Elapsed;

            // Session is today, check current time
            String[] startTimeParts = start.split(":");
            int startTimeMinutes = (Integer.parseInt(startTimeParts[0]) * 60) + Integer.parseInt(startTimeParts[1]);

            String[] endTimeParts = end.split(":");
            int endTimeMinutes = (Integer.parseInt(endTimeParts[0]) * 60) + Integer.parseInt(endTimeParts[1]);

            int currentTimeMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

            if (currentTimeMinutes < startTimeMinutes)
                return TimeState.Future;
            else if (currentTimeMinutes > endTimeMinutes)
                return TimeState.Elapsed;
            else
                return TimeState.Ongoing;
        }

        public boolean equals(Session other){

            return hash.equals(other.hash);
        }

        public void update(DisplaySession other){

            this.visible = other.visible;
        }
    }
}
