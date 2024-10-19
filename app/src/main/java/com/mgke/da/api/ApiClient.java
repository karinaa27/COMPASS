package com.mgke.da.api;

import com.mgke.da.models.ConversionResponse;
import com.mgke.da.models.CurrencyResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "https://api.currencylayer.com/";
    private static Retrofit retrofit = null;
    private static CurrencyApi currencyApi;

    // Метод для инициализации Retrofit
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

    // Метод для получения курсов валют
    public static void getLiveRates(String apiKey) {
        getCurrencyApi().getLiveRates(apiKey).enqueue(new Callback<CurrencyResponse>() {
            @Override
            public void onResponse(Call<CurrencyResponse> call, Response<CurrencyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CurrencyResponse rates = response.body();
                    // Обработка данных, например:
                    // Log.d("Currency Rates", rates.toString());
                } else {
                    // Обработка ошибки
                    // Log.e("API Error", "Response not successful: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<CurrencyResponse> call, Throwable t) {
                // Обработка ошибок
                // Log.e("API Error", "Request failed: " + t.getMessage());
            }
        });
    }
    public static Call<ConversionResponse> convertCurrency(String apiKey, String fromCurrency, String toCurrency, double amount) {
        return getCurrencyApi().convertCurrency(apiKey, fromCurrency, toCurrency, amount);
    }
}