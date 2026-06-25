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
-- Type: Prepared, QuerySize: 1, Batch: False

select
    next value
for
    child_entity_seq;


-- Name: dataSource, Isolation: NONE, Success: True
-- Type: Prepared, QuerySize: 1, Batch: False

select
    next value
for
    child_entity_seq;


-- Name: dataSource, Isolation: NONE, Success: True
-- Type: Prepared, QuerySize: 1, Batch: False

select
    next value
for
    child_entity_seq;


-- Name: dataSource, Isolation: NONE, Success: True
-- Type: Prepared, QuerySize: 1, Batch: False

select
    next value
for
    child_entity_seq;


-- Name: dataSource, Isolation: NONE, Success: True
-- Type: Prepared, QuerySize: 1, Batch: True, BatchSize: 5

insert
into
    child_entity
    (name, parent_id, id)
values
    (?, ?, ?);
-- Params: (Child 1, 1, 1)
--         (Child 2, 1, 2)
--         (Child 3, 1, 3)
--         (Child 4, 1, 4)
--         (Child 5, 1, 5)
