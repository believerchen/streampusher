package com.believer.magicfilter.filter.helper;

import com.believer.magicfilter.filter.advanced.MagicBeautyFilter;
import com.believer.magicfilter.filter.base.gpuimage.GPUImageFilter;


/**
 * 滤镜简单工厂
 *
 * @author Created by jz on 2017/5/2 16:56
 */
public class MagicFilterFactory {

    public static GPUImageFilter initFilters(MagicFilterType type) {
        switch (type) {
            case NONE:
                return new GPUImageFilter() ;
            case BEAUTY:
                return new MagicBeautyFilter();
            default:
                return null;
        }
    }
}
