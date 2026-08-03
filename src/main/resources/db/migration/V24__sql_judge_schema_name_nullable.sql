-- SQL Judge: schema_name is derived at provision time (q_<id>), so it must be
-- nullable at insert time. SqlProblemProvisioningService.ensureSchemaName()
-- assigns the final value before provisioning runs.
ALTER TABLE sql_problems ALTER COLUMN schema_name DROP NOT NULL;
