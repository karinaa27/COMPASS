package com.mgke.da.api;

import com.mgke.da.models.ConversionResponse;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "https://api.currencylayer.com/";
    private static Retrofit retrofit = null;
    private static CurrencyApi currencyApi;

    public static CurrencyApi getCurrencyApi() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            currencyApi = retrofit.create(CurrencyApi.class);
        }
        return currencyApi;
    }
    public static Call<ConversionResponse> convertCurrency(String apiKey, String fromCurrency, String toCurrency, double amount) {
        return getCurrencyApi().convertCurrency(apiKey, fromCurrency, toCurrency, amount);
    }
}