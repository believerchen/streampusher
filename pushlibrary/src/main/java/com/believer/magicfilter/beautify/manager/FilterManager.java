package com.believer.magicfilter.beautify.manager;




import com.believer.magicfilter.beautify.filter.GLDisplayFilter;
import com.believer.magicfilter.beautify.filter.GLImageFilter;
import com.believer.magicfilter.beautify.filter.GLRealtimeBeautyFilter;
import com.believer.magicfilter.beautify.filter.WhitenOrReddenFilter;
import com.believer.magicfilter.beautify.group.GLDefaultFilterGroup;
import com.believer.magicfilter.beautify.group.GLImageFilterGroup;
import com.believer.magicfilter.beautify.utils.GLFilterIndex;
import com.believer.magicfilter.beautify.utils.type.GLFilterGroupType;
import com.believer.magicfilter.beautify.utils.type.GLFilterType;

import java.util.HashMap;

/**
 * Filter管理类
 * Created by cain on 17-7-25.
 */

public final class FilterManager {

    private static HashMap<GLFilterType, GLFilterIndex> mIndexMap = new HashMap<GLFilterType, GLFilterIndex>();
    static {
        mIndexMap.put(GLFilterType.NONE, GLFilterIndex.NoneIndex);
        // 美颜
        mIndexMap.put(GLFilterType.REALTIMEBEAUTY, GLFilterIndex.BeautyIndex);
        // 红润
        mIndexMap.put(GLFilterType.WHITENORREDDEN, GLFilterIndex.BeautyIndex);
    }

    private FilterManager() {}

    public static GLImageFilter getFilter(GLFilterType type) {
        switch (type) {
            // 白皙还是红润
            case WHITENORREDDEN:
                return new WhitenOrReddenFilter();
            // 实时磨皮
            case REALTIMEBEAUTY:
                return new GLRealtimeBeautyFilter();
            case NONE:      // 没有滤镜
            case SOURCE:    // 原图
            default:
                return new GLDisplayFilter();
        }
    }

    /**
     * 获取滤镜组
     * @return
     */
    public static GLImageFilterGroup getFilterGroup() {
        return new GLDefaultFilterGroup();
    }

    public static GLImageFilterGroup getFilterGroup(GLFilterGroupType type) {
        switch (type) {
            // 默认滤镜组
            case DEFAULT:
            default:
                return new GLDefaultFilterGroup();
        }
    }

    /**
     * 获取层级
     * @param Type
     * @return
     */
    public static GLFilterIndex getIndex(GLFilterType Type) {
        GLFilterIndex index = mIndexMap.get(Type);
        if (index != null) {
            return index;
        }
        return GLFilterIndex.NoneIndex;
    }
}
