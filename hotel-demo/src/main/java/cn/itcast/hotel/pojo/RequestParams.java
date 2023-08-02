package cn.itcast.hotel.pojo;

import lombok.Data;

/**
 * @author Yushun Shao
 * @date 2023/5/18 19:27
 * @description: request params
 */
@Data
public class RequestParams {
    private String key;
    private Integer page;
    private Integer size;
    private String sortBy;

    // 下面是新增的过滤条件参数
    private String city;
    private String brand;
    private String starName;
    private Integer minPrice;
    private Integer maxPrice;
    private String location;

}
