package uk.co.jaymehta.csafeedback;

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by Jay on 04/07/2015.
 */
public class PostHelper {

    public PostHelper() {}

    public static String postRequest(String sUrl, ContentValues values) throws IOException {
        URL url = new URL(sUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setDoInput(true);
        conn.setDoOutput(true);

        String towrite = getQuery(values);
        conn.setFixedLengthStreamingMode(towrite.length());

        OutputStream os = conn.getOutputStream();

        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, "UTF-8"));
        writer.write(towrite);
        writer.flush();
        writer.close();
        os.close();

        InputStream in = new BufferedInputStream(conn.getInputStream());
        java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";

        conn.disconnect();

        return result;
    }

    private static String getQuery(ContentValues vals) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        Set<Map.Entry<String, Object>> s=vals.valueSet();
        Iterator itr = s.iterator();

        while(itr.hasNext())
        {
            if (first)
                first = false;
            else
                result.append("&");

            Map.Entry me = (Map.Entry)itr.next();
            result.append(URLEncoder.encode(me.getKey().toString(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(me.getValue().toString(), "UTF-8"));
        }

        return result.toString();
    }
}
