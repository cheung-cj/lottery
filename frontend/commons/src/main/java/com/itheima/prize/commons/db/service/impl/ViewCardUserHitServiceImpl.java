package com.itheima.prize.commons.db.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.prize.commons.db.entity.CardUser;
import com.itheima.prize.commons.db.entity.CardUserDto;
import com.itheima.prize.commons.db.entity.ViewCardUserHit;
import com.itheima.prize.commons.db.mapper.ViewCardUserHitMapper;
import com.itheima.prize.commons.db.service.GameLoadService;
import com.itheima.prize.commons.db.service.ViewCardUserHitService;
import com.itheima.prize.commons.utils.ApiResult;
import com.itheima.prize.commons.utils.PageBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * @author shawn
 * @description 针对表【view_card_user_hit】的数据库操作Service实现
 * @createDate 2023-12-26 11:58:48
 */
@Service
public class ViewCardUserHitServiceImpl extends ServiceImpl<ViewCardUserHitMapper, ViewCardUserHit>
        implements ViewCardUserHitService {

    @Autowired
    private ViewCardUserHitMapper viewCardUserHitMapper;

    @Autowired
    private GameLoadService loadService;

    @Override
    public ApiResult info(HttpSession session) {
        // 1、从session获取用户信息
        CardUser user = (CardUser) session.getAttribute("user");
        if (user == null) {
            return new ApiResult(0, "登录超时", null);
        }
        // 2、拷贝用户数据
        CardUserDto cardUserDto = new CardUserDto(user);
        cardUserDto.setGames(loadService.getGamesNumByUserId(user.getId()));
        cardUserDto.setProducts(loadService.getPrizesNumByUserId(user.getId()));
        return new ApiResult(1, "成功", cardUserDto);
    }

    /**
     * 当前用户所有奖品
     * @param session 会话
     * @param gameid 活动id
     * @param curpage 当前页
     * @param limit  每页限制
     * @return
     */
    @Override
    public ApiResult hit(HttpSession session, int gameid, int curpage, int limit) {
        // 1、从session获取用户信息
        CardUser user = (CardUser) session.getAttribute("user");
        if (user == null) {
            return new ApiResult(0, "登录超时", null);
        }
        // 2、添加过滤条件
        QueryWrapper<ViewCardUserHit> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userid", user.getId())
                .eq(gameid != -1, "gameid", gameid);
        // 3、分页查询
        Page<ViewCardUserHit> pageInfo = viewCardUserHitMapper.selectPage(new Page<>(curpage, limit), queryWrapper);
        return new ApiResult(1, "成功", new PageBean<ViewCardUserHit>(pageInfo));
    }

    /**
     * 获取当前活动，所有中奖用户
     * @param gameid 活动id
     * @param curpage 当前页
     * @param limit 每页限制
     * @return
     */
    @Override
    public ApiResult<PageBean<ViewCardUserHit>> gameHitByGameId(int gameid, int curpage, int limit) {
        // 增加查询条件
        QueryWrapper<ViewCardUserHit> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("gameid", gameid);
        // 2、分页查询
        Page<ViewCardUserHit> pageInfo = viewCardUserHitMapper.selectPage(new Page<>(curpage, limit), queryWrapper);
        return new ApiResult(1, "成功", new PageBean<ViewCardUserHit>(pageInfo));
    }


//        // 2.1、额外数据: 参与的活动数，中奖数
//        LambdaQueryWrapper<ViewCardUserHit> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(ViewCardUserHit::getUserid,user.getId());
//        Long products = viewCardUserHitMapper.selectCount(queryWrapper);
//        cardUserDto.setProducts(Math.toIntExact(products));
//        cardUserDto.setGames(viewCardUserHitMapper.queryGamesById(user.getId()));


}




