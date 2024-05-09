package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) { // @Transactional 此处设计多张表的修改，使用该注解保证方法的原子性
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish); // 对象属性拷贝
        //向菜品表dish插入1条数据
        dishMapper.insert(dish);
        //获取菜品的主键值
        Long dishId = dish.getId(); // 通过DishMapper.xml文件中的useGeneratedKeys:true实现
        List<DishFlavor> flavors = dishDTO.getFlavors(); // 拿到口味数据的集合
        if(flavors != null && flavors.size() > 0){ // 用户提交的口味数据可以为空
        //向口味表dish_flavor插入n条, 需要一个新的Mapper,批量插入，直接传入集合
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
        //批量插入
            dishFlavorMapper.insertBatch(flavors);
        }
    }
}
