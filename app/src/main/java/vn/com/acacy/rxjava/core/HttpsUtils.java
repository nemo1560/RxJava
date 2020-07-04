package vn.com.acacy.rxjava.core;

import android.os.Build;
import android.os.StrictMode;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class HttpsUtils {
    private static final String TAG = HttpsUtils.class.getSimpleName();
    private static final Charset charset = Charset.forName("UTF-8");
    private static final int CONNECTION_TIMEOUT = 20 * 1000;
    private static final long MCC_TIMEOUT = 5 * 60 * 1000; /* 5 seconds */
    private static int SOCKET_TIMEOUT = 5 * 60 * 1000;

    public static final String LOGIN = "https://v2_tuyendung.acacy.com.vn/users/authenticate"; //temp value
    public static final String UPLOAD = "https://v2_tuyendung.acacy.com.vn/api/SaveFile"; //temp value

    public static TrustManager[] getWrappedTrustManagers(TrustManager[] trustManagers) {
        final X509TrustManager originalTrustManager = (X509TrustManager) trustManagers[0];
        return new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return originalTrustManager.getAcceptedIssuers();
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        try {
                            originalTrustManager.checkClientTrusted(certs, authType);
                        } catch (CertificateException ignored) {
                        }
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        try {
                            originalTrustManager.checkServerTrusted(certs, authType);
                        } catch (CertificateException ignored) {
                        }
                    }
                }
        };
    }

    public static SSLSocketFactory getSSLSocketFactory() {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = null;
            Certificate ca = cf.generateCertificate(caInput);
            caInput.close();

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, getWrappedTrustManagers(tmf.getTrustManagers()), null);

            return sslContext.getSocketFactory();
        } catch (Exception e) {
            return HttpsURLConnection.getDefaultSSLSocketFactory();
        }
    }

    public static HttpClient getNewHttpClient() {

        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            org.apache.http.conn.ssl.SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);

        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }

    public static String post(String Uri, JSONObject object) throws IOException {
        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        String str = null;
        BufferedReader reader = null;
        HttpsURLConnection conn = null;
        try {
            URL url = new URL(Uri);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(getSSLSocketFactory());
            conn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                    return hv.verify(hostname, session);
        }
    });
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setReadTimeout(SOCKET_TIMEOUT);
            conn.setRequestProperty("Content-Type", "application/json; text/plain; */*");
            conn.setRequestProperty("Accept", "application/json");
            OutputStream output = null;
            try {
                output = conn.getOutputStream();
                output.write(object.toString().getBytes(charset));
            } catch (IOException e) {
                throw e;
            } finally {
                if (output != null)
                    try {
                        output.close();
                    } catch (IOException logOrIgnore) {
                        return null;
                    }
            }

            conn.connect();

            Log.d("Http ResponseMessage", conn.getResponseMessage());
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder buf = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null) {
                buf.append(line + "\n");
            }

            str = buf.toString();

        } catch (IOException e) {
            throw e;
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return str;
    }

    public static String postHTTPs(String Uri, Map<String,String> paramaters) throws IOException {
        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        String str = null;
        BufferedReader reader = null;
        HttpsURLConnection conn = null;
        String query = "";
        try {
            URL url = new URL(Uri);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(getSSLSocketFactory());
            conn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                    return hv.verify(hostname, session);
                }
            });
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setReadTimeout(SOCKET_TIMEOUT);
            conn.setRequestProperty("Content-Type", "application/json; text/plain; */*");
            conn.setRequestProperty("Accept", "application/json");
            if (paramaters != null && !paramaters.isEmpty()) {
                for (String key : paramaters.keySet()) {
                    if (paramaters.get(key) != null) {
                        query += key
                                + "="
                                + URLEncoder.encode(paramaters.get(key),
                                "utf-8") + "&";
                    }
                }
            }
            OutputStream output = null;
            try {
                output = conn.getOutputStream();
                output.write(query.getBytes(charset));
            } catch (IOException e) {
                throw e;
            } finally {
                if (output != null)
                    try {
                        output.close();
                    } catch (IOException logOrIgnore) {
                        return null;
                    }
            }

            conn.connect();

            Log.d("Http ResponseMessage", conn.getResponseMessage());
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder buf = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null) {
                buf.append(line + "\n");
            }

            str = buf.toString();

        } catch (IOException e) {
            throw e;
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return str;
    }

    public static String getHTTPs(String Uri, Map<String,String> paramaters) throws IOException {
        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        String str = null;
        BufferedReader reader = null;
        HttpsURLConnection conn = null;
        String query = "";
        try {
            URL url = new URL(Uri);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(getSSLSocketFactory());
            conn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                    return hv.verify(hostname, session);
                }
            });
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setReadTimeout(SOCKET_TIMEOUT);
            conn.setRequestProperty("Content-Type", "application/json; text/plain; */*");
            conn.setRequestProperty("Accept", "application/json");
            if (paramaters != null && !paramaters.isEmpty()) {
                for (String key : paramaters.keySet()) {
                    if (paramaters.get(key) != null) {
                        query += key
                                + "="
                                + URLEncoder.encode(paramaters.get(key),
                                "utf-8") + "&";
                    }
                }
            }
            OutputStream output = null;
            try {
                output = conn.getOutputStream();
                output.write(query.getBytes(charset));
            } catch (IOException e) {
                throw e;
            } finally {
                if (output != null)
                    try {
                        output.close();
                    } catch (IOException logOrIgnore) {
                        return null;
                    }
            }

            conn.connect();

            Log.d("Http ResponseMessage", conn.getResponseMessage());
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder buf = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null) {
                buf.append(line + "\n");
            }

            str = buf.toString();

        } catch (IOException e) {
            throw e;
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return str;
    }

    public static String postHTTPs_urlencode(String Uri, Map<String, String> paramaters) throws ClientProtocolException, IOException {
        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        String str = null;
        BufferedReader reader = null;
        HttpsURLConnection conn = null;
        String query = "";
        try {
            URL url = new URL(Uri);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(getSSLSocketFactory());
            conn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                    return hv.verify(hostname, session);
                }
            });
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setReadTimeout(SOCKET_TIMEOUT);
            conn.setRequestProperty("Accept-Charset", "utf-8");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            if (paramaters != null && !paramaters.isEmpty()) {
                for (String key : paramaters.keySet()) {
                    if (paramaters.get(key) != null) {
                        query += key + "=" + URLEncoder.encode(paramaters.get(key), "utf-8") + "&";
                    }
                }
            }
            Log.e("Http paramaters", query);
            OutputStream output = null;
            try {
                output = conn.getOutputStream();
                output.write(query.getBytes(charset));
            } catch (IOException e) {
                throw e;
            } finally {
                if (output != null)
                    try {
                        output.close();
                    } catch (IOException logOrIgnore) {
                        return null;
                    }
            }
            Log.d("Http ResponseMessage", conn.getResponseMessage());
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder buf = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null) {
                buf.append(line + "\n");
            }

            str = buf.toString();

        } catch (IOException e) {
            throw e;
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return str;
    }

    public static String postHTTP(String Uri, Map<String,String> paramaters) throws IOException {
        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        String str = null;
        BufferedReader reader = null;
        HttpURLConnection conn = null;
        String query = "";
        try {
            URL url = new URL(Uri);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setReadTimeout(SOCKET_TIMEOUT);
            conn.setRequestProperty("Content-Type", "application/json; text/plain; */*");
            conn.setRequestProperty("Accept", "application/json");
            if (paramaters != null && !paramaters.isEmpty()) {
                for (String key : paramaters.keySet()) {
                    if (paramaters.get(key) != null) {
                        query += key
                                + "="
                                + URLEncoder.encode(paramaters.get(key),
                                "utf-8") + "&";
                    }
                }
            }
            OutputStream output = null;
            try {
                output = conn.getOutputStream();
                output.write(query.getBytes(charset));
            } catch (IOException e) {
                throw e;
            } finally {
                if (output != null)
                    try {
                        output.close();
                    } catch (IOException logOrIgnore) {
                        return null;
                    }
            }

            conn.connect();

            Log.d("Http ResponseMessage", conn.getResponseMessage());
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder buf = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null) {
                buf.append(line + "\n");
            }

            str = buf.toString();

        } catch (IOException e) {
            throw e;
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return str;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String ConvertInputStream(InputStream inputStream, Charset charset) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        String line = null;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        return stringBuilder.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String uploadGetFile(String uri, Map<String, String> paramaters, File file, String token) throws IOException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeyManagementException {
        int redirects = 0;
        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        HttpClient httpClient = getNewHttpClient();

        final HttpGet postRequest = new HttpGet(uri);
        FileBody sender = new FileBody(file);
        MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();

        reqEntity.addPart("uploadfile", sender);
        reqEntity.addTextBody("filename", file.getName());
        postRequest.setHeader("Authorization", "Bearer "+token);
        HttpParams params = postRequest.getParams();
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, SOCKET_TIMEOUT);
        params.setLongParameter(ConnManagerPNames.TIMEOUT, MCC_TIMEOUT);

        if (paramaters != null && !paramaters.isEmpty()) {
            for (String key : paramaters.keySet()) {
                if (paramaters.get(key) != null) {
                    reqEntity.addTextBody(key, paramaters.get(key).toString());
                }
            }
        }
        HttpResponse response = null;
        while (redirects < 2) {
            try {
                response = httpClient.execute(postRequest);
                int responseCode = response.getStatusLine().getStatusCode();
                if(responseCode == 200 || responseCode == 201) {

                    InputStream inputStream = response.getEntity().getContent();
                    String json = ConvertInputStream(inputStream, charset);
                    return json;
                }
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                httpClient.getConnectionManager().shutdown();
                redirects++;
                continue;
            } catch (ConnectTimeoutException e) {
                e.printStackTrace();
                httpClient.getConnectionManager().shutdown();
                redirects++;
                continue;
            } catch (IOException e) {
                Log.w(TAG, e.getMessage(), e);
                throw new IOException(e.toString());
            } finally {
                httpClient.getConnectionManager().shutdown();
            }
            redirects++;
        }
        return  null;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String uploadPostFile(String uri, Map<String, String> paramaters, File file, String token) throws IOException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeyManagementException {
        int redirects = 0;
        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        HttpClient httpClient = getNewHttpClient();

        final HttpPost postRequest = new HttpPost(uri);
        FileBody sender = new FileBody(file);
        MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();

        reqEntity.addPart("uploadfile", sender);
        reqEntity.addTextBody("filename", file.getName());
        postRequest.setHeader("Authorization", "Bearer "+token);
        HttpParams params = postRequest.getParams();
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, SOCKET_TIMEOUT);
        params.setLongParameter(ConnManagerPNames.TIMEOUT, MCC_TIMEOUT);

        if (paramaters != null && !paramaters.isEmpty()) {
            for (String key : paramaters.keySet()) {
                if (paramaters.get(key) != null) {
                    reqEntity.addTextBody(key, paramaters.get(key).toString());
                }
            }
        }
        HttpResponse response = null;
        while (redirects < 2) {
            try {
                response = httpClient.execute(postRequest);
                int responseCode = response.getStatusLine().getStatusCode();
                if(responseCode == 200 || responseCode == 201) {

                    InputStream inputStream = response.getEntity().getContent();
                    String json = ConvertInputStream(inputStream, charset);
                    return json;
                }
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                httpClient.getConnectionManager().shutdown();
                redirects++;
                continue;
            } catch (ConnectTimeoutException e) {
                e.printStackTrace();
                httpClient.getConnectionManager().shutdown();
                redirects++;
                continue;
            } catch (IOException e) {
                Log.w(TAG, e.getMessage(), e);
                throw new IOException(e.toString());
            } finally {
                httpClient.getConnectionManager().shutdown();
            }
            redirects++;
        }
        return  null;
    }

    public static boolean uploadViaHttps(String uri, Map<String, String> paramaters, byte[]... data) throws IOException {
        int redirects = 0;
        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        final HttpPost postRequest = new HttpPost(uri);
        MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();
        HttpParams params = postRequest.getParams();
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, SOCKET_TIMEOUT);
        params.setLongParameter(ConnManagerPNames.TIMEOUT, MCC_TIMEOUT);
        if (data != null) {

            for (int i = 0; i < data.length; i++) {
                byte[] _data = data[i];
                // ByteArrayBody sender = new ByteArrayBody(_data, "data" +
                // i);
                reqEntity.addPart("data" + i, new ByteArrayBody(_data, "data" + i));
            }

        }

        if (paramaters != null && !paramaters.isEmpty()) {
            for (String key : paramaters.keySet()) {
                if (paramaters.get(key) != null) {
                    String _text = paramaters.get(key).toString();
                    reqEntity.addTextBody(key, _text);
                }
            }
        }


        postRequest.setEntity(reqEntity.build());
        HttpResponse response = null;
        HttpClient client = null;
        while (redirects < 2) {
            try {
                client = new DefaultHttpClient();
                client.getConnectionManager().getSchemeRegistry().register(new Scheme("https", MySSLSocketFactory.getSocketFactory(), 443));
                response = client.execute(postRequest);
                int responseCode = response.getStatusLine().getStatusCode();
                String mgs = response.getStatusLine().getReasonPhrase();
                switch (responseCode) {
                    case HttpStatus.SC_OK:
                        return true;
                    case HttpStatus.SC_REQUEST_TIMEOUT:
                        redirects++;
                        continue;
                    default:
                        break;
                }
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                client.getConnectionManager().shutdown();
                redirects++;
                continue;
            } catch (ConnectTimeoutException e) {
                e.printStackTrace();
                client.getConnectionManager().shutdown();
                redirects++;
                continue;
            } catch (IOException e) {
                Log.w(TAG, e.getMessage(), e);
                throw new IOException(e.toString());
            } finally {
                client.getConnectionManager().shutdown();
            }
            redirects++;
        }
        return false;
    }

    public static boolean uploadFileHttps(String uri, Map<String, String> paramaters, File file) throws IOException {

        try {
            int redirects = 0;
            if (Build.VERSION.SDK_INT > 9) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
            }
            final HttpPost postRequest = new HttpPost(uri);
            FileBody sender = new FileBody(file);
            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

            reqEntity.addPart("uploadfile", sender);
            try {
                reqEntity.addPart("filename", new StringBody(file.getName()));
            } catch (UnsupportedEncodingException e) {
                Log.e("uploadFile", e.toString(), e);
                return false;
            }
            HttpParams params = postRequest.getParams();
            params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
            params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, SOCKET_TIMEOUT);
            params.setLongParameter(ConnManagerPNames.TIMEOUT, MCC_TIMEOUT);

            if (paramaters != null && !paramaters.isEmpty()) {
                for (String key : paramaters.keySet()) {
                    if (paramaters.get(key) != null) {
                        try {
                            reqEntity.addPart(key, new StringBody(paramaters.get(key), charset));
                        } catch (UnsupportedEncodingException e) {
                            Log.w(TAG, e.getMessage(), e);
                            throw new IOException(e.toString());
                        }
                    }
                }
            }
            postRequest.setEntity(reqEntity);
            HttpResponse response = null;
            HttpClient client = null;
            while (redirects < 2) {
                try {
                    client = new DefaultHttpClient();
                    client.getConnectionManager().getSchemeRegistry().register(new Scheme("https", MySSLSocketFactory.getSocketFactory(), 443));
                    response = client.execute(postRequest);
                    int responseCode = response.getStatusLine().getStatusCode();
                    String msg = response.getStatusLine().getReasonPhrase();
                    switch (responseCode) {
                        case HttpStatus.SC_OK:
                            return true;
                        case HttpStatus.SC_REQUEST_TIMEOUT:
                            redirects++;
                            continue;
                        default:
                            break;
                    }
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                    client.getConnectionManager().shutdown();
                    redirects++;
                    continue;
                } catch (ConnectTimeoutException e) {
                    e.printStackTrace();
                    client.getConnectionManager().shutdown();
                    redirects++;
                    continue;
                } catch (ClientProtocolException e) {
                    Log.w(TAG, e.getMessage(), e);
                    throw new IOException(e.toString());
                } catch (IOException e) {
                    Log.w(TAG, e.getMessage(), e);
                    throw new IOException(e.toString());
                } finally {
                    client.getConnectionManager().shutdown();
                }
                redirects++;
            }
        } catch (Exception ex) {

        }

        return false;
    }

    public static boolean canConnect(URL url) {
        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        HttpURLConnection conn = null;
        boolean canConnect = false;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            canConnect = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!(conn instanceof HttpURLConnection)) {
            canConnect = false;
        }
        if (conn != null) {
            conn.disconnect();
        }
        Log.i("Internet Connect", "" + canConnect);
        return canConnect;
    }
}
