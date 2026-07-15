package com.esn.fitdiet.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.esn.fitdiet.R;
import com.esn.fitdiet.SummaryIntegrityChecker;
import com.esn.fitdiet.ui.custom.PixelProgressBar;
import com.esn.fitdiet.ui.diet.FoodRecognitionActivity;
import com.esn.fitdiet.ui.onboarding.OnboardingActivity;

/**
 * 主界面：导航入口 + 经验条展示。
 * onResume 触发 SummaryIntegrityChecker 回填。
 */
public class MainActivity extends AppCompatActivity {

    private PixelProgressBar expBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        expBar = findViewById(R.id.expBar);
        expBar.setMax(100);
        expBar.setProgress(25);
        expBar.setSegmentCount(8);
        expBar.setFillColor(getColor(R.color.pixel_green));

        Button btnOnboarding = findViewById(R.id.btnOnboarding);
        btnOnboarding.setOnClickListener(v ->
                startActivity(new Intent(this, OnboardingActivity.class)));

        Button btnFood = findViewById(R.id.btnFoodRecognition);
        btnFood.setOnClickListener(v ->
                startActivity(new Intent(this, FoodRecognitionActivity.class)));

        // 占位：Boss 挑战按钮暂时无效，后续接入 BattleActivity
        Button btnBattle = findViewById(R.id.btnBattle);
        btnBattle.setOnClickListener(v -> {
            // TODO: BattleActivity
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SummaryIntegrityChecker.check(this);
    }
}
