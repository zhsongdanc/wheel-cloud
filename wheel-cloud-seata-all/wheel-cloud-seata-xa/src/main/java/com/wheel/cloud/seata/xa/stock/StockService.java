package com.wheel.cloud.seata.xa.stock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {
    private final StockMapper stockMapper;
    public StockService(StockMapper stockMapper) { this.stockMapper = stockMapper; }

    @Transactional(transactionManager = "stockTxManager")
    public void deduct(Long productId, Integer count) {
        if (stockMapper.deduct(productId, count) == 0) {
            throw new RuntimeException("库存不足: productId=" + productId);
        }
    }
}
