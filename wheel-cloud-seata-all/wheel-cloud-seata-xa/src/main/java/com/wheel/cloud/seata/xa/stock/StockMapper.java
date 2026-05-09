package com.wheel.cloud.seata.xa.stock;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {
    @Update("UPDATE t_stock SET used=used+#{count},residue=residue-#{count} WHERE product_id=#{productId} AND residue>=#{count}")
    int deduct(@Param("productId") Long productId, @Param("count") Integer count);
}
