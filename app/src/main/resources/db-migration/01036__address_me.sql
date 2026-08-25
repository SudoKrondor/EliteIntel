-- Whether the commander wants to be addressed at all.
--
-- On by default, and set on for everyone here: being addressed by name, rank or honorific is the
-- behaviour every commander has today. Switching it off silences the address itself, not the sentence
-- around it.
alter table player
    add column is_address_me_on boolean not null default 1;

update player
set is_address_me_on = 1;
