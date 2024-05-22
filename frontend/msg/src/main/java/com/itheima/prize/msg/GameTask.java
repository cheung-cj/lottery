package com.itheima.prize.msg;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itheima.prize.commons.config.RedisKeys;
import com.itheima.prize.commons.db.entity.*;
import com.itheima.prize.commons.db.service.CardGameProductService;
import com.itheima.prize.commons.db.service.CardGameRulesService;
import com.itheima.prize.commons.db.service.CardGameService;
import com.itheima.prize.commons.db.service.GameLoadService;
import com.itheima.prize.commons.utils.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 活动信息预热，每隔1分钟执行一次
 * 查找未来1分钟内（含），要开始的活动
 */
@Component
public class GameTask {
    private final static Logger log = LoggerFactory.getLogger(GameTask.class);
    @Autowired
    private CardGameService gameService;
    @Autowired
    private CardGameProductService gameProductService;
    @Autowired
    private CardGameRulesService gameRulesService;
    @Autowired
    private GameLoadService gameLoadService;
    @Autowired
    private RedisUtil redisUtil;

    @Scheduled(cron = "0 * * * * ?")
    public void execute() {
        System.out.printf("scheduled!" + new Date());
        Date now = new Date();
        // （开始时间 > 当前时间）&& （开始时间 <= 当前时间+1分钟）
        LambdaQueryWrapper<CardGame> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.gt(CardGame::getStarttime, now)
                .le(CardGame::getStarttime, DateUtil.offsetMinute(now, 1));
        List<CardGame> cardGameList = gameService.list(queryWrapper);
        // 无预热活动
        if (cardGameList.isEmpty()) {
            return;
        }
        cardGameList.forEach(cardGame -> {
            // 开始时间
            long start = cardGame.getStarttime().getTime();
            // 结束时间
            long end = cardGame.getEndtime().getTime();
            // 设置缓存过期时间
            long expire = (end - now.getTime()) / 1000;
            // 活动持续时间
            long dur = end - start;
            // 活动加载标志 =》 不可修改
            cardGame.setStatus(1);
            redisUtil.set(RedisKeys.INFO + cardGame.getId(), cardGame, -1);

            // 加载活动的奖品
            List<CardProductDto> productDtoList = gameLoadService.getByGameId(cardGame.getId());
            Map<Integer, CardProduct> map = new HashMap<>();
            productDtoList.forEach(p -> map.put(p.getId(), p));

            // 奖品数量
            LambdaQueryWrapper<CardGameProduct> productQueryWrapper = new LambdaQueryWrapper<>();
            productQueryWrapper.eq(CardGameProduct::getGameid, cardGame.getId());
            List<CardGameProduct> productList = gameProductService.list(productQueryWrapper);

            // 令牌桶
            List<Long> tokenList = new ArrayList<>();
            productList.forEach(cgp -> {
                // 有多少个奖品 就有多少个令牌
                for (int i = 0; i < cgp.getAmount(); i++) {
                    // 令牌key 活动期间中的某个时刻
                    long rnd = start + new Random().nextInt((int) dur);
                    // 优化：(二次随机)防止时间段奖品多时重复
                    long token = rnd * 1000 + new Random().nextInt(999);
                    // 放入令牌桶
                    tokenList.add(token);
                    // 放入redis
                    redisUtil.set(RedisKeys.TOKEN + cardGame.getId() + "_" + token, map.get(cgp.getProductid()), expire);
                }
            });

            // 令牌排序 => 按时间戳
            Collections.sort(tokenList);
            // 从右侧压入队列，从左到右
            redisUtil.rightPushAll(RedisKeys.TOKENS + cardGame.getId(), tokenList);
            redisUtil.expire(RedisKeys.TOKENS + cardGame.getId(), expire);

            // 奖品策略配置信息
            QueryWrapper<CardGameRules> rulesQueryWrapper = new QueryWrapper<>();
            rulesQueryWrapper.eq("gameid", cardGame.getId());
            List<CardGameRules> rulesList = gameRulesService.list(rulesQueryWrapper);
            rulesList.forEach(rule -> {
                // 最大中奖次数
                redisUtil.hset(RedisKeys.MAXGOAL + cardGame.getId(), rule.getUserlevel() + "", rule.getGoalTimes());
                // 可抽奖次数
                redisUtil.hset(RedisKeys.MAXENTER + cardGame.getId(), rule.getUserlevel() + "", rule.getEnterTimes());
            });
            // 设置存活时间
            redisUtil.expire(RedisKeys.MAXGOAL + cardGame.getId(), expire);
            redisUtil.expire(RedisKeys.MAXENTER + cardGame.getId(), expire);

            // 更新status
            gameService.updateById(cardGame);
        });
    }
}