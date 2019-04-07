package com.ak.uobtimetable.API;

import org.apache.commons.lang3.StringUtils;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

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
        public String errorId;
    }

    public class Department {
        public String id;
        public String name;
        public int courseCount;
    }

    public class Level {
        public String name;
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
        public List<Level> levels;
    }

    public class Session {
        public String moduleName;
        public int day;
        public String dayName;
        public String start;
        public String end;
        public float length;
        public String lengthStr;
        public String type;
        public List<String> rooms;
        public List<String> roomsShort;
        public String hash;
        public boolean isValid;
    }

    public class SessionResponse extends Response {
        public String timetableUrl;
        public String courseName;
        public String dateRange;
        public List<DisplaySession> sessions;
    }

    public class DisplaySession extends Session {

        public boolean visible;
        private final String MODULE_UNAVAILABLE = "Module name unavailable";

        public DisplaySession(){

            visible = true;
        }

        public String getTitle(){

            if (isValid == false)
                return MODULE_UNAVAILABLE;

            return moduleName;
        }

        public String getLongTitle(){

            if (isValid == false)
                return MODULE_UNAVAILABLE + " - " + type;

            return moduleName + " - " + type;
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

            sb.append("On " + dayName + "\n");
            sb.append("At " + start + "\n");
            sb.append("For " + lengthStr + "\n");

            List<String> roomList = longRoomNames ? rooms : roomsShort;
            if (roomList.size() == 0)
                sb.append("No rooms listed\n");
            else
                sb.append("In " + StringUtils.join(roomList, ", ") + "\n");

            return sb.toString();
        }

        public TimeState getState(){

            ZonedDateTime date = ZonedDateTime.now(ZoneId.of("Europe/London"));

            // Get current day of week
            int currentDayOfWeek = date.getDayOfWeek().getValue() - 1;

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

            int currentTimeMinutes = date.getHour() * 60 + date.getMinute();

            if (currentTimeMinutes < startTimeMinutes)
                return TimeState.Future;
            else if (currentTimeMinutes > endTimeMinutes)
                return TimeState.Elapsed;
            else
                return TimeState.Ongoing;
        }

        public DayOfWeek getDayOfWeek(){

            return new DayOfWeek[]{
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
            }[this.day];
        }

        public boolean equals(Session other){

            return hash.equals(other.hash);
        }

        public void update(DisplaySession other){

            this.visible = other.visible;
        }
    }
}
