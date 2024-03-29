create database if not exists coop;
create database if not exists local_pi;


-- USE coop;
USE local_pi;

CREATE TABLE IF NOT EXISTS `users` (
    `USER_ID` varchar(32) NOT NULL,
    `USERNAME` varchar(128) NOT NULL,
    `PASSWORD` varchar(128) NOT NULL,
    `ENABLED` tinyint(1) NOT NULL,
    PRIMARY KEY (`USER_ID`),
    UNIQUE KEY `USERNAME` (`USERNAME`)
 );

CREATE TABLE IF NOT EXISTS `authorities` (
	`AUTHORITY_ID` MEDIUMINT NOT NULL AUTO_INCREMENT,
    `USER_ID` varchar(128) NOT NULL,
    `AUTHORITY` varchar(128) NOT NULL,
    PRIMARY KEY (`AUTHORITY_ID`),
    UNIQUE KEY `AUTHORITIES_UNIQUE` (`USER_ID`,`AUTHORITY`),
    CONSTRAINT `AUTHORITIES_FK1` FOREIGN KEY (`USER_ID`) REFERENCES `users` (`USER_ID`)
 );
 
 CREATE TABLE IF NOT EXISTS `pis` (
	`PI_ID` varchar(32) NOT NULL,
    `AWS_IOT_THING_ID` VARCHAR(256) NOT NULL,
    `CLIENT_ID` varchar(32) NOT NULL,
	PRIMARY KEY (`PI_ID`),
    UNIQUE KEY `PIS UNIQUE` (`AWS_IOT_THING_ID`),
    UNIQUE KEY `PIS UNIQUE CLIENT_ID` (`CLIENT_ID`)
 );
 
CREATE TABLE IF NOT EXISTS `coops` (
	`COOP_ID` varchar(32) NOT NULL,
    `USER_ID` varchar(128) NOT NULL,
    `NAME` varchar(128) NOT NULL,
    `PI_ID` varchar(32) NOT NULL,
    PRIMARY KEY (`COOP_ID`),
    UNIQUE KEY `COOPS_UNIQUE_PI` (`PI_ID`),
    CONSTRAINT `COOPS_FK1` FOREIGN KEY (`USER_ID`) REFERENCES `users` (`USER_ID`),
    CONSTRAINT `COOPS_FK2` FOREIGN KEY (`PI_ID`) REFERENCES `pis` (`PI_ID`)
 );
 
  CREATE TABLE IF NOT EXISTS `component_serials` (
	`SERIAL_NUMBER` VARCHAR(32) NOT NULL,
    `TYPE` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`SERIAL_NUMBER`)
 );
 
 CREATE TABLE IF NOT EXISTS `components` (
	`COMPONENT_ID` VARCHAR(32) NOT NULL,
	`SERIAL_NUMBER` VARCHAR(32) NOT NULL,
	`COOP_ID` varchar(32) NOT NULL,
    `NAME` VARCHAR(128) NOT NULL,
    PRIMARY KEY (`COMPONENT_ID`),
    UNIQUE KEY (`SERIAL_NUMBER`),
    CONSTRAINT `COMPONENTS_FK1` FOREIGN KEY (`COOP_ID`) REFERENCES `coops` (`COOP_ID`),
    CONSTRAINT `COMPONENTS_FK2` FOREIGN KEY (`SERIAL_NUMBER`) REFERENCES `component_serials` (`SERIAL_NUMBER`),
    INDEX (`COOP_ID`)
 );
 
 CREATE TABLE IF NOT EXISTS `component_config` (
	`COMPONENT_ID` VARCHAR(32) NOT NULL,
	`CONFIG_KEY` varchar(128) NOT NULL,
    `CONFIG_VALUE` VARCHAR(32) NOT NULL,
    UNIQUE KEY (`COMPONENT_ID`, `CONFIG_KEY`),
    CONSTRAINT `COMPONENT_CONFIG_FK1` FOREIGN KEY (`COMPONENT_ID`) REFERENCES `components` (`COMPONENT_ID`),
    INDEX (`COMPONENT_ID`)
 );
 
CREATE TABLE IF NOT EXISTS `metrics` (
	`DT` bigint NOT NULL,
    `YEAR` int NOT NULL,
    `MONTH` int NOT NULL,
    `WEEK` int NOT NULL,
    `DAY` int NOT NULL,
    `QUARTER_DAY` int NOT NULL,
    `HOUR` int NOT NULL,
	`COOP_ID` varchar(32) NOT NULL,
    `COMPONENT_ID` varchar(32) NOT NULL,
    `METRIC` varchar(32) NOT NULL,
    `VALUE` float NOT NULL,
    PRIMARY KEY (`DT`, `COOP_ID`, `COMPONENT_ID`, `METRIC`),
    CONSTRAINT `METRICS_FK1` FOREIGN KEY (`COOP_ID`) REFERENCES `coops` (`COOP_ID`),
    CONSTRAINT `METRICS_FK2` FOREIGN KEY (`COMPONENT_ID`) REFERENCES `components` (`COMPONENT_ID`),
    INDEX (`COOP_ID`, `YEAR`, `METRIC`),
    INDEX (`COOP_ID`, `MONTH`, `METRIC`),
    INDEX (`COOP_ID`, `WEEK`, `METRIC`),
    INDEX (`COOP_ID`, `DAY`, `METRIC`),
    INDEX (`COOP_ID`, `QUARTER_DAY`, `METRIC`),
    INDEX (`COOP_ID`, `HOUR`, `METRIC`)
 );