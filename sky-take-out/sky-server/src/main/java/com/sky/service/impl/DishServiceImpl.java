package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
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

    /**
     * 根据类别id查询菜品列表
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE) // 默认为停售状态
                .build();
        return dishMapper.list(dish);
    }

    /**
     * 起售或停售菜品
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        // TODO 修改套菜品的status字段
        //停售菜品时，判断是否有套餐包含该菜品，否则无法停售"
        if(status == StatusConstant.DISABLE){
            // 根据id查询菜品数据
            Dish dish = dishMapper.getById(id);
            // 根据菜品id查询dishID
            Long dishId = dish.getId();
            Long num = dishMapper.CountByDishId(dishId);
            if(num != 0){
                throw new SetmealEnableFailedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
            }
        }
        // 使用构建器 即@builder注解
        Dish dish = Dish.builder()
                .status(status)
                .id(id)
                .build();

        dishMapper.update(dish); //传实体类达到传不同字段的目的
    }


    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}
