-- Name:dataSource, Isolation:NONE, Success:True
-- Type:Prepared, Batch:False, QuerySize:1, BatchSize:0
-- Params:[()]
    insert 
    into
        sample_entity
        (id) 
    values
        (default);


-- Name:dataSource, Isolation:NONE, Success:True
-- Type:Prepared, Batch:False, QuerySize:1, BatchSize:0
-- Params:[(Some Child,1)]
    insert 
    into
        child_entity
        (name, parent_id, id) 
    values
        (?, ?, default);
