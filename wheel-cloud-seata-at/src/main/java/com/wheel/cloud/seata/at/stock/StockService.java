package com.wheel.cloud.seata.at.stock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final StockMapper stockMapper;

    public StockService(StockMapper stockMapper) {
        this.stockMapper = stockMapper;
    }

    @Transactional(transactionManager = "stockTxManager")
    public void deduct(Long productId, Integer count) {
        int rows = stockMapper.deduct(productId, count);
        if (rows == 0) {
            throw new RuntimeException("库存不足: productId=" + productId + ", 需要=" + count);
        }
    }
}
