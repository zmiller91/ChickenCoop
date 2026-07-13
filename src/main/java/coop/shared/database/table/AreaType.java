package coop.shared.database.table;

/**
 * What kind of physical place an Area represents - purely a tag for now (no behavior attached, unlike
 * DeviceType/Device), intended for the frontend to key dashboard/workflow UI off of later. Adding a new kind
 * of place is just a new value here, no schema change.
 */
public enum AreaType {
    GARDEN,
    CHICKEN_COOP,
    OTHER
}
