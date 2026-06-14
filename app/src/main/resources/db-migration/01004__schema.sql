alter table game_session
    add column pushToTalkEnabled boolean default false;

alter table game_session
    add column pushToTalkControllerName VARCHAR(256);

alter table game_session
    add column pushToTalkButtonIndex integer default -1;

alter table game_session
    add column pushToTalkToggleMode boolean default true;
