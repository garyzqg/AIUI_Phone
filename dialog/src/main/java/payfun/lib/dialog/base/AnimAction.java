package payfun.lib.dialog.base;

import payfun.lib.dialog.R;

/**
 * @author : zhangqg
 * date   : 2023/3/1 15:30
 * desc   : <功能简述>
 */
public interface AnimAction {

    /**
     * 默认动画效果
     */
    int ANIM_DEFAULT = -1;

    /**
     * 没有动画效果
     */
    int ANIM_EMPTY = 0;

    /**
     * 缩放动画
     */
    int ANIM_SCALE = R.style.ScaleAnimStyle;

    /**
     * IOS 动画
     */
    int ANIM_IOS = R.style.IOSAnimStyle;

    /**
     * 吐司动画
     */
    int ANIM_TOAST = android.R.style.Animation_Toast;

    /**
     * 顶部弹出动画
     */
    int ANIM_TOP = R.style.TopAnimStyle;

    /**
     * 底部弹出动画
     */
    int ANIM_BOTTOM = R.style.BottomAnimStyle;

    /**
     * 左边弹出动画
     */
    int ANIM_LEFT = R.style.LeftAnimStyle;

    /**
     * 右边弹出动画
     */
    int ANIM_RIGHT = R.style.RightAnimStyle;
}
