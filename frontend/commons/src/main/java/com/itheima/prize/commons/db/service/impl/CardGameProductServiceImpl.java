package com.itheima.prize.commons.db.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.prize.commons.db.entity.CardGameProduct;
import com.itheima.prize.commons.db.entity.CardProductDto;
import com.itheima.prize.commons.db.mapper.CardGameProductMapper;
import com.itheima.prize.commons.db.service.CardGameProductService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
* @author shawn
* @description 针对表【card_game_product】的数据库操作Service实现
* @createDate 2023-12-26 11:58:48
*/
@Service
public class CardGameProductServiceImpl extends ServiceImpl<CardGameProductMapper, CardGameProduct>
    implements CardGameProductService{

    @Autowired
    private CardGameProductMapper cardGameProductMapper;

    @Override
    public List<CardProductDto> getByGameId(Integer gameId) {
        // 根据gameId查询产品
        QueryWrapper<CardGameProduct> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("game_id",gameId);
        List<CardProductDto> list = new ArrayList<>();
        // 对象拷贝到CardProductDto
        cardGameProductMapper.selectList(queryWrapper).forEach(cardGameProduct -> {
            CardProductDto cardProductDto = new CardProductDto();
            BeanUtils.copyProperties(cardGameProduct,cardProductDto);
            list.add(cardProductDto);
        });
       return list;
    }
}




