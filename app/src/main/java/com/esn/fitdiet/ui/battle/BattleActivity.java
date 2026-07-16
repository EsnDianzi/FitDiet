package com.esn.fitdiet.ui.battle;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.esn.fitdiet.MainApplication;
import com.esn.fitdiet.R;
import com.esn.fitdiet.data.local.entity.LevelProgress;
import com.esn.fitdiet.data.local.entity.MonsterDef;
import com.esn.fitdiet.data.repository.AppContainer;
import com.esn.fitdiet.data.repository.BattleRepository;
import com.esn.fitdiet.databinding.ActivityBattleBinding;
import com.esn.fitdiet.game.BattleManager;
import com.esn.fitdiet.game.GameBalance;
import com.esn.fitdiet.util.AppExecutors;
import com.esn.fitdiet.util.DateUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 训练/Boss 战页面（重写版 v3）。
 *
 * <p>交互流程：
 * <ol>
 *   <li>选择预设组合 或 自选肌群（可多选 / toggle）</li>
 *   <li>点击"开始训练"进入战斗</li>
 *   <li>第 1 个肌群 Boss 出现（血条 = 动作数）</li>
 *   <li>完成一组实际动作后点"完成一组" → 血条 -1 → 自动进入组间休息倒计时（60s）</li>
 *   <li>休息结束或跳过 → 可继续下一组</li>
 *   <li>血条归零 → Boss 击杀 → 下一肌群（自动进入动作间休息 90s）</li>
 *   <li>所有肌群完成 → 结算弹窗 → 持久化</li>
 * </ol>
 */
public class BattleActivity extends AppCompatActivity {

    private ActivityBattleBinding binding;
    private BattleRepository battleRepo;

    /** 战斗会话状态机 */
    private BattleManager.Session session;
    /** 已选择的肌群计划 */
    private final List<BattleManager.GroupPlan> groupPlans = new ArrayList<>();
    /** 回蓝倒计时 */
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable restCountdownRunnable;
    private int restSecondsRemaining;

    private static final String[] MUSCLE_GROUPS = {"胸", "背", "腿", "肩", "臂", "核心"};

    // ── 预设组合模板（PRD 4.2.2） ──
    private static final String[][] PRESET_COMBOS = {
            {"胸", "臂"},           // 胸 + 二头
            {"背", "臂"},           // 背 + 三头（简化：臂统一）
            {"腿"},                 // 臀腿
            {"肩", "核心"},         // 肩 + 核心
            {"胸", "背", "腿", "肩", "臂", "核心"}  // 全身
    };

    private Button[] presetButtons;
    private Button[] muscleButtons;
    private int selectedPresetIndex = -1; // -1 表示未选择预设

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBattleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MainApplication app = (MainApplication) getApplication();
        AppContainer di = app.getContainer();
        battleRepo = di.getBattleRepository();

        // ── 预设组合按钮 ──
        setupPresetButtons();

        // ── 自定义肌群按钮 ──
        setupMuscleButtons();

        // ── 开始训练 ──
        binding.btnStartBattle.setOnClickListener(v -> startBattle());

        // ── 战斗按钮 ──
        binding.btnCompleteRound.setOnClickListener(v -> onCompleteRound());
        binding.btnSkipRest.setOnClickListener(v -> onSkipRest());
        binding.btnEndSession.setOnClickListener(v -> confirmEndSession());
        binding.btnBack.setOnClickListener(v -> finish());

        // 绑定底部导航栏
        com.esn.fitdiet.ui.common.BottomNavHelper.bind(this, findViewById(R.id.navBar));

        // 初始状态：无选择
        updateSelectionUI();
    }

    /** 绑定 5 个预设组合按钮 */
    private void setupPresetButtons() {
        presetButtons = new Button[]{
                binding.btnPresetChestBi,
                binding.btnPresetBackTri,
                binding.btnPresetLegs,
                binding.btnPresetShoulderCore,
                binding.btnPresetFullBody
        };
        for (int i = 0; i < presetButtons.length; i++) {
            final int idx = i;
            presetButtons[i].setOnClickListener(v -> selectPreset(idx));
        }
    }

    /** 选择预设组合（覆盖当前自定义选择） */
    private void selectPreset(int presetIndex) {
        selectedPresetIndex = presetIndex;
        groupPlans.clear();
        String[] muscles = PRESET_COMBOS[presetIndex];
        for (String mg : muscles) {
            groupPlans.add(createGroupPlan(mg));
        }
        updateSelectionUI();
        Toast.makeText(this, "已选择：" + presetButtons[presetIndex].getText(), Toast.LENGTH_SHORT).show();
    }

    /** 肌群选择按钮（toggle 多选） */
    private void setupMuscleButtons() {
        muscleButtons = new Button[]{
                binding.btnMuscle1, binding.btnMuscle2, binding.btnMuscle3,
                binding.btnMuscle4, binding.btnMuscle5, binding.btnMuscle6
        };
        for (int i = 0; i < muscleButtons.length; i++) {
            final String mg = MUSCLE_GROUPS[i];
            muscleButtons[i].setOnClickListener(v -> toggleCustomMuscle(mg));
        }
    }

    /** 自选肌群 toggle：在列表中存在则移除，否则追加 */
    private void toggleCustomMuscle(String muscleGroup) {
        // 一旦自定义选择，就取消预设高亮
        selectedPresetIndex = -1;

        int existingIdx = findGroupIndex(muscleGroup);
        if (existingIdx >= 0) {
            groupPlans.remove(existingIdx);
        } else {
            groupPlans.add(createGroupPlan(muscleGroup));
        }
        updateSelectionUI();
    }

    private int findGroupIndex(String muscleGroup) {
        for (int i = 0; i < groupPlans.size(); i++) {
            if (muscleGroup.equals(groupPlans.get(i).monster.muscleGroup)) {
                return i;
            }
        }
        return -1;
    }

    private BattleManager.GroupPlan createGroupPlan(String muscleGroup) {
        int rounds = defaultRoundsForMuscle(muscleGroup);
        MonsterDef m = new MonsterDef();
        m.name = muscleGroup + "部 Boss";
        m.muscleGroup = muscleGroup;
        m.difficulty = roundsToDifficulty(rounds);
        m.iconRes = getIconForMuscle(muscleGroup);
        return new BattleManager.GroupPlan(m, rounds);
    }

    /** 肌群 → 默认动作数（PRD: 大肌群 4-5, 中 3-4, 小 2-3） */
    private int defaultRoundsForMuscle(String muscle) {
        switch (muscle) {
            case "胸": case "背": case "腿": return 4;
            case "肩": return 3;
            case "臂": case "核心": return 3;
            default: return 3;
        }
    }

    private int roundsToDifficulty(int rounds) {
        if (rounds <= 2) return 1;
        if (rounds <= 3) return 2;
        if (rounds <= 4) return 3;
        if (rounds <= 6) return 4;
        return 5;
    }

    /** 更新选择区 UI：摘要文字 + 按钮高亮 + 开始按钮状态 */
    private void updateSelectionUI() {
        // 摘要
        if (groupPlans.isEmpty()) {
            binding.tvSelectedMuscles.setText("请选择训练肌群");
            binding.tvSelectedMuscles.setTextColor(getColor(R.color.apple_text_tertiary));
        } else {
            StringBuilder sb = new StringBuilder("已选: ");
            for (int i = 0; i < groupPlans.size(); i++) {
                if (i > 0) sb.append(" → ");
                sb.append(groupPlans.get(i).monster.muscleGroup)
                        .append("(").append(groupPlans.get(i).rounds).append("组)");
            }
            binding.tvSelectedMuscles.setText(sb.toString());
            binding.tvSelectedMuscles.setTextColor(getColor(R.color.apple_accent));
        }

        // 预设按钮高亮
        for (int i = 0; i < presetButtons.length; i++) {
            boolean active = (i == selectedPresetIndex);
            presetButtons[i].setBackgroundResource(active
                    ? R.drawable.bg_battle_preset_selected
                    : R.drawable.bg_battle_preset);
            presetButtons[i].setTextColor(active ? getColor(R.color.apple_accent) : Color.WHITE);
        }

        // 自定义肌群按钮高亮
        for (int i = 0; i < muscleButtons.length; i++) {
            boolean active = findGroupIndex(MUSCLE_GROUPS[i]) >= 0;
            muscleButtons[i].setBackgroundResource(active
                    ? R.drawable.bg_battle_muscle_selected
                    : R.drawable.bg_battle_muscle);
            muscleButtons[i].setTextColor(active ? Color.BLACK : Color.WHITE);
        }

        // 开始按钮状态
        boolean hasSelection = !groupPlans.isEmpty();
        binding.btnStartBattle.setEnabled(hasSelection);
        binding.btnStartBattle.setAlpha(hasSelection ? 1.0f : 0.45f);
    }

    /** 开始战斗 */
    private void startBattle() {
        if (groupPlans.isEmpty()) {
            Toast.makeText(this, "请先选择肌群", Toast.LENGTH_SHORT).show();
            return;
        }

        session = new BattleManager.Session(groupPlans, GameBalance.DEFAULT);
        session.selectCombo(); // IDLE → COMBO_SELECTED
        session.startAction(); // → ACTION_ACTIVE

        // 隐藏选择区，显示战斗区
        binding.tvPageTitle.setText("训练中");
        binding.comboPresets.setVisibility(View.GONE);
        binding.tvComboLabel.setVisibility(View.GONE);
        binding.tvCustomLabel.setVisibility(View.GONE);
        binding.customMuscleRow1.setVisibility(View.GONE);
        binding.customMuscleRow2.setVisibility(View.GONE);
        binding.tvSelectedMuscles.setVisibility(View.GONE);
        binding.btnStartBattle.setVisibility(View.GONE);
        binding.battleArea.setVisibility(View.VISIBLE);

        updateBattleUI();
    }

    /** 用户点"完成一组" */
    private void onCompleteRound() {
        if (session == null) return;

        try {
            // 如果当前在 RESTING 状态，先 restComplete
            if (session.getState() == BattleManager.State.RESTING) {
                session.restComplete();
            }
            // 在 ROUND_ACTIVE 或 ACTION_ACTIVE 状态下开始/完成 round
            if (session.getState() == BattleManager.State.ACTION_ACTIVE) {
                session.startRound();
            }
            if (session.getState() == BattleManager.State.ROUND_ACTIVE) {
                session.completeRound();
            }
        } catch (IllegalStateException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        updateBattleUI();

        // 检查状态
        if (session.getState() == BattleManager.State.RESTING) {
            startRestTimer();
        } else if (session.getState() == BattleManager.State.SESSION_DONE) {
            onSessionDone();
        } else if (session.getState() == BattleManager.State.BOSS_DOWN) {
            // Boss 被击败，自动进入下一肌群
            onBossDefeated();
        }
    }

    /** Boss 被击败 → 动作间休息 90s → 进入下一肌群 */
    private void onBossDefeated() {
        updateBattleUI();
        binding.tvRestLabel.setText("动作间休息");
        restSecondsRemaining = GameBalance.REST_ACTION_DEFAULT_S;
        binding.btnCompleteRound.setEnabled(false);
        binding.btnSkipRest.setVisibility(View.VISIBLE);
        startRestCountdown(() -> {
            // 休息结束 → 开始下一肌群
            try {
                session.startAction();
            } catch (IllegalStateException ignored) { }
            updateBattleUI();
            binding.btnCompleteRound.setEnabled(true);
            binding.btnSkipRest.setVisibility(View.GONE);
            binding.tvRestTimer.setVisibility(View.GONE);
            binding.tvRestLabel.setVisibility(View.GONE);
        });
    }

    /** 启动组间休息倒计时（60s） */
    private void startRestTimer() {
        binding.tvRestLabel.setText("组间休息");
        restSecondsRemaining = GameBalance.REST_ROUND_DEFAULT_S;
        binding.btnCompleteRound.setEnabled(false);
        binding.btnSkipRest.setVisibility(View.VISIBLE);
        startRestCountdown(() -> {
            // 休息结束 → 可继续下一组
            binding.btnCompleteRound.setEnabled(true);
            binding.btnSkipRest.setVisibility(View.GONE);
            binding.tvRestTimer.setVisibility(View.GONE);
            binding.tvRestLabel.setVisibility(View.GONE);
            binding.tvRound.setText(String.format(Locale.getDefault(),
                    "第 %d/%d 组 — %s",
                    session.getRoundInAction() + 1,
                    groupPlans.get(session.getActionIndex()).rounds,
                    groupPlans.get(session.getActionIndex()).monster.muscleGroup));
        });
    }

    /** 倒计时核心逻辑 */
    private void startRestCountdown(Runnable onFinish) {
        if (restCountdownRunnable != null) handler.removeCallbacks(restCountdownRunnable);

        binding.tvRestTimer.setVisibility(View.VISIBLE);
        binding.tvRestLabel.setVisibility(View.VISIBLE);
        binding.tvRestTimer.setText(String.format(Locale.getDefault(), "%d", restSecondsRemaining));

        restCountdownRunnable = new Runnable() {
            @Override
            public void run() {
                restSecondsRemaining--;
                if (restSecondsRemaining <= 0) {
                    binding.tvRestTimer.setVisibility(View.GONE);
                    binding.tvRestLabel.setVisibility(View.GONE);
                    onFinish.run();
                } else {
                    binding.tvRestTimer.setText(String.format(Locale.getDefault(), "%d", restSecondsRemaining));
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.postDelayed(restCountdownRunnable, 1000);
    }

    /** 跳过休息 */
    private void onSkipRest() {
        if (restCountdownRunnable != null) handler.removeCallbacks(restCountdownRunnable);
        binding.tvRestTimer.setVisibility(View.GONE);
        binding.tvRestLabel.setVisibility(View.GONE);
        binding.btnCompleteRound.setEnabled(true);
        binding.btnSkipRest.setVisibility(View.GONE);

        // 如果在 Boss 被击败后的休息中 → 进入下一肌群
        if (session != null && session.getState() == BattleManager.State.BOSS_DOWN) {
            try {
                session.startAction();
            } catch (IllegalStateException ignored) { }
        }
        updateBattleUI();
    }

    /** 更新战斗 UI */
    private void updateBattleUI() {
        if (session == null) return;

        int actionIdx = session.getActionIndex();
        int blood = session.getBloodRemaining();
        BattleManager.State state = session.getState();

        if (actionIdx < groupPlans.size()) {
            BattleManager.GroupPlan gp = groupPlans.get(actionIdx);
            binding.tvBossName.setText(gp.monster.name);
            binding.tvBossInfo.setText(String.format(Locale.getDefault(),
                    "%s · 难度 Lv.%d · %d/%d 组",
                    gp.monster.muscleGroup, gp.monster.difficulty,
                    session.getRoundInAction(), gp.rounds));
            binding.ivBossSprite.setImageResource(
                    gp.monster.iconRes != 0 ? gp.monster.iconRes : R.drawable.ic_pixel_hero);
            binding.bossHpBar.setMax(gp.rounds);
            binding.bossHpBar.setProgress(blood);
            binding.tvRound.setText(String.format(Locale.getDefault(),
                    "第 %d/%d 组 — %s",
                    session.getRoundInAction() + 1, gp.rounds, gp.monster.muscleGroup));
        }

        if (state == BattleManager.State.SESSION_DONE) {
            binding.tvRound.setText("全部完成！");
        }
    }

    /** 确认提前结束训练 */
    private void confirmEndSession() {
        new AlertDialog.Builder(this)
                .setTitle("提前结束训练")
                .setMessage("已完成的部分仍会计入进度，确定结束吗？")
                .setPositiveButton("确定", (d, w) -> onSessionDone())
                .setNegativeButton("继续训练", null)
                .show();
    }

    /** 训练全部结束：结算并持久化 */
    private void onSessionDone() {
        binding.btnCompleteRound.setEnabled(false);
        binding.btnSkipRest.setVisibility(View.GONE);
        if (restCountdownRunnable != null) handler.removeCallbacks(restCountdownRunnable);
        binding.tvRestTimer.setVisibility(View.GONE);
        binding.tvRestLabel.setVisibility(View.GONE);
        binding.tvRound.setText("训练完成！");

        AppExecutors.getInstance().diskIO().execute(() -> {
            LevelProgress before = battleRepo.getProgress();
            int oldLevel = (before != null) ? before.level : 1;
            int oldStreak = (before != null) ? before.streakDays : 0;

            // 同步执行：completeComboSync 内部完成所有写入后才返回
            // 这样紧跟着 getProgress() 能读到最新值
            battleRepo.completeComboSync(groupPlans, DateUtil.today());

            LevelProgress after = battleRepo.getProgress();
            int newLevel = (after != null) ? after.level : oldLevel;
            int newStreak = (after != null) ? after.streakDays : oldStreak;

            // 用 Repository 内部已计算好的奖励（不再重复算）
            int totalExp = (after != null && before != null) ? after.totalExp - before.totalExp : 0;
            int totalCoins = (after != null && before != null) ? after.coins - before.coins : 0;

            final int finalExp = totalExp, finalCoins = totalCoins;
            final int finalNewLevel = newLevel, finalOldLevel = oldLevel, finalStreak = newStreak;
            AppExecutors.getInstance().mainThread().execute(() -> showRewardDialog(
                    finalExp, finalCoins, finalStreak, finalOldLevel, finalNewLevel));
        });
    }

    /** 自定义奖励弹窗（卡片式，比纯文本更美观） */
    private void showRewardDialog(int exp, int coins, int streak, int oldLevel, int newLevel) {
        View view = getLayoutInflater().inflate(R.layout.dialog_battle_result, null);
        ((TextView) view.findViewById(R.id.tvRewardExp)).setText("+" + exp);
        ((TextView) view.findViewById(R.id.tvRewardCoins)).setText("+" + coins);
        ((TextView) view.findViewById(R.id.tvRewardStreak)).setText(streak + " 天");

        View levelUpContainer = view.findViewById(R.id.levelUpContainer);
        if (newLevel > oldLevel) {
            levelUpContainer.setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.tvRewardLevel))
                    .setText("Lv." + oldLevel + " → Lv." + newLevel);
        } else {
            levelUpContainer.setVisibility(View.GONE);
        }

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("返回首页", (d, w) -> {
                    Intent intent = new Intent(this, com.esn.fitdiet.ui.home.HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    /** 肌群 → 精灵图 */
    private int getIconForMuscle(String muscle) {
        switch (muscle) {
            case "胸": return R.drawable.ic_pixel_boss_chest;
            case "背": return R.drawable.ic_pixel_boss_back;
            case "腿": return R.drawable.ic_pixel_boss_legs;
            case "肩": return R.drawable.ic_pixel_boss_shoulder;
            case "臂": return R.drawable.ic_pixel_boss_arm;
            case "核心": return R.drawable.ic_pixel_boss_core;
            default: return R.drawable.ic_pixel_boss_fullbody;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (restCountdownRunnable != null) {
            handler.removeCallbacks(restCountdownRunnable);
        }
        binding = null;
    }
}
