
-- non-transactional
PRAGMA foreign_keys = ON;

-- comment 1
-- comment 2
create table test_migrations (
  id              integer primary key, -- inline comment
  original_name   text
);

insert into test_migrations (original_name) values ('test 1');
