package cn.itcast.hotel.pojo;

import lombok.Data;

import java.util.List;

/**
 * @author Yushun Shao
 * @date 2023/5/18 19:29
 * @description: page result
 */
@Data
public class PageResult {
    private Long total;
    private List<HotelDoc> hotels;

    public PageResult() {
    }

    public PageResult(Long total, List<HotelDoc> hotels) {
        this.total = total;
        this.hotels = hotels;
    }
}
