package cc.infrai.examples.fieldservice.domain;

import java.time.Instant;
import java.util.List;

public record WorkOrder(
        String id,
        String customerName,
        String serviceAddress,
        DispatchStatus dispatchStatus,
        List<WorkPhoto> photos,
        TechnicianFollowUp followUp) {

    public WorkOrder {
        photos = List.copyOf(photos);
    }

    public enum DispatchStatus { SCHEDULED, EN_ROUTE, ON_SITE, COMPLETED, CANCELLED }

    public record WorkPhoto(String caption, String objectReference, Instant capturedAt) {}

    public record TechnicianFollowUp(String technicianName, String summary, Instant completedAt) {}
}
