package com.joheenchakraborty.v3;


import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Joheen Chakraborty on 8/6/2017.
 */

public class AsteroidClass extends Fragment {

    ArrayList<Asteroid> asteroids = new ArrayList<Asteroid>();
    private final String baseURL = "https://ssd.jpl.nasa.gov/sbdb_query.cgi?obj_group=neo;obj_kind=ast;obj_numbered=all;ast_orbit_class=IEO;ast_orbit_class=IMB;ast_orbit_class=TNO;ast_orbit_class=ATE;ast_orbit_class=MBA;ast_orbit_class=PAA;ast_orbit_class=APO;ast_orbit_class=OMB;ast_orbit_class=HYA;ast_orbit_class=AMO;ast_orbit_class=TJN;ast_orbit_class=AST;ast_orbit_class=MCA;ast_orbit_class=CEN;OBJ_field=0;ORB_field=0;table_format=HTML;max_rows=500;format_option=comp;query=Generate%20Table;c_fields=AcBsBhBiApBuAh;c_sort=;.cgifields=format_option;.cgifields=obj_kind;.cgifields=obj_group;.cgifields=obj_numbered;.cgifields=ast_orbit_class;.cgifields=table_format;.cgifields=com_orbit_class&page=";
    private final int NUM_PAGES = 33, ENTRIES_PER_PAGE = 500;
    private int numRelevantPages = -1, entriesLastPage;

    int earthmoonimg = R.drawable.earth_moon, earthasteroidimg = R.drawable.earth_asteroid, infoButtonImg = R.drawable.info, hazard = R.drawable.hazard, blank = R.drawable.blank;
    int[] comparisonImages = {R.drawable.question_mark, R.drawable.less, R.drawable.greater};
    private ProgressDialog pd;

    private static String[] urls;
    private static ArrayList<String> asteroidData = new ArrayList<String>();
    private static ArrayList<Integer> asteroidLines = new ArrayList<Integer>();

    private RecyclerView recyclerView;
    private AsteroidAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private String[] sortKeywords = {"Alphabetical (A -> Z)", "Alphabetical (Z -> A)", "Diameter (L -> G)", "Diameter (G -> L)", "Period (L -> G)", "Period (G -> L)", "Semi-major axis (L -> G)", "Semi-major axis (G -> L)",
            "Closest Sun approach (L -> G)", "Closest Sun approach (G -> L)", "Closest Earth approach (L -> G)", "Closest Earth approach (G -> L)", "Potentially hazardous"};
    Spinner spinner;
    ArrayAdapter<String> spinnerAdapter;

    private final String DATAFILE = "asteroiddatafile";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        readAsteroidWebPage(baseURL);

        View view = inflater.inflate(R.layout.asteroid_recyclerview, container, false);

        recyclerView = (RecyclerView)view.findViewById(R.id.asteroidRV);
        layoutManager = new LinearLayoutManager(view.getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new AsteroidAdapter(recyclerView);
        recyclerView.setAdapter(adapter);

        spinner = (Spinner)view.findViewById(R.id.asteroidsort);
        spinnerAdapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_spinner_item, sortKeywords);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                try {
                    if (asteroids.size() > 0) {
                        pd = ProgressDialog.show(getActivity(), "Loading", "Sorting asteroids");
                        Collections.sort(asteroids, new CustomComparator(sortKeywords[i]));
                        pd.dismiss();
                        adapter.notifyDataSetChanged();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        final View v1 = view;
        adapter.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                if (asteroids.size() <= NUM_PAGES*ENTRIES_PER_PAGE-ENTRIES_PER_PAGE+entriesLastPage) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            readAsteroidWebPage(baseURL);

                            adapter.notifyDataSetChanged();
                            adapter.setLoaded();
                        }
                    }, 5000);
                } else {
                    Toast.makeText(v1.getContext(), "Loading data completed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    public void readAsteroidWebPage(String baseURL) {
        numRelevantPages++;
        while (numRelevantPages>NUM_PAGES) numRelevantPages--;
        String url = baseURL;
        url += Integer.toString(NUM_PAGES - numRelevantPages);
        AsteroidClass.AsteroidData task = new AsteroidClass.AsteroidData();
        task.execute(new String[]{url});
    }

    public void openWebpage(View view, String URL) {
        Uri uriUrl = Uri.parse(URL);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
    }

    public class AsteroidAdapter extends RecyclerView.Adapter {

        private final int VIEW_TYPE_ITEM = 0, VIEW_TYPE_LOADING = 1;
        private OnLoadMoreListener onLoadMoreListener;
        private boolean isLoading;
        private int visibleThreshold = 4;
        private int lastVisibleItem, totalItemCount;


        public void setOnLoadMoreListener(OnLoadMoreListener mOnLoadMoreListener) {
            onLoadMoreListener = mOnLoadMoreListener;
        }


        public AsteroidAdapter(final RecyclerView recyclerView) {

            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager)recyclerView.getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView1, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    totalItemCount = linearLayoutManager.getItemCount();
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                    if (!isLoading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                        if (onLoadMoreListener != null) {
                            onLoadMoreListener.onLoadMore();
                        }
                        isLoading = true;
                    }
                }
            });
        }

        class LoadingViewHolder extends RecyclerView.ViewHolder {
            public ProgressBar progressBar;

            public LoadingViewHolder(View view) {
                super(view);
                progressBar = (ProgressBar)view.findViewById(R.id.asteroidprogressbar);
            }
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            public int currentItem;
            public TextView asteroidName, asteroidData, comparisonScale;
            public ImageView earthAsteroid, earthMoon, infoButton, comparison, hazard;
            public ItemViewHolder(View view) {
                super(view);
                asteroidName = (TextView)view.findViewById(R.id.asteroid_name);
                asteroidData = (TextView)view.findViewById(R.id.asteroid_data);
                comparisonScale = (TextView)view.findViewById(R.id.comparisonScale_asteroid);
                earthAsteroid = (ImageView)view.findViewById(R.id.earth_asteroid_compare);
                earthMoon = (ImageView)view.findViewById(R.id.earth_moon_compare);
                infoButton = (ImageView)view.findViewById(R.id.info_asteroid);
                comparison = (ImageView)view.findViewById(R.id.asteroid_comparison);
                hazard = (ImageView)view.findViewById(R.id.hazardSign);
            }
        }

        @Override
        public int getItemViewType(int i) {
            return asteroids.get(i) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup vg, int viewType) {
            if (viewType == VIEW_TYPE_ITEM) {
                View v = LayoutInflater.from(vg.getContext()).inflate(R.layout.asteroid_item, vg, false);
                return new ItemViewHolder(v);
            }
            else if (viewType == VIEW_TYPE_LOADING) {
                View v = LayoutInflater.from(vg.getContext()).inflate(R.layout.asteroid_loader, vg, false);
                return new LoadingViewHolder(v);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder vh, int i) {
            if (vh instanceof ItemViewHolder) {
                final int i1 = i;
                ItemViewHolder itemViewHolder = (ItemViewHolder) vh;
                itemViewHolder.earthAsteroid.setImageResource(earthasteroidimg);
                itemViewHolder.earthMoon.setImageResource(earthmoonimg);
                itemViewHolder.infoButton.setImageResource(infoButtonImg);
                itemViewHolder.comparison.setImageResource(comparisonImages[1]);
                itemViewHolder.comparison.setImageResource(comparisonImages[asteroids.get(i).compareToEarth()]);
                itemViewHolder.infoButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        openWebpage(v, asteroids.get(i1).getUrl());
                    }
                });
                itemViewHolder.asteroidName.setText(asteroids.get(i).getName());
                itemViewHolder.asteroidData.setText(asteroids.get(i).toString());
                itemViewHolder.comparisonScale.setText("\t\t" + String.valueOf(asteroids.get(i).scaleToEarth()) + "x");
                if (asteroids.get(i).getPotentiallyHazardous().equals("Y"))
                    itemViewHolder.hazard.setImageResource(hazard);
                else itemViewHolder.hazard.setImageResource(blank);
            }
            else if (vh instanceof LoadingViewHolder) {
                LoadingViewHolder loadingViewHolder = (LoadingViewHolder)vh;
                loadingViewHolder.progressBar.setIndeterminate(true);
            }
        }

        @Override
        public int getItemCount() {
            return asteroids == null ? 0 : asteroids.size();
        }

        public void setLoaded() {
            isLoading = false;
        }
    }

    public class AsteroidData extends AsyncTask<String, Integer, Void> {

        Asteroid init = new Asteroid();
        @Override
        protected void onPreExecute() {
            String[] loadingMessages = {"Searching for space rocks...",
                    "Seeking out distant worlds...",
                    "Looking for shooting stars...",
                    "Floating in deep space..."};
            Random rand = new Random();
            pd = ProgressDialog.show(getActivity(), "Loading", loadingMessages[rand.nextInt(4)]);
        }

        @Override
        protected Void doInBackground(String... urls) {

            String responseStr = null;

            try {
                for (String url : urls) {
                    DefaultHttpClient httpClient = new DefaultHttpClient();
                    HttpGet get = new HttpGet(url);
                    HttpResponse httpResponse = httpClient.execute(get);
                    HttpEntity httpEntity = httpResponse.getEntity();
                    responseStr = EntityUtils.toString(httpEntity);
                    }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                init.addData(bodyToLines(responseStr));
                initAsteroids();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (numRelevantPages == 0) entriesLastPage = asteroids.size();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            try {
                Collections.sort(asteroids, new CustomComparator(sortKeywords[0]));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            pd.dismiss();

            adapter.notifyDataSetChanged();
        }

        public ArrayList<String> bodyToLines(String body) {
            ArrayList<String> lines = new ArrayList<String>();

            String str = "";
            for (int i = 0; i < body.length(); i++) {
                if (body.charAt(i) == '\n') {
                    lines.add(str);
                    str = "";
                }
                else
                    str += body.charAt(i);
            }

            return lines;
        }

        private void initAsteroids() throws IOException {  //fills the exoplanets arraylist
            Asteroid a = new Asteroid();
            for (int i = asteroids.size(); i < a.getNumAsteroids(); i++) {
                asteroids.add(new Asteroid(i));
            }
        }
    }

    class CustomComparator implements Comparator<Asteroid> {

        String sortParamter = null;

        public CustomComparator(String s) {
            sortParamter = s;
        }

        @Override
        public int compare(Asteroid a1, Asteroid a2) {
            switch (sortParamter) {
                case "Alphabetical (A -> Z)":
                    return (a1.getName()).compareTo(a2.getName());
                case "Alphabetical (Z -> A)":
                    return -1*(a1.getName()).compareTo(a2.getName());
                case "Diameter (L -> G)":
                    if (a1.getDiameterVal() == 0 && a1.getDiameterVal() < a2.getDiameterVal())
                        return 1;
                    if (a2.getDiameterVal() == 0)
                        return -1;
                    return ((Double)a1.getDiameterVal()).compareTo(a2.getDiameterVal());
                case "Diameter (G -> L)":
                    return -1*((Double)a1.getDiameterVal()).compareTo(a2.getDiameterVal());
                case "Period (L -> G)":
                    if (a1.getPeriodVal() == 0 && a1.getPeriodVal() < a2.getPeriodVal())
                        return 1;
                    if (a2.getPeriodVal() == 0)
                        return -1;
                    return ((Double)a1.getPeriodVal()).compareTo(a2.getPeriodVal());
                case "Period (G -> L)":
                    return -1*((Double)a1.getPeriodVal()).compareTo(a2.getPeriodVal());
                case "Semi-major axis (L -> G)":
                    if (a1.getSMAVal() == 0 && a1.getSMAVal() < a2.getSMAVal())
                        return 1;
                    if (a2.getSMAVal() == 0)
                        return -1;
                    return ((Double)a1.getSMAVal()).compareTo(a2.getSMAVal());
                case "Semi-major axis (G -> L)":
                    return -1*((Double)a1.getSMAVal()).compareTo(a2.getSMAVal());
                case "Closest Sun approach (L -> G)":
                    if (a1.getPerihelionVal() == 0 && a1.getPerihelionVal() < a2.getPerihelionVal())
                        return 1;
                    if (a2.getPerihelionVal() == 0)
                        return -1;
                    return ((Double)a1.getPerihelionVal()).compareTo(a2.getPerihelionVal());
                case "Closest Sun approach (G -> L)":
                    return -1*((Double)a1.getPerihelionVal()).compareTo(a2.getPerihelionVal());
                case "Closest Earth approach (L -> G)":
                    if (a1.getEarthMOIDVal() == 0 && a1.getEarthMOIDVal() < a2.getEarthMOIDVal())
                        return 1;
                    if (a2.getEarthMOIDVal() == 0)
                        return -1;
                    return ((Double)a1.getEarthMOIDVal()).compareTo(a2.getEarthMOIDVal());
                case "Closest Earth approach (G -> L)":
                    return -1*((Double)a1.getEarthMOIDVal()).compareTo(a2.getEarthMOIDVal());
                case "Potentially hazardous":
                    return -1*((String)a1.getPotentiallyHazardous()).compareTo(a2.getPotentiallyHazardous());
                default:
                    return 0;
            }
        }
    }

    class Asteroid {
        private final int numPages = 33, entriesPerPage = 500;;
        private String name, diameter, period, semiMajorAxis, perihelion, earthMOID, potentiallyHazardous;
        private double diameterVal, periodVal, SMAVal, perihelionVal, earthMOIDVal;

        public Asteroid() {
        }   //for the init exoplanet

        public void addData(ArrayList<String> data) throws IOException {    //for the init exoplanet
            int startIndex = asteroidData.size();
            for (int i = 0; i < data.size(); i++)
                asteroidData.add(data.get(i));
            findAsteroidLines(asteroidData, startIndex);
        }

        public Asteroid(int num) {
            name = findNameFromLine(num, asteroidData);
            //the numbers added to the first parameter correspond to the column in the table on the website
            periodVal = findProperty(asteroidLines.get(num)+1, asteroidData).equals("&nbsp;") ? 0 : Double.parseDouble(findProperty(asteroidLines.get(num)+1, asteroidData));
            period = findProperty(asteroidLines.get(num)+1, asteroidData).equals("&nbsp;") ? "Unknown" : (findProperty(asteroidLines.get(num)+1, asteroidData) + " years");
            SMAVal = findProperty(asteroidLines.get(num)+2, asteroidData).equals("&nbsp;") ? 0 : Double.parseDouble(findProperty(asteroidLines.get(num)+2, asteroidData));
            semiMajorAxis = findProperty(asteroidLines.get(num)+2, asteroidData).equals("&nbsp;") ? "Unknown" : (findProperty(asteroidLines.get(num)+2, asteroidData) + " AU");
            perihelionVal = findProperty(asteroidLines.get(num)+3, asteroidData).equals("&nbsp;") ? 0 : Double.parseDouble(findProperty(asteroidLines.get(num)+3, asteroidData));
            perihelion = findProperty(asteroidLines.get(num)+3, asteroidData).equals("&nbsp;") ? "Unknown" : (findProperty(asteroidLines.get(num)+3, asteroidData) + " AU");
            diameterVal = findProperty(asteroidLines.get(num)+4, asteroidData).equals("&nbsp;") ? 0 : Double.parseDouble(findProperty(asteroidLines.get(num)+4, asteroidData));
            diameter = findProperty(asteroidLines.get(num)+4, asteroidData).equals("&nbsp;") ? "Unknown" : (findProperty(asteroidLines.get(num)+4, asteroidData)) + " km";
            earthMOIDVal = findProperty(asteroidLines.get(num)+5, asteroidData).equals("&nbsp;") ? 0 : Double.parseDouble(findProperty(asteroidLines.get(num)+5, asteroidData));
            earthMOID = findProperty(asteroidLines.get(num)+5, asteroidData).equals("&nbsp;") ? "Unknown" : (findProperty(asteroidLines.get(num)+5, asteroidData)) + " lunar distances";
            potentiallyHazardous = findProperty(asteroidLines.get(num)+6, asteroidData).equals("nbsp;") ? "Unknown" : (findProperty(asteroidLines.get(num)+6, asteroidData));
        }


        private void findAsteroidLines(ArrayList<String> asteroidData, int startIndex) {
            for (int i = startIndex; i < asteroidData.size(); i++) {
                if (isNameLine(i, asteroidData)) {
                    asteroidLines.add(i);
                }
            }
        }

        private boolean isNameLine(int lineNum, ArrayList<String> asteroidData) {
            if (asteroidData.get(lineNum).toLowerCase().contains("target=\"_blank\" title=\"details\"".toLowerCase()))
                return true;
            return false;
        }

        private String findNameFromLine(int num, ArrayList<String> asteroidData) {
            int lineNum = asteroidLines.get(num);
            String line = asteroidData.get(lineNum);
            if (isNameLine(lineNum, asteroidData)) {
                String str;
                int aLoc = line.indexOf("</a>");
                int bracketLoc = aLoc;
                while (line.charAt(bracketLoc) != '>') bracketLoc--;
                System.out.println(bracketLoc);
                System.out.println(aLoc);
                str = line.substring(bracketLoc+1, aLoc);
                aLoc = 0;
                while (str.charAt(aLoc) == ' ') aLoc++;
                return str.substring(aLoc);
            }
            return null;
        }

        public String getUrl() {
            String res = "", temp = "";
            res += "https://ssd.jpl.nasa.gov/sbdb.cgi?sstr=";
            temp = "";
            temp = name.substring(name.indexOf('(')+1, name.indexOf(')'));
            for (int i = 0; i < temp.length(); i++) {
                if (temp.charAt(i) != ' ')
                    res += temp.charAt(i);
                else res += "%20";
            }
            return res;
        }

        private String findProperty(int lineNum, ArrayList<String> asteroidData) {
            //finds </td>
            int tdLoc = asteroidData.get(lineNum).indexOf("</td>");
            //finds > before </td>
            int bracketLoc = tdLoc;
            while (!(asteroidData.get(lineNum).charAt(bracketLoc) == '>')) bracketLoc--;
            if (asteroidData.get(lineNum).substring(bracketLoc+1, tdLoc).equals("")) return null;
            return asteroidData.get(lineNum).substring(bracketLoc+1, tdLoc);
        }

        public int getNumAsteroids() {
            return asteroidLines.size();
        }

        public String getName() {
            return name;
        }

        public String getPotentiallyHazardous() {
            return potentiallyHazardous;
        }

        public double getDiameterVal() {
            return diameterVal;
        }

        public double getPeriodVal() {
            return periodVal;
        }

        public double getSMAVal() {
            return SMAVal;
        }

        public double getPerihelionVal() {
            return perihelionVal;
        }

        public double getEarthMOIDVal() {
            return earthMOIDVal;
        }

        public int compareToEarth() {
            if (!(earthMOID.equals("Unknown"))) {
                if (earthMOIDVal < 1) //if the planet is more massive than Earth; 317.8 is MJupiter/MEarth
                    return 1;
                return 2;
            }
            return 0;
        }

        public double scaleToEarth() {
            return earthMOIDVal;
        }

        public String toString() {
            String output = "";
            if (!diameter.equals("Unknown"))
                output += ("\nDiameter: " + diameter);
            if (!semiMajorAxis.equals("Unknown"))
                output += ("\nSemi-major axis: " + semiMajorAxis);
            if (!period.equals("Unknown"))
                output += ("\nPeriod: "+ period);
            if (!perihelion.equals("Unknown"))
                output += ("\nClosest Sun approach: " + perihelion);
            if (!earthMOID.equals("Unknown"))
                output += ("\nClosest Earth approach: " + Integer.toString((int)(earthMOIDVal*384400)) + " km");
            if (!potentiallyHazardous.equals("Unknown"))
                output += ("\nPotentially hazardous? " + potentiallyHazardous);
            output += "\n";

            return output;
        }
    }
}
