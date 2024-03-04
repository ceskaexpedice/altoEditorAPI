CREATE SEQUENCE version_id_seq INCREMENT BY 1 START WITH 1 MINVALUE 0;
CREATE TABLE version (id SERIAL NOT NULL, datum TIMESTAMP NOT NULL, version BIGINT NOT NULL, PRIMARY KEY (id));

CREATE SEQUENCE users_id_seq INCREMENT BY 1 START WITH 1 MINVALUE 0;
CREATE TABLE users(id SERIAL NOT NULL, login VARCHAR(255) NOT NULL, PRIMARY KEY (id));
CREATE UNIQUE INDEX users_user_index on users (login);

CREATE SEQUENCE digitalobject_id_seq INCREMENT BY 1 START WITH 1 MINVALUE 0;
CREATE TABLE digitalobject(id SERIAL NOT NULL, rUserId INT NOT NULL, instance VARCHAR(255) NOT NULL, pid VARCHAR(255) NOT NULL, version VARCHAR(255) NOT NULL, datum TIMESTAMP NOT NULL, state VARCHAR(20) NOT NULL, PRIMARY KEY (id));
CREATE UNIQUE INDEX digitalobject_userPid_index ON digitalobject (rUserId, pid);
CREATE UNIQUE INDEX digitalobject_pidVersion_index ON digitalobject (pid, version);

ALTER TABLE digitalobject ADD CONSTRAINT digitalobject_user_fk FOREIGN KEY (rUserId) REFERENCES users (ID);

INSERT INTO version (id, datum, version) VALUES (NEXTVAL('version_id_seq'), NOW(), '1');

-- verze db 2
CREATE SEQUENCE batch_id_seq INCREMENT BY 1 START WITH 1 MINVALUE 0;
CREATE TABLE batch (id SERIAL NOT NULL, pid VARCHAR(255) NOT NULL, createDate TIMESTAMP NOT NULL, updateDate TIMESTAMP NOT NULL, state VARCHAR(20) NOT NULL, substate VARCHAR(20), priority VARCHAR(255) NOT NULL, instance VARCHAR(255) NOT NULL, type VARCHAR(255), estimateItemNumber INT, log VARCHAR(255), objectId INT, PRIMARY KEY (id));
INSERT INTO version (id, datum, version) VALUES (NEXTVAL('version_id_seq'), NOW(), '2');

-- verze db 3
ALTER TABLE digitalobject ADD COLUMN label VARCHAR(1024);
ALTER TABLE digitalobject ADD COLUMN parentPid VARCHAR(255);
ALTER TABLE digitalobject ADD COLUMN parentLabel VARCHAR(255);
INSERT INTO version (id, datum, version) VALUES (NEXTVAL('version_id_seq'), NOW(), '3');