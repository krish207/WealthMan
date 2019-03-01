package com.example.WealthMan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.WealthMan.APIInterface.*;
import com.example.WealthMan.detail.view.DetailActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


public class HomeActivity extends AppCompatActivity {
    private ArrayList<IconBean> mIconBeenList = new ArrayList<>();
    private ListView lv;
    private String latestprice;
    private IconAdapter sa;
    private DatabaseHelper DatabaseHelper;
    EditText mTextURI;
    Button mButtonOk;
//    Button mButtonTest;
    ArrayList<WatchListData> wl_data = new ArrayList<>();

    DatabaseHelper db;
    boolean dbsuccess = true;
    public static final String MY_PREFS_FILE = "wealthman_prefs";

//    final TextView mTextView = (TextView)findViewById(R.id.text);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        final int userid = intent.getIntExtra("UserId", 1);
        db = new DatabaseHelper(this);
        SharedPreferences preference = getSharedPreferences(MY_PREFS_FILE, MODE_PRIVATE);
        SharedPreferences.Editor editor = preference.edit();
        long now = System.currentTimeMillis();
        long updateDue = preference.getLong("updateDue", 0);
//        if (false) {  // Calc next time to update
        System.out.println("After Read Prefs: Now = " + now + " , Due = " + updateDue);
        if (updateDue < now) {  // Calc next time to update
            System.out.println("Symbol Update due");
            updateDue = now + TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS);
            if (updateSymbols()) {
                editor.putLong("updateDue", updateDue);   // Store new time to update
                editor.commit();
                System.out.println("At commit: Now = " + now + " , Due = " + updateDue);
//                System.out.println("Update was saved");
            } else
                Toast.makeText(HomeActivity.this, "Error updating Stock Symbols", Toast.LENGTH_LONG).show();
        } else
            System.out.println("No Symbol Update is due...");
        if (!preference.getBoolean("setupDone", false)) {
            long res = db.createWatchlist();  //special one time add
            if (res > 0) {
                editor.putBoolean("setupDone", true);
                editor.commit();
            } else
                Toast.makeText(HomeActivity.this, "Database Error creating Watch List", Toast.LENGTH_LONG).show();
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ativity_test_watchlist);
        lv = (ListView)findViewById(R.id.lv);
        //为listview添加adapter
        lv.setAdapter(new IconAdapter(this,mIconBeenList));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                IconBean stock = mIconBeenList.get(position);
                Intent intent = new Intent(HomeActivity.this,com.example.WealthMan.detail.view.DetailActivity.class);
                intent.putExtra("Symbol",stock.symbol);
                intent.putExtra("UserID",userid);
                startActivity(intent);
            }
        });

        mButtonOk = (Button) findViewById(R.id.button);
        mTextURI = (EditText) findViewById(R.id.url_to_fetch);

        final TextView mTextView = (TextView) findViewById(R.id.text);
       mTextURI.append("");
        final RequestQueue queue = Volley.newRequestQueue(this);

        String symbols = db.getWatchList().trim();
        String url = "https://api.iextrading.com/1.0/stock/market/batch?symbols=" + symbols + "&types=quote";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.equals("{}"))
                            Toast.makeText(HomeActivity.this, "Nothing Returned", Toast.LENGTH_LONG).show();
                        else {
                            GsonBuilder gsonBuilder = new GsonBuilder();
                            gsonBuilder.registerTypeAdapter(Batches.class, new CompanyListDeserializer());
                            Batches myData = gsonBuilder.create().fromJson(response, Batches.class);
                            Gson gsonPretty = new GsonBuilder().setPrettyPrinting().create();
                            String json = gsonPretty.toJson(myData);
                            System.out.println(json);
                            initData(myData);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        queue.add(stringRequest);


        mButtonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getApplicationContext(), DetailActivity.class);
                String symbol = mTextURI.getText().toString().trim();
                intent.putExtra("Symbol", symbol);
                intent.putExtra("UserID", userid);
                startActivity(intent);
            }
        });
    }



    public boolean updateSymbols() {
        final RequestQueue queue = Volley.newRequestQueue(this);
        System.out.println("Updating Symbols now");
        String symbolUrl = "https://api.iextrading.com/1.0/ref-data/symbols";
        final GsonBuilder gsonSymbols = new GsonBuilder();
        dbsuccess = false;
        StringRequest symbolRequest = new StringRequest(Request.Method.GET, symbolUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        stockSym[] mySyms = gsonSymbols.create().fromJson(response, stockSym[].class);
                        // Make two DB entries, one for symbol-symbol lookup
                        // Other for name-symbol lookup
                        System.out.println("Length = " + mySyms.length);
                        long val = 0;
                        for (int i = 0; i < mySyms.length; i++) {
                            val += db.addSymbol(mySyms[i].symbol, mySyms[i].symbol);
                            val += db.addSymbol(mySyms[i].name, mySyms[i].symbol);
                            //System.out.println(mySyms[i].name +"   ,  " +mySyms[i].symbol);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(HomeActivity.this, "That didn't work! Do you have an internet connection?", Toast.LENGTH_LONG).show();
            }
        });
        // Add the request to the RequestQueue.
        queue.add(symbolRequest);
        return true;
    }

    public void getData(String jsonData) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Batches.class, new CompanyListDeserializer());
        Batches watchList = gsonBuilder.create().fromJson(jsonData, Batches.class);
        for (int index = 0; index < watchList.batches.size(); index++) {
            System.out.println(index + "\t" + watchList.batches.get(index).quote.symbol + "\t" + watchList.batches.get(index).quote.latestPrice + "\t" + watchList.batches.get(index).quote.change);
            for (int i = 0; i < watchList.batches.size(); i++) {
                WatchListData temp = new WatchListData();
                temp.setChange(watchList.batches.get(i).quote.change);
                temp.setPrice(watchList.batches.get(i).quote.latestPrice);
                temp.setName(watchList.batches.get(i).quote.companyName);
                temp.setSymbol(watchList.batches.get(i).quote.symbol);
                wl_data.add(temp);
            }
        }
    }

    private void initData(Batches jsonData) {

        for (int i = 0 ; i < jsonData.batches.size(); i++) {
            IconBean symbol = new IconBean(
                    jsonData.batches.get(i).quote.symbol.trim(),
                    jsonData.batches.get(i).quote.companyName.trim(),
                    jsonData.batches.get(i).quote.latestPrice,
                    jsonData.batches.get(i).quote.change
                );
            mIconBeenList.add(symbol);
        }
    }
}
