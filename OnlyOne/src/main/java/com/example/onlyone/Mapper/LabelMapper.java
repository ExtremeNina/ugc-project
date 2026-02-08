package com.example.onlyone.Mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.onlyone.Entity.Label;

import java.util.List;

public interface LabelMapper extends BaseMapper<Label> {

    List<Label> selectLabelList(List<Long> labelIdList);
}
