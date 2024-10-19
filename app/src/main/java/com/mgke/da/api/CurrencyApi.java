package com.mgke.da.api;

import com.mgke.da.models.ConversionResponse;
import com.mgke.da.models.CurrencyResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CurrencyApi {

    @GET("live")
    Call<CurrencyResponse> getLiveRates(@Query("access_key") String apiKey);

    @GET("convert")
    Call<ConversionResponse> convertCurrency(
            @Query("access_key") String apiKey,
            @Query("from") String fromCurrency,
            @Query("to") String toCurrency,
            @Query("amount") double amount
    );
}