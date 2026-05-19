package com.baomidou.mybatisplus.extension.dialect;

import com.baomidou.mybatisplus.extension.dialect.impl.DamengDialect;
import com.baomidou.mybatisplus.extension.dialect.impl.MySqlDialect;
import com.baomidou.mybatisplus.extension.dialect.impl.PostgreSqlDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * BACKTICK token 适配器单元测试。保护 4.1 引入的 translator 行为不回归。
 */
class DialectQuoteTranslatorTest {

    @AfterEach
    void restore() {
        DialectRegistry.use(new MySqlDialect());
    }

    @Test
    void mysql_is_noop() {
        DialectRegistry.use(new MySqlDialect());
        String sql = "SELECT `id`, `name` FROM `user`";
        assertEquals(sql, DialectQuoteTranslator.translate(sql));
    }

    @Test
    void postgres_translates_to_double_quotes() {
        DialectRegistry.use(new PostgreSqlDialect());
        assertEquals(
            "SELECT \"id\", \"name\" FROM \"user\"",
            DialectQuoteTranslator.translate("SELECT `id`, `name` FROM `user`")
        );
    }

    @Test
    void dameng_translates_to_double_quotes() {
        DialectRegistry.use(new DamengDialect());
        assertEquals(
            "SELECT \"id\" FROM \"user\" WHERE \"status\" = 1",
            DialectQuoteTranslator.translate("SELECT `id` FROM `user` WHERE `status` = 1")
        );
    }

    @Test
    void preserves_string_literals_pg() {
        DialectRegistry.use(new PostgreSqlDialect());
        // 字符串字面值里的反引号不应被翻译
        String input = "SELECT `id` FROM `user` WHERE `note` = 'has `backtick` inside'";
        String expected = "SELECT \"id\" FROM \"user\" WHERE \"note\" = 'has `backtick` inside'";
        assertEquals(expected, DialectQuoteTranslator.translate(input));
    }

    @Test
    void handles_sql_single_quote_escape() {
        DialectRegistry.use(new PostgreSqlDialect());
        // SQL 标准 '' 转义代表一个单引号；字面值内反引号仍不翻译
        String input = "WHERE `note` = 'it''s a `test`'";
        String expected = "WHERE \"note\" = 'it''s a `test`'";
        assertEquals(expected, DialectQuoteTranslator.translate(input));
    }

    @Test
    void preserves_table_alias_pattern() {
        DialectRegistry.use(new PostgreSqlDialect());
        assertEquals(
            "FROM \"user\" AS \"u\" LEFT JOIN \"order\" AS \"o\" ON \"u\".\"id\" = \"o\".\"user_id\"",
            DialectQuoteTranslator.translate(
                "FROM `user` AS `u` LEFT JOIN `order` AS `o` ON `u`.`id` = `o`.`user_id`")
        );
    }

    @Test
    void empty_input_returns_unchanged() {
        DialectRegistry.use(new PostgreSqlDialect());
        assertEquals("", DialectQuoteTranslator.translate(""));
        assertNull(DialectQuoteTranslator.translate(null));
        assertEquals("plain sql no backticks", DialectQuoteTranslator.translate("plain sql no backticks"));
    }

    @Test
    void unpaired_backtick_preserved() {
        DialectRegistry.use(new PostgreSqlDialect());
        // 不成对的反引号原样保留（防御性输出，不破坏 SQL）
        String input = "SELECT `id FROM users";  // 不成对
        assertEquals(input, DialectQuoteTranslator.translate(input));
    }

    @Test
    void custom_dialect_uses_its_quoteIdentifier() {
        // 适配器模式验证：任何自定义方言只要实现 quoteIdentifier 就能 work
        SqlDialect oracle = new MySqlDialect() {
            @Override public DbType dbType() { return DbType.CUSTOM; }
            @Override public String quoteIdentifier(String name) {
                return "[" + name + "]";  // 假装 SQL Server 风格
            }
        };
        DialectRegistry.use(oracle);
        assertEquals(
            "SELECT [id] FROM [user]",
            DialectQuoteTranslator.translate("SELECT `id` FROM `user`")
        );
    }

    @Test
    void identifier_with_quote_inside_is_escaped_pg() {
        DialectRegistry.use(new PostgreSqlDialect());
        // PG quoteIdentifier 把双引号转义为两个双引号
        assertEquals(
            "SELECT \"weird\"\"name\"",
            DialectQuoteTranslator.translate("SELECT `weird\"name`")
        );
    }
}
