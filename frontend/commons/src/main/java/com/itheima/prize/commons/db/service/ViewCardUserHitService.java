package com.itheima.prize.commons.db.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.prize.commons.db.entity.ViewCardUserHit;
import com.itheima.prize.commons.utils.ApiResult;
import com.itheima.prize.commons.utils.PageBean;

import javax.servlet.http.HttpSession;

/**
* @author shawn
* @description 针对表【view_card_user_hit】的数据库操作Service
* @createDate 2023-12-26 11:58:48
*/
public interface ViewCardUserHitService extends IService<ViewCardUserHit> {

    ApiResult info(HttpSession session);

    ApiResult hit(HttpSession session, int gameid, int curpage, int limit);

    ApiResult<PageBean<ViewCardUserHit>> gameHitByGameId(int gameid, int curpage, int limit);
}
