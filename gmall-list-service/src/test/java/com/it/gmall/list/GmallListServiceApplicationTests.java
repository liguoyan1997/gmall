package com.it.gmall.list;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

    @Autowired
    private JestClient jestClient;

    @Test
    public void contextLoads() throws IOException {
        String query = "{\n" +
                "    \"query\":{\n" +
                "      \"match_phrase\": {\"name\":\"operation red\"}\n" +
                "    }\n" +
                "}";
        /*查询get*/
        Search build = new Search.Builder(query).addIndex("movie_index").addType("movie").build();

        /*执行动作*/
        SearchResult execute = jestClient.execute(build);

        /*获取数据*/
        List<SearchResult.Hit<Map, Void>> hits = execute.getHits(Map.class);

        for (SearchResult.Hit<Map, Void> hit : hits) {
            Map source = hit.source;
            System.out.println(source.get("name"));
        }
    }

}
