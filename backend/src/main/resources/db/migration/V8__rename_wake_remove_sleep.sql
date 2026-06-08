UPDATE node_commands SET command = 'REQUEST_READINGS' WHERE command = 'WAKE';
DELETE FROM node_commands WHERE command = 'SLEEP';
