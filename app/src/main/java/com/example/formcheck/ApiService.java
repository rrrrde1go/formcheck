package com.example.formcheck;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/workouts")
    Call<Void> sendWorkout(@Body WorkoutRequest request);

    @GET("api/workouts")
    Call<List<WorkoutRequest>> getWorkouts();
}