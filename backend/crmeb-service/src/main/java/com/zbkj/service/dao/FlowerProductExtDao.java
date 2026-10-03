package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.flower.FlowerProductExt;
import com.zbkj.common.request.flower.FlowerProductSearchRequest;
import com.zbkj.common.response.flower.FlowerProductCardResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FlowerProductExtDao extends BaseMapper<FlowerProductExt> {
    List<FlowerProductCardResponse> searchOnSale(@Param("request") FlowerProductSearchRequest request);
    List<FlowerProductCardResponse> searchAdmin(@Param("request") FlowerProductSearchRequest request,
                                               @Param("warningStock") Integer warningStock);
    int countAdmin(@Param("request") FlowerProductSearchRequest request,
                   @Param("warningStock") Integer warningStock);
    List<FlowerProductCardResponse> filterSources(@Param("onSale") boolean onSale);
    int countByProductIds(@Param("productIds") List<Integer> productIds);
}
