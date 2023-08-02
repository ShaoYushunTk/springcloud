package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

import static cn.itcast.hotel.constants.HotelConstants.MAPPING_TEMPLATE;

/**
 * @author Yushun Shao
 * @date 2023/5/17 18:50
 * @description: hotel index test
 */
@SpringBootTest
public class HotelDocumentTest {
    private RestHighLevelClient client;
    @Autowired
    private IHotelService hotelService;

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

    /**
     * POST /hotel/_doc/id
     * @throws IOException
     */
    @Test
    void testAddDocument() throws IOException {
        //根据id查询
        Hotel hotel = hotelService.getById(46829L);
        //转化为文档类型
        HotelDoc hotelDoc = new HotelDoc(hotel);
        // 1.request对象
        IndexRequest request = new IndexRequest("hotel").id(hotelDoc.getId().toString());
        // 2.JSON文档
        request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
        // 3.发送请求
        client.index(request, RequestOptions.DEFAULT);
    }

    /**
     * GET /hotel/_doc/id
     */
    @Test
    void testGetDocument() throws IOException {
        // 1.request对象
        GetRequest request = new GetRequest("hotel", "46829");
        // 2.发送请求
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        // 3.解析响应
        String json = response.getSourceAsString();

        HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
        System.out.println(hotelDoc);
    }

    @Test
    void testUpdateDocument() throws IOException {
        UpdateRequest request = new UpdateRequest("hotel", "46829");

        request.doc(
                "price", "888",
                "score", "50"
        );

        client.update(request, RequestOptions.DEFAULT);
    }

    @Test
    void testDeleteDocument() throws IOException {
        DeleteRequest request = new DeleteRequest("hotel", "46829");

        client.delete(request, RequestOptions.DEFAULT);
    }

    @Test
    void testBulkRequest() throws IOException {
        // 批量查询酒店数据
        List<Hotel> hotels = hotelService.list();

        BulkRequest request = new BulkRequest();
        // 转换为文档类型
//        for (Hotel hotel : hotels) {
//            HotelDoc hotelDoc = new HotelDoc(hotel);
//            request.add(new IndexRequest("hotel")
//                    .id(hotelDoc.getId().toString())
//                    .source(JSON.toJSONString(hotelDoc),XContentType.JSON));
//        }

        hotels.stream()
                .map(HotelDoc::new)
                .forEach(hotelDoc -> request.add(new IndexRequest("hotel")
                        .id(hotelDoc.getId().toString())
                        .source(JSON.toJSONString(hotelDoc), XContentType.JSON)));


        client.bulk(request, RequestOptions.DEFAULT);
    }
}
