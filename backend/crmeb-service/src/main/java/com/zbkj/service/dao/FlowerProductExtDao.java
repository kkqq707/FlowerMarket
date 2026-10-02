package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.flower.FlowerProductExt;
import com.zbkj.common.request.flower.FlowerProductSearchRequest;
import com.zbkj.common.response.flower.FlowerProductCardResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FlowerProductExtDao extends BaseMapper<FlowerProductExt> {
    List<FlowerProductCardResponse> searchOnSale(@Param("request") FlowerProductSearchRequest request);
    List<FlowerProductCardResponse> searchAdmin(@Param("request") FlowerProductSearchRequest request);
    int countByProductIds(@Param("productIds") List<Integer> productIds);
}
