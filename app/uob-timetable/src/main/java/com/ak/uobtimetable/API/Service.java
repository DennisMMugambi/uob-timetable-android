package com.ak.uobtimetable.API;

import android.content.Context;

import com.ak.uobtimetable.Utilities.AndroidUtilities;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import com.ak.uobtimetable.Utilities.Logger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Provides methods for accessing the HTTP timetable API
 */
public class Service {

    private OkHttpClient okHttpClient;
    private Gson gson;
    private Context context;

    private final String API_ENDPOINT = "https://adriankeenan.co.uk/uobtimetable/api/index.php/courses";

    public Service(Context appContext){

        context = appContext;
        okHttpClient = new OkHttpClient();
        gson = makeGson();
    }

    public static Gson makeGson(){

        return new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    }

    private Request makeRequest(String url){

        // Build a new request with an explicitly set User-Agent, otherwise the UA
        // gets set to "okhttp/x.y.z"
        String userAgent = System.getProperty("http.agent");
        int buildVersion = AndroidUtilities.buildVersionCode(context);
        return new Request.Builder().url(url)
            .addHeader("User-Agent", userAgent)
            .addHeader("App-Version", Integer.valueOf(buildVersion).toString())
            .build();
    }

    private String getString(String url) throws IOException {

        Logger.getInstance().debug("HTTP", "Request for: " + url);

        try {
            Request request = makeRequest(url);
            Response response = okHttpClient.newCall(request).execute();

            // Body string can only be read once, so store a copy
            String bodyString = response.body().string();

            // OkHTTP will remove Content-Encoding for a compressed response, as it is no longer
            // relevant to the output data
            // https://github.com/square/okhttp/wiki/Calls#rewriting-responses
            boolean responseCompressed = response.header("Content-Encoding") == null;
            if (responseCompressed == false)
                Logger.getInstance().warn("HTTP", "Response was not compressed");
            else
                Logger.getInstance().info("HTTP", "Response was compressed");

            // Log other request details
            Logger.getInstance()
                .info("HTTP", "Response body size kb: " + String.format("%.02f", bodyString.length() / 1024.0))
                .info("HTTP", "Response code: " + response.code());

            return bodyString;

        } catch (SocketTimeoutException e) {
            throw new IOException("Communication with server timed out. Please check your internet connection.", e);
        } catch (Exception e){
            throw new IOException("Failed to download from server.", e);
        }
    }

    /**
     * Gets a list of courses and departments from the API
     * @return CourseResponse
     * @throws Exception
     */
    public Models.CourseResponse getCourses() throws Exception {

        String json = getString(this.API_ENDPOINT);
        Models.CourseResponse response =  gson.fromJson(json, new TypeToken<Models.CourseResponse>() {}.getType());

        if (response == null)
            throw new Exception("Gson returned null: " + json);

        Logger.getInstance().info("Service", "getCourses response time: " + String.format("%.02f", response.responseTime));

        return response;
    }

    /**
     * Gets a list of sessions for a given course from the API
     * @param url URL for the API call
     * @return SessionResponse
     * @throws Exception
     */
    public Models.SessionResponse getSessions(String url) throws Exception {

        String json = getString(url);
        Models.SessionResponse response = gson.fromJson(json, new TypeToken<Models.SessionResponse>() {}.getType());

        if (response == null)
            throw new Exception("Gson returned null: " + json);

        Logger.getInstance().info("Service", "getSessions response time: " + String.format("%.02f", response.responseTime));

        // If error, return response now. Further code access attributes
        // that will be null due to error response.
        if (response.error)
            return response;

        // Ensure all sessions are visible by default
        for (Models.DisplaySession session : response.sessions)
            session.visible = true;

        // Log invalid
        for (Models.DisplaySession session : response.sessions) {
            if (session.isValid == false)
                Logger.getInstance().warn("Service", "Invalid session: " + session.getLongTitle());
        }

        // Log no sessions
        if (response.sessions.size() == 0)
            Logger.getInstance().warn("Service", "0 sessions in response for URL: " + url);

        return response;
    }

    /**
     * Copies attributes from one list of sessions (eg whether hidden) to another, wherever the same
     * session exists in both lists.
     * @param newSessionList The list of sessions to copy attributes to
     * @param oldSessionList The list of sessions to copy attributes from
     * @return newSessionList
     */
    public List<Models.DisplaySession> syncSessionLists(List<Models.DisplaySession> newSessionList,
                                                        List<Models.DisplaySession> oldSessionList){

        int updatedSessions = 0;
        for (Models.DisplaySession newSession : newSessionList){
            for (Models.DisplaySession oldSession : oldSessionList){
                if (newSession.equals(oldSession)) {
                    newSession.update(oldSession);
                    updatedSessions++;
                    break;
                }
            }
        }

        Logger.getInstance()
            .info("Service", "Sessions updated: " + updatedSessions)
            .info("Service", "Sessions removed from new list: " + (oldSessionList.size() - updatedSessions));

        return newSessionList;
    }
}
