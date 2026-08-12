alter table player
    add column localLlmAddress text default 'http://localhost:1234/v1/chat/completions';
alter table game_session
    add column localLlmCommandModel text default '';
alter table game_session
    add column localLlmQueryModel text default '';