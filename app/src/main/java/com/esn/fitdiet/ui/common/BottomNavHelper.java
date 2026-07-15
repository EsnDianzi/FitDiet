package com.esn.fitdiet.ui.common;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

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
 *   <li>当前页对应的 tab 高亮（accent 绿）</li>
 *   <li>点击 tab 跳转：目标 = 当前页 → finish()；其他页 → startActivity + finish()</li>
 * </ul>
 */
public final class BottomNavHelper {

    private BottomNavHelper() { }

    /** 5 个 tab 标签 TextView，按 home/diet/train/stats/profile 顺序。 */
    private enum Tab { HOME, DIET, TRAIN, STATS, PROFILE }

    /** 当前 Activity 绑定 5 个 tab 的点击事件 + 高亮当前页。 */
    public static void bind(Activity activity, View navBar) {
        if (navBar == null) return;
        TextView btnHome = navBar.findViewById(R.id.btnTabHome);
        TextView btnDiet = navBar.findViewById(R.id.btnTabDiet);
        TextView btnTrain = navBar.findViewById(R.id.btnTabTrain);
        TextView btnStats = navBar.findViewById(R.id.btnTabStats);
        TextView btnProfile = navBar.findViewById(R.id.btnTabProfile);

        // 高亮当前 tab
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

    private static void highlight(TextView tv, boolean active) {
        tv.setTextColor(active
                ? tv.getResources().getColor(R.color.apple_accent)
                : tv.getResources().getColor(R.color.apple_text_tertiary));
    }
}
