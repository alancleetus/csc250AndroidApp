package com.example.rsalogin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MY-LOG";

    private URL url;
    private final String pubKeyStr = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCekzw0bd3j3snnA0mRZFyuNE0jSZtk2WZi1EPTfoUh47aE1XPoap2pBEIFUi91LQsbY0DpCKL2FfyWT+wjHhqngUU/fWiYZsB0IbqMK1ITWB+0FR8txDzYfrQKCHabaLU8VV2bx38YGOQ8en5UBQm49TMkw59riEkoNWwn1+CNjwIDAQAB";
    private String privKeyStr = "MIICWwIBAAKBgQCekzw0bd3j3snnA0mRZFyuNE0jSZtk2WZi1EPTfoUh47aE1XPoap2pBEIFUi91LQsbY0DpCKL2FfyWT+wjHhqngUU/fWiYZsB0IbqMK1ITWB+0FR8txDzYfrQKCHabaLU8VV2bx38YGOQ8en5UBQm49TMkw59riEkoNWwn1+CNjwIDAQABAoGAXrEvT2OYD/229VM6OC8FRSWINp06xQMpJ7T3d7DikTUohbPtDgm0cfxP7FuCaWdnbYhcd4unvGmutpetO987LIDITFOkyJsEJ9B44b/GYfMuHtRBbRjjzDz9qFtv0RnzJCUIw5RA6o46pP1Wzf/DHY7jjXeAK/r6EUmlo+AzyfkCQQDOtTtpaIdC2nUiWhlcQJ+Tpjp3hjHzhIJrNGfx3Pt+GT99SBYbICYx8CqyWzDbLzXJiicLWIXpvn99cUn/s/4DAkEAxGOrsLhF1uuLWoH/h+3PQlnx9oHnMCJsvmNpOQbTA2gKlHqLYJq93SjWhddGGrCRAyKTKmh79EK24FJUOesyhQJAJOsXfl8N8XHFA+qlpuVf2uYQgTJ1j3G2PWFxwy/dtwrZXQ3X7OZUDA1CAvLoie0npSRIju0zlajRal0mg0XpcwJAUyBNH/Qfu6Tfy2OTIZoLTG59+HVrwfgQR1YUx8qXrq1vLQHh1PYqv131z5kbV1i9SLJt0FmBfhZvCexPMmTXcQJAPbQs4zAikz1eA4rXM5v3oHQhE3qihAqpFzKLYsQ4qgBP/XD3XO4aIxAYankjS++vid28TCrPmDb7S4FjtHmMgg==";
    private String serverPubKeyStr;
    private APIInterface apiInterface;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button registerButton = (Button) this.findViewById(R.id.registerButton);
        Button loginButton = (Button) this.findViewById(R.id.loginButton);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), ScanActivity.class);
                startActivityForResult(myIntent, 1);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), ScanActivity.class);
                startActivityForResult(myIntent, 2);
            }
        });

        prefs = getSharedPreferences("My_prefs", MODE_PRIVATE);
        serverPubKeyStr = "";
        if(prefs.getString("websitePublicKey", null)!= null) {
               serverPubKeyStr = prefs.getString("websitePublicKey", null);
               Log.d(TAG,"serverPubKey = "+serverPubKeyStr);
            }else {Log.d(TAG,"serverPubKey not found");}

        OkHttpClient okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(APIInterface.ENDPOINT)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        apiInterface = retrofit.create(APIInterface.class);
    }

    private void registerUser(String recvString) {
        Log.d(TAG, "registerUser: Starting...");
        Log.d(TAG, "registerUser: recvString = "+recvString);

        String encText;
        JSONObject jsonRecv = null;
        JSONObject jsonToSend = null;

        //de-stringify json object
        try {
            jsonRecv = new JSONObject(recvString);
            Log.d(TAG, "registerUser: jsonRecv = "+jsonRecv);
        } catch (Throwable t) {
            Log.e(TAG, "Could not parse malformed JSON: \"" + recvString + "\"");
        }

        //save server public key to keystore
        try {
            prefs.edit().putString("websitePublicKey", jsonRecv.getString("webPubKey")).apply();
            Log.i(TAG, "registerUser: saving web pub key = "+jsonRecv.getString("webPubKey"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //make json object to send
        try {
            jsonToSend = new JSONObject();
            jsonToSend.put(jsonRecv.getString("userName"), pubKeyStr);
            Log.d(TAG, "registerUser: jsonToSend = "+jsonToSend);
        } catch (Exception e) {
            Log.e(TAG, "registerUser: Could not generate JSON to send");
        }

        //get return link
        try {
            url = new URL(jsonRecv.getString("returnLink"));
            Log.d(TAG, "registerUser: url = "+url.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //enc json object using website's publickey
        try {
            JSONObject obj = new JSONObject();

            try {
                obj.put("plainText", jsonToSend.toString());
                obj.put("publicKey", jsonRecv.getString("webPubKey"));
            }catch (Exception e){ e.printStackTrace(); }

            Log.d(TAG, "registerUser: encrypting obj = "+obj.toString());

            RequestBody body = RequestBody.create(MediaType.parse("text/plain"), obj.toString());
            Call<String> encUsingPubKey = apiInterface.encryptUsingPublicKey(body);
            encUsingPubKey.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    String cipherText = response.body();
                    Log.d(TAG, "encUsingPubKey: cipherText = "+cipherText);

                    JSONObject obj2 = new JSONObject();

                    try {
                        obj2.put("cipherText", cipherText);
                    }catch (Exception e){ e.printStackTrace(); }

                    RequestBody body = RequestBody.create(MediaType.parse("text/plain"), obj2.toString());
                    Call<String> register = apiInterface.register(body);
                    register.enqueue(new Callback<String>() {
                         @Override
                         public void onResponse(Call<String> call, Response<String> response) {
                             Log.d(TAG, "register: response.body() = "+response.body());
                             Log.d(TAG, "register: Finished Registering user");
                             Toast.makeText(getApplicationContext(), "Register Complete", Toast.LENGTH_SHORT).show();
                         }

                         @Override
                         public void onFailure(Call<String> call, Throwable t) {
                             Log.d(TAG, "register: Error Registering user");
                         }
                    });
                }
                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e(TAG, "encUsingPubKey error");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loginUser(String recvString) {
        Log.d(TAG, "loginUser: Starting...");
        Log.d(TAG, "loginUser: recvString = "+recvString);


        try {
            JSONObject obj = new JSONObject();

            try {
                obj.put("cipherText", recvString);
                obj.put("privateKey", privKeyStr);
            } catch (Exception e) {
                e.printStackTrace();
            }


            //Step 1: decrypt recvString using my private key
            Log.d(TAG, "loginUser: decrypting obj = " + obj.toString());
            RequestBody body = RequestBody.create(MediaType.parse("text/plain"), obj.toString());
            Call<String> decUsingPrivKey = apiInterface.decryptUsingPrivateKey(body);
            decUsingPrivKey.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    Log.d(TAG, "loginUser: decUsingPrivKey response: "+response.body());

                    JSONObject obj2 = new JSONObject();

                    try {
                        obj2.put("cipherText", response.body());
                        obj2.put("publicKey", serverPubKeyStr);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //Step 2: further decrypt recvString using my server public key
                    Log.d(TAG, "loginUser: decrypting obj2 = " + obj2.toString());
                    RequestBody body2 = RequestBody.create(MediaType.parse("text/plain"), obj2.toString());
                    Call<String> decUsingPubKey = apiInterface.decryptUsingPublicKey(body2);
                    decUsingPubKey.enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            Log.d(TAG, "loginUser: decUsingPubKey response: "+response.body());

                            JSONObject jsonRecv = null;
                            JSONObject jsonToSend = null;

                            //de-stringify json object
                            try {
                                jsonRecv = new JSONObject(response.body());
                                Log.d(TAG, "loginUser: jsonRecv = "+jsonRecv);
                            } catch (Throwable t) {
                                Log.e(TAG, "Could not parse malformed JSON: \"" + response.body() + "\"");
                            }

                            //make json object to send
                            try {
                                jsonToSend = new JSONObject();
                                jsonToSend.put("sessionKey", jsonRecv.getString("sessionKey"));
                                Log.d(TAG, "loginUser: jsonToSend = "+jsonToSend);
                            } catch (Exception e) {
                                Log.e(TAG, "loginUser: Could not generate JSON to send");
                            }

                            //Step 3: send login token to server
                            Log.d(TAG, "loginUser: sending jsonToSend = " + jsonToSend.toString());
                            RequestBody body3 = RequestBody.create(MediaType.parse("text/plain"), jsonToSend.toString());
                            Call<String> login = apiInterface.login(body3);
                            login.enqueue(new Callback<String>() {
                                @Override
                                public void onResponse(Call<String> call, Response<String> response) {
                                    Log.d(TAG, "loginUser: Login response: "+response.body());
                                    Log.d(TAG, "loginUser: Login Complete");
                                    Toast.makeText(getApplicationContext(), "Login Complete", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Call<String> call, Throwable t) {
                                    Log.d(TAG, "loginUser: error logging in");
                                }
                            });
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Log.d(TAG, "loginUser: error decUsingPubKey");
                        }
                    });
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.d(TAG, "loginUser: error decUsingPrivKey");
                }
            });

            //Step 2: decrypt token using website public key

        }catch(Exception e){ e.printStackTrace();}




        /*
        String encText;


        //send json object to return link
        try {
            url = new URL(jsonRecv.getString("returnLink"));
            Log.d(TAG, "loginUser: url = "+url.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (jsonToSend != null) {
            Log.d(TAG, "loginUser: Sending POST to: " + url.toString());
            new PostLogin().execute(jsonToSend.toString());
        }

        Log.d(TAG, "loginUser: Finished Login user");

         */
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) return;

        Log.d(TAG, "onActivityResult: Returning from ScanActivity with resultCode: " + resultCode + " requestCode: " + requestCode);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            String qrCodeVal = data.getStringExtra("qrCodeVal").toString();
            Log.d(TAG, "onActivityResult: Calling registerUser() with data = " + qrCodeVal.toString());
            registerUser(qrCodeVal);
        } else if (requestCode == 2 && resultCode == RESULT_OK) {

            String qrCodeVal = data.getStringExtra("qrCodeVal");
            Log.d(TAG, "onActivityResult: Calling loginUser() with data = " + qrCodeVal);
            loginUser(qrCodeVal);
        }
    }

    class PostLogin extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            try {
                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) { return true; }
                });

                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, new X509TrustManager[]{new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }}, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

                //https://www.baeldung.com/httpurlconnection-post
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; utf-8");
                con.setRequestProperty("Accept", "application/json");
                con.setDoOutput(true);
                con.connect();

                Log.d(TAG, "PostLogin: Starting Connection...");

                Log.d(TAG, "PostLogin: sending data = "+strings[0]);

                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = strings[0].getBytes("utf-8");
                    os.write(input, 0, input.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                int code = con.getResponseCode();
                Log.d(TAG, "PostLogin: response code = " + code);

                try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    Log.d(TAG, "PostLogin: response = " + response.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "PostLogin: Ending Connection...");
                con.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

    }

}