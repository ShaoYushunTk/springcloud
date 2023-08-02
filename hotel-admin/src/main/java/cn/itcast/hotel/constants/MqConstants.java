package cn.itcast.hotel.constants;

/**
 * @author Yushun Shao
 * @date 2023/5/22 21:35
 * @description: mq
 */
public class MqConstants {
    /**
     * 交换机
     */
    public final static String HOTEL_EXCHANGE = "hotel.topic";

    /**
     * 监听新增和修改的队列
     */
    public final static String HOTEL_INSERT_QUEUE = "hotel.insert.queue";
    /**
     * 监听删除的队列
     */
    public final static String HOTEL_DELETE_QUEUE = "hotel.delete.queue";

    /**
     * 监听新增和修改的routing key
     */
    public final static String HOTEL_INSERT_KEY = "hotel.insert";
    /**
     * 监听删除的routing key
     */
    public final static String HOTEL_DELETE_KEY = "hotel.delete";
}
