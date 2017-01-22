package androidlauncher.alexaapp;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public String jsonStr = "";
    protected static ArrayList<String> names;
    protected static ArrayList<String> lines;
    protected static ArrayAdapter<String> itemsAdapter;
    private ListView lvItems;
    private int selected = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Intent voiceIntent = getIntent();
        String  speaker = voiceIntent.getStringExtra("speaker");
        if(speaker == null)
            speaker = "Alexa";
        else
            speaker= voiceIntent.getStringExtra("speaker");


        getSupportActionBar().setTitle("Alexa App (" + speaker + ")");

        lvItems = (ListView) findViewById(R.id.lvItems);
        names = new ArrayList<String>();
        lines = new ArrayList<String>();

        //set up adapter and listeners
        itemsAdapter = new ArrayAdapter<String>(this, R.layout.lv_item, names);
        lvItems.setAdapter(itemsAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Currently selected: " + names.get(selected), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        connect();
        setupListViewListener();
    }

    protected void connect() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            CallAPI call = new CallAPI();
            call.execute("listFiles", "");
        }

        else{
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Error")
                    .setMessage( "Need to be connected to internet!!")
                    .setPositiveButton(android.R.string.yes, null)
                    .show();
        }

    }

    // Attaches a long click listener and click listener to the listview
    private void setupListViewListener() {

        lvItems.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("TestAPI", "Pressed " + names.get(position));
                selected = position;
                CallAPI call = new CallAPI();
                call.execute("selectFile", names.get(position));
                Snackbar.make(view, "Currently selected: " + names.get(selected), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_voices) {
            Intent intent = new Intent(MainActivity.this, VoiceActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class CallAPI extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            final String link = "https://stormy-wildwood-84879.herokuapp.com";
            Uri builtUri = Uri.parse(link).buildUpon()
                    .appendPath(params[0])
                    .appendQueryParameter("fname", params[1])
                    .build();

            URL url = null;
            try {
                url = new URL(builtUri.toString());
                Log.d("STATE", builtUri.toString());
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                Log.d("STATE", "After connect");
                //Toast.makeText(getBaseContext(), "Succesfully connected!", Toast.LENGTH_LONG).show();

            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    Log.d("TestAPI", "Inputstream is null");
                    return null;
                }

                Log.d("TestAPI", "Inputstream not null!");
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                    Log.d("TestAPI", line);

                    Log.d("TestAPI", "size:  " + names.size());
                }
                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                jsonStr = buffer.toString();
                return jsonStr;

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.d("TestAPI", "Error closing  stream", e);
                    }
                }

                return null;
            }

        }

        protected void onPostExecute(String str) {
            if(names.size() == 0) {
                String[] arr = jsonStr.split(",");
                for (int i = 0; i < arr.length; i++) {
                    Log.d("TestAPI", arr[i]);
                    if(i == arr.length -1)
                        itemsAdapter.add(arr[i].replace("\n", ""));
                    else
                        itemsAdapter.add(arr[i]);
                }
            }
        }

    }
}