-- Name: dataSource, Isolation: NONE, Success: True
-- Type: Prepared, QuerySize: 1, Batch: False

insert
into
    sample_entity
    (id)
values
    (default);


-- Name: dataSource, Isolation: NONE, Success: True
-- Type: Prepared, QuerySize: 1, Batch: False

select
    next value
for
    child_entity_seq;


-- Name: dataSource, Isolation: NONE, Success: True
-- Type: Prepared, QuerySize: 1, Batch: True, BatchSize: 1

insert
into
    child_entity
    (name, parent_id, id)
values
    (?, ?, ?);
-- Params: (Some Child, 1, 1)
