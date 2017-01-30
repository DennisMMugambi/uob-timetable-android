package com.ak.uobtimetable.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.ak.uobtimetable.R;
import com.ak.uobtimetable.Utilities.Logger;


/**
 * Fragment containing a WebView which loads the term dates page from the university website.
 */
public class TermDatesFragment extends Fragment {

    public WebView wvContent;
    public boolean triedLoad;

    public enum Args {
        loadOnStart
    }

    public TermDatesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TermDatesFragment.
     */
    public static TermDatesFragment newInstance(boolean loadOnStart) {

        TermDatesFragment fragment = new TermDatesFragment();
        Bundle args = new Bundle();
        args.putBoolean(Args.loadOnStart.name(), loadOnStart);
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

        triedLoad = false;

        View view = inflater.inflate(R.layout.fragment_term_dates, container, false);

        // Load WebView content
        // We do not get an error if this fails for some reason
        wvContent = (WebView)view.findViewById(R.id.wvContent);

        if (getArguments().getBoolean(Args.loadOnStart.name()))
            tryLoad();

        return view;
    }

    public void tryLoad(){

        if (triedLoad == true)
            return;

        // Load the content.
        // This is rather chunky (>2mb), so load when requested.
        wvContent.loadUrl("https://www.beds.ac.uk/about-us/our-university/dates");

        triedLoad = true;
        Logger.getInstance().debug("TermDatesFragment", "WebView loaded");
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
