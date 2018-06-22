//Código creado por Aarón Angulo

package com.itson.potrobeacon;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by orlan on 08/03/2018.
 */

public class HttpRequestAsync extends AsyncTask<Void, Void, Void>
{
    private String url;

    public HttpRequestAsync(String url)
    {
        this.url = url;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

    }

    @Override
    protected Void doInBackground(Void... params)
    {
        try
        {
            URL url = new URL(this.url);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try
            {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                System.out.println(this.url);
            }
            finally
            {
                urlConnection.disconnect();
            }
        }
        catch(Exception ex)
        {

        }
        return null;
    }

    protected void onPostExecute(Boolean result)
    {

    }

    private String readStream(InputStream is) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            int i = is.read();
            while(i != -1) {
                bo.write(i);
                i = is.read();
            }
            return bo.toString();
        } catch (IOException e) {
            return "";
        }
    }
}
