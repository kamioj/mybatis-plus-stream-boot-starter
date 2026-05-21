-- 部门种子数据
INSERT INTO qs_dept (id, name) VALUES (1, '工程部'), (2, '产品部'), (3, '运营部');

-- 用户种子数据
INSERT INTO qs_user (id, name, age, salary, dept_id) VALUES
    (1, 'Alice', 30, 25000, 1),
    (2, 'Bob',   25, 18000, 1),
    (3, 'Carol', 28, 22000, 2),
    (4, 'Dave',  35, 30000, 2),
    (5, 'Eve',   22, 15000, 3);
