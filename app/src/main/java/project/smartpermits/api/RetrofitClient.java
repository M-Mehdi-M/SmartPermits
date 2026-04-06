package project.smartpermits.api;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    private static final String BASE_URL = "http://192.168.137.1:5000/api/";
    private static RetrofitClient instance;
    private final ApiService apiService;
    private final SharedPreferences prefs;

    private RetrofitClient(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences("smart_permits_prefs", Context.MODE_PRIVATE);

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            String token = prefs.getString("auth_token", "");
            if (token != null && !token.isEmpty()) {
                Request request = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .build();
                return chain.proceed(request);
            }
            return chain.proceed(original);
        };

        Interceptor responseInterceptor = chain -> {
            okhttp3.Response response = chain.proceed(chain.request());
            if (response.code() == 401) {
                String token = prefs.getString("auth_token", "");
                if (token != null && !token.isEmpty()) {
                    prefs.edit().remove("auth_token").apply();
                    android.content.Intent intent = new android.content.Intent("project.smartpermits.SESSION_EXPIRED");
                    context.getApplicationContext().sendBroadcast(intent);
                }
            }
            return response;
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(responseInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public static synchronized RetrofitClient getInstance(Context context) {
        if (instance == null) {
            instance = new RetrofitClient(context);
        }
        return instance;
    }

    public ApiService getApi() {
        return apiService;
    }

    public String getBaseUrl() {
        return BASE_URL;
    }

    public void saveToken(String token) {
        prefs.edit().putString("auth_token", token).apply();
    }

    public void saveUserRole(String role) {
        prefs.edit().putString("user_role", role).apply();
    }

    public void saveUserName(String name) {
        prefs.edit().putString("user_name", name).apply();
    }

    public void saveUserEmail(String email) {
        prefs.edit().putString("user_email", email).apply();
    }

    public void saveAvatarUrl(String avatarUrl) {
        prefs.edit().putString("avatar_url", avatarUrl).apply();
    }

    public void saveUserId(int userId) {
        prefs.edit().putInt("user_id", userId).apply();
    }

    public String getToken() {
        return prefs.getString("auth_token", "");
    }

    public String getUserRole() {
        return prefs.getString("user_role", "");
    }

    public String getUserName() {
        return prefs.getString("user_name", "");
    }

    public String getUserEmail() {
        return prefs.getString("user_email", "");
    }

    public String getAvatarUrl() {
        return prefs.getString("avatar_url", "");
    }

    public int getUserId() {
        return prefs.getInt("user_id", 0);
    }

    public void clearSession() {
        prefs.edit().clear().apply();
        instance = null;
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }
}

