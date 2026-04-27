package com.wheel.cloud.seata.demo.web;

import com.wheel.cloud.seata.core.model.BranchSession;
import com.wheel.cloud.seata.core.model.GlobalSession;
import com.wheel.cloud.seata.rm.ResourceManager;
import com.wheel.cloud.seata.tm.TransactionManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/seata")
public class SeataDemoController {

    private final TransactionManager transactionManager;

    private final ResourceManager resourceManager;

    public SeataDemoController(TransactionManager transactionManager, ResourceManager resourceManager) {
        this.transactionManager = transactionManager;
        this.resourceManager = resourceManager;
    }

    @GetMapping("/demo/begin-and-commit")
    public Map<String, Object> beginAndCommit(@RequestParam(defaultValue = "demo-service") String applicationId,
                                              @RequestParam(defaultValue = "create-order") String transactionName) {
        GlobalSession globalSession = transactionManager.begin(applicationId, transactionName);
        BranchSession branchOne = resourceManager.registerBranch(globalSession.getXid(), "order-db", "AT");
        BranchSession branchTwo = resourceManager.registerBranch(globalSession.getXid(), "stock-db", "AT");
        GlobalSession committed = transactionManager.commit(globalSession.getXid());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("xid", committed.getXid());
        response.put("globalStatus", committed.getStatus());
        response.put("branchCount", committed.getBranches().size());
        response.put("branchOneId", branchOne.getBranchId());
        response.put("branchTwoId", branchTwo.getBranchId());
        return response;
    }

    @GetMapping("/demo/begin-and-rollback")
    public Map<String, Object> beginAndRollback(@RequestParam(defaultValue = "demo-service") String applicationId,
                                                @RequestParam(defaultValue = "create-order") String transactionName) {
        GlobalSession globalSession = transactionManager.begin(applicationId, transactionName);
        BranchSession branchOne = resourceManager.registerBranch(globalSession.getXid(), "order-db", "AT");
        BranchSession branchTwo = resourceManager.registerBranch(globalSession.getXid(), "stock-db", "AT");
        GlobalSession rolledBack = transactionManager.rollback(globalSession.getXid());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("xid", rolledBack.getXid());
        response.put("globalStatus", rolledBack.getStatus());
        response.put("branchCount", rolledBack.getBranches().size());
        response.put("branchOneId", branchOne.getBranchId());
        response.put("branchTwoId", branchTwo.getBranchId());
        return response;
    }
}
