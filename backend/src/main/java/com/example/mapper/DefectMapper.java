package com.example.mapper;

import com.example.entity.Defect;
import java.util.List;

public interface DefectMapper {

    List<Defect> selectAll();

    List<Defect> selectByFilters(Defect defect);

    Defect selectById(Integer id);

    int insertDefect(Defect defect); // 使用 useGeneratedKeys 回填 id

    int updateDefect(Defect defect);

    int deleteDefect(Integer id);

}
