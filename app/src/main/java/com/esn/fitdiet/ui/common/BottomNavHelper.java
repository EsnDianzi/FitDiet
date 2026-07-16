package com.esn.fitdiet.ui.common;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.esn.fitdiet.R;
import com.esn.fitdiet.ui.battle.BattleActivity;
import com.esn.fitdiet.ui.diet.FoodRecognitionActivity;
import com.esn.fitdiet.ui.home.HomeActivity;
import com.esn.fitdiet.ui.profile.ProfileActivity;
import com.esn.fitdiet.ui.stats.StatsActivity;

/**
 * 底部导航栏通用绑定工具。
 * 用法：BottomNavHelper.bind(activity, viewBinding) 一行搞定。
 *
 * <p>行为：
 * <ul>
 *   <li>当前页对应的 tab 高亮（accent 绿，图标 + 文字同步变色）</li>
 *   <li>点击 tab 跳转：目标 = 当前页 → 消费；其他页 → startActivity + finish()</li>
 * </ul>
 */
public final class BottomNavHelper {

    private BottomNavHelper() { }

    /** 5 个 tab 标签，按 home/diet/train/stats/profile 顺序。 */
    private enum Tab { HOME, DIET, TRAIN, STATS, PROFILE }

    /** 当前 Activity 绑定 5 个 tab 的点击事件 + 高亮当前页。 */
    public static void bind(Activity activity, View navBar) {
        if (navBar == null) return;

        LinearLayout btnHome = navBar.findViewById(R.id.btnTabHome);
        LinearLayout btnDiet = navBar.findViewById(R.id.btnTabDiet);
        LinearLayout btnTrain = navBar.findViewById(R.id.btnTabTrain);
        LinearLayout btnStats = navBar.findViewById(R.id.btnTabStats);
        LinearLayout btnProfile = navBar.findViewById(R.id.btnTabProfile);

        // 高亮当前 tab（图标 + 文字同步变色）
        Tab current = detectCurrent(activity);
        highlight(btnHome, current == Tab.HOME);
        highlight(btnDiet, current == Tab.DIET);
        highlight(btnTrain, current == Tab.TRAIN);
        highlight(btnStats, current == Tab.STATS);
        highlight(btnProfile, current == Tab.PROFILE);

        // 绑定点击
        btnHome.setOnClickListener(v -> go(activity, HomeActivity.class, Tab.HOME, current));
        btnDiet.setOnClickListener(v -> go(activity, FoodRecognitionActivity.class, Tab.DIET, current));
        btnTrain.setOnClickListener(v -> go(activity, BattleActivity.class, Tab.TRAIN, current));
        btnStats.setOnClickListener(v -> go(activity, StatsActivity.class, Tab.STATS, current));
        btnProfile.setOnClickListener(v -> go(activity, ProfileActivity.class, Tab.PROFILE, current));
    }

    private static Tab detectCurrent(Activity a) {
        if (a instanceof HomeActivity) return Tab.HOME;
        if (a instanceof FoodRecognitionActivity) return Tab.DIET;
        if (a instanceof BattleActivity) return Tab.TRAIN;
        if (a instanceof StatsActivity) return Tab.STATS;
        if (a instanceof ProfileActivity) return Tab.PROFILE;
        return Tab.HOME;
    }

    private static void go(Activity from, Class<? extends Activity> target, Tab targetTab, Tab current) {
        if (targetTab == current) {
            // 已在当前页：点自己不下沉，只消费
            return;
        }
        Intent i = new Intent(from, target);
        // 清理栈顶，让返回行为符合「切换 tab」
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        from.startActivity(i);
        from.finish();
    }

    /**
     * 高亮 / 取消高亮 tab：
     * - 激活：图标 + 文字均变 accent 绿
     * - 未激活：图标 + 文字均为三级灰
     */
    private static void highlight(LinearLayout tab, boolean active) {
        if (tab == null) return;
        int color = ContextCompat.getColor(tab.getContext(),
                active ? R.color.apple_accent : R.color.apple_text_tertiary);
        // tab 内有 1 个 ImageView + 1 个 TextView
        for (int i = 0; i < tab.getChildCount(); i++) {
            View child = tab.getChildAt(i);
            if (child instanceof ImageView) {
                ImageViewCompat.setImageTintList((ImageView) child, ColorStateList.valueOf(color));
            } else if (child instanceof TextView) {
                ((TextView) child).setTextColor(color);
            }
        }
    }
}
