package project.smartpermits.api;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import project.smartpermits.models.AiAnalysisResponse;
import project.smartpermits.models.AnalyticsResponse;
import project.smartpermits.models.Appointment;
import project.smartpermits.models.AppointmentRequest;
import project.smartpermits.models.ChangePasswordRequest;
import project.smartpermits.models.Comment;
import project.smartpermits.models.CommentRequest;
import project.smartpermits.models.FcmTokenRequest;
import project.smartpermits.models.LoginRequest;
import project.smartpermits.models.LoginResponse;
import project.smartpermits.models.MessageResponse;
import project.smartpermits.models.Permit;
import project.smartpermits.models.PermitRequest;
import project.smartpermits.models.PermitType;
import project.smartpermits.models.RegisterRequest;
import project.smartpermits.models.ReviewRequest;
import project.smartpermits.models.ProfileUpdateRequest;
import project.smartpermits.models.Document;
import project.smartpermits.models.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<LoginResponse> register(@Body RegisterRequest request);

    @POST("auth/change-password")
    Call<MessageResponse> changePassword(@Body ChangePasswordRequest request);

    @POST("auth/fcm-token")
    Call<MessageResponse> saveFcmToken(@Body FcmTokenRequest request);

    @GET("auth/profile")
    Call<User> getProfile();

    @PUT("auth/profile")
    Call<User> updateProfile(@Body ProfileUpdateRequest request);

    @Multipart
    @POST("auth/profile/avatar")
    Call<User> uploadAvatar(@Part MultipartBody.Part file);

    @DELETE("auth/delete-account")
    Call<MessageResponse> deleteAccount();

    @GET("permits")
    Call<List<Permit>> getMyPermits();

    @GET("permits")
    Call<List<Permit>> searchMyPermits(@Query("search") String search, @Query("status") String status, @Query("type") String type);

    @POST("permits")
    Call<Permit> createPermit(@Body PermitRequest request);

    @GET("permits/{id}")
    Call<Permit> getPermit(@Path("id") int id);

    @Multipart
    @POST("permits/{id}/upload")
    Call<Document> uploadDocument(@Path("id") int id, @Part MultipartBody.Part file, @Part("document_label") okhttp3.RequestBody label);

    @POST("permits/{id}/pay")
    Call<Permit> payPermit(@Path("id") int id);

    @POST("permits/{id}/renew")
    Call<Permit> renewPermit(@Path("id") int id);

    @POST("permits/{id}/trash")
    Call<MessageResponse> trashPermit(@Path("id") int id);

    @POST("permits/{id}/restore")
    Call<Permit> restorePermit(@Path("id") int id);

    @DELETE("permits/{id}/permanent-delete")
    Call<MessageResponse> permanentDeletePermit(@Path("id") int id);

    @GET("permits/trash")
    Call<List<Permit>> getTrash();

    @DELETE("permits/trash/empty")
    Call<MessageResponse> emptyTrash();

    @GET("permits/pending")
    Call<List<Permit>> getPendingPermits();

    @GET("permits/pending")
    Call<List<Permit>> searchPendingPermits(@Query("search") String search, @Query("type") String type);

    @GET("permits/reviewed")
    Call<List<Permit>> getReviewedPermits();

    @POST("permits/{id}/review")
    Call<Permit> reviewPermit(@Path("id") int id, @Body ReviewRequest request);

    @GET("permits/{id}/comments")
    Call<List<Comment>> getComments(@Path("id") int id);

    @POST("permits/{id}/comments")
    Call<Comment> addComment(@Path("id") int id, @Body CommentRequest request);

    @POST("permits/{id}/appointment")
    Call<Appointment> scheduleAppointment(@Path("id") int id, @Body AppointmentRequest request);

    @GET("appointments")
    Call<List<Appointment>> getAppointments();

    @PUT("appointments/{id}")
    Call<Appointment> updateAppointment(@Path("id") int id, @Body AppointmentRequest request);

    @GET("permits/{id}/certificate")
    Call<ResponseBody> downloadCertificate(@Path("id") int id);

    @GET("permits/stats/analytics")
    Call<AnalyticsResponse> getAnalytics();

    @GET("permit-types")
    Call<List<PermitType>> getPermitTypes();

    @POST("permits/{id}/ai-analyze")
    Call<AiAnalysisResponse> triggerAiAnalysis(@Path("id") int id);

    @GET("permits/{id}/verify-blockchain")
    Call<ResponseBody> verifyBlockchain(@Path("id") int id);
}
