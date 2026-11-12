package com.tyut.studyroombookingcheckin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyut.studyroombookingcheckin.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {

    /**
     * 统计用户名 / 学号占用情况（包含已逻辑删除的账号，避免唯一索引冲突）
     *
     * @param value     待校验的用户名
     * @param excludeId 需要排除的用户 ID，新增时传 null
     */
    @Select("SELECT COUNT(*) FROM t_user WHERE " +
            "(#{excludeId,jdbcType=BIGINT} IS NULL OR id <> #{excludeId,jdbcType=BIGINT}) " +
            "AND (username = #{value} OR student_no = #{value})")
    int countUsernameOrStudentNo(@Param("value") String value, @Param("excludeId") Long excludeId);

    /**
     * 统计除指定用户外，仍处于启用状态的管理员数量
     */
    @Select("SELECT COUNT(*) FROM t_user WHERE deleted = 0 AND role = 1 AND status = 1 AND id <> #{excludeId}")
    int countEnabledAdminsExcluding(@Param("excludeId") Long excludeId);
}
