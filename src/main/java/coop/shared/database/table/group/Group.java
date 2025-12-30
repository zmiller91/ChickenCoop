package coop.shared.database.table.group;

import lombok.Data;

import java.util.List;

@Data
public class Group {

    private String id;
    private String name;
    private List<GroupResource> resources;

}
