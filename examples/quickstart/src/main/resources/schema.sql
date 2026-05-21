-- 用户表（DROP + CREATE：每次启动重建，演示可重复运行）
DROP TABLE IF EXISTS qs_user;
CREATE TABLE qs_user (
    id      BIGINT      PRIMARY KEY,
    name    VARCHAR(64) NOT NULL,
    age     INT,
    salary  INT,
    dept_id BIGINT,
    deleted TINYINT     DEFAULT 0
);

-- 部门表（用于演示 JOIN 查询）
DROP TABLE IF EXISTS qs_dept;
CREATE TABLE qs_dept (
    id   BIGINT      PRIMARY KEY,
    name VARCHAR(64) NOT NULL
);
