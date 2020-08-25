package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.AsyncTaskLoader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    ArrayAdapter arrayAdapter;
    ArrayList<String> newsArrayList;
    ArrayList<String> content;
    ListView listView;
    SQLiteDatabase myDatabase;
    int n;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //DB

        myDatabase= this.openOrCreateDatabase("NewsContent",MODE_PRIVATE,null);
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS  newsTitleArticleContent(title VARCHAR, articleContent VARCHAR)");



        newsArrayList=new ArrayList<String>();
        content=new ArrayList<String>();
        listView=(ListView) findViewById(R.id.listView);
        arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,newsArrayList);
        listView.setAdapter(arrayAdapter);

        DownloadTask task=new DownloadTask();
        try{
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");


        }
        catch(Exception e)
        {
            e.printStackTrace();
        }


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Intent intent=new Intent(getApplicationContext(),SecondActivity.class);
                intent.putExtra("article_content",content.get(i));
                startActivity(intent);

            }
        });
        updateListView();

}


    public void updateListView()
    {
        Cursor c=myDatabase.rawQuery("SELECT * FROM newsTitleArticleContent",null);

        int titleIndex=c.getColumnIndex("title");
        int articleContentIndex=c.getColumnIndex("articleContent");

        if(c.moveToFirst())
        {
            newsArrayList.clear();
            content.clear();
            do{
                newsArrayList.add(c.getString(titleIndex));
                content.add(c.getString(articleContentIndex));
                Log.i("Title",c.getString(titleIndex));

            }while(c.moveToNext());
            arrayAdapter.notifyDataSetChanged();

        }
//        arrayAdapter.notifyDataSetChanged();
    }


    public class DownloadTask extends AsyncTask<String,Void,String>
    {

        @Override
        protected String doInBackground(String... urls) {

            String result="";
            URL url;
            HttpURLConnection urlConnection=null;

            try{
                url=new URL(urls[0]);
                urlConnection=(HttpURLConnection) url.openConnection();
                InputStream inputStream= urlConnection.getInputStream();
                InputStreamReader inputStreamReader=new InputStreamReader(inputStream);

                int data=inputStreamReader.read();
                while(data!=-1)
                {
                    char current=(char) data;
                    result+=current;
                    data=inputStreamReader.read();

                }
                Log.i("URLcontent",result);
                JSONArray jsonArray=new JSONArray(result);
                n=5;
                if(jsonArray.length()<5)
                {
                    n=jsonArray.length();
                }

                myDatabase.execSQL("DELETE FROM newsTitleArticleContent");

                for(int i=0;i<n;i++)
                {
                    String id=jsonArray.getString(i);
                    url=new URL("https://hacker-news.firebaseio.com/v0/item/"+id+".json?print=pretty");

                    String articleResult="";
                    urlConnection=(HttpURLConnection) url.openConnection();
                    inputStream= urlConnection.getInputStream();
                    inputStreamReader=new InputStreamReader(inputStream);

                    data=inputStreamReader.read();
                    while(data!=-1)
                    {
                        char current=(char) data;
                        articleResult+=current;
                        data=inputStreamReader.read();
                    }
                    Log.i("ArticleInfo",articleResult);
                    JSONObject jsonObject=new JSONObject(articleResult);
                    String title=jsonObject.getString("title");
                    url=new URL(jsonObject.getString("url"));
                    urlConnection=(HttpURLConnection) url.openConnection();
                    inputStream= urlConnection.getInputStream();
                    inputStreamReader=new InputStreamReader(inputStream);
                    data=inputStreamReader.read();

//                    String articleContent=jsonObject.getString("url");
                    String articleContent="";
                    while(data!=-1)
                    {
                        char current=(char) data;
                        articleContent+=current;
                        data=inputStreamReader.read();
                    }
                    String sql="INSERT INTO newsTitleArticleContent(title,articleContent) VALUES(?,?)";
                    SQLiteStatement statement=myDatabase.compileStatement(sql);
                    statement.bindString(1,title);
                    statement.bindString(2,articleContent);
                    statement.execute();
                }
                return result;
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }

}