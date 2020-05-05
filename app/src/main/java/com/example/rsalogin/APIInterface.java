package com.example.rsalogin;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIInterface {
    String ENDPOINT = "https://athena.ecs.csus.edu/~cleetusa/csc250/";

    @POST("RSAUtil.php")
    Call<String> encryptUsingPublicKey(@Body RequestBody body);

    @POST("RSAUtil.php")
    Call<String> encryptUsingPrivateKey(@Body RequestBody body);

//    @Headers("Content-Type: application/json")
    @POST("RSAUtil.php")
    Call<String> decryptUsingPublicKey(@Body RequestBody body);

    @POST("RSAUtil.php")
    Call<String> decryptUsingPrivateKey(@Body RequestBody body);

    @POST("login.php")
    Call<String> login(@Body RequestBody body);

    @POST("register.php")
    Call<String> register(@Body RequestBody body);
}
