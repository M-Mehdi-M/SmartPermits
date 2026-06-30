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

    private static RetrofitClient instance;
    private final String baseUrl;
    private final ApiService apiService;
    private final SharedPreferences prefs;
    private final Context appContext;

    private RetrofitClient(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext
                .getSharedPreferences("smart_permits_prefs", Context.MODE_PRIVATE);

        baseUrl = ApiConfig.getApiUrl(appContext);

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
                    clearSession();
                    android.content.Intent intent = new android.content.Intent("project.smartpermits.SESSION_EXPIRED");
                    appContext.sendBroadcast(intent);
                }
            }
            return response;
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(responseInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
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
        return baseUrl;
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
        project.smartpermits.NotificationService.stop(appContext);
        SocketIOManager.getInstance(appContext).disconnect();
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        boolean followSystem = prefs.getBoolean("follow_system_theme", false);
        boolean notif = prefs.getBoolean("notifications_enabled", true);
        String lang = prefs.getString("app_language", "");
        prefs.edit().clear().apply();
        prefs.edit()
                .putBoolean("dark_mode", darkMode)
                .putBoolean("follow_system_theme", followSystem)
                .putBoolean("notifications_enabled", notif)
                .putString("app_language", lang)
                .apply();
        instance = null;
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }
}

