CREATE TABLE IF NOT EXISTS `component_port_config` (
	`COMPONENT_ID` VARCHAR(32) NOT NULL,
	`PORT_INDEX` INT NOT NULL,
	`CONFIG_KEY` VARCHAR(128) NOT NULL,
	`CONFIG_VALUE` VARCHAR(32) NOT NULL,
	PRIMARY KEY (`COMPONENT_ID`, `PORT_INDEX`, `CONFIG_KEY`),
	CONSTRAINT `COMPONENT_PORT_CONFIG_FK1` FOREIGN KEY (`COMPONENT_ID`) REFERENCES `components` (`COMPONENT_ID`),
	INDEX (`COMPONENT_ID`)
);

-- Backfill: default_duration/manual_cutoff used to be component-level. Copy whatever an existing valve
-- component already had configured down into all 8 ports, so existing setups don't lose their values
-- now that these keys are per-port.
INSERT INTO component_port_config (COMPONENT_ID, PORT_INDEX, CONFIG_KEY, CONFIG_VALUE)
SELECT c.COMPONENT_ID, t.idx, cc.CONFIG_KEY, cc.CONFIG_VALUE
FROM components c
JOIN component_serials cs ON cs.SERIAL_NUMBER = c.SERIAL_NUMBER
JOIN component_config cc ON cc.COMPONENT_ID = c.COMPONENT_ID AND cc.CONFIG_KEY IN ('default_duration', 'manual_cutoff') AND cc.CONFIG_VALUE <> ''
JOIN (SELECT 0 AS idx UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7) t
WHERE cs.TYPE = 'VALVE';
