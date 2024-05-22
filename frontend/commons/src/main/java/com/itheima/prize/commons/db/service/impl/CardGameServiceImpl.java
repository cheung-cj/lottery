package com.itheima.prize.commons.db.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.prize.commons.db.entity.CardGame;
import com.itheima.prize.commons.db.entity.CardProductDto;
import com.itheima.prize.commons.db.mapper.CardGameMapper;
import com.itheima.prize.commons.db.mapper.GameLoadMapper;
import com.itheima.prize.commons.db.service.CardGameService;
import com.itheima.prize.commons.utils.ApiResult;
import com.itheima.prize.commons.utils.PageBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author shawn
 * @description 针对表【card_game】的数据库操作Service实现
 * @createDate 2023-12-26 11:58:48
 */
@Service
public class CardGameServiceImpl extends ServiceImpl<CardGameMapper, CardGame>
        implements CardGameService {

    @Autowired
    private CardGameMapper cardGameMapper;

    @Autowired
    private GameLoadMapper gameLoadMapper;

    @Value("${picture.oldUrl}")
    private String oldUrl;

    @Value("${picture.newUrl}")
    private String newUrl;


    @Override
    public ApiResult<CardGame> info(int gameid) {
        CardGame cardGame = cardGameMapper.selectById(gameid);
        cardGame.setPic(cardGame.getPic().replace(oldUrl, newUrl));
        return new ApiResult<>(1, "成功", cardGame);
    }

    /**
     * 奖品信息
     *
     * @param gameid 活动id
     * @return
     */
    @Override
    public ApiResult<List<CardProductDto>> products(int gameid) {
        List<CardProductDto> productDtoList = gameLoadMapper.getByGameId(gameid);
        productDtoList.forEach(cardProduct -> cardProduct.setPic(cardProduct.getPic().replace(oldUrl, newUrl)));
        return new ApiResult<>(1, "成功", productDtoList);
    }

    /**
     * 活动列表
     *
     * @param status  活动状态（-1=全部，0=未开始，1=进行中，2=已结束）
     * @param curpage 当前页
     * @param limit   当前页数
     * @return
     */
    @Override
    public ApiResult gameList(int status, int curpage, int limit) {
        // 1、增加查询条件
        QueryWrapper<CardGame> queryWrapper = new QueryWrapper<>();
        Date now = new Date();
        queryWrapper
                // 未开始
                .gt(status == 0, "starttime", now)
                // 进行中
                .le(status == 1, "starttime", now)
                .ge(status == 1, "endtime", now)
                // 已结束
                .lt(status == 2, "endtime", now)
                // 排序
                .orderByDesc("starttime");
        // 2、分页查询
        Page<CardGame> pageInfo = cardGameMapper.selectPage(new Page<>(curpage, limit), queryWrapper);
        PageBean<CardGame> cardGamePageBean = new PageBean<>(pageInfo);
        cardGamePageBean.getItems().forEach(item -> item.setPic(item.getPic().replace(oldUrl, newUrl)));
        return new ApiResult(1, "成功", cardGamePageBean);
    }
}




