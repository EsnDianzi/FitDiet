package com.esn.fitdiet.data.local;

import com.esn.fitdiet.R;
import com.esn.fitdiet.data.local.entity.MonsterDef;
import com.esn.fitdiet.domain.model.Equipment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pre-seeded training "monsters" used by the gamified exercise screen. */
public final class MonsterPresets {

    /** 肌群 → 像素 Boss 图回退表。无对应图时回退 hero。 */
    private static final Map<String, Integer> MUSCLE_ICON_MAP = new HashMap<>();
    static {
        MUSCLE_ICON_MAP.put("胸", R.drawable.ic_pixel_boss_chest);
        MUSCLE_ICON_MAP.put("背", R.drawable.ic_pixel_boss_back);
        MUSCLE_ICON_MAP.put("腿", R.drawable.ic_pixel_boss_legs);
    }
    private static final int DEFAULT_ICON = R.drawable.ic_pixel_hero;

    private MonsterPresets() { }

    public static List<MonsterDef> all() {
        List<MonsterDef> list = new ArrayList<>();
        list.add(m(1, "胸甲虫", "胸", "平板俯卧撑消灭它", 30, 10, 1, null, true));
        list.add(m(2, "背岩魔", "背", "俯身划船挑战", 40, 12, 2, null, true));
        list.add(m(3, "腿巨兽", "腿", "徒手深蹲连击", 45, 14, 2, null, true));
        list.add(m(4, "肩峰鸟", "肩", "哑铃推举", 35, 12, 2, eq(Equipment.DUMBBELL), true));
        list.add(m(5, "臂刃客", "手臂", "哑铃弯举", 30, 10, 1, eq(Equipment.DUMBBELL), true));
        list.add(m(6, "核心蛇", "核心", "卷腹连段", 25, 8, 1, null, true));
        list.add(m(7, "深蹲魔王", "腿", "杠铃深蹲Boss", 70, 25, 4, eq(Equipment.BARBELL), true));
        list.add(m(8, "卧推巨像", "胸", "杠铃卧推Boss", 70, 25, 4, eq(Equipment.BARBELL), true));
        list.add(m(9, "硬拉暴君", "背", "杠铃硬拉Boss", 90, 30, 5, eq(Equipment.BARBELL), true));
        list.add(m(10, "弹力带精灵", "全身", "弹力带全身训练", 35, 12, 2, eq(Equipment.BAND), true));
        list.add(m(11, "波比幽灵", "全身", "波比跳连击", 60, 20, 3, null, true));
        list.add(m(12, "卷腹小妖", "核心", "仰卧起坐", 20, 6, 1, null, true));
        return list;
    }

    private static List<Equipment> eq(Equipment e) {
        return Arrays.asList(e);
    }

    private static MonsterDef m(long id, String name, String muscle, String desc,
                                int exp, int coins, int diff, List<Equipment> equip, boolean unlocked) {
        MonsterDef d = new MonsterDef();
        d.id = id;
        d.name = name;
        d.muscleGroup = muscle;
        d.desc = desc;
        d.baseExp = exp;
        d.baseCoins = coins;
        d.difficulty = diff;
        d.equipmentNeeded = (equip == null) ? new ArrayList<>() : equip;
        // 有对应像素图就用，否则回退 hero
        Integer res = MUSCLE_ICON_MAP.get(muscle);
        d.iconRes = (res != null) ? res : DEFAULT_ICON;
        d.defaultUnlocked = unlocked;
        return d;
    }
}
