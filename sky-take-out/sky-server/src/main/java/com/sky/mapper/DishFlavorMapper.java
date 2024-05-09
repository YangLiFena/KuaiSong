package com.sky.mapper;


import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
    /**
     * 批量插入口味数据（会用到动态sql,所以写到xml映射文件中）
     * @param flavors
     */
    void insertBatch(List<DishFlavor> flavors);
}
