package coop.local.scheduler;

import coop.local.database.job.Job;
import coop.local.state.LocalStateProvider;
import coop.shared.pi.config.ComponentState;
import coop.shared.pi.config.CoopState;
import coop.shared.pi.config.GlobalResourceState;
import coop.shared.pi.config.GroupResourceState;
import lombok.Data;

import java.time.Duration;
import java.util.*;

/**
 * This classes responsibility is to manage the resources defined in the state object. It maintains a list of who is
 * consuming what.
 *
 * NOTE: Consumers that don't complete within a set number of hours will be automatically expired.
 */
public class ResourceManager {

    private static final String DELIMITER = "::";

    //TODO: Make this configurable?
    private static final Duration EXPIRY = Duration.ofHours(6);

    private final Map<String, Resource> resources = new HashMap<>();
    private final LocalStateProvider stateProvider;

    public ResourceManager(LocalStateProvider stateProvider) {
        this.stateProvider = stateProvider;
    }

    private Resource getOrCreate(GroupResourceState r) {
        String key = r.getGroupId() + DELIMITER + r.getDeviceType().name();
        Resource resource = resources.getOrDefault(key, new Resource());
        resource.setConcurrency(r.getConcurrency());
        resources.put(key, resource);
        return resource;
    }

    private Resource getOrCreate(GlobalResourceState r) {
        String key = "GLOBAL" + DELIMITER + r.getDeviceType().name();
        Resource resource = resources.getOrDefault(key, new Resource());
        resource.setConcurrency(r.getConcurrency());
        resources.put(key, resource);
        return resource;
    }

    private Resource getOrCreate(ComponentState component) {
        String key = "COMPONENT" + DELIMITER + component.getComponentId();
        Resource resource = resources.getOrDefault(key, new Resource());
        resource.setConcurrency(1);
        resources.put(key, resource);
        return resource;
    }

    private List<Resource> getGroupResources(ComponentState component) {

        CoopState coop = stateProvider.getConfig();
        if(coop == null || coop.getGroupResources() == null) {
            return Collections.emptyList();
        }

        return coop.getGroupResources()
                .stream()
                .filter(r ->
                        r.getGroupId().equals(component.getGroupId()) &&
                        r.getDeviceType().equals(component.getDeviceType()))
                .map(this::getOrCreate)
                .toList();
    }

    private List<Resource> getGlobalResources(ComponentState component) {

        CoopState coop = stateProvider.getConfig();
        if(coop == null || coop.getGlobalResources() == null) {
            return Collections.emptyList();
        }

        return coop.getGlobalResources()
                .stream()
                .filter(r -> r.getDeviceType().equals(component.getDeviceType()))
                .map(this::getOrCreate)
                .toList();
    }

    private List<Resource> getResources(ComponentState component) {
        List<Resource> resources = new ArrayList<>();
        resources.add(getOrCreate(component));
        resources.addAll(getGlobalResources(component));
        resources.addAll(getGroupResources(component));
        return resources;
    }

    private boolean hasCapacity(ComponentState component) {
        return getResources(component).stream().allMatch(Resource::hasCapacity);
    }

    private void consume(ComponentState component) {
        getResources(component).forEach(c -> c.addConsumer(component.getComponentId()));
    }

    public synchronized boolean tryToConsume(Job job) {

        ComponentState component = componentForJob(job);
        if (component == null) return false;

        if(hasCapacity(component)){
            consume(component);
            return true;
        }

        return false;
    }

    synchronized void forceConsumption(Job job) {
        ComponentState component = componentForJob(job);
        if (component != null) consume(component);
    }

    synchronized public void stopConsuming(Job job) {
        ComponentState component = componentForJob(job);
        if(component != null) {
            getResources(component).forEach(c -> c.removeConsumer(component.getComponentId()));
        }
    }

    private ComponentState componentForJob(Job job) {
        CoopState state = stateProvider.getConfig();
        if(state != null) {
            return state.getComponents()
                    .stream()
                    .filter( c -> c.getComponentId().equals(job.getComponentId()))
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    @Data
    private static class Resource {
        private int concurrency;
        private Map<String, Consumer> consumers = new HashMap<>();

        private boolean hasCapacity() {
            purgeExpiredEntries();
            return consumers.size() < concurrency;
        }

        private void purgeExpiredEntries() {
            consumers.entrySet().removeIf(e -> e.getValue().isExpired());
        }

        private void addConsumer(String componentId) {
            purgeExpiredEntries();
            consumers.put(componentId, new Consumer(componentId));
        }

        private void removeConsumer(String componentId) {
            consumers.remove(componentId);
        }
    }

    @Data
    private static class Consumer {
        private String componentId;
        private long consumedAt;

        private Consumer(String componentId) {
            this.componentId = componentId;
            this.consumedAt = System.currentTimeMillis();
        }

        private boolean isExpired() {
            long now = System.currentTimeMillis();
            return now - consumedAt > EXPIRY.toMillis();
        }
    }

}
