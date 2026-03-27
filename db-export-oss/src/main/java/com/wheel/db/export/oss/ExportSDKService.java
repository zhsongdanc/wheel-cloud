package com.wheel.db.export.oss;

import com.wheel.db.export.oss.enums.DbType;

import java.util.List;

public abstract class ExportSDKService<T> {



    public void export() {
        // 1. 根据用户配置的数据库类型、数据库连接信息、表名、查询字段查询一批数据
        // 2. 将查询的数据加入buffer中，如果达到minPartFileSize，开始分片上传
        // 3. 循环1、2步骤，直到查询完所有数据


    }


    // TODO 我应该把超时时间这个参数放进去吗？
    // TODO 添加minPartFileSize参数是因为腾讯云/阿里云的OSS可能对分片文件最小限制不同，但我不知道参数放这里是否合适
    public abstract List<T> queryList(DbType dbType, Integer lastId, Integer batchSize, Integer minPartFileSize);

    public abstract void exportFile();

}
