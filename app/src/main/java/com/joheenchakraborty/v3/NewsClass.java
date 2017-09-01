package com.joheenchakraborty.v3;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.net.Uri;

import com.squareup.picasso.Picasso;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Joheen Chakraborty on 8/10/2017.
 */

public class NewsClass extends Fragment {

    private ArrayList<NewsArticle> newsArticles;
    private final String URL = "https://twitter.com/astroguide_news";
    private ProgressDialog pd;
    private ArrayList<Integer> newsLines = new ArrayList<Integer>(), imageLines = new ArrayList<Integer>(), dateLines = new ArrayList<Integer>();
    private String[] newsData;

    private RecyclerView recyclerView;
    private NewsClass.NewsAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private final String DATAFILE = "newsdatafile";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        readNewsWebPage(URL);

        View view = inflater.inflate(R.layout.news_recyclerview, container, false);

        recyclerView = (RecyclerView)view.findViewById(R.id.newsRV);
        layoutManager = new LinearLayoutManager(view.getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new NewsAdapter();
        recyclerView.setAdapter(adapter);

        return view;
    }

    public void readNewsWebPage(String URL) {
        NewsData task = new NewsData();
        task.execute(new String[]{URL});
    }

    public void openWebpage(View view, String URL) {
        Uri uriUrl = Uri.parse(URL);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
    }

    class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ViewHolder> {

        NewsArticle na = new NewsArticle();

        class ViewHolder extends RecyclerView.ViewHolder {
            public TextView articleName, articleDesc, articleDate;
            public ImageView articleImage;
            public View v;
            public ViewHolder(View view) {
                super(view);
                v = view;
                articleName = (TextView)view.findViewById(R.id.articletitle);
                articleDesc = (TextView)view.findViewById(R.id.articledesc);
                articleDate = (TextView)view.findViewById(R.id.articledate);
                articleImage = (ImageView)view.findViewById(R.id.articleimage);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup vg, int viewType) {
            View v = LayoutInflater.from(vg.getContext()).inflate(R.layout.news_item, vg, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(final ViewHolder vh, int i) {
            final int i1 = i;

            final String url = na.getImageUrlFromLine(newsData[imageLines.get(i)]);
            try {
                Picasso.with(getContext()).load(url).into(vh.articleImage);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            vh.v.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    openWebpage(view, newsArticles.get(i1).getUrl(i1));
                }
            });

            vh.articleName.setText(newsArticles.get(i).getName());
            vh.articleDesc.setText(newsArticles.get(i).getDescription());
            vh.articleDate.setText(newsArticles.get(i).getDate());
        }

        @Override
        public int getItemCount() {
            return newsArticles == null ? 0 : newsArticles.size();
        }
    }

    class NewsData extends AsyncTask<String, Integer, Void> {

        NewsArticle init = new NewsClass.NewsArticle();
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
                init.setData(bodyToLines(responseStr));
                newsArticles = initNews();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
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

        private ArrayList<NewsArticle> initNews() throws IOException {  //fills the news arraylist
            NewsArticle n = new NewsArticle();
            ArrayList<NewsArticle> newsArticles = new ArrayList<NewsArticle>();
            for (int i = 0; i < n.getNumArticles(); i++) {
                newsArticles.add(new NewsArticle(i));
            }
            return newsArticles;
        }
    }

    class NewsArticle {

        private String title, description, date;

        public NewsArticle() {
        }

        public NewsArticle(int i) {
            title = getNameFromLine(newsData[newsLines.get(i)]);
            description = getDescFromLine(newsData[newsLines.get(i)]);
            date = getDateFromLine(newsData[dateLines.get(i)]);
        }

        public void setData(String[] data) {
            newsData = data;
            findNewsLines(newsData);
            findImageLines(newsData);
            findDateLines(newsData);
        }

        private void findNewsLines(String[] newsData) {
            for (int i = 0; i < newsData.length; i++) {
                if (isNameLine(newsData[i])) {
                    newsLines.add(i);
                }
            }
        }

        private boolean isNameLine(String line) {
            if (line.toLowerCase().contains("  <p class=\"TweetTextSize TweetTextSize--normal js-tweet-text tweet-text\" lang=\"en\" data-aria-label-part=\"0\">".toLowerCase()))
                return true;
            return false;
        }

        public String getNameFromLine(String line) {
            int closeBracketIndex;
            closeBracketIndex = line.indexOf('>');
            int firstQuoteIndex=closeBracketIndex, lastQuoteIndex=closeBracketIndex;
            try {
                while (line.charAt(firstQuoteIndex) != ';')
                firstQuoteIndex++;
                lastQuoteIndex = firstQuoteIndex + 1;
                while (line.charAt(lastQuoteIndex) != '&') lastQuoteIndex++;
                return line.substring(firstQuoteIndex+1, lastQuoteIndex);
            }
            catch (Exception e) {
                return "url decode failed";
            }
        }

        public String getDescFromLine(String line) {
            int firstStarIndex=0, lastStarIndex=0, i;
            String res = "\n";
            firstStarIndex = line.indexOf('*');
            lastStarIndex = firstStarIndex + 1;
            while (line.charAt(lastStarIndex) != '*') lastStarIndex++;
            res += line.substring(firstStarIndex+1, lastStarIndex);
            res += "...";
            return res;
        }

        private void findImageLines(String[] newsData) {
            for (int i = 0; i < newsData.length; i++) {
                if (isImageLine(newsData[i])) {
                    imageLines.add(i);
                }
            }
        }

        public boolean isImageLine(String line) {
            if (line.toLowerCase().contains("  <img data-aria-label-part src=\"".toLowerCase()))
                return true;
            return false;
        }

        public String getImageUrlFromLine(String line) {
            int firstQuoteIndex, lastQuoteIndex=0;
            firstQuoteIndex = line.indexOf('\"');
            lastQuoteIndex = firstQuoteIndex + 1;
            while (line.charAt(lastQuoteIndex) != '\"') lastQuoteIndex++;
            return line.substring(firstQuoteIndex+1, lastQuoteIndex);
        }

        private void findDateLines(String[] newsData) {
            for (int i = 0; i < newsData.length; i++) {
                if (isDateLine(newsData[i])) {
                    dateLines.add(i);
                }
            }
        }

        private boolean isDateLine(String line) {
            if (line.toLowerCase().contains("class=\"tweet-timestamp js-permalink js-nav js-tooltip\" title=\"".toLowerCase()))
                return true;
            return false;
        }

        public String getDateFromLine(String line) {
            int startIndex = line.indexOf("class=\"tweet-timestamp js-permalink js-nav js-tooltip\" title=\"") + "class=\"tweet-timestamp js-permalink js-nav js-tooltip\" title=\"".length();
            while (line.charAt(startIndex) != '-') startIndex++;
            int endIndex = startIndex + 3;
            while (line.charAt(endIndex) != '\"') endIndex++;
            return line.substring(startIndex+1, endIndex);
        }

        public String getUrl(int i) {
            String line = newsData[newsLines.get(i)];
            String searchFor = "class=\"twitter-timeline-link\" target=\"_blank\" title=\"";
            int startIndex = line.indexOf(searchFor) + searchFor.length();
            int endIndex = startIndex + 1;
            while (line.charAt(endIndex) != '\"')
                endIndex++;
            return line.substring(startIndex, endIndex);
        }

        public int getNumArticles() {
            return newsLines.size();
        }

        public String getName() {
            return title;
        }

        public String getDate() {
            return date;
        }

        public String getDescription() {
            return description;
        }
    }
}