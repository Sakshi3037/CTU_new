package com.example.admin.ctu;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import java.io.IOException;

public class Home extends Activity{
    TextView source, dest;
    SharedPreferences sp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ActionBar mActionBar = getActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        getActionBar().setIcon(
                new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        LayoutInflater mInflater = LayoutInflater.from(this);
        View mCustomView = mInflater.inflate(R.layout.action_home, null);
        TextView mTitleTextView = (TextView) mCustomView.findViewById(R.id.title_text);
        mTitleTextView.setText("CTU");
        mTitleTextView.setTextColor(Color.WHITE);
        mActionBar.setCustomView(mCustomView);
        mActionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#1753a4")));
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(true);
        source = (TextView) findViewById(R.id.source);
        dest = (TextView) findViewById(R.id.dest);
        if(!isNetworkAvailable())
        {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Alert");
            alertDialog.setMessage("Internet is not working, not able to check for updates");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int value = sp.getInt("Version", 1);
        if(value == 1) {
            {
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt("Version", 1);
                editor.commit();
            }
        }
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("http://nodejs-wirelessnetwork.rhcloud.com/isChanged");
        HttpResponse httpResponse;
        try {
            httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            String responseString = EntityUtils.toString(httpEntity, "UTF-8");
            updateDatabase(responseString);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }
    public void accident(View v)
    {
        Intent intent = new Intent(getBaseContext(), emergency.class);
        startActivity(intent);
    }
    public void showMap(View view) {
        Intent intent = new Intent(getBaseContext(), map.class);
        startActivity(intent);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private class updateDatabase extends AsyncTask<Integer, Void, Integer> {
        ProgressDialog dialog;
        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(Home.this);
            dialog.setTitle("Updating...");
            dialog.setMessage("Please wait...");
            dialog.setIndeterminate(true);
            dialog.show();
        }
        @Override
        protected Integer doInBackground(Integer... params) {
            database d = new database(getBaseContext());
            d.open();
            d.updateDb();
            int ver = params[0];
            extDatabase ed = new extDatabase(d);
            try {
                ed.updateDatabase();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("Version", ver);
            editor.commit();
            return null;
        }
        protected void onPostExecute(Integer result) {
            dialog.dismiss();
        }
    }
    private void updateDatabase(String serverVer) throws JSONException {
        database d = new database(getBaseContext());
        d.open();
        sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int v = sp.getInt("Version", 0);
        int ver = Integer.parseInt(String.valueOf(serverVer.charAt(0)));
        if(ver != v) {
           new updateDatabase().execute(ver);
        }
    }

    public void search(View view)
    {
        String s = source.getText().toString();
        String d = dest.getText().toString();
        if(!(s.isEmpty()) && !(d.isEmpty()))
        {
            Intent intent = new Intent(getBaseContext(), Details.class);
            intent.putExtra("search", "route");
            intent.putExtra("valSource", s);
            intent.putExtra("valDest", d);
            startActivity(intent);
        }
        else
        {
            Toast.makeText(getBaseContext(), "Mention both source and destination first",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void dest(View view)
    {
        String so = source.getText().toString();
        Intent intent = new Intent(getBaseContext(), Details.class);
        intent.putExtra("search", "dest");
        intent.putExtra("valSource", so);
        startActivityForResult(intent, 1);
    }

    public void source(View view) {
        String de = dest.getText().toString();
        Intent intent = new Intent(getBaseContext(), Details.class);
        intent.putExtra("search", "source");
        intent.putExtra("valDest", de);
        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case 0 : {
                if (resultCode == Activity.RESULT_OK) {
                    source.setText(data.getStringExtra("result"));
                }
                break;
            }
            case 1: {
                 if (resultCode == Activity.RESULT_OK) {
                     dest.setText(data.getStringExtra("result"));
                 }
            }
        }
    }

    public void swap(View view)
    {
        String temp = source.getText().toString();
        source.setText(dest.getText().toString());
        dest.setText(temp);
    }
}
