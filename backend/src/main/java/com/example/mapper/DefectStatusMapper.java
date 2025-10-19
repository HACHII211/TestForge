package com.example.mapper;

import com.example.entity.DefectStatus;
import java.util.List;

public interface DefectStatusMapper {

    /**
     * 查询全部状态（按 id 排序）
     */
    List<DefectStatus> selectAll();

    /**
     * 根据 id 查询
     */
    DefectStatus selectById(Integer id);

    /**
     * 插入新状态，返回受影响行数（建议在 XML 中使用 useGeneratedKeys 回填 id）
     */
    int insert(DefectStatus status);

    /**
     * 更新状态
     */
    int update(DefectStatus status);

    /**
     * 根据 id 删除
     */
    int deleteById(Integer id);
}
