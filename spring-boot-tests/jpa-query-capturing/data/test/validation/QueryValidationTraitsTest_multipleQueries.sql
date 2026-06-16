-- Name: dataSource, Isolation: NONE, Success: True
-- Type: Statement, QuerySize: 2, Batch: True, BatchSize: 2

insert
into
    sample_entity
    (id)
values
    (1);

insert
into
    child_entity
    (name, parent_id, id)
values
    ('Child', 1, 1);
