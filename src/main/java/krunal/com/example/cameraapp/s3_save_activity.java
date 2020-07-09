package krunal.com.example.cameraapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import static android.widget.Toast.LENGTH_LONG;

public class s3_save_activity extends AppCompatActivity {

    public JSONObject jsonObject;
    String data = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        setContentView(R.layout.test);

        s3_save_activity.FetchAPIData process = new s3_save_activity.FetchAPIData();
        process.execute("https://7e5cfl9exg.execute-api.us-east-1.amazonaws.com/deploy1/getweather","dummy");

        JSONObject jo = null;
        String descvalue = null;
        String treatmentvalue = null;
        String urlValue = null;
        try {
            jo = new JSONObject(message.trim());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String diseasevalue = null;
        try {
            diseasevalue = (String) jo.get("Disease");
            descvalue = (String) jo.get("Description");
            treatmentvalue = (String) jo.get("Treatment");
            urlValue = (String) jo.get("URL");

            TextView textView1 = (TextView) findViewById(R.id.diseasevalue);
            textView1.setText(diseasevalue);
            TextView textView2 = (TextView) findViewById(R.id.descvalue);
            textView2.setText(descvalue);
            TextView textView3 = (TextView) findViewById(R.id.treatmentvalue);
            textView3.setText(treatmentvalue);

            TextView head1 = (TextView) findViewById(R.id.diseasekey);
            TextView head2 = (TextView) findViewById(R.id.desckey);
            TextView head3 = (TextView) findViewById(R.id.treatmentkey);
            new DownloadImageTask((ImageView) findViewById(R.id.imageView2))
                    .execute(urlValue);

            if (diseasevalue.equals("HEALTHY")) {
                head1.setText("Status");
                head2.setVisibility(View.GONE);
                head3.setVisibility(View.GONE);
                textView2.setVisibility(View.GONE);
                textView3.setVisibility(View.GONE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public class FetchAPIData extends AsyncTask<String,Void,Void> {

        String displayText = "";

        @Override
        protected Void doInBackground(String... params) {
            try {
                Log.i("fetch Json-Method", params[1]);
                URL url = new URL(params[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                httpURLConnection.setRequestProperty("Accept","application/json");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("key", params[1]);

                Log.i("fetch Json-Method", jsonParam.toString());
                try(OutputStream dos = httpURLConnection.getOutputStream()) {

                    byte[] input = jsonParam.toString().getBytes("utf-8");

                    dos.write(input, 0, input.length);

                }

                Log.i("STATUS", String.valueOf(httpURLConnection.getResponseCode()));
                Log.i("MSG" , httpURLConnection.getResponseMessage());

                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = "";
                while (line != null){
                    line = bufferedReader.readLine();
                    data = data + line;
                }

                Log.i("Data : " , data);
                jsonObject = new JSONObject(data.trim());
                Iterator<String> keys = jsonObject.keys();

                Log.i("Keys : ", keys.toString());

                for(int i = 0; i<jsonObject.names().length(); i++){
                    Log.v("Object Info", "key = " + jsonObject.names().getString(i) + " value = " + jsonObject.get(jsonObject.names().getString(i)));
                    displayText = displayText + jsonObject.names().getString(i) + " : " + jsonObject.get(jsonObject.names().getString(i)) +"\n";
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            TextView textView4 = (TextView) findViewById(R.id.weatherValue);
            textView4.setText(displayText);
        }
    }
}

