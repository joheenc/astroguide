package com.joheenchakraborty.v3;


/**
 * Created by Joheen Chakraborty on 8/6/2017.
 */

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.Spinner;
import android.widget.TextView;

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

public class ExoClass extends Fragment {

    ArrayList<Exoplanet> exoplanets;
    int exoimage = R.drawable.exoplanetimg, earthimage = R.drawable.earthimage, infoButtonImg = R.drawable.info;
    int[] comparisonImages = {R.drawable.question_mark, R.drawable.mass_greater, R.drawable.mass_less, R.drawable.radius_greater, R.drawable.radius_less};
    private final String URL = "https://en.wikipedia.org/wiki/List_of_exoplanets_(full)";
    private ProgressDialog pd;
    private static ArrayList<Integer> planetLines = new ArrayList<Integer>();
    private static String[] exoData;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private String[] sortKeywords = {"Alphabetical (A -> Z)", "Alphabetical (Z -> A)", "Mass (L -> G)", "Mass (G -> L)", "Radius (L -> G)", "Radius (G -> L)", "Period (L -> G)", "Period (G -> L)", "Semi-major axis (L -> G)", "Semi-major axis (G -> L)",
                                    "Distance (L -> G)", "Distance (G -> L)", "Disc. year (L -> G)", "Disc. year (G -> L)"};
    Spinner spinner;
    ArrayAdapter<String> spinnerAdapter;

    private final String DATAFILE = "exodatafile";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        readExoWebPage(URL);

        View view = inflater.inflate(R.layout.exo_recyclerview, container, false);

        recyclerView = (RecyclerView)view.findViewById(R.id.exoplanetRV);
        layoutManager = new LinearLayoutManager(view.getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ExoAdapter();
        recyclerView.setAdapter(adapter);

        spinner = (Spinner)view.findViewById(R.id.exosort);
        spinnerAdapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_spinner_item, sortKeywords);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    if (exoplanets != null) {
                        pd = ProgressDialog.show(getActivity(), "Loading", "Sorting exoplanets");
                        Collections.sort(exoplanets, new CustomComparator(sortKeywords[i]));
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

        return view;
    }

    public void readExoWebPage(String URL) {
        ExoplanetData task = new ExoplanetData();
        task.execute(new String[]{URL});
    }

    public void openWebpage(View view, String URL) {
        Uri uriUrl = Uri.parse(URL);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
    }

    class ExoAdapter extends RecyclerView.Adapter<ExoAdapter.ViewHolder> {

        class ViewHolder extends RecyclerView.ViewHolder {
            public int currentItem;
            public TextView exoName, exoData, comparisonScale;
            public ImageView exoImage, earthImage, infoButton, comparison;
            public ViewHolder(View view) {
                super(view);
                exoName = (TextView)view.findViewById(R.id.exoplanet_name);
                exoImage = (ImageView)view.findViewById(R.id.exoImage);
                earthImage = (ImageView)view.findViewById(R.id.earth);
                infoButton = (ImageView)view.findViewById(R.id.infoButton);
                comparison = (ImageView)view.findViewById(R.id.comparison);
                exoData = (TextView)view.findViewById(R.id.exoplanet_data);
                comparisonScale = (TextView)view.findViewById(R.id.comparisonScale);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup vg, int viewType) {
            View v = LayoutInflater.from(vg.getContext()).inflate(R.layout.exo_item, vg, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder vh, int i) {
            final int i1 = i;
            vh.exoImage.setImageResource(exoimage);
            vh.earthImage.setImageResource(earthimage);
            vh.infoButton.setImageResource(infoButtonImg);
            vh.comparison.setImageResource(comparisonImages[1]);
            vh.comparison.setImageResource(comparisonImages[exoplanets.get(i).compareToEarth()]);

            vh.infoButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openWebpage(v, exoplanets.get(i1).getUrl());
                }
            });
            vh.exoName.setText(exoplanets.get(i).getName());
            vh.exoData.setText(exoplanets.get(i).toString());
            String str = String.valueOf(exoplanets.get(i).scaleToEarth());
            if (str.length() > 4) str = str.substring(0,4);
            vh.comparisonScale.setText("\t\t" + str +"x");
        }

        @Override
        public int getItemCount() {
            return exoplanets == null ? 0 : exoplanets.size();
        }
    }

    class ExoplanetData extends AsyncTask<String, Integer, Void> {

        Exoplanet init = new Exoplanet();
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
                init.setData(bodyToLines(responseStr));
                exoplanets = initExoplanets();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            try {
                Collections.sort(exoplanets, new CustomComparator(sortKeywords[0]));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            pd.dismiss();

            adapter.notifyDataSetChanged();
        }

        public String[] bodyToLines(String body) {
            int lineCounter = 0;		//count the number of lines to initialize the array
            for (int i = 0; i < body.length(); i++) {
                if (body.charAt(i) == '\n')
                    lineCounter++;
            }

            String[] lines = new String[lineCounter];

            int strIndex = 0, lnIndex = 0;
            String str = "";
            while (lnIndex < lineCounter) {		//figure out a way to turn body into lines[]
                if (body.charAt(strIndex) == '\n') {
                    lines[lnIndex] = str;
                    lnIndex++;
                    str = "";
                }
                else
                    str += body.charAt(strIndex);
                strIndex++;
            }
            return lines;
        }

        private ArrayList<Exoplanet> initExoplanets() throws IOException {  //fills the exoplanets arraylist
            Exoplanet e = new Exoplanet();
            ArrayList<Exoplanet> exoplanets = new ArrayList<Exoplanet>();
            for (int i = 0; i < e.getNumPlanets(); i++) {
                exoplanets.add(new Exoplanet(i));
            }
            return exoplanets;
        }
    }

    class CustomComparator implements Comparator<Exoplanet> {

        String sortParameter = null;

        public CustomComparator(String s) {
            sortParameter = s;
        }

        @Override
        public int compare(Exoplanet e1, Exoplanet e2) {
            switch (sortParameter) {
                case "Alphabetical (A -> Z)":
                    return ((String)e1.getName()).compareTo(e2.getName());
                case "Alphabetical (Z -> A)":
                    return -1*((String)e1.getName()).compareTo(e2.getName());
                case "Mass (L -> G)":
                    if (e1.getMassVal() == 0 && e1.getMassVal() < e2.getMassVal())
                        return 1;
                    if (e2.getMassVal() == 0)
                        return -1;
                    return ((Double)e1.getMassVal()).compareTo(e2.getMassVal());
                case "Mass (G -> L)":
                    return -1*((Double)e1.getMassVal()).compareTo(e2.getMassVal());
                case "Radius (L -> G)":
                    if (e1.getRadVal() == 0 && e1.getRadVal() < e2.getRadVal())
                        return 1;
                    if (e2.getRadVal() == 0)
                        return -1;
                    return ((Double)e1.getRadVal()).compareTo(e2.getRadVal());
                case "Radius (G -> L)":
                    return -1*((Double)e1.getRadVal()).compareTo(e2.getRadVal());
                case "Period (L -> G)":
                    if (e1.getPeriodVal() == 0 && e1.getPeriodVal() < e2.getPeriodVal())
                        return 1;
                    if (e2.getPeriodVal() == 0)
                        return -1;
                    return ((Double)e1.getPeriodVal()).compareTo(e2.getPeriodVal());
                case "Period (G -> L)":
                    return -1*((Double)e1.getPeriodVal()).compareTo(e2.getPeriodVal());
                case "Semi-major axis (L -> G)":
                    if (e1.getSMAVal() == 0 && e1.getSMAVal() < e2.getSMAVal())
                        return 1;
                    if (e2.getSMAVal() == 0)
                        return -1;
                    return ((Double)e1.getSMAVal()).compareTo(e2.getSMAVal());
                case "Semi-major axis (G -> L)":
                    return -1*((Double)e1.getSMAVal()).compareTo(e2.getSMAVal());
                case "Distance (L -> G)":
                    if (e1.getDistanceVal() == 0 && e1.getDistanceVal() < e2.getDistanceVal())
                        return 1;
                    if (e2.getDistanceVal() == 0)
                        return -1;
                    return ((Double)e1.getDistanceVal()).compareTo(e2.getDistanceVal());
                case "Distance (G -> L)":
                    return -1*((Double)e1.getDistanceVal()).compareTo(e2.getDistanceVal());
                case "Disc. year (L -> G)":
                    return ((Double)e1.getDiscYearVal()).compareTo(e2.getDiscYearVal());
                case "Disc. year (G -> L)":
                    return -1*((Double)e1.getDiscYearVal()).compareTo(e2.getDiscYearVal());
                default:
                    return 0;
            }
        }
    }

    class Exoplanet {

        private String name, discMethod, mass, radius, period, semiMajorAxis, temp, distance, discYear, url;
        private double massVal, radVal, periodVal, SMAVal, discYearVal,  distanceVal;

        public Exoplanet() {}   //for the init exoplanet

        public Exoplanet(int num) throws IOException  {
            name = findNameFromLine(num, exoData);
            //the numbers added to the first parameter correspond to the column in the table on the website
            massVal = findProperty(planetLines.get(num)+1, exoData) == null ? 0 : Double.parseDouble(findProperty(planetLines.get(num)+1, exoData));
            mass = findProperty(planetLines.get(num)+1, exoData) == null ? "Unknown" : (findProperty(planetLines.get(num)+1, exoData) + " Jupiter masses");
            radVal = findProperty(planetLines.get(num)+2, exoData) == null ? 0 : Double.parseDouble(findProperty(planetLines.get(num)+2, exoData));
            radius = findProperty(planetLines.get(num)+2, exoData) == null ? "Unknown" : (findProperty(planetLines.get(num)+2, exoData) + " Jupiter radii");
            periodVal = findProperty(planetLines.get(num)+3, exoData) == null ? 0 : Double.parseDouble(findProperty(planetLines.get(num)+3, exoData));
            period = findProperty(planetLines.get(num)+3, exoData) == null ? "Unknown" : (findProperty(planetLines.get(num)+3, exoData) + " days");
            SMAVal = findProperty(planetLines.get(num)+4, exoData) == null ? 0 : Double.parseDouble(findProperty(planetLines.get(num)+4, exoData));
            semiMajorAxis = findProperty(planetLines.get(num)+4, exoData) == null ? "Unknown" : (findProperty(planetLines.get(num)+4, exoData) + " AU");
            temp = findProperty(planetLines.get(num)+5, exoData) == null ? "Unknown" : (findProperty(planetLines.get(num)+5, exoData) + " K");
            discMethod = findProperty(planetLines.get(num)+6, exoData);
            discYearVal = findProperty(planetLines.get(num)+7, exoData) == null ? 0 : Double.parseDouble(findProperty(planetLines.get(num)+7, exoData));
            discYear = findProperty(planetLines.get(num)+7, exoData) == null ? "Unknown" : findProperty(planetLines.get(num)+7, exoData);
            distanceVal = findProperty(planetLines.get(num)+8, exoData) == null ? 0 : Double.parseDouble(findProperty(planetLines.get(num)+8, exoData));
            distance = findProperty(planetLines.get(num)+8, exoData) == null ? "Unknown" : (findProperty(planetLines.get(num)+8, exoData) + " lightyears");
        }

        public void setData(String[] data) throws IOException {    //for the init exoplanet
            exoData = data;
            findPlanetLines(exoData);
        }

        private void findPlanetLines(String[] exoData) {
            for (int i = 0; i < exoData.length; i++) {
                if (isNameLine(i, exoData)) {
                    planetLines.add(i);
                }
            }
        }

        private boolean isNameLine(int lineNum, String[] exoData) {
            if (exoData[lineNum].length() > 12 && exoData[lineNum].substring(0, 12).equals("<td><a href="))
                return true;
            return false;
        }

        private String findNameFromLine(int num, String[] exoData) {
            int lineNum = planetLines.get(num);
            if (isNameLine(lineNum, exoData)) {
                //finds </a>
                int aLoc = exoData[lineNum].indexOf("</a>");
                //finds > before </a>
                int bracketLoc = aLoc;
                while (!(exoData[lineNum].charAt(bracketLoc) == '>')) bracketLoc--;
                return exoData[lineNum].substring(bracketLoc+1, aLoc);
            }
            return null;
        }

        private String findProperty(int lineNum, String[] exoData) {
            //finds </td>
            int tdLoc = exoData[lineNum].indexOf("</td>");
            //finds > before </td>
            int bracketLoc = tdLoc;
            while (!(exoData[lineNum].charAt(bracketLoc) == '>')) bracketLoc--;
            if (exoData[lineNum].substring(bracketLoc+1, tdLoc).equals("")) return null;
            return exoData[lineNum].substring(bracketLoc+1, tdLoc);
        }

        public String getExoDataLine(int line) {
            return exoData[line];
        }

        public String getName() {
            return name;
        }

        public int getNumPlanets() {
            return planetLines.size();
        }

        public double getMassVal() {
            return massVal;
        }

        public double getRadVal() {
            return radVal;
        }

        public double getPeriodVal() {
            return periodVal;
        }

        public double getSMAVal() {
            return SMAVal;
        }

        public double getDiscYearVal() {
            return discYearVal;
        }

        public double getDistanceVal() {
            return distanceVal;
        }

        public String getDiscMethod() {
            return discMethod;
        }

        public String getUrl() {
            String res = "";
            res += "https://exoplanetarchive.ipac.caltech.edu/cgi-bin/DisplayOverview/nph-DisplayOverview?objname=";
            String temp = "";
            for (int i = 0; i < name.length(); i++) {
                if (!(name.charAt(i) == '+'))
                    temp += name.charAt(i);
                else temp += "%2B";
            }
            temp = temp.replace(' ', '+');
            res += temp;
            res += "&type=CONFIRMED_PLANET";
            return res;
        }

        public int compareToEarth() {
            if (!(mass.equals("Unknown"))) {
                if (massVal * 317.8 > 1) //if the planet is more massive than Earth; 317.8 is MJupiter/MEarth
                    return 1;
                return 2;
            }
            if (!(radius.equals("Unknown"))) {
                if (radVal * 11.0 > 1) //if the planet is more massive than Earth; 11.0 is RJupiter/REarth
                    return 3;
                return 4;
            }

            return 0;
        }

        public double scaleToEarth() {
            if (this.compareToEarth() == 1 || this.compareToEarth() == 2)
                return (massVal*317.8);
            else if (this.compareToEarth() == 3 || this.compareToEarth() == 4)
                return ((radVal*11.0));

            return 0;
        }

        public String toString() {
            String output = "";
            try {
                if (!mass.equals("Unknown"))
                    output += ("\nMass: " + mass);
            } catch(Exception e) {e.printStackTrace();}
            try {
                if (!radius.equals("Unknown"))
                    output += ("\nRadius: " + radius);
            } catch(Exception e) {e.printStackTrace();}
            try {
                if (!period.equals("Unknown"))
                    output += ("\nPeriod: "+ period);
            } catch(Exception e) {e.printStackTrace();}
            try {
                if (!semiMajorAxis.equals("Unknown"))
                    output += ("\nSemi-major axis: " + semiMajorAxis);
            } catch(Exception e) {e.printStackTrace();}
            try {
                if (!temp.equals("Unknown"))
                    output += ("\nTemperature: " + temp);
            } catch(Exception e) {e.printStackTrace();}
            try {
                if (!discYear.equals("Unknown"))
                    output += ("\nDiscovery Year: " + discYear);
            } catch(Exception e) {e.printStackTrace();}
            try {
                if (!distance.equals("Unknown"))
                    output += ("\nDistance: " + distance);
            } catch(Exception e) {e.printStackTrace();}
            try {
                if (!discMethod.equals("Unknown"))
                    output += ( "\nDiscovery method: " + discMethod);
            } catch(Exception e) {e.printStackTrace();}
            output += "\n";

            return output;
        }
    }
}