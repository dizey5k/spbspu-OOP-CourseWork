package com.example.coursework.api;

import com.example.coursework.model.AggregatedRecord;
import com.example.coursework.model.ApiSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class BibleSource implements ApiSource {
    private static final String URL = "https://justbible.ru/api/bible?translation=rst";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "justbible";
    }

    @Override
    public String getDisplayName() {
        return "Just Bible (Синодальный перевод)";
    }

    @Override
    public List<AggregatedRecord> fetchData() throws IOException {
        Request request = new Request.Builder().url(URL).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            assert response.body() != null;
            JsonNode root = mapper.readTree(response.body().byteStream());
            List<AggregatedRecord> records = new ArrayList<>();
            records.add(new AggregatedRecord(getName(), root));
            return records;
        }
    }
}