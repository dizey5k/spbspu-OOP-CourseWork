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

public class JsonPlaceholderUsersSource implements ApiSource {
    private static final String URL = "https://jsonplaceholder.typicode.com/users";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "jsonplaceholder-users";
    }

    @Override
    public String getDisplayName() {
        return "JSONPlaceholder Users";
    }

    @Override
    public List<AggregatedRecord> fetchData() throws IOException {
        Request request = new Request.Builder().url(URL).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            JsonNode root = mapper.readTree(response.body().byteStream());
            List<AggregatedRecord> records = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode user : root) {
                    records.add(new AggregatedRecord(getName(), user));
                }
            }
            return records;
        }
    }
}