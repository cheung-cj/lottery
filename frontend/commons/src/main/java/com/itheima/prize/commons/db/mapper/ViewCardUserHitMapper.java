package com.itheima.prize.commons.db.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.prize.commons.db.entity.ViewCardUserHit;

/**
* @author shawn
* @description 针对表【view_card_user_hit】的数据库操作Mapper
* @createDate 2023-12-26 11:58:48
* @Entity com.itheima.prize.commons.db.entity.ViewCardUserHit
*/
public interface ViewCardUserHitMapper extends BaseMapper<ViewCardUserHit> {


    int queryGamesById(Integer userid);
}




