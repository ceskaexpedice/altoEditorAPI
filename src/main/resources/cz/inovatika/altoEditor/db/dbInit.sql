CREATE SEQUENCE version_id_seq INCREMENT BY 1 START WITH 1 MINVALUE 0;
CREATE TABLE version (id SERIAL NOT NULL, datum TIMESTAMP NOT NULL, version BIGINT NOT NULL, PRIMARY KEY (id));

CREATE SEQUENCE users_id_seq INCREMENT BY 1 START WITH 1 MINVALUE 0;
CREATE TABLE users(id SERIAL NOT NULL, login VARCHAR(255) NOT NULL, PRIMARY KEY (id));
CREATE UNIQUE INDEX users_user_index on users (login);

CREATE SEQUENCE digitalobject_id_seq INCREMENT BY 1 START WITH 1 MINVALUE 0;
CREATE TABLE digitalobject(id SERIAL NOT NULL, rUserId INT NOT NULL, instance VARCHAR(255) NOT NULL, pid VARCHAR(255) NOT NULL, version VARCHAR(255) NOT NULL, datum TIMESTAMP NOT NULL, state VARCHAR(20) NOT NULL, PRIMARY KEY (id));
CREATE UNIQUE INDEX digitalobject_userPid_index ON digitalobject (rUserId, pid);
CREATE UNIQUE INDEX digitalobject_pidVersion_index ON digitalobject (pid, versionXml);

ALTER TABLE digitalobject ADD CONSTRAINT digitalobject_user_fk FOREIGN KEY (rUserId) REFERENCES users (ID);

INSERT INTO version (id, datum, version) VALUES (NEXTVAL('version_id_seq'), NOW(), '1');
