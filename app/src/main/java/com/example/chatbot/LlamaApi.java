package com.example.chatbot;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface LlamaApi {
    @FormUrlEncoded
    @POST("/chat")
    Call<String> sendMessage(@Field("userMessage") String message);
}
