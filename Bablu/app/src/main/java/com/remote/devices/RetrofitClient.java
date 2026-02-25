package com.remote.devices;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * A singleton Retrofit client to ensure only one instance is used throughout the app.
 */
public class RetrofitClient {

    // Set to your live server URL provided by Render
    private static final String BASE_URL = "https://bengal-tiger-admin-production.up.railway.app";

    private static Retrofit retrofit = null;

    /**
     * Returns the singleton instance of the ApiService.
     *
     * @return The ApiService instance.
     */
    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
