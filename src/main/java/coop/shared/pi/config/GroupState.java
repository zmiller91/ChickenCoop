package coop.shared.pi.config;

import coop.shared.database.table.group.GroupResource;
import lombok.Data;

import java.util.List;

@Data
public class GroupState {

    private String id;
    private List<GroupResource> resources;
}
