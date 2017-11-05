package com.ak.uobtimetable.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.ak.uobtimetable.R;
import com.ak.uobtimetable.Utilities.Logging.Logger;


/**
 * Fragment containing a WebView which loads the term dates page from the university website.
 */
public class TermDatesFragment extends Fragment {

    private WebView wvContent;
    private ProgressBar pbLoad;
    private boolean triedLoad;

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

        pbLoad = (ProgressBar)view.findViewById(R.id.pbLoad);
        pbLoad.setVisibility(View.GONE);

        // Load WebView content
        // We do not get an error if this fails for some reason
        wvContent = (WebView)view.findViewById(R.id.wvContent);

        // Set custom WebChromeClient for reporting progress
        wvContent.setWebChromeClient(new TermDateWebChromeClient(this));

        if (getArguments().getBoolean(Args.loadOnStart.name()))
            tryLoad();

        return view;
    }

    public void tryLoad(){

        if (triedLoad == true)
            return;

        // Load the content. This is rather chunky (>2mb), so load when requested.
        wvContent.loadUrl("https://www.beds.ac.uk/about-us/our-university/dates");

        // NOTE: Don't bother trying to detect errors. It's a complete mess,
        // see http://stackoverflow.com/questions/32769505/webviewclient-onreceivederror-deprecated-new-version-does-not-detect-all-errors/33419123#comment69949752_33419123

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

    private class TermDateWebChromeClient extends WebChromeClient {

        private TermDatesFragment fragment;

        public TermDateWebChromeClient(TermDatesFragment fragment){

            this.fragment = fragment;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {

            super.onProgressChanged(view, newProgress);

            // Set progress bar visible based on progress
            // Will still progress to 100% if an error is encountered
            if (newProgress > 0 && newProgress < 100)
                fragment.pbLoad.setVisibility(View.VISIBLE);
            else if (newProgress == 100)
                fragment.pbLoad.setVisibility(View.GONE);

            Logger.getInstance().info("TermDatesFragment", "Webview load %: " + newProgress);
        }
    }
}
