package com.itheima.prize.api.action;

import cn.hutool.core.date.DateUtil;
import com.itheima.prize.api.config.LuaScript;
import com.itheima.prize.commons.config.RabbitKeys;
import com.itheima.prize.commons.config.RedisKeys;
import com.itheima.prize.commons.db.entity.*;
import com.itheima.prize.commons.utils.ApiResult;
import com.itheima.prize.commons.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/act")
@Api(tags = {"抽奖模块"})
public class ActController {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private LuaScript luaScript;


    @Value("${picture.oldUrl}")
    private String oldUrl;

    @Value("${picture.newUrl}")
    private String newUrl;

    @GetMapping("/limits/{gameid}")
    @ApiOperation(value = "剩余次数")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "gameid", value = "活动id", example = "1", required = true)
    })
    public ApiResult<Object> limits(@PathVariable int gameid, HttpServletRequest request) {
        //获取活动基本信息
        CardGame game = (CardGame) redisUtil.get(RedisKeys.INFO + gameid);
        if (game == null) {
            return new ApiResult<>(-1, "活动未加载", null);
        }
        //获取当前用户
        HttpSession session = request.getSession();
        CardUser user = (CardUser) session.getAttribute("user");
        if (user == null) {
            return new ApiResult(-1, "未登陆", null);
        }
        //用户可抽奖次数
        Integer enter = (Integer) redisUtil.get(RedisKeys.USERENTER + gameid + "_" + user.getId());
        if (enter == null) {
            enter = 0;
        }
        //根据会员等级，获取本活动允许的最大抽奖次数
        Integer maxenter = (Integer) redisUtil.hget(RedisKeys.MAXENTER + gameid, user.getLevel() + "");
        //如果没设置，默认为0，即：不限制次数
        maxenter = maxenter == null ? 0 : maxenter;

        //用户已中奖次数
        Integer count = (Integer) redisUtil.get(RedisKeys.USERHIT + gameid + "_" + user.getId());
        if (count == null) {
            count = 0;
        }
        //根据会员等级，获取本活动允许的最大中奖数
        Integer maxcount = (Integer) redisUtil.hget(RedisKeys.MAXGOAL + gameid, user.getLevel() + "");
        //如果没设置，默认为0，即：不限制次数
        maxcount = maxcount == null ? 0 : maxcount;

        //幸运转盘类，先给用户随机剔除，再获取令牌，有就中，没有就说明抢光了
        //一般这种情况会设置足够的商品，卡在随机上
        Integer randomRate = (Integer) redisUtil.hget(RedisKeys.RANDOMRATE + gameid, user.getLevel() + "");
        if (randomRate == null) {
            randomRate = 100;
        }

        Map map = new HashMap();
        map.put("maxenter", maxenter);
        map.put("enter", enter);
        map.put("maxcount", maxcount);
        map.put("count", count);
        map.put("randomRate", randomRate);

        return new ApiResult<>(1, "成功", map);
    }


    @GetMapping("/go/{gameid}")
    @ApiOperation(value = "抽奖")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "gameid", value = "活动id", example = "1", required = true)})
    public ApiResult<Object> act(@PathVariable int gameid, HttpServletRequest request) {
        // 1、获取当前活动
        CardGame cardGame = (CardGame) redisUtil.get(RedisKeys.INFO + gameid);
        // 2、判断活动状态
        Date now = new Date();
        if (cardGame == null || cardGame.getStarttime().getTime() > now.getTime()) {
            return new ApiResult<>(-1, "活动未开始", null);
        }
        if (cardGame.getEndtime().getTime() < now.getTime()) {
            return new ApiResult<>(-1, "活动已结束", null);
        }
        // 活动持续时间
        long dur = (cardGame.getEndtime().getTime() - cardGame.getStarttime().getTime()) / 1000;

        // 3、获取当前用户
        CardUser user = (CardUser) request.getSession().getAttribute("user");
        if (user == null) {
            return new ApiResult<>(-1, "未登录", null);
        }

        // 4、判断用户有无参与过活动
        if (!redisUtil.hasKey(RedisKeys.USERENTER + user.getId() + "_" + gameid)) {
            // 第一次参与抽奖
            redisUtil.set(RedisKeys.USERENTER + user.getId() + "_" + gameid, 1, dur);
            // 发送异步消息 => mq 新增数据
            CardUserGame cardUserGame = new CardUserGame();
            cardUserGame.setUserid(user.getId());
            cardUserGame.setGameid(gameid);
            cardUserGame.setCreatetime(now);
            rabbitTemplate.convertAndSend(RabbitKeys.QUEUE_PLAY, cardUserGame);
        }

        // 5、判断用户中奖次数
        Integer userTimes = (Integer) redisUtil.get(RedisKeys.USERHIT + gameid + "_" + user.getId());
        if (userTimes == null) {
            userTimes = 0;
            redisUtil.set(RedisKeys.USERHIT + gameid + "_" + user.getId(), userTimes, dur);
        }
        // 6、获取最大中奖数
        Integer maxTimes = (Integer) redisUtil.hget(RedisKeys.MAXGOAL + gameid, user.getLevel() + "");
        // 不存在，则无限次
        if (maxTimes == null) {
            maxTimes = 0;
        }
        if (maxTimes > 0 && userTimes >= maxTimes) {
            return new ApiResult<>(-1, "您已达到最大中奖数", null);
        }
        // 7、利用lua判断中奖
        Long token = luaScript.tokenCheck(RedisKeys.TOKENS + gameid, String.valueOf(now.getTime()));
        if (token == 0) {
            return new ApiResult<>(-1, "奖已抽完", null);
        } else if (token == 1) {
            return new ApiResult<>(0, "未中奖", null);
        }
        // 中奖
        CardProduct cardProduct = (CardProduct) redisUtil.get(RedisKeys.TOKEN + gameid + "_" + token);
        cardProduct.setPic(cardProduct.getPic().replace(oldUrl, newUrl));
        // 用户次数+1
        redisUtil.incr(RedisKeys.USERHIT + gameid + "_" + user.getId(), 1);
        // 发送异步消息
        CardUserHit cardUserHit = new CardUserHit();
        cardUserHit.setGameid(gameid);
        cardUserHit.setUserid(user.getId());
        cardUserHit.setProductid(cardProduct.getId());
        cardUserHit.setHittime(now);
        rabbitTemplate.convertAndSend(RabbitKeys.EXCHANGE_DIRECT, RabbitKeys.QUEUE_HIT, cardUserHit);
        return new ApiResult<>(1, "恭喜中奖", cardProduct);
    }

    @GetMapping("/info/{gameid}")
    @ApiOperation(value = "缓存信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "gameid", value = "活动id", example = "1", required = true)})
    public ApiResult info(@PathVariable int gameid) {
        Map<String, Object> data = new HashMap<>();
        CardGame cardGame = (CardGame) redisUtil.get(RedisKeys.INFO + gameid);
        cardGame.setPic(cardGame.getPic().replace(oldUrl, newUrl));
        if (cardGame != null) {
            // 封装令牌
            Map<String, Object> tokenMap = new HashMap<>();
            List<Object> list = redisUtil.lrange(RedisKeys.TOKENS + gameid, 0, -1);
            // 判空
            if (list != null && !list.isEmpty()) {
                list.forEach(token -> {
                    CardProduct cardProduct = (CardProduct) redisUtil.get(RedisKeys.TOKEN + gameid + "_" + token);
                    cardProduct.setPic(cardProduct.getPic().replace(oldUrl, newUrl));
                    String key = DateUtil.format(DateUtil.date((Long) token / 1000), "yyyy-MM-dd HH:mm:ss.SSS");
                    tokenMap.put(key, cardProduct);
                });
            }
            // 获取活动详情
            data.put(RedisKeys.INFO + gameid, cardGame);
            // 获取所有令牌
            data.put(RedisKeys.TOKENS + gameid, tokenMap);
            // 获取奖品策略
            data.put(RedisKeys.MAXGOAL + gameid, redisUtil.hmget(RedisKeys.MAXGOAL + gameid));
            data.put(RedisKeys.MAXENTER + gameid, redisUtil.hmget(RedisKeys.MAXENTER + gameid));
        }
        return new ApiResult<>(200, "缓存信息", data);
    }
}
