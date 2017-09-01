package com.joheenchakraborty.v3;


import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.util.Random;

/**
 * Created by Joheen Chakraborty on 8/10/2017.
 */

public class APODClass extends Fragment {

    private final String URL = "https://apod.nasa.gov/apod/astropix.html";
    private ProgressDialog pd;
    private String[] apodData;
    private String imgUrl, credit="", desc="", name="'";
    private ImageView img;
    private TextView creditTV, nameTV, descTV;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        APODData apodData = new APODData();
        apodData.execute(new String[]{URL});

        View view = inflater.inflate(R.layout.apod_layout, container, false);

        try {
            img = (ImageView)view.findViewById(R.id.apodImage);
            img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openWebpage(view, URL);
                }
            });
            creditTV = (TextView)view.findViewById(R.id.apodcred);
            creditTV.setGravity(Gravity.CENTER);
            nameTV = (TextView)view.findViewById(R.id.apodname);
            nameTV.setGravity(Gravity.CENTER);
            descTV = (TextView)view.findViewById(R.id.apoddesc);
            descTV.setGravity(Gravity.CENTER);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return view;
    }

    public void openWebpage(View view, String URL) {
        Uri uriUrl = Uri.parse(URL);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
    }

    class APODData extends AsyncTask<String, Integer, Void> {

        @Override
        protected void onPreExecute() {
            String[] loadingMessages = {"Searching for space rocks...",
                    "Exploring distant worlds...",
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
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            try {
                apodData = bodyToLines(responseStr);
                imgUrl = "https://apod.nasa.gov/apod/";
                for (int i = 0; i < apodData.length; i++) {
                    if (apodData[i].toLowerCase().contains("<IMG SRC=".toLowerCase())) {
                        imgUrl += (apodData[i].substring(10)).substring(0, (apodData[i].substring(10)).length() - 1);
                    }
                }
                Log.e("errormsg", imgUrl);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            getCredit();
            getDesc();

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            Picasso.with(getActivity()).setLoggingEnabled(true);
            try {
                Picasso.with(getActivity()).load(imgUrl).fit().centerInside().into(img);
                creditTV.setText(credit);
                nameTV.setText(name);
                descTV.setText(desc);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            pd.dismiss();
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

        public void getCredit() {
            int first=0, last=0;
            for (int i = 0; i < apodData.length; i++) {
                if (apodData[i].toLowerCase().contains("credit".toLowerCase())) {
                    first = i;
                    while (!apodData[i].toLowerCase().contains("</center>".toLowerCase())) {
                        i++;
                    }
                    last = i-1;
                }
            }
            try {
                int dank, meme;
                int nameline = first - 1;
                while (!apodData[nameline].contains("<"))
                    nameline--;
                dank = apodData[nameline].indexOf('>');
                meme = dank;
                while (apodData[nameline].charAt(meme) != '<')
                    meme++;
                name += apodData[nameline].substring(dank + 1, meme);
                name += "'";
                boolean isAdding = true;
                for (int i = first; i <= last; i++) {
                    for (int j = 0; j < apodData[i].length(); j++) {
                        if (apodData[i].charAt(j) == '<')
                            isAdding = false;
                        if (apodData[i].charAt(j) == '>') {
                            isAdding = true;
                            j++;
                        }
                        try {
                            if (isAdding && apodData[i].charAt(j) != '\n')
                                credit += apodData[i].charAt(j);
                        } catch (Exception e) {
                        }
                    }
                }
                credit += "\n";
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void getDesc() {
            int first=0, last=0;
            for (int i = 0; i < apodData.length; i++) {
                if (apodData[i].toLowerCase().contains("Explanation".toLowerCase())) {
                    first = i+1;
                    while (!apodData[i].toLowerCase().contains("<center>".toLowerCase())) {
                        i++;
                    }
                    last = i;
                }
            }
            boolean isAdding = true;
            for (int i = first; i < last; i++) {
                for (int j = 0; j < apodData[i].length(); j++) {
                    if (apodData[i].charAt(j) == '<') {
                        isAdding = false;
//                        desc += ' ';
                    }
                    if (isAdding) {
                        if (apodData[i].charAt(j) != '\n')
                            desc += apodData[i].charAt(j);
                        else desc += ' ';
                    }
                    if (apodData[i].charAt(j) == '>') {
                        isAdding = true;
                    }
                }
                desc += ' ';
            }
            desc += "\n\n\n\nNOTE: Not all images are viewable in-app for all devices.\n" +
                    "If you can't see today's APOD, try again tomorrow.\n\n\n";
        }
    }
}
