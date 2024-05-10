package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
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

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }


    /**
     * 菜品批量删除
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 判断当前菜品是否能够删除--是否存在起售中的菜品
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                // 如果是起售中，抛出业务异常
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
            // 判断当前菜品是否能够删除--是否被套餐关联
            List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
            if (setmealIds != null && setmealIds.size() > 0){
                // 当前菜品关联了套餐不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
            }

            // 删除菜品数据-方法1
//            for (Long idd: ids) {
//                dishMapper.deleteById(idd);
//                // 删除菜品关联的口味数据
//                dishFlavorMapper.deleteByDishId(idd);
//            }

            // delete from dish where id in (?,?,...) //批量删除
            // 删除菜品数据-方法2（避免方法1中可能会发生的，发出过多sql语句的情况(因为有for循环)--提升性能）
            dishMapper.deleteByIds(ids);
            // delete from dish_flavor where dishid in (?,?,...) //批量删除
            dishFlavorMapper.deleteByDishIds(ids);
        }
    }

    /**
     * 根据id查询菜品和关联的口味数据
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {
        // 根据id查询菜品数据
        Dish dish = dishMapper.getById(id);
        // 根据菜品id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        // 将查询到的数据封装到VO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 根据id修改菜品基本信息和对应的口味信息
     * @param dishDTO
     */
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        // 修改菜品基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);
        // 先删除当前菜品原先关联的口味数据，再插入新的口味数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            //向口味表dish_flavor插入n条
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            //批量插入
            dishFlavorMapper.insertBatch(flavors);
        }

    }
}
