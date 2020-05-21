package ac.ds.wstest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface TestAPI {
    // TODO: ATG: set server endpoint
    @Headers("Content-Type: application/json")
    @POST("/")
    Call<String> report(@Body String body);
}