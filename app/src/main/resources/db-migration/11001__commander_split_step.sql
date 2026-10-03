-- Records the one-time steps of the commander split that leave no schema change behind to test for.
--
-- Moving a column out (material_names.amount, hunting_ground.forgotten, the exo-mastery flags) is its own marker,
-- because the column is dropped once its data has moved. The location flags live inside each row's JSON, so
-- moving them changes no schema, and without a record the step would scan every location row on every start.

CREATE TABLE IF NOT EXISTS commander_split_step
(
    step
    TEXT
    PRIMARY
    KEY,
    doneAt
    TEXT
    NOT
    NULL
    DEFAULT (
    datetime
(
    'now'
))
    );
