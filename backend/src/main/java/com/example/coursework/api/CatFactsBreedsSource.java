package com.example.coursework.api;

import com.example.coursework.model.AggregatedRecord;
import com.example.coursework.model.ApiSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CatFactsBreedsSource implements ApiSource {
    private static final String URL = "https://catfact.ninja/breeds?limit=5";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "catfacts-breeds";
    }

    @Override
    public String getDisplayName() {
        return "Cat Facts Breeds";
    }

    @Override
    public List<AggregatedRecord> fetchData() throws IOException {
        Request request = new Request.Builder().url(URL).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            JsonNode root = mapper.readTree(response.body().byteStream());
            JsonNode data = root.get("data");
            List<AggregatedRecord> records = new ArrayList<>();
            if (data != null && data.isArray()) {
                for (JsonNode breed : data) {
                    records.add(new AggregatedRecord(getName(), breed));
                }
            }
            return records;
        }
    }
}