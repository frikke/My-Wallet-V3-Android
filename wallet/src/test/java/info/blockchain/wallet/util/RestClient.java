package info.blockchain.wallet.util;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class RestClient {

    public static Retrofit getRetrofitApiInstance(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl("https://api.blockchain.info/")
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
    }
}
