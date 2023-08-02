package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {
    @Autowired
    private RestHighLevelClient client;
    @Override
    public PageResult search(RequestParams params){
        try {
            // 1. request
            SearchRequest request = new SearchRequest("hotel");

            buildBasicQuery(params, request);

            // 2.3分页
            int page = params.getPage();
            int size = params.getSize();
            request.source().from((page - 1) * size).size(size);

            // 2.4排序 找到附近酒店
            String location = params.getLocation();
            if(location != null && !"".equals(location)){
                request.source().sort(SortBuilders
                        .geoDistanceSort("location", new GeoPoint(location))
                        .order(SortOrder.ASC)
                        .unit(DistanceUnit.KILOMETERS));
            }

            // 3. 发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4. 解析响应
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 对品牌，星级，城市做bucket聚合
      * @return
     */
    @Override
    public Map<String, List<String>> filters(RequestParams params) {
        try {
            SearchRequest request = new SearchRequest("hotel");

            // DSL
            buildAggregation(request);

            // query限定聚合范围
            buildBasicQuery(params, request);

            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

            Map<String, List<String>> result = new HashMap<>();

            Aggregations aggregations = response.getAggregations();
            //根据名称获取品牌结果
            List<String> brandList = getAggByName(aggregations, "brandAgg");
            List<String> cityList = getAggByName(aggregations, "cityAgg");
            List<String> starList = getAggByName(aggregations, "starAgg");
            result.put("brand", brandList);
            result.put("city", cityList);
            result.put("starName", starList);

            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 自动补全
     * @param prefix
     * @return
     */
    @Override
    public List<String> getSuggestions(String prefix) {
        try {
            // request
            SearchRequest request = new SearchRequest("hotel");

            // DSL
            request.source().suggest(new SuggestBuilder().addSuggestion(
                    "suggestions", SuggestBuilders.completionSuggestion("suggestion")
                            .prefix(prefix)
                            .skipDuplicates(true)
                            .size(100)
            ));

            // 发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

            //解析结果
            Suggest suggest = response.getSuggest();
            CompletionSuggestion suggestions = suggest.getSuggestion("suggestions");
            List<String> result = new ArrayList<>();
            for (CompletionSuggestion.Entry.Option option : suggestions.getOptions()) {
                String s = option.getText().toString();
                result.add(s);
            }

            return result;
        } catch (IOException e) {

            throw new RuntimeException(e);
        }

    }

    @Override
    public void deleteById(Long id) {
        try {
            // request
            DeleteRequest request = new DeleteRequest("hotel", id.toString());
            // 发送请求
            client.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void insertById(Long id) {
        try {
            // 根据id查询
            Hotel hotel = getById(id);
            // request
            HotelDoc hotelDoc = new HotelDoc(hotel);
            IndexRequest request = new IndexRequest("hotel").id(hotelDoc.getId().toString());
            // DSL
            request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
            // 发送请求
            client.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> getAggByName(Aggregations aggregations, String aggName) {
        Terms bucketTerms = aggregations.get(aggName);
        List<String> brandList = new ArrayList<>();
        List<? extends Terms.Bucket> buckets = bucketTerms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            String brand = bucket.getKeyAsString();
            brandList.add(brand);
        }
        return brandList;
    }

    private static void buildAggregation(SearchRequest request) {
        request.source().size(0);
        request.source().aggregation(AggregationBuilders.
                terms("brandAgg").
                field("brand").
                size(100));
        request.source().aggregation(AggregationBuilders.
                terms("cityAgg").
                field("city").
                size(100));
        request.source().aggregation(AggregationBuilders.
                terms("starAgg").
                field("starName").
                size(100));
    }

    private static void buildBasicQuery(RequestParams params, SearchRequest request) {
        // 2. DSL
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 2.1关键字搜索
        String key = params.getKey();
        if(key == null || "".equals(key)){
            boolQuery.must(QueryBuilders.matchAllQuery());
        }else{
            boolQuery.must(QueryBuilders.matchQuery("all", key));
        }
        // 2.2条件过滤
        // 2.2.1城市 term
        if(params.getCity() != null && !"".equals(params.getCity())){
            boolQuery.filter(QueryBuilders.termQuery("city", params.getCity()));
        }
        // 2.2.2品牌 term
        if(params.getBrand() != null && !"".equals(params.getBrand())){
            boolQuery.filter(QueryBuilders.termQuery("brand", params.getBrand()));
        }
        // 2.2.3星级 term
        if(params.getStarName() != null && !"".equals(params.getStarName())){
            boolQuery.filter(QueryBuilders.termQuery("starName", params.getStarName()));
        }
        // 2.2.4价格 range
        if(params.getMinPrice() != null && params.getMaxPrice() != null){
            boolQuery.filter(QueryBuilders.rangeQuery("price")
                    .gte(params.getMinPrice())
                    .lte(params.getMaxPrice())
                    );
        }

        // function score query
        FunctionScoreQueryBuilder functionScoreQueryBuilder =
                QueryBuilders.functionScoreQuery(
                        // 原始查询 相关性算分
                        boolQuery,
                        // function score数组
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                        // 过滤条件
                                        QueryBuilders.termQuery("isAD", true),
                                        // 算分函数 权重
                                        ScoreFunctionBuilders.weightFactorFunction(10)
                                )
                        });

        request.source().query(functionScoreQueryBuilder);
    }

    private PageResult handleResponse(SearchResponse response) {
        // 4. 解析结果
        SearchHits searchHits = response.getHits();
        long total = searchHits.getTotalHits().value;

        SearchHit[] hits = searchHits.getHits();
        List<HotelDoc> hotelDocs = new ArrayList<>();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);

            Object[] sortValues = hit.getSortValues();
            if(sortValues.length > 0){
                Object distance = sortValues[0];
                hotelDoc.setDistance(distance);
            }

            hotelDocs.add(hotelDoc);
        }

        return new PageResult(total, hotelDocs);
    }
}
