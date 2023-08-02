package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static cn.itcast.hotel.constants.HotelConstants.MAPPING_TEMPLATE;

/**
 * @author Yushun Shao
 * @date 2023/5/17 18:50
 * @description: hotel index test
 */
public class HotelSearchTest {
    private RestHighLevelClient client;

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.147.100:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }

    @Test
    void testMatchAll() throws IOException {
        // 1. request
        SearchRequest request = new SearchRequest("hotel");

        // 2. DSL
        request.source().query(QueryBuilders.matchAllQuery());

        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //System.out.println(response);

        // 4. 解析结果
        handleResponse(response);
    }

    @Test
    void testMatch() throws IOException {
        // 1. request
        SearchRequest request = new SearchRequest("hotel");

        // 2. DSL match
        request.source().query(QueryBuilders.matchQuery("all", "如家"));

        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //System.out.println(response);

        handleResponse(response);
    }

    @Test
    void testBool() throws IOException {
        // 1. request
        SearchRequest request = new SearchRequest("hotel");

        // 2. DSL match
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termQuery("city","上海"));
        boolQuery.filter(QueryBuilders.rangeQuery("price").lte(500));
        request.source().query(boolQuery);

        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //System.out.println(response);

        handleResponse(response);
    }

    @Test
    void testPageAndSort() throws IOException {
        int page = 2, size = 5;

        // 1. request
        SearchRequest request = new SearchRequest("hotel");

        // 2. DSL
        // 2.1查询
        request.source().query(QueryBuilders.matchAllQuery());

        // 2.2分页
        request.source().from((page-1) * size).size(size);

        // 2.3排序
        request.source().sort("price", SortOrder.ASC);

        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //System.out.println(response);

        // 4. 解析结果
        handleResponse(response);
    }

    @Test
    void testHighlight() throws IOException {
        // 1. request
        SearchRequest request = new SearchRequest("hotel");

        // 2. DSL
        // 2.1查询
        request.source().query(QueryBuilders.matchQuery("all", "如家"));

        // 2.2高亮
        request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));

        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //System.out.println(response);

        // 4. 解析结果
        SearchHits searchHits = response.getHits();
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);

            // 获取Highlight的map，拿到其中的name字段
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();

            if(!CollectionUtils.isEmpty(highlightFields)){
                HighlightField highlightField = highlightFields.get("name");
                if(highlightField != null){
                    String name = highlightField.getFragments()[0].string();
                    hotelDoc.setName(name);
                }
            }
            System.out.println(hotelDoc);
        }
    }

    @Test
    void testAggregation() throws IOException {
        SearchRequest request = new SearchRequest("hotel");

        // DSL
        request.source().size(0);
        request.source().aggregation(AggregationBuilders
                .terms("brandAgg")
                .field("brand")
                .size(20));

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        // 解析结果
        Aggregations aggregations = response.getAggregations();
        Terms brandTerms = aggregations.get("brandAgg");

        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();

        for (Terms.Bucket bucket : buckets) {
            String brand = bucket.getKeyAsString();
            System.out.println(brand);
        }
    }

    @Test
    void testSuggest() throws IOException {
        SearchRequest request = new SearchRequest("hotel");

        request.source().suggest(new SuggestBuilder().addSuggestion(
                "suggestions",
                SuggestBuilders.completionSuggestion("suggestion")
                        .prefix("h")
                        .skipDuplicates(true)
                        .size(10)
        ));

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        Suggest suggest = response.getSuggest();

        CompletionSuggestion suggestion = suggest.getSuggestion("suggestions");

        for (CompletionSuggestion.Entry.Option option : suggestion.getOptions()) {
            String s = option.getText().toString();
            System.out.println(s);
        }
    }

    private static void handleResponse(SearchResponse response) {
        // 4. 解析结果
        SearchHits searchHits = response.getHits();
        long total = searchHits.getTotalHits().value;
        System.out.println(total);

        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            System.out.println("HotelDoc: " + hotelDoc);
        }
    }

}
