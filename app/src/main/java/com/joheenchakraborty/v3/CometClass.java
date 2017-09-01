package com.joheenchakraborty.v3;


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

/**
 * Created by Joheen Chakraborty on 8/6/2017.
 */

public class CometClass extends Fragment {

    ArrayList<Comet> comets;

    private final String URL = "https://ssd.jpl.nasa.gov/sbdb_query.cgi?obj_group=neo;obj_kind=com;obj_numbered=all;com_orbit_class=HYP;com_orbit_class=ETc;com_orbit_class=PAR;com_orbit_class=CTc;com_orbit_class=JFC;com_orbit_class=JFc;com_orbit_class=HTC;com_orbit_class=COM;OBJ_field=0;ORB_field=0;table_format=HTML;max_rows=500;format_option=comp;query=Generate%20Table;c_fields=AcBiBqBsApBu;c_sort=;.cgifields=format_option;.cgifields=obj_kind;.cgifields=obj_group;.cgifields=obj_numbered;.cgifields=ast_orbit_class;.cgifields=table_format;.cgifields=com_orbit_class&page=1";
    private ProgressDialog pd;
    private static ArrayList<Integer> cometLines = new ArrayList<Integer>();
    private static String[] cometData;

    int cometSunImage = R.drawable.comet_sun, earthSunImage = R.drawable.earth_sun, infoButtonImg = R.drawable.info;
    int[] comparisonImages = {R.drawable.question_mark, R.drawable.greater, R.drawable.less};

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private String[] sortKeywords = {"Alphabetical (A -> Z)", "Alphabetical (Z -> A)", "Closest Sun approach (L -> G)", "Closest Sun approach (G -> L)", "Period (L -> G)", "Period (G -> L)",
                                    "Diameter (L -> G)", "Diameter (G -> L)", "Closest Earth approach (L -> G)", "Closest Earth approach (G -> L)"};
    Spinner spinner;
    ArrayAdapter<String> spinnerAdapter;

    private final String DATAFILE = "cometdatafile";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        readCometWebPage(URL);

        View view = inflater.inflate(R.layout.comet_recyclerview, container, false);

        recyclerView = (RecyclerView)view.findViewById(R.id.cometRV);
        layoutManager = new LinearLayoutManager(view.getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new CometAdapter();
        recyclerView.setAdapter(adapter);

        spinner = (Spinner)view.findViewById(R.id.cometsort);
        spinnerAdapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_spinner_item, sortKeywords);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    if (comets != null) {
                        pd = ProgressDialog.show(getActivity(), "Loading", "Sorting comets");
                        Collections.sort(comets, new CustomComparator(sortKeywords[i]));
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

    public void readCometWebPage(String URL) {
        CometData task = new CometData();
        task.execute(new String[]{URL});
    }

    public void openWebpage(View view, String URL) {
        Uri uriUrl = Uri.parse(URL);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
    }

    class CometAdapter extends RecyclerView.Adapter<CometAdapter.ViewHolder> {

        class ViewHolder extends RecyclerView.ViewHolder {
            public TextView cometName, cometData, comparisonScale;
            public ImageView earthSun, cometSun, infoButton, comparison;
            public ViewHolder(View view) {
                super(view);
                cometName = (TextView)view.findViewById(R.id.cometname);
                cometSun = (ImageView)view.findViewById(R.id.cometsunimage);
                earthSun = (ImageView)view.findViewById(R.id.earthsunimage);
                infoButton = (ImageView)view.findViewById(R.id.cometinfobutton);
                comparison = (ImageView)view.findViewById(R.id.cometcomparison);
                cometData = (TextView)view.findViewById(R.id.cometdata);
                comparisonScale = (TextView)view.findViewById(R.id.cometcomparisonscale);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup vg, int viewType) {
            View v = LayoutInflater.from(vg.getContext()).inflate(R.layout.comet_item, vg, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder vh, int i) {
            final int i1 = i;
            vh.cometName.setText(comets.get(i).getName());
            vh.cometData.setText(comets.get(i).toString());
            vh.cometSun.setImageResource(cometSunImage);
            vh.earthSun.setImageResource(earthSunImage);
            vh.comparison.setImageResource(comparisonImages[comets.get(i).compareToEarth()]);
            vh.comparisonScale.setText(Double.toString(comets.get(i).getPerihelionVal()) + "x");
            vh.infoButton.setImageResource(infoButtonImg);

            vh.infoButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openWebpage(v, comets.get(i1).getURL());
                }
            });
        }

        @Override
        public int getItemCount() {
            return comets == null ? 0 : comets.size();
        }
    }

    class CometData extends AsyncTask<String, Integer, Void> {

        Comet init = new Comet();
        @Override
        protected  void onPreExecute() {
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
                comets = initComets();
            }
            catch (Exception e ) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            try {
                Collections.sort(comets, new CustomComparator(sortKeywords[0]));
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

        private ArrayList<Comet> initComets() throws IOException {
            Comet c = new Comet();
            ArrayList<Comet> comets = new ArrayList<Comet>();
            for (int i = 0; i < c.getNumComets(); i++) {
                comets.add(new Comet(i));
            }
            return comets;
        }
    }

    class CustomComparator implements Comparator<Comet> {

        String sortParameter = null;

        public CustomComparator(String s) {
            sortParameter = s;
        }

        @Override
        public int compare(Comet c1, Comet c2) {
            switch (sortParameter) {
                case "Alphabetical (A -> Z)":
                    return ((String)c1.getName()).compareTo(c2.getName());
                case "Alphabetical (Z -> A)":
                    return -1*((String)c1.getName()).compareTo(c2.getName());
                case "Closest Sun approach (L -> G)":
                    if (c1.getPerihelionVal() == 0 && c1.getPerihelionVal() < c2.getPerihelionVal())
                        return 1;
                    if (c2.getPerihelionVal() == 0)
                        return -1;
                    return ((Double)c1.getPerihelionVal()).compareTo(c2.getPerihelionVal());
                case "Closest Sun approach (G -> L)":
                    return -1*((Double)c1.getPerihelionVal()).compareTo(c2.getPerihelionVal());
                case "Period (L -> G)":
                    if (c1.getPeriodVal() == 0 && c1.getPeriodVal() < c2.getPeriodVal())
                        return 1;
                    if (c2.getPeriodVal() == 0)
                        return -1;
                    return ((Double)c1.getPeriodVal()).compareTo(c2.getPeriodVal());
                case "Period (G -> L)":
                    return -1*((Double)c1.getPeriodVal()).compareTo(c2.getPeriodVal());
                case "Diameter (L -> G)":
                    if (c1.getDiameterVal() == 0 && c1.getDiameterVal() < c2.getDiameterVal())
                        return 1;
                    if (c2.getDiameterVal() == 0)
                        return -1;
                    return ((Double)c1.getDiameterVal()).compareTo(c2.getDiameterVal());
                case "Diameter (G -> L)":
                    return -1*((Double)c1.getDiameterVal()).compareTo(c2.getDiameterVal());
                case "Closest Earth approach (L -> G)":
                    if (c1.getEarthMOIDVal() == 0 && c1.getEarthMOIDVal() < c2.getEarthMOIDVal())
                        return 1;
                    if (c2.getEarthMOIDVal() == 0)
                        return -1;
                    return ((Double)c1.getEarthMOIDVal()).compareTo(c2.getEarthMOIDVal());
                case "Closest Earth approach (G -> L)":
                    return -1*((Double)c1.getEarthMOIDVal()).compareTo(c2.getEarthMOIDVal());
                default:
                    return 0;
            }
        }
    }

    class Comet {

        private String name, perihelion, timePerihelion, period, diameter, earthMOID;
        private double perihelionVal, periodVal, diameterVal, earthMOIDVal;

        public Comet() {}

        public Comet(int num) throws IOException {
            name = findNameFromLine(num, cometData);

            perihelionVal = findProperty(cometLines.get(num)+1, cometData).equals("&nbsp;") ? 0 : Double.parseDouble(findProperty(cometLines.get(num)+1, cometData));
            perihelion = findProperty(cometLines.get(num)+1, cometData).equals("&nbsp;") ? "Unknown" : (findProperty(cometLines.get(num)+1, cometData) + " AU");
            timePerihelion = findProperty(cometLines.get(num)+2, cometData).equals("&nbsp;") ? "Unknown" : (findProperty(cometLines.get(num)+2, cometData));
            periodVal = findProperty(cometLines.get(num)+3, cometData).equals("&nbsp;") ? 0 : Double.parseDouble(findProperty(cometLines.get(num)+3, cometData));
            period = findProperty(cometLines.get(num)+3, cometData).equals("&nbsp;") ? "Unknown" : (findProperty(cometLines.get(num)+3, cometData) + " years");
            diameterVal = findProperty(cometLines.get(num)+4, cometData).equals("&nbsp;") ? 0 : Double.parseDouble(findProperty(cometLines.get(num)+4, cometData));
            diameter = findProperty(cometLines.get(num)+4, cometData).equals("&nbsp;") ? "Unknown" : (findProperty(cometLines.get(num)+4, cometData) + " km");
            earthMOIDVal = findProperty(cometLines.get(num)+5, cometData).equals("&nbsp;") ? 0 : Double.parseDouble(findProperty(cometLines.get(num)+5, cometData));
            earthMOID = findProperty(cometLines.get(num)+5, cometData).equals("&nbsp;") ? "Unknown" : (findProperty(cometLines.get(num)+5, cometData) + " lunar distances");
        }

        public void setData(String[] data) throws IOException {
            cometData = data;
            findCometLines(cometData);
        }

        private void findCometLines(String[] cometData) {
            for (int i = 0; i < cometData.length; i++) {
                if (isNameLine(i, cometData)) {
                    cometLines.add(i);
                }
            }
        }

        private boolean isNameLine(int lineNum, String[] asteroidData) {
            if (asteroidData[lineNum].toLowerCase().contains("target=\"_blank\" title=\"details\"".toLowerCase()))
                return true;
            return false;
        }

        private String findNameFromLine(int num, String[] cometData) {
            int lineNum = cometLines.get(num);
            String line = cometData[lineNum];
            if (isNameLine(lineNum, cometData)) {
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

        private String findProperty(int lineNum, String[] cometData) {
            //finds </td>
            int tdLoc = cometData[lineNum].indexOf("</td>");
            //finds > before </td>
            int bracketLoc = tdLoc;
            while (!(cometData[lineNum].charAt(bracketLoc) == '>')) bracketLoc--;
            if (cometData[lineNum].substring(bracketLoc+1, tdLoc).equals("")) return null;
            return cometData[lineNum].substring(bracketLoc+1, tdLoc);
        }

        public int compareToEarth() {
            if (!perihelion.equals("Unknown")) {
                if (perihelionVal > 1.0)
                    return 1;
                else return 2;
            }
            return 0;
        }

        public String getURL() {
            String res = "https://ssd.jpl.nasa.gov/sbdb.cgi?sstr=";
            int i = 0;
            if (name.toLowerCase().contains("73P".toLowerCase())) {
                res += "73P-";
                String str = "";
                i = name.length()-1;
                while (name.charAt(i) != '-')
                    i--;
                str = name.substring(i+1, name.length());
                if (str.equals("Wachmann 3"))
                    return "https://ssd.jpl.nasa.gov/sbdb.cgi?ID=c00073_0";
                else {
                    res += str;
                    return res;
                }
            }
            else {
                try {
                    while (name.charAt(i) != '/')
                        i++;
                    int meme = Integer.parseInt(name.substring(0, i-1));
                    res += name.substring(0, i);
                    return res;
                }
                catch (Exception e) {
                    i = name.indexOf('/');
                    int end = name.indexOf('(') - 1;
                    if (end == -2) end = name.length();
                    String str = name.substring(i+1, end);
                    String dank = "";
                    for (int x = 0; x < str.length(); x++) {
                        if (str.charAt(x) != ' ') dank += str.charAt(x);
                        else dank += "%20";
                    }
                    res += dank;
                    return res;
                }
            }
        }

        public String getName() {
            return name;
        }

        public double getPerihelionVal() {
            return perihelionVal;
        }

        public double getPeriodVal() {
            return periodVal;
        }

        public double getDiameterVal() {
            return diameterVal;
        }

        public double getEarthMOIDVal() {
            return earthMOIDVal;
        }

        public int getNumComets() {
            return cometLines.size();
        }

        public String toString() {
            String output = "";
            if (!perihelion.equals("Unknown"))
                output += ("\nClosest Sun approach: " + perihelion);
            if (!timePerihelion.equals("Unknown"))
                output += ("\nMost recent closest Sun approach: " + timePerihelion);
            if (!period.equals("Unknown"))
                output += ("\nPeriod: "+ period);
            if (!diameter.equals("Unknown"))
                output += ("\nDiameter: " + diameter);
            if (!earthMOID.equals("Unknown"))
                output += ("\nClosest Earth approach: " + earthMOID);

            return output;
        }
    }
}