-- DELETE FROM users;

LOAD DATA INFILE '/var/lib/mysql-files/users.csv'
REPLACE
INTO TABLE users
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(@user_id)
SET
    id = CAST(TRIM(TRAILING '\r' FROM @user_id) AS UNSIGNED),
    name = CONCAT(
        'load-test-user-',
        TRIM(TRAILING '\r' FROM @user_id)
    ),
    created_at = NOW();