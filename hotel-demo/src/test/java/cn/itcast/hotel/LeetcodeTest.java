package cn.itcast.hotel;

import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Yushun Shao
 * @date 2023/5/24 9:36
 * @description:
 */
public class LeetcodeTest {
    @Test
    int findCircleNum(int[][] isConnected) {
        int cities = isConnected.length;
        int province = 0;
        Queue<Integer> queue = new LinkedList<>();
        boolean[] visited = new boolean[cities];
        for (int i = 0; i < cities; i++) {
            if(!visited[i]){
                queue.offer(i);
                while (!queue.isEmpty()){
                    int j = queue.poll();
                    visited[j] = true;
                    for (int k = 0; k < cities; k++) {
                        if(isConnected[j][k] == 1 && !visited[k]){
                            visited[k] = true;
                            queue.offer(k);
                        }
                    }
                }
                province++;
            }
        }
        return province;
    }
}
