package com.wheel.cloud.seata.xa.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    @Update("UPDATE t_order SET status=1 WHERE id=#{id} AND status=0")
    int complete(@Param("id") Long id);
}
