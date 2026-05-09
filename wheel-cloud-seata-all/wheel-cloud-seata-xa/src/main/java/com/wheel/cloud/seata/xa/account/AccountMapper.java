package com.wheel.cloud.seata.xa.account;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.math.BigDecimal;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {
    @Update("UPDATE t_account SET used=used+#{money},residue=residue-#{money} WHERE user_id=#{userId} AND residue>=#{money}")
    int deduct(@Param("userId") Long userId, @Param("money") BigDecimal money);
}
