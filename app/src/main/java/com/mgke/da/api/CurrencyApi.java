package com.mgke.da.api;

import com.mgke.da.models.ConversionResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CurrencyApi {

    @GET("convert")
    Call<ConversionResponse> convertCurrency(
            @Query("access_key") String apiKey,
            @Query("from") String fromCurrency,
            @Query("to") String toCurrency,
            @Query("amount") double amount
    );
}