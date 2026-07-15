package com.esn.fitdiet.data.local;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;

import com.esn.fitdiet.data.local.entity.DailySummary;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Room 升级迁移测试（TC-X-04）：v1 → v2。
 *
 * <p>本环境为纯单元测试（无 Instrumentation / 模拟器），故不依赖
 * {@code MigrationTestHelper}，改为：以 v1 导出 schema 的 createSql 手工建表 → 写入一行
 * （不含 weightKg）→ 执行 {@link AppDatabase#MIGRATION_1_2} → 用
 * {@code PRAGMA table_info} 校验「新增 weightKg 列」且「既有行数据完整」。
 *
 * <p>v1 / v2 schema 从 {@code databases/.../1.json} 与 {@code 2.json}（与 app/schemas 同步复制）
 * 读取，避免硬编码、随 schema 演进自洽。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU)
public class AppDatabaseMigrationTest {

    private static final String DATE = "2026-07-10";

    @Test
    public void migrate1To2_preservesDataAndAddsWeightKg() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();

        // 1) 读取 v1 schema 的 daily_summary createSql
        String createSql = readCreateSql("databases/com.esn.fitdiet.data.local.AppDatabase/1.json",
                "daily_summary");
        assertNotNull("v1 createSql", createSql);
        // Room 的 createSql 含 ${TABLE_NAME} 占位符，需替换为真实表名
        createSql = createSql.replace("${TABLE_NAME}", "daily_summary");

        // 2) 以 v1 结构建库并写入一行（v1 无 weightKg 列）
        SupportSQLiteOpenHelper.Configuration config = SupportSQLiteOpenHelper.Configuration.builder(ctx)
                .name("mig-test")
                .callback(new SupportSQLiteOpenHelper.Callback(1) {
                    @Override
                    public void onCreate(SupportSQLiteDatabase db) {
                        // 由测试手动执行 v1 createSql，保持与旧版行为一致
                    }

                    @Override
                    public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                        // no-op
                    }
                })
                .build();
        SupportSQLiteOpenHelper helper = new FrameworkSQLiteOpenHelperFactory().create(config);
        SupportSQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL(createSql);
        db.execSQL("INSERT INTO daily_summary " +
                "(date, intakeCalories, intakeProtein, intakeCarbs, intakeFat, " +
                "bmrCalories, exerciseCalories, dailyActivityCalories, totalBurned, " +
                "netCalories, updatedAt) VALUES " +
                "('" + DATE + "', 800, 40, 100, 30, 1600, 200, 300, 2100, -1300, 1000)");

        // 3) 执行迁移
        AppDatabase.MIGRATION_1_2.migrate(db);

        // 4) 校验迁移后列结构（含新增 weightKg）
        Set<String> columns = tableColumns(db, "daily_summary");
        assertTrue("原列保留", columns.containsAll(java.util.Arrays.asList(
                "date", "intakeCalories", "netCalories", "updatedAt")));
        assertTrue("新增 weightKg 列", columns.contains("weightKg"));

        // 5) 既有行数据完整 + weightKg 为 NULL
        Cursor r = db.query("SELECT date, intakeCalories, netCalories, weightKg " +
                "FROM daily_summary WHERE date='" + DATE + "'");
        assertTrue(r.moveToFirst());
        assertEquals(DATE, r.getString(r.getColumnIndexOrThrow("date")));
        assertEquals(800, r.getDouble(r.getColumnIndexOrThrow("intakeCalories")), 0.001);
        assertEquals(-1300, r.getDouble(r.getColumnIndexOrThrow("netCalories")), 0.001);
        int wIdx = r.getColumnIndexOrThrow("weightKg");
        assertTrue("weightKg 列存在", wIdx >= 0);
        assertTrue("迁移后旧行 weightKg 为 NULL", r.isNull(wIdx));
        r.close();
        db.close();
    }

    /** 读 vX schema JSON，抽取指定表的 createSql。 */
    private String readCreateSql(String resourcePath, String tableName) {
        ClassLoader cl = getClass().getClassLoader();
        try (InputStream in = cl.getResourceAsStream(resourcePath)) {
            assertNotNull("schema 资源存在: " + resourcePath, in);
            JsonObject root = new Gson().fromJson(new InputStreamReader(in), JsonObject.class);
            JsonArray entities = root.getAsJsonObject("database").getAsJsonArray("entities");
            for (int i = 0; i < entities.size(); i++) {
                JsonObject e = entities.get(i).getAsJsonObject();
                if (tableName.equals(e.get("tableName").getAsString())) {
                    return e.get("createSql").getAsString();
                }
            }
        } catch (Exception e) {
            throw new AssertionError("读取 schema 失败: " + resourcePath, e);
        }
        return null;
    }

    /** 取表所有列名。 */
    private Set<String> tableColumns(SupportSQLiteDatabase db, String table) {
        Set<String> cols = new HashSet<>();
        Cursor c = db.query("PRAGMA table_info(" + table + ")");
        int nameIdx = c.getColumnIndexOrThrow("name");
        while (c.moveToNext()) {
            cols.add(c.getString(nameIdx));
        }
        c.close();
        return cols;
    }
}
